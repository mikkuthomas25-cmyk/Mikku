"""Experiment drivers. Usage: python sim/experiments.py [E1 E2 ...] [--reps N]

Each experiment writes results/<name>.csv with one row per replication.
Common random numbers: replication r uses seed BASE_SEED + r in every configuration.
"""
from __future__ import annotations

import argparse
import itertools
import os
import sys
import time
from multiprocessing import Pool
from pathlib import Path

import numpy as np
import pandas as pd

sys.path.insert(0, str(Path(__file__).resolve().parent))
from model import Params, fit_calibration, run, with_  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "results"
BASE_SEED = 20261025
BASE = Params()


def _job(args):
    cfg, seed = args
    p = with_(BASE, **cfg)
    r = run(p, seed)
    r.update({f"cfg_{k}": (str(v) if isinstance(v, tuple) else v) for k, v in cfg.items()})
    r["seed"] = seed
    return r


def grid(**axes):
    keys = list(axes)
    return [dict(zip(keys, vals)) for vals in itertools.product(*axes.values())]


def run_grid(name, configs, reps, procs=os.cpu_count()):
    jobs = [(c, BASE_SEED + r) for c in configs for r in range(reps)]
    t = time.time()
    with Pool(procs) as pool:
        rows = pool.map(_job, jobs, chunksize=4)
    df = pd.DataFrame(rows)
    df.to_csv(OUT / f"{name}.csv", index=False)
    print(f"{name}: {len(configs)} configs x {reps} reps in {time.time() - t:.0f}s")
    return df


SPEC_95 = (0.95, 0.90, 0.95)
SPEC_98 = (0.98, 0.90, 0.95)
TAUS = [0.0, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 0.99]


def E1(reps):
    """Accuracy vs circularity: sensor RMSEP x confidence threshold x cotton spec."""
    return run_grid("E1_accuracy_vs_circularity",
                    grid(sigma=[0.01, 0.02, 0.035, 0.05, 0.066], tau=TAUS, spec=[SPEC_95, SPEC_98]), reps)


def E2(reps):
    """Fallback design: manual capacity x manual accuracy x threshold."""
    return run_grid("E2_fallback_design",
                    grid(tau=TAUS, manual_servers=[0, 1, 2, 4, 8], manual_accuracy=[0.75, 0.85, 0.95],
                         spec=[SPEC_95, SPEC_98]), reps)


def E3(reps):
    """Bale size x cotton specification (acceptance cliff)."""
    specs = [(s, 0.90, 0.95) for s in (0.95, 0.96, 0.97, 0.98, 0.99)]
    return run_grid("E3_bale_spec", grid(bale_kg=[50, 100, 200, 500, 1000], spec=specs), reps)


def E4(reps):
    """Digital product passport penetration x sensor quality x DPP data error."""
    return run_grid("E4_dpp",
                    grid(dpp_share=[0.0, 0.1, 0.25, 0.5, 0.75, 1.0], sigma=[0.035, 0.066],
                         dpp_error=[0.02, 0.10], spec=[SPEC_95, SPEC_98]), reps)


OAT = {  # one-at-a-time sensitivity: (low, high) around Params() defaults
    "mass_mean_kg": (0.25, 0.50),
    "p_elastane_mono": (0.10, 0.40),
    "p_dark": (0.0, 0.15),
    "p_discharge": (0.0, 0.05),
    "manual_servers": (1, 4),
    "manual_service_s": (10.0, 40.0),
    "manual_accuracy": (0.75, 0.95),
    "bale_kg": (100.0, 500.0),
    "qc_sd": (0.0, 0.01),
    "share_pes_mono": (0.10, 0.22),
    "share_pc_blend": (0.06, 0.24),
    "sigma": (0.02, 0.066),
}


def E6(reps):
    """One-at-a-time sensitivity of the main KPIs to every assumed parameter."""
    configs = []
    for spec in (SPEC_95, SPEC_98):
        for tau in (0.0, 0.8):
            configs.append(dict(spec=spec, tau=tau))
            for k, (lo, hi) in OAT.items():
                configs.append({k: lo, "spec": spec, "tau": tau})
                configs.append({k: hi, "spec": spec, "tau": tau})
    return run_grid("E6_oat_sensitivity", configs, reps)


POLICY_FAMILIES = (
    [dict(policy="fixed", tau=t) for t in (0.0, 0.5, 0.6, 0.7, 0.8, 0.9)]
    + [dict(policy="capacity", tau=t) for t in (0.6, 0.8, 1.0)]
    + [dict(policy="quality", tau=1.0, budget_safety=b) for b in (1.0, 0.8, 0.6, 0.4)]
)


def E7(reps):
    """Delegation-policy comparison across operating conditions (RQ2 extended).

    Policy parameters are selected on replications 0..reps/2-1 and evaluated on the
    remaining replications (holdout) in the analysis, to avoid optimistic selection bias.
    """
    conds = grid(spec=[SPEC_95, SPEC_98], p_discharge=[0.0, 0.01, 0.02],
                 manual_servers=[1, 2, 4], manual_accuracy=[0.85, 0.95])
    configs = [{**c, **pol} for c in conds for pol in POLICY_FAMILIES]
    return run_grid("E7_policy_comparison", configs, reps)


def E8(reps):
    """Capacity-aware deferral with a recalibrated confidence score (same conditions and
    seeds as E7, so results pair with E7's uncalibrated policy B and fixed policy A)."""
    xs, ys = fit_calibration(BASE)
    cmap = (tuple(float(x) for x in xs), tuple(float(y) for y in ys))
    conds = grid(spec=[SPEC_95, SPEC_98], p_discharge=[0.0, 0.01, 0.02],
                 manual_servers=[1, 2, 4], manual_accuracy=[0.85, 0.95])
    configs = [{**c, "policy": "capacity", "tau": 1.0, "conf_map": cmap} for c in conds]
    return run_grid("E8_calibrated_deferral", configs, reps)


# ---------------- E5: drift and the data loop ----------------
KAPPA0, N0, WEEKS, NOVEL_MAX = 2.0, 500.0, 52, 0.4  # ASSUMPTIONS, see docs/parameters.md


def kappa(n_labels, manual_accuracy):
    n_eff = n_labels * manual_accuracy
    return 1 + (KAPPA0 - 1) * (1 + n_eff / N0) ** -0.5


DRIFT_POLICIES = {
    "static_tau0": dict(tau=0.0, retrain=None, audit_share=0.0),
    "weekly_tau0_audit2": dict(tau=0.0, retrain=1, audit_share=0.02),
    "weekly_tau0.8": dict(tau=0.8, retrain=1, audit_share=0.0),
    "quarterly_tau0.8": dict(tau=0.8, retrain=13, audit_share=0.0),
    "static_tau0.8": dict(tau=0.8, retrain=None, audit_share=0.0),
}


def _drift_traj(args):
    pol_name, rep, spec = args
    pol = DRIFT_POLICIES[pol_name]
    labels, labels_at_last_train, k = 0, 0, KAPPA0
    rows = []
    for w in range(WEEKS):
        if pol["retrain"] and w > 0 and w % pol["retrain"] == 0:
            labels_at_last_train = labels
            k = kappa(labels_at_last_train, BASE.manual_accuracy)
        p = with_(BASE, tau=pol["tau"], audit_share=pol["audit_share"], spec=spec,
                  novel_share=NOVEL_MAX * w / (WEEKS - 1), novel_kappa=k)
        r = run(p, BASE_SEED + 1000 * rep + w)
        labels += r["novel_labelled"]
        r.update(policy=pol_name, rep=rep, week=w, kappa=k, labels=labels, cfg_spec=str(spec))
        rows.append(r)
    return rows


def E5(reps):
    t = time.time()
    jobs = [(pn, r, s) for pn in DRIFT_POLICIES for r in range(reps) for s in (SPEC_95, SPEC_98)]
    with Pool(os.cpu_count()) as pool:
        out = pool.map(_drift_traj, jobs)
    df = pd.DataFrame([row for traj in out for row in traj])
    df.to_csv(OUT / "E5_drift.csv", index=False)
    print(f"E5: {len(jobs)} trajectories x {WEEKS} weeks in {time.time() - t:.0f}s")
    return df


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("exps", nargs="*", default=["E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8"])
    ap.add_argument("--reps", type=int, default=20)
    a = ap.parse_args()
    OUT.mkdir(exist_ok=True)
    for e in a.exps:
        globals()[e](a.reps if e != "E5" else max(5, a.reps // 2))
