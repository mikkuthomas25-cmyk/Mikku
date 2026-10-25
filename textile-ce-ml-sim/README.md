# Accuracy is not circularity — ML sorting × textile fibre-to-fibre recycling (simulation study)

Research project: how machine-learning sorting decisions (confidence thresholds, manual
fallback, digital product passports, drift and retraining) propagate to recycler bale
acceptance and closed-loop yield in post-consumer textile recycling.

| Path | Content |
|---|---|
| `paper/manuscript.md` | Paper draft |
| `docs/literature.md` | Every source used, with verification status (read first) |
| `docs/parameters.md` | Every model parameter, its value, range and source / ASSUMPTION label |
| `sim/model.py` | Item-level stochastic discrete-event model |
| `sim/experiments.py` | Experiments E1–E6 (common random numbers) |
| `sim/analysis.py` | Summary tables (mean ± 95% CI half-width) and figures |
| `tests/test_model.py` | Verification tests (incl. Erlang-B check of the queue) |
| `results/` | Raw replication CSVs, `tables/`, `figures/` |

## Reproduce

```bash
pip install numpy scipy pandas matplotlib pytest
python -m pytest -q tests
python sim/experiments.py E1 E3 E4 E6 --reps 30
python sim/experiments.py E2 --reps 20
python sim/experiments.py E5 --reps 20      # 10 trajectories per policy and spec
python sim/analysis.py
```

Full run ≈ 1–1.5 h on 4 cores.

## Known limitations (also in the paper)

- The sensor is a parametric measurement-error model calibrated to published RMSEP
  values, not a classifier trained on spectra. Replacing it with a model trained on the
  NIST NIR-SORT dataset (doi:10.18434/mds2-3325) is planned; downloading it was blocked
  by this environment's network policy.
- Several parameters (manual station, bale size, discharge error, DPP error, drift
  learning curve) are assumptions; they are varied in E2–E6.
- No plant data on bale acceptance were available, so the model supports comparative,
  not predictive, conclusions.
