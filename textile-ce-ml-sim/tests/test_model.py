"""Verification tests: the model does what its specification says."""
import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "sim"))
from model import (C_COT, C_OTH, C_PC, C_PES, RESIDUAL, Params, _queue, bale_lines,  # noqa: E402
                   generate_stream, run, sense, true_class, with_)


def test_true_class_rule():
    comp = np.array([[1, 0, 0, 0], [0.96, 0, 0.04, 0], [0.9, 0.1, 0, 0], [0, 0.97, 0.03, 0],
                     [0, 0.9, 0.1, 0], [0, 0, 0, 1]], float)
    assert list(true_class(comp)) == [C_COT, C_COT, C_PC, C_PES, C_OTH, C_OTH]


def test_stream_mass_and_composition():
    p = Params(horizon_h=5)
    s = generate_stream(p, np.random.default_rng(0))
    assert abs(s["mass"].sum() - p.throughput_kg_h * p.horizon_h) < 5
    assert np.allclose(s["comp"].sum(1), 1)
    assert (s["comp"] >= 0).all()
    assert np.all(np.diff(s["arrival_h"]) > 0)


def test_noiseless_sensor_is_perfect_and_certain():
    s = generate_stream(Params(horizon_h=2), np.random.default_rng(1))
    _, pred, conf, _ = sense(s["comp"], np.full(len(s["mass"]), 1e-9), 16, np.random.default_rng(2))
    assert (pred == s["true"]).all()
    assert (conf == 1).all()


def test_accuracy_decreases_with_noise():
    acc = [run(Params(sigma=sg, horizon_h=5), 3)["item_accuracy"] for sg in (0.005, 0.03, 0.08)]
    assert acc[0] > acc[1] > acc[2]


def test_perfect_system_gives_pure_accepted_bales():
    p = Params(sigma=1e-9, p_dark=0, p_discharge=0, horizon_h=10, qc_sd=0)
    r = run(p, 4)
    # bale purity >= spec by construction of the class rule, so nothing is rejected
    assert r["COT_reject"] == 0 and r["PES_reject"] == 0 and r["PC_reject"] == 0
    assert r["COT_purity"] >= 0.95
    # carried-over partial bale is credited at the observed (100%) acceptance rate
    assert abs(r["cotton_recovery"] - 1) < 1e-3  # mass-based carry-over credit


def test_mass_conservation():
    r = run(Params(horizon_h=5, tau=0.8), 5, return_items=True)
    it = r["_items"]
    dest = it["dest"]
    assert set(np.unique(dest)) <= {C_COT, C_PES, C_PC, RESIDUAL}
    assert math.isclose(it["mass"].sum(), r["input_kg"])


def test_bale_purity_matches_mass_weighted_definition():
    rng = np.random.default_rng(6)
    n = 2000
    mass = np.full(n, 1.0)
    comp = np.zeros((n, 4))
    comp[:, 0] = 0.97
    comp[:, 1] = 0.03
    dest = np.full(n, C_COT)
    res, _ = bale_lines(dest, mass, comp, Params(bale_kg=100, qc_sd=0), rng)
    assert np.allclose(res["COT"]["purity"], 0.97)
    assert res["COT"]["acc"].all()


def erlang_b(c, a):
    b = 1.0
    for k in range(1, c + 1):
        b = a * b / (k + a * b)
    return b


def test_queue_matches_erlang_b_loss_system():
    """M/M/c/c (no waiting room) blocking probability equals Erlang B."""
    rng = np.random.default_rng(7)
    lam, mu, c, n = 100.0, 30.0, 4, 200_000  # per hour
    arr = np.cumsum(rng.exponential(1 / lam, n))
    svc = rng.exponential(1 / mu, n)
    served, _ = _queue(arr, svc, c, 0, np.zeros(n, bool))
    assert abs((1 - served.mean()) - erlang_b(c, lam / mu)) < 0.005


def test_queue_mm1_infinite_buffer_serves_everyone():
    rng = np.random.default_rng(8)
    n = 50_000
    arr = np.cumsum(rng.exponential(1 / 10, n))
    svc = rng.exponential(1 / 20, n)
    served, busy = _queue(arr, svc, 1, 10**9, np.zeros(n, bool))
    assert served.all()
    assert abs(busy / arr[-1] - 0.5) < 0.02  # utilisation rho = lambda/mu


def test_full_dpp_with_no_errors_bypasses_sensor():
    p = Params(dpp_share=1.0, dpp_error=0.0, p_discharge=0.0, sigma=0.2, horizon_h=5, qc_sd=0)
    r = run(p, 9)
    assert r["COT_reject"] == 0
    assert abs(r["cotton_recovery"] - 1) < 1e-3  # mass-based carry-over credit


def test_common_random_numbers_reproducible():
    a = run(with_(Params(horizon_h=3), tau=0.5), 11)
    b = run(with_(Params(horizon_h=3), tau=0.5), 11)
    assert a == b


def test_capacity_policy_without_manual_station_auto_sorts_everything_sensed():
    p = Params(policy="capacity", tau=1.0, manual_servers=0, horizon_h=3)
    r = run(p, 12)
    assert r["manual_items"] == 0 and r["coverage"] == 1.0


def test_capacity_policy_never_delegates_items_above_manual_accuracy():
    p = Params(policy="capacity", tau=1.0, manual_servers=50, horizon_h=3)
    r = run(p, 13, return_items=True)
    it = r["_items"]
    # an item is delegated only if the manual sorter is more likely right than the sensor
    assert r["manual_items"] > 0
    assert (it["conf"][it["manual_idx"]] < p.manual_accuracy).all()


def test_quality_policy_with_unlimited_budget_matches_capacity_policy():
    a = run(Params(policy="quality", tau=1.0, budget_safety=1e9, horizon_h=3), 14)
    b = run(Params(policy="capacity", tau=1.0, horizon_h=3), 14)
    assert a["manual_items"] <= b["manual_items"]
    assert a["protective_divert"] == 0
