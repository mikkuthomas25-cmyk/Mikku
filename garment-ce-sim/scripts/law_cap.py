"""RQ3: how much does the 50 % price cap of loi 2026-602 blunt the penalty?"""
from multiprocessing import Pool

import pandas as pd

from _common import RESULTS
from gcesim.calibration import load_calibrated
from gcesim.model import run_model
from gcesim.scenarios import combine

VARIANTS = {"BAU": {}, "law (cap)": {"ff_law": True}, "law (no cap)": {"ff_law": True, "law_cap": False},
            "law x0.5 (cap)": {"ff_law": True, "law_mult": 0.5}}


def one(args):
    name, seed = args
    sc = {**combine("S0_BAU"), **VARIANTS[name]}
    d = run_model(load_calibrated(), sc, seed=seed).loc[2025:2040]
    return {"variant": name, "seed": seed, "new_kt": d.new_kt.sum(), "ghg": d.ghg_total.sum(),
            "uf_share_2040": d.loc[2040, "uf_share"], "dur_new_2040": d.loc[2040, "dur_new"],
            "price_2040": d.loc[2040, "price_index"]}


if __name__ == "__main__":
    with Pool() as pool:
        r = pd.DataFrame(pool.map(one, [(v, s) for v in VARIANTS for s in range(400, 410)]))
    t = r.groupby("variant").mean(numeric_only=True).drop(columns="seed")
    t["new_kt_vs_bau_%"] = 100 * (t.new_kt / t.loc["BAU", "new_kt"] - 1)
    t.to_csv(RESULTS / "law_cap.csv")
    print(t.round(3).to_string())
