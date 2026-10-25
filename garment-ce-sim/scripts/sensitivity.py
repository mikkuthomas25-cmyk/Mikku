"""Global sensitivity analysis (Sobol, Saltelli sampling) over parameters with role 'gsa'.

Outputs: cumulative 2025-2040 virgin fibre demand under BAU, the reduction delivered
by the combined policy (S5 - S0), and the emergent displacement rate.
"""
import sys
from multiprocessing import Pool

import numpy as np
import pandas as pd
from SALib.analyze import sobol
from SALib.sample import sobol as sobol_sample

from _common import RESULTS
from gcesim.calibration import load_calibrated
from gcesim.model import run_model
from gcesim.params import bounds
from gcesim.scenarios import combine

B = bounds("gsa")
PROBLEM = {"num_vars": len(B), "names": list(B.name), "bounds": [[l, h] for l, h in zip(B.low, B.high)]}
SL = slice(2025, 2040)


def evaluate(x):
    p = load_calibrated(dict(zip(PROBLEM["names"], x)))
    p["n_agents"] = 1500
    bau = run_model(p, combine("S0_BAU"), seed=11)
    comb = run_model(p, combine("S5_COMBINED"), seed=11)
    off = run_model(p, combine("S0_BAU"), seed=11, sh_off_from=2025)
    vf = bau.loc[SL, "virgin_fibre_kt"].sum()
    policy = vf - comb.loc[SL, "virgin_fibre_kt"].sum()
    disp = (off.loc[SL, "new_kt"].sum() - bau.loc[SL, "new_kt"].sum()) / bau.loc[SL, "sh_kt"].sum()
    ghg = bau.loc[SL, "ghg_total"].sum()
    return vf, policy, disp, ghg


if __name__ == "__main__":
    n = int(sys.argv[1]) if len(sys.argv) > 1 else 64
    X = sobol_sample.sample(PROBLEM, n, calc_second_order=False, seed=7)
    with Pool() as pool:
        Y = np.array(pool.map(evaluate, X))
    np.save(RESULTS / "sobol_Y.npy", Y)
    rows = []
    for j, out in enumerate(["virgin_fibre_bau", "policy_reduction_s5", "displacement", "ghg_bau"]):
        si = sobol.analyze(PROBLEM, Y[:, j], calc_second_order=False, seed=7)
        for k, name in enumerate(PROBLEM["names"]):
            rows.append({"output": out, "param": name, "S1": si["S1"][k], "S1_conf": si["S1_conf"][k],
                         "ST": si["ST"][k], "ST_conf": si["ST_conf"][k]})
    res = pd.DataFrame(rows)
    res.to_csv(RESULTS / "sobol_indices.csv", index=False)
    for out, g in res.groupby("output"):
        print(f"\n{out} (top total-order indices)")
        print(g.sort_values("ST", ascending=False).head(8)[["param", "S1", "ST", "ST_conf"]].round(3).to_string(index=False))
    print("\nOutput distributions (mean, 5%, 95%):")
    for j, out in enumerate(["virgin_fibre_bau", "policy_reduction_s5", "displacement", "ghg_bau"]):
        print(out, np.round([Y[:, j].mean(), np.quantile(Y[:, j], 0.05), np.quantile(Y[:, j], 0.95)], 3))
