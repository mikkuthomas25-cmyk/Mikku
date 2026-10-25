"""RQ1: emergent displacement rate from paired runs (second-hand channel on vs off from 2025).

displacement = (new items without SH - new items with SH) / SH items bought, 2025-2040.
Also maps displacement over a grid of rebound x residual-life of second-hand items.
"""
from itertools import product
from multiprocessing import Pool

import numpy as np
import pandas as pd

from _common import RESULTS
from gcesim.calibration import load_calibrated
from gcesim.model import run_model
from gcesim.scenarios import combine

SEEDS = range(200, 210)


def displacement(args):
    rebound, life, seed = args
    p = load_calibrated(extra={"rebound": rebound, "sh_life_frac": life})
    sc = combine("S0_BAU")
    on = run_model(p, sc, seed=seed)
    off = run_model(p, sc, seed=seed, sh_off_from=2025)
    sl = slice(2025, 2040)
    d_new = off.loc[sl, "new_kt"].sum() - on.loc[sl, "new_kt"].sum()
    d_ghg = off.loc[sl, "ghg_total"].sum() - on.loc[sl, "ghg_total"].sum()
    sh = on.loc[sl, "sh_kt"].sum()
    return {"rebound": rebound, "sh_life_frac": life, "seed": seed, "displacement": d_new / sh,
            "sh_kt_cum": sh, "new_kt_avoided": d_new, "ghg_avoided_kt": d_ghg}


if __name__ == "__main__":
    base = load_calibrated()
    grid = list(product([0.0, 0.15, 0.3, 0.45, 0.6], [0.4, 0.6, 0.8, 1.0], SEEDS))
    with Pool() as pool:
        res = pd.DataFrame(pool.map(displacement, grid))
    res.to_csv(RESULTS / "displacement_grid.csv", index=False)
    tab = res.groupby(["rebound", "sh_life_frac"]).displacement.mean().unstack()
    print("Mean displacement rate (rows: rebound, cols: residual life of SH item):")
    print(tab.round(2).to_string())
    c = res[(res.rebound == base.rebound) & (res.sh_life_frac == base.sh_life_frac)]
    print(f"\nCentral case (rebound={base.rebound}, sh_life_frac={base.sh_life_frac}): "
          f"{c.displacement.mean():.2f} [{c.displacement.min():.2f}, {c.displacement.max():.2f}] over seeds")
