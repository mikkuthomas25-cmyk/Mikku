"""Item-level stochastic simulation of ML-assisted sorting for fibre-to-fibre textile recycling.

One replication simulates a sorting line over a horizon (hours). Each garment is an
entity with a mass and a true fibre composition. An NIR+ML sensor model returns a noisy
composition estimate (noise level = the quantification RMSEP reported for NIR blend
models) and a confidence score. A selective-classification policy either auto-routes
the item or sends it to a finite-capacity manual re-sort station (discrete-event,
multi-server, finite buffer). Output lines are baled and each bale is accepted or
rejected by the downstream recycler against a purity specification.

Parameter defaults and their sources are listed in docs/parameters.md; every value
without an empirical source is labelled ASSUMPTION there and is varied in experiments.
"""
from __future__ import annotations

import heapq
from dataclasses import dataclass, replace

import numpy as np

# Fibre axes of the composition vector.
COTTON, PES, ELASTANE, OTHER = 0, 1, 2, 3
# Sort classes / output lines.
CLASSES = ("COT", "PES", "PC", "OTH")
C_COT, C_PES, C_PC, C_OTH = 0, 1, 2, 3
RESIDUAL = 4  # destination code for the residual / downcycling stream
BALED = (C_COT, C_PES, C_PC)


@dataclass(frozen=True)
class Params:
    # --- stream composition (shares of items) ---
    share_cotton_mono: float = 0.42      # L5: cotton 42%
    share_pc_blend: float = 0.12         # L5: polycotton 12%
    share_other_blend: float = 0.20      # L5: blends 32% in total
    share_pes_mono: float = 0.16         # ASSUMPTION (remaining 26% split PES / other mono)
    share_other_mono: float = 0.10       # ASSUMPTION
    pc_cotton_fracs: tuple = (0.90, 0.80, 0.60, 0.50, 0.35)   # ASSUMPTION
    pc_cotton_probs: tuple = (0.15, 0.15, 0.25, 0.20, 0.25)   # ASSUMPTION
    p_elastane_mono: float = 0.20        # R3: ~1 in 5 single-material items
    elastane_cotton_range: tuple = (0.01, 0.05)  # R3
    elastane_pes_range: tuple = (0.01, 0.10)     # R3 (up to 20% reported; capped)
    mass_mean_kg: float = 0.35           # ASSUMPTION
    mass_cv: float = 0.6                 # ASSUMPTION
    # --- line ---
    throughput_kg_h: float = 1000.0      # M8: Fibersort ~0.9-1.2 t/h
    horizon_h: float = 40.0              # one week, single shift (ASSUMPTION)
    # --- sensor / ML model ---
    sigma: float = 0.035                 # composition RMSE; Q1: 0.01 (benchtop) .. 0.066 (sorter)
    p_dark: float = 0.05                 # ASSUMPTION: carbon-black / dark items (Q2)
    p_discharge: float = 0.02            # ASSUMPTION: mechanical ejection error (pathway: S7)
    novel_share: float = 0.0             # share of items from a shifted population (drift)
    novel_kappa: float = 1.0             # noise multiplier for novel items
    n_conf_draws: int = 64               # resamples used for the confidence score
    tau: float = 0.0                     # confidence threshold for auto-routing
    audit_share: float = 0.0             # random share sent to manual for labelling (drift study)
    # delegation policy: "fixed" (A), "capacity" (B) or "quality" (C); see delegate_online()
    policy: str = "fixed"
    conf_map: tuple = ()                 # optional calibration map ((x...), (y...)) applied to confidence
    budget_safety: float = 0.5           # policy C: share of (1 - spec) allowed for sensor-visible contamination
    # --- digital product passport ---
    dpp_share: float = 0.0
    dpp_error: float = 0.02              # ASSUMPTION
    # --- manual re-sort station ---
    manual_servers: int = 2              # ASSUMPTION
    manual_buffer: int = 20              # ASSUMPTION
    manual_service_s: float = 20.0       # ASSUMPTION
    manual_service_cv: float = 0.5       # ASSUMPTION
    manual_accuracy: float = 0.85        # ASSUMPTION informed by L6/L7 label error rates
    # --- baling and acceptance ---
    bale_kg: float = 200.0               # ASSUMPTION (observed 50-200+ kg)
    spec: tuple = (0.95, 0.90, 0.95)     # COT (R1 range 0.95-0.98), PES (R2 0.80-0.90), PC (ASSUMPTION)
    qc_sd: float = 0.005                 # ASSUMPTION


def stream_shares(p: Params) -> np.ndarray:
    s = np.array([p.share_cotton_mono, p.share_pes_mono, p.share_pc_blend,
                  p.share_other_blend, p.share_other_mono])
    return s / s.sum()


def generate_stream(p: Params, rng: np.random.Generator) -> dict:
    """Garments arriving within the horizon, in arrival order."""
    target_mass = p.throughput_kg_h * p.horizon_h
    sig = np.sqrt(np.log1p(p.mass_cv**2))
    mu = np.log(p.mass_mean_kg) - sig**2 / 2
    n = int(target_mass / p.mass_mean_kg * 1.2) + 100
    mass = rng.lognormal(mu, sig, n)
    cum = np.cumsum(mass)
    n = int(np.searchsorted(cum, target_mass)) + 1
    mass, cum = mass[:n], cum[:n]

    cat = rng.choice(5, size=n, p=stream_shares(p))
    comp = np.zeros((n, 4))
    m = cat == 0
    e = np.where(rng.random(n) < p.p_elastane_mono, rng.uniform(*p.elastane_cotton_range, n), 0.0)
    comp[m, COTTON], comp[m, ELASTANE] = 1 - e[m], e[m]
    m = cat == 1
    e = np.where(rng.random(n) < p.p_elastane_mono, rng.uniform(*p.elastane_pes_range, n), 0.0)
    comp[m, PES], comp[m, ELASTANE] = 1 - e[m], e[m]
    m = cat == 2
    f = rng.choice(p.pc_cotton_fracs, size=n, p=p.pc_cotton_probs)
    comp[m, COTTON], comp[m, PES] = f[m], 1 - f[m]
    m = cat == 3
    comp[m] = rng.dirichlet([1.0, 1.0, 0.3, 1.5], size=int(m.sum()))
    comp[cat == 4, OTHER] = 1.0

    return dict(
        mass=mass,
        arrival_h=(cum - mass) / p.throughput_kg_h,
        comp=comp,
        true=true_class(comp),
        dark=rng.random(n) < p.p_dark,
        novel=rng.random(n) < p.novel_share,
        has_dpp=rng.random(n) < p.dpp_share,
    )


def true_class(comp: np.ndarray) -> np.ndarray:
    """Composition rule that defines the correct sort class (works on any trailing-4 array)."""
    cot, pes = comp[..., COTTON], comp[..., PES]
    out = np.full(cot.shape, C_OTH)
    out[(cot + pes) >= 0.95] = C_PC
    out[pes >= 0.95] = C_PES
    out[cot >= 0.95] = C_COT
    return out


def _rule2(cot: np.ndarray, pes: np.ndarray) -> np.ndarray:
    """true_class() on the two axes the rule depends on (cotton, PES)."""
    out = np.full(cot.shape, C_OTH)
    out[(cot + pes) >= 0.95] = C_PC
    out[pes >= 0.95] = C_PES
    out[cot >= 0.95] = C_COT
    return out


def sense(comp: np.ndarray, sigma_item: np.ndarray, n_draws: int, rng: np.random.Generator):
    """NIR+ML sensor: noisy composition estimate, predicted class and confidence.

    The regression model estimates the cotton and PES mass fractions with unbiased
    Gaussian error whose SD is the item's RMSEP (the quantity NIR studies report).
    The class is the composition rule applied to the estimate. The confidence is the
    share of `n_draws` resamples around the estimate (same error model) that fall in
    the predicted class: a measurement-uncertainty-aware plug-in confidence.
    """
    s = sigma_item[:, None]
    est = comp[:, :2] + rng.standard_normal((len(comp), 2)) * s
    pred = _rule2(est[:, 0], est[:, 1])
    conf = np.empty(len(comp))
    efor = np.empty(len(comp))
    chunk = 50_000
    for a in range(0, len(comp), chunk):
        b = min(a + chunk, len(comp))
        draws = est[a:b, None, :] + rng.standard_normal((b - a, n_draws, 2)) * s[a:b, None, :]
        cls = _rule2(draws[..., 0], draws[..., 1])
        conf[a:b] = (cls == pred[a:b, None]).mean(axis=1)
        # estimated non-target mass fraction if the item joins its predicted line
        # (unclipped point estimate, so that sums over a bale are unbiased)
        e, pk = est[a:b], pred[a:b]
        tgt = np.where(pk == C_COT, e[:, 0], np.where(pk == C_PES, e[:, 1], e[:, 0] + e[:, 1]))
        efor[a:b] = np.where(pk == C_OTH, 0.0, 1 - tgt)
    return est, pred, conf, efor


def nearest_wrong_class(comp: np.ndarray, true: np.ndarray) -> np.ndarray:
    """Most plausible wrong class for an item (used for manual and DPP errors)."""
    proto = np.array([[1.0, 0, 0, 0], [0, 1.0, 0, 0], [0.6, 0.4, 0, 0], [0, 0, 0, 1.0]])
    dist = np.linalg.norm(comp[:, None, :] - proto[None, :, :], axis=2)
    dist[np.arange(len(comp)), true] = np.inf
    return dist.argmin(axis=1)


def manual_station(arrival_h: np.ndarray, p: Params, rng: np.random.Generator):
    """FCFS multi-server queue with finite waiting room (G/G/c/c+K), arrivals in order.

    Returns (served mask, total busy time in hours).
    """
    n = len(arrival_h)
    served = np.zeros(n, bool)
    if n == 0 or p.manual_servers == 0:
        return served, 0.0
    sig = np.sqrt(np.log1p(p.manual_service_cv**2))
    mu = np.log(p.manual_service_s / 3600) - sig**2 / 2
    svc = rng.lognormal(mu, sig, n)
    return _queue(arrival_h, svc, p.manual_servers, p.manual_buffer, served, p.horizon_h)


def _queue(arrival_h, svc, c, K, served, horizon=np.inf):
    free_at = [0.0] * c          # min-heap of server release times
    waiting: list[float] = []    # min-heap of start times of queued (not yet started) items
    busy = 0.0
    for i in range(len(arrival_h)):
        t = arrival_h[i]
        while waiting and waiting[0] <= t:
            heapq.heappop(waiting)
        start = max(t, free_at[0])
        if start > t and len(waiting) >= K:
            continue             # waiting room full -> overflow to residual
        heapq.heapreplace(free_at, start + svc[i])
        if start > t:
            heapq.heappush(waiting, start)
        served[i] = True
        busy += max(0.0, min(start + svc[i], horizon) - min(start, horizon))
    return served, busy


def bale_lines(dest, mass, comp, p: Params, rng):
    """Cut each output line into bales (arrival order) and apply recycler acceptance.

    Returns per-line dicts and, per item, whether its bale was accepted.
    The trailing incomplete bale of each line is carried over and excluded.
    """
    item_accepted = np.zeros(len(mass), bool)
    res = {}
    for k in BALED:
        idx = np.flatnonzero(dest == k)
        empty = dict(n_bales=0, accepted_kg=0.0, baled_kg=0.0, purity=np.array([]), acc=np.array([], bool),
                     scale=1.0)
        if len(idx) == 0:
            res[CLASSES[k]] = empty
            continue
        m = mass[idx]
        frac = comp[idx, COTTON] + comp[idx, PES] if k == C_PC else comp[idx, k]
        bid = (np.cumsum(m) // p.bale_kg).astype(int)
        nb = int(bid.max())
        if nb == 0:
            res[CLASSES[k]] = empty
            continue
        keep = bid < nb
        bm = np.bincount(bid[keep], m[keep], minlength=nb)
        bt = np.bincount(bid[keep], (m * frac)[keep], minlength=nb)
        purity = bt / bm
        acc = purity + rng.normal(0, p.qc_sd, nb) >= p.spec[k]
        item_accepted[idx[keep]] = acc[bid[keep]]
        # The trailing partial bale is carried over to the next period. Its items are
        # neither lost nor accepted yet; flow KPIs are scaled by line mass / baled mass,
        # i.e. the carried-over mass is credited at this line's observed acceptance rate.
        scale = float(m.sum() / bm.sum())
        res[CLASSES[k]] = dict(n_bales=nb, accepted_kg=float(bm[acc].sum()) * scale, baled_kg=float(bm.sum()),
                               purity=purity, acc=acc, scale=scale)
    return res, item_accepted


def delegate_online(cand, arrival_h, mass, pred, conf, efor, p: Params, rng):
    """Online delegation for policies B (capacity-aware) and C (downstream-quality-aware).

    Decisions use only information available on the line at the item's arrival: the
    sensor estimate and confidence, the manual queue state, and (policy C) the line's
    running estimate of the open bale's contamination. Returns per-item codes:
    0 = auto-route to predicted line, 1 = manual station, 2 = divert to residual.
    """
    n = len(mass)
    code = np.zeros(n, np.int8)
    sig = np.sqrt(np.log1p(p.manual_service_cv**2))
    mu = np.log(p.manual_service_s / 3600) - sig**2 / 2
    svc = rng.lognormal(mu, sig, n)
    c, K = p.manual_servers, p.manual_buffer
    free_at = [0.0] * max(c, 1)
    waiting: list[float] = []
    busy = 0.0
    a_m = p.manual_accuracy
    # policy C state per baled line: open-bale mass and estimated foreign mass
    open_m = np.zeros(3)
    open_f = np.zeros(3)
    budget = np.array([(1 - s) * p.bale_kg * p.budget_safety for s in p.spec])

    for i in np.flatnonzero(cand):
        t = arrival_h[i]
        while waiting and waiting[0] <= t:
            heapq.heappop(waiting)
        start = max(t, free_at[0]) if c else np.inf
        room = c > 0 and (start <= t or len(waiting) < K)
        k = pred[i]
        m = mass[i]
        if p.policy == "capacity" or k == C_OTH:
            # delegate only when the manual sorter is more likely right than the sensor
            want = conf[i] < min(p.tau, a_m)
            threat = False
        else:  # "quality"
            # per-kg contamination allowance left in the open bale; an item threatens the
            # bale if its estimated foreign fraction exceeds that allowance
            allow = (budget[k] - open_f[k]) / max(p.bale_kg - open_m[k], m)
            threat = efor[i] > allow
            want = threat
        if want and room:
            code[i] = 1
            heapq.heapreplace(free_at, start + svc[i])
            if start > t:
                heapq.heappush(waiting, start)
            busy += max(0.0, min(start + svc[i], p.horizon_h) - min(start, p.horizon_h))
        elif threat:
            code[i] = 2  # would break the bale's contamination budget and cannot be checked
        elif k != C_OTH:
            # auto-routed: charge the item's estimated foreign mass to the open bale
            # (manual outcomes are unknown at decision time and are not tracked)
            open_m[k] += m
            open_f[k] += m * efor[i]
            if open_m[k] >= p.bale_kg:
                open_m[k] -= p.bale_kg
                open_f[k] = open_f[k] * open_m[k] / (open_m[k] + p.bale_kg)
    return code, busy, svc


def run(p: Params, seed: int, return_items: bool = False) -> dict:
    """One replication; returns scalar KPIs (and optionally item-level arrays)."""
    rng = np.random.default_rng(seed)
    s = generate_stream(p, rng)
    n = len(s["mass"])
    comp, true, mass = s["comp"], s["true"], s["mass"]

    sigma_item = np.full(n, p.sigma)
    sigma_item[s["novel"]] *= p.novel_kappa
    _, pred, conf, efor = sense(comp, sigma_item, p.n_conf_draws, rng)
    if p.conf_map:
        conf = np.interp(conf, *p.conf_map)

    dest = np.full(n, RESIDUAL)
    dpp = s["has_dpp"]
    dark = s["dark"]
    audit = rng.random(n) < p.audit_share
    dpp_cls = np.where(rng.random(n) < p.dpp_error, nearest_wrong_class(comp, true), true)
    mis_draw = rng.random(n) < p.p_discharge
    shift = rng.integers(1, 4, n)

    if p.policy == "fixed":
        auto = (conf >= p.tau) & ~audit
        dest[auto] = pred[auto]
        # carbon-black / dark items give no usable spectrum: detected by low signal and
        # sent to the residual (dark) fraction, as reported for NIR textile sorters (Q2)
        auto = auto | dark | dpp
        dest[dark] = RESIDUAL
        dest[dpp] = dpp_cls[dpp]
        unsure = np.flatnonzero(~auto)
        served, busy = manual_station(s["arrival_h"][unsure], p, rng)
        srv = unsure[served]
    else:
        cand = ~dark & ~dpp
        code, busy, _ = delegate_online(cand, s["arrival_h"], mass, pred, conf, efor, p, rng)
        to_manual = (code == 1) | (audit & cand)
        dest[cand & (code == 0)] = pred[cand & (code == 0)]
        dest[dark] = RESIDUAL
        dest[dpp] = dpp_cls[dpp]
        auto = ~to_manual
        unsure = np.flatnonzero(to_manual)
        srv = unsure  # admission was decided online; audit items bypass the capacity check
        served = np.ones(len(unsure), bool)

    # mechanical discharge error on machine-routed items: lands in a random other bin
    mis = auto & mis_draw
    bins = np.array([C_COT, C_PES, C_PC, RESIDUAL])
    pos = np.searchsorted(bins, np.where(dest == C_OTH, RESIDUAL, dest))
    dest[mis] = bins[(pos[mis] + shift[mis]) % 4]

    wrong = rng.random(len(srv)) > p.manual_accuracy
    dest[srv] = np.where(wrong, nearest_wrong_class(comp[srv], true[srv]), true[srv])

    dest[dest == C_OTH] = RESIDUAL
    bales, item_acc = bale_lines(dest, mass, comp, p, rng)

    total = mass.sum()
    cot_items = true == C_COT
    cotton_avail = (mass * comp[:, COTTON])[cot_items].sum()
    cotton_recovered = (mass * comp[:, COTTON])[(dest == C_COT) & item_acc].sum() * bales["COT"]["scale"]
    sensed = ~dpp
    out = dict(
        n_items=n,
        input_kg=float(total),
        item_accuracy=float((pred[sensed] == true[sensed]).mean()) if sensed.any() else np.nan,
        auto_accuracy=float((pred[auto & sensed] == true[auto & sensed]).mean()) if (auto & sensed).any() else np.nan,
        coverage=float(auto.mean()),
        manual_share=float(len(unsure) / n),
        manual_overflow=float((~served).sum() / len(unsure)) if len(unsure) else 0.0,
        manual_util=float(busy / (p.horizon_h * max(p.manual_servers, 1))),
        cotton_recovery=float(cotton_recovered / cotton_avail) if cotton_avail else np.nan,
        residual_share=float(mass[dest == RESIDUAL].sum() / total),
        novel_labelled=int(s["novel"][srv].sum()),
        manual_items=int(len(srv)),
        protective_divert=float(mass[code == 2].sum() / total) if p.policy == "quality" else 0.0,
    )
    acc_total = 0.0
    for k in BALED:
        b = bales[CLASSES[k]]
        name = CLASSES[k]
        out[f"{name}_bales"] = b["n_bales"]
        out[f"{name}_reject"] = float(1 - b["acc"].mean()) if b["n_bales"] else np.nan
        out[f"{name}_purity"] = float(b["purity"].mean()) if b["n_bales"] else np.nan
        out[f"{name}_accepted_share"] = b["accepted_kg"] / total
        acc_total += b["accepted_kg"]
    out["f2f_share"] = (bales["COT"]["accepted_kg"] + bales["PES"]["accepted_kg"]) / total
    out["closed_loop_share"] = acc_total / total
    if return_items:
        out["_items"] = dict(s, pred=pred, conf=conf, dest=dest, item_acc=item_acc, manual_idx=srv)
        out["_bale_purity"] = {k: v["purity"] for k, v in bales.items()}
    return out


def with_(p: Params, **kw) -> Params:
    return replace(p, **kw)


def fit_calibration(p: Params, seed: int = 99, bins: int = 16) -> tuple:
    """Monotone (isotonic-style) map from confidence score to observed accuracy.

    Fitted on a separate calibration replication; returned as ((x...), (y...)) for
    Params.conf_map. Monotonicity is enforced with a running maximum.
    """
    q = replace(p, conf_map=(), policy="fixed", tau=0.0)
    it = run(q, seed, return_items=True)["_items"]
    m = ~it["dark"] & ~it["has_dpp"]
    conf, ok = it["conf"][m], (it["pred"] == it["true"])[m]
    edges = np.quantile(conf, np.linspace(0, 1, bins + 1))
    idx = np.clip(np.searchsorted(edges, conf, side="right") - 1, 0, bins - 1)
    xs = np.array([conf[idx == b].mean() for b in range(bins) if (idx == b).any()])
    ys = np.maximum.accumulate([ok[idx == b].mean() for b in range(bins) if (idx == b).any()])
    xs, keep = np.unique(xs, return_index=True)
    return (tuple(np.r_[0.0, xs, 1.0]), tuple(np.r_[0.0, np.asarray(ys)[keep], 1.0]))

