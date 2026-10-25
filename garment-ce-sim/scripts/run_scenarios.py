"""Run all policy scenarios x stress tests x seeds (2010-2040) with calibrated parameters."""
from multiprocessing import Pool

import numpy as np
import pandas as pd

from _common import RESULTS, add_derived
from gcesim.calibration import load_calibrated
from gcesim.model import run_model
from gcesim.scenarios import SCENARIOS, STRESS, combine

SEEDS = range(100, 120)


def one(args):
    sc, st, seed = args
    df = add_derived(run_model(load_calibrated(), combine(sc, st), seed=seed))
    df["scenario"], df["stress"], df["seed"] = sc, st, seed
    return df.reset_index()


if __name__ == "__main__":
    jobs = [(sc, st, s) for sc in SCENARIOS for st in STRESS for s in SEEDS]
    with Pool() as pool:
        runs = pd.concat(pool.map(one, jobs), ignore_index=True)
    runs.to_csv(RESULTS / "scenario_runs.csv.gz", index=False)

    proj = runs[runs.year >= 2025]
    cum = proj.groupby(["scenario", "stress", "seed"])[
        ["new_kt", "virgin_fibre_kt", "ghg_total", "overflow_total", "exports_total", "residual_waste",
         "f2f_output", "sh_kt"]].sum()
    y40 = runs[runs.year == 2040].set_index(["scenario", "stress", "seed"])[
        ["collection_rate", "uf_share", "dur_new", "price_index", "sort_capacity", "n_sorters"]]
    y40.columns = [c + "_2040" for c in y40.columns]
    allm = cum.join(y40)
    base = allm.xs("S0_BAU", level="scenario")
    delta = (allm - base.reindex(allm.index.droplevel("scenario")).values)
    summ = allm.groupby(["stress", "scenario"]).agg(["mean", lambda x: x.quantile(0.05), lambda x: x.quantile(0.95)])
    summ.columns = [f"{a}_{'mean' if b == 'mean' else ('p05' if b == '<lambda_0>' else 'p95')}" for a, b in summ.columns]
    summ.to_csv(RESULTS / "scenario_summary.csv")
    d = delta.groupby(["stress", "scenario"]).mean()
    d.to_csv(RESULTS / "scenario_delta_vs_bau.csv")
    cols = ["new_kt", "virgin_fibre_kt", "ghg_total", "overflow_total", "exports_total", "residual_waste", "f2f_output"]
    print("Cumulative 2025-2040, mean over seeds (kt; GHG ktCO2e):")
    print(allm.groupby(["stress", "scenario"])[cols].mean().round(0).to_string())
    print("\nDifference vs BAU (same stress, same seed):")
    print(d[cols].round(0).to_string())
    print("\n2040 state:")
    print(allm.groupby(["stress", "scenario"])[list(y40.columns)].mean().round(3).to_string())
