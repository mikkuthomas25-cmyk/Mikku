"""Calibration to French TLC observations 2009-2024 (differential evolution).

Objective: weighted sum of squared relative errors between simulated and
observed values, using common random numbers (fixed seeds) to keep the
stochastic objective smooth.
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import pandas as pd
from scipy.optimize import differential_evolution

from .data import Observed
from .model import run_model
from .params import bounds, load_params

# simulated column for each observed variable
TARGET_MAP = {
    "collected": "collected",
    "sorted": "sorted",
    "sh_domestic_sales": "sh_kt",
    "sh_buyer_share": "sh_buyer_share",
    "sh_items_per_buyer": "sh_items_per_buyer",
    "reuse_share_sorted": "reuse_share",
}
WEIGHTS = {"collected": 1.0, "sorted": 1.0, "sh_domestic_sales": 0.5, "sh_buyer_share": 0.5,
           "sh_items_per_buyer": 0.5, "reuse_share_sorted": 0.5}
LOW_WEIGHT = 0.25


def target_table(obs: Observed | None = None) -> pd.DataFrame:
    obs = obs or Observed()
    t = obs.targets()
    t = t[t.variable.isin(TARGET_MAP)].copy()
    t["weight"] = t.variable.map(WEIGHTS) * np.where(t.use == "target_lowweight", LOW_WEIGHT, 1.0)
    return t


def simulated_at(df: pd.DataFrame, t: pd.DataFrame) -> np.ndarray:
    out = []
    for r in t.itertuples():
        col = TARGET_MAP[r.variable]
        yr = min(max(r.year, df.index.min()), df.index.max())
        out.append(df.loc[yr, col])
    return np.array(out)


def loss(df: pd.DataFrame, t: pd.DataFrame) -> float:
    sim = simulated_at(df, t)
    rel = (sim - t.value.values) / t.value.values
    return float(np.sum(t.weight.values * rel**2))


def fit_stats(df: pd.DataFrame, t: pd.DataFrame) -> pd.DataFrame:
    t = t.copy()
    t["simulated"] = simulated_at(df, t)
    t["rel_error"] = (t.simulated - t.value) / t.value
    return t


class Objective:
    """Picklable objective for parallel differential evolution."""

    def __init__(self, names, seeds, t, base):
        self.names, self.seeds, self.t, self.base = names, seeds, t, base

    def __call__(self, x):
        p = self.base.copy()
        p.update(dict(zip(self.names, x)))
        try:
            return float(np.mean([loss(run_model(p, seed=s, end=2024), self.t) for s in self.seeds]))
        except Exception:
            return 1e6


def calibrate(seeds=(1, 2), maxiter=60, popsize=8, workers=1, out: Path | None = None, verbose=True) -> dict:
    b = bounds("calibrate")
    names = list(b.name)
    lims = list(zip(b.low.astype(float), b.high.astype(float)))
    t = target_table()
    base = load_params()

    obj = Objective(names, seeds, t, base)
    res = differential_evolution(obj, lims, maxiter=maxiter, popsize=popsize, seed=42, tol=1e-6,
                                 polish=False, workers=workers, updating="deferred" if workers != 1 else "immediate",
                                 disp=verbose)
    best = dict(zip(names, map(float, res.x)))
    result = {"params": best, "loss": float(res.fun), "nfev": int(res.nfev), "seeds": list(seeds)}
    if out:
        Path(out).write_text(json.dumps(result, indent=2))
    return result


def load_calibrated(path: Path | None = None, extra: dict | None = None):
    path = path or Path(__file__).resolve().parents[2] / "results" / "calibration.json"
    p = load_params()
    if Path(path).exists():
        p.update(json.loads(Path(path).read_text())["params"])
    p.update(extra or {})
    return p
