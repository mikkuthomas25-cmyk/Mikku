"""Brand agents: price, durability and recyclability respond to EPR fees.

Four market segments (ultra-fast, fast, mid, premium), each with several brand
agents. Consumer segments choose among brands with a multinomial logit on log
price and (for conscious consumers) durability. Brands decide each year whether
to upgrade durability or recyclability when the fee saving outweighs the unit
cost increase (bounded-rational logit choice).
"""
from __future__ import annotations

import numpy as np

UF, FAST, MID, PREMIUM = 0, 1, 2, 3
SEGMENTS = ("uf", "fast", "mid", "premium")
BASE_SHARE_NON_UF = np.array([0.45, 0.30, 0.10])  # fast, mid, premium among non-UF (assumption)


def law_penalty_range(year: int) -> tuple[float, float]:
    """Per-item penalty band of loi n° 2026-602 (EUR), in force 1 Sep 2026."""
    if year < 2026:
        return 0.0, 0.0
    if year == 2026:
        return 0.5 / 3, 12.0 / 3  # four months of the year
    if year < 2030:
        return 0.5, 12.0
    return 2.0, 19.5


class Brands:
    def __init__(self, p, rng: np.random.Generator):
        self.p = p
        self.rng = rng
        m = p.n_brands_per_segment
        self.seg = np.repeat(np.arange(4), m)
        nb = len(self.seg)
        base_price = np.array([p.price_uf, p.price_fast, p.price_mid, p.price_premium])
        base_dur = np.array([p.dur_uf, p.dur_fast, p.dur_mid, p.dur_premium])
        base_rec = np.array([p.rec_uf, p.rec_fast, p.rec_mid, p.rec_premium])
        self.price0 = base_price[self.seg] * rng.normal(1, 0.1, nb).clip(0.7, 1.3)
        self.dur = (base_dur[self.seg] + rng.normal(0, 0.03, nb)).clip(0.3, 1.0)
        self.rec = (base_rec[self.seg] + rng.normal(0, 0.05, nb)).clip(0.05, 1.0)
        self.cost_up = np.zeros(nb)  # cumulative unit-cost increase from upgrades (EUR/item)
        self.fee = np.full(nb, p.base_fee)
        self.lam = p.logit_price
        self.delta = np.array([0.0, 0.0, p.logit_dur_consc])  # durability weight by consumer segment
        mean_delta = p.seg_share_consc * p.logit_dur_consc
        seg_target = np.concatenate([[p.uf_share_2024], (1 - p.uf_share_2024) * BASE_SHARE_NON_UF / BASE_SHARE_NON_UF.sum()])
        seg_price = np.array([self.price0[self.seg == s].mean() for s in range(4)])
        seg_dur = np.array([self.dur[self.seg == s].mean() for s in range(4)])
        asc_seg = np.log(seg_target) + self.lam * np.log(seg_price) - mean_delta * seg_dur
        self.asc = asc_seg[self.seg] - np.log(m)
        self.uf_shift = 0.0
        self.volume = np.zeros(nb)

    # ----------------------------------------------------------------
    def fees(self, year: int, sc: dict) -> np.ndarray:
        p = self.p
        f = np.full(len(self.seg), p.base_fee)
        if year >= sc.get("dur_bonus_start", 2023):
            f -= np.where(self.dur >= 0.8, p.dur_bonus * sc.get("bonus_mult", 1.0), 0.0)
        if year >= 2025:
            f += np.where(self.rec < 0.5, p.rec_malus * sc.get("malus_mult", 1.0), 0.0)
        if sc.get("ff_law", False):
            lo, hi = law_penalty_range(year)
            frac = np.clip((0.8 - self.dur) / 0.8, 0, 1)
            pen = np.where(self.dur < 0.8, lo + (hi - lo) * frac, 0.0) * sc.get("law_mult", 1.0)
            cap = 0.5 * self.price0 / 1.2 if sc.get("law_cap", True) else np.inf  # 50% of pre-tax price
            f += np.minimum(pen, cap)
        return f

    def prices(self) -> np.ndarray:
        return self.price0 + self.cost_up + self.p.pass_through * self.fee

    def shares(self, price=None) -> np.ndarray:
        """Brand choice probabilities per consumer segment (3 x n_brands)."""
        price = self.prices() if price is None else price
        u = self.asc + np.where(self.seg == UF, self.uf_shift, 0.0) - self.lam * np.log(price)
        u = u[None, :] + self.delta[:, None] * self.dur[None, :]
        u -= u.max(axis=1, keepdims=True)
        e = np.exp(u)
        return e / e.sum(axis=1, keepdims=True)

    def market(self, demand_by_cseg: np.ndarray) -> dict:
        s = self.shares()
        vol = demand_by_cseg @ s  # items by brand
        tot = vol.sum()
        w = vol / tot if tot > 0 else np.full(len(vol), 1 / len(vol))
        self.volume = vol
        price = self.prices()
        return {
            "price_index": float(w @ price),
            "dur_new": float(w @ self.dur),
            "rec_new": float(w @ self.rec),
            "uf_share": float(w[self.seg == UF].sum()),
            "fee_mean": float(w @ self.fee),
            "seg_mix": np.array([w[self.seg == k].sum() for k in range(4)]),
        }

    def force_uf_share(self, target: float, demand_by_cseg: np.ndarray):
        """Historical mode: shift the ultra-fast ASC so its volume share matches target."""
        lo, hi = -10.0, 10.0
        for _ in range(40):
            self.uf_shift = 0.5 * (lo + hi)
            vol = demand_by_cseg @ self.shares()
            share = vol[self.seg == UF].sum() / vol.sum()
            if share < target:
                lo = self.uf_shift
            else:
                hi = self.uf_shift

    def decide_upgrades(self, year: int, sc: dict):
        """Bounded-rational upgrade of durability and recyclability."""
        p = self.p
        step = p.upgrade_step
        cur = self.fees(year, sc)
        # durability upgrade
        saved_dur = self.dur.copy()
        self.dur = np.minimum(self.dur + step, 1.0)
        saving = cur - self.fees(year, sc)
        self.dur = saved_dur
        cost = p.upgrade_cost_frac * self.price0
        prob = 1 / (1 + np.exp(-p.upgrade_logit * (saving - cost) / cost))
        prob = np.where(self.dur >= 1.0, 0.0, prob)
        up = self.rng.random(len(prob)) < prob
        self.dur = np.where(up, np.minimum(self.dur + step, 1.0), self.dur)
        self.cost_up += np.where(up, cost, 0.0)
        # recyclability upgrade (cheaper: material choice)
        saved_rec = self.rec.copy()
        self.rec = np.minimum(self.rec + 2 * step, 1.0)
        saving = cur - self.fees(year, sc)
        self.rec = saved_rec
        cost_r = 0.5 * p.upgrade_cost_frac * self.price0
        prob = 1 / (1 + np.exp(-p.upgrade_logit * (saving - cost_r) / cost_r))
        prob = np.where(self.rec >= 1.0, 0.0, prob)
        up = self.rng.random(len(prob)) < prob
        self.rec = np.where(up, np.minimum(self.rec + 2 * step, 1.0), self.rec)
        self.cost_up += np.where(up, cost_r, 0.0)
        return int(up.sum())
