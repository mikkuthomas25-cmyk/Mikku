import sys
from pathlib import Path

import numpy as np
import pandas as pd
import pytest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from gcesim import load_params, run_model, Observed  # noqa: E402
from gcesim.brands import law_penalty_range  # noqa: E402
from gcesim.scenarios import SCENARIOS  # noqa: E402


@pytest.fixture(scope="module")
def base_run():
    return run_model(seed=7, end=2035)


def test_registry_sources_exist():
    reg = pd.read_csv(ROOT / "data" / "parameters.csv")
    src = set(pd.read_csv(ROOT / "data" / "sources.csv").source_id)
    obs = pd.read_csv(ROOT / "data" / "france_tlc_observed.csv")
    assert set(reg.source_id) <= src
    assert set(obs.source_id) <= src
    gsa = reg[reg.role.isin(["gsa", "calibrate"])]
    assert (gsa.low <= gsa.value).all() and (gsa.value <= gsa.high).all()


def test_historical_purchases_forced(base_run):
    obs = Observed()
    for y in (2015, 2019, 2024):
        assert base_run.loc[y, "new_kt"] == pytest.approx(obs.interp("placed_on_market", y), rel=1e-3)


def test_household_disposal_balance(base_run):
    d = base_run
    lhs = d.discards_kt
    rhs = d.p2p_kt + d.collected_hh + d.residual_waste
    assert np.allclose(lhs, rhs, rtol=1e-9)
    assert np.allclose(d.collected, d.collected_hh + d.sh_surplus)


def test_downstream_mass_balance(base_run):
    d = base_run
    backlog_change = d.backlog.diff().fillna(d.backlog.iloc[0])
    lhs = d.collected
    rhs = d.unsorted_export + d.sorted + d.overflow_export + d.overflow_incin + backlog_change
    assert np.allclose(lhs, rhs, rtol=1e-9, atol=1e-6)
    out = (d.reuse_domestic + d.reuse_export + d.recycling_open + d.wipes + d.energy_recovery
           + d.sort_residual + d.f2f_output)
    assert np.allclose(d.sorted, out, rtol=1e-9, atol=1e-6)


def test_non_negative(base_run):
    cols = ["new_kt", "sh_kt", "collected", "sorted", "backlog", "recycling_open", "residual_waste", "f2f_output"]
    assert (base_run[cols] >= -1e-9).all().all()


def test_deterministic_seed():
    a = run_model(seed=3, end=2026)
    b = run_model(seed=3, end=2026)
    pd.testing.assert_frame_equal(a, b)


def test_law_penalty_band():
    assert law_penalty_range(2025) == (0.0, 0.0)
    assert law_penalty_range(2028) == (0.5, 12.0)
    assert law_penalty_range(2031) == (2.0, 19.5)


def test_law_reduces_ultra_fast_share():
    base = run_model(seed=5, end=2032, scenario=SCENARIOS["S0_BAU"])
    law = run_model(seed=5, end=2032, scenario=SCENARIOS["S1_FF_LAW"])
    assert law.loc[2032, "uf_share"] < base.loc[2032, "uf_share"]
    assert law.loc[2032, "price_index"] > base.loc[2032, "price_index"]


def test_sh_switch_off_raises_new_purchases():
    on = run_model(seed=5, end=2030)
    off = run_model(seed=5, end=2030, sh_off_from=2025)
    assert off.loc[2030, "sh_kt"] == 0
    assert off.loc[2026:2030, "new_kt"].sum() > on.loc[2026:2030, "new_kt"].sum()
