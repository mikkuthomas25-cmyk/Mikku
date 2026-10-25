# Textile EPR in France: a calibrated hybrid SD-ABM

A simulation study of circular-economy policy in the French textile, linen and footwear (TLC)
sector. It is a hybrid system-dynamics / agent-based model, calibrated to 15 years of
Eco TLC / Refashion data. It is used to evaluate EPR levers, including the 2026 anti-fast-fashion
law, over 2025–2040.

Python is the reference implementation. An AnyLogic port for visualisation is planned
(see `docs/02_model_description_ODD.md` §2.9).

## Documents

| | |
|---|---|
| `docs/01_research_design.md` | Problem, research questions, hypotheses, contribution |
| `docs/02_model_description_ODD.md` | Full model description (ODD protocol) |
| `docs/03_data.md` | Data sources, issues, calibration and validation targets |
| `docs/04_open_questions.md` | Decisions needed from the author |
| `data/` | Sources, observed series and parameter registry (all inputs, with provenance) |

## Run

```bash
pip install numpy pandas scipy SALib matplotlib pytest
PYTHONPATH=src python -m pytest -q tests        # mass balance + behaviour tests
python scripts/calibrate.py                      # ~30-40 min on 4 cores -> results/calibration.json
python scripts/run_scenarios.py                  # 6 scenarios x 3 stress tests x 20 seeds
python scripts/displacement.py                   # RQ1 displacement grid
python scripts/sensitivity.py 64                 # Sobol GSA
python scripts/figures.py                        # results/figures/*.png
```

## Layout

```
src/gcesim/
  params.py       parameter registry loader
  data.py         observed series, interpolation, targets
  consumers.py    consumer agents (Bass adoption, habits, wardrobe, rebound)
  brands.py       brand agents (logit market, fees, loi 2026-602 penalty, upgrades)
  sorting.py      sorting operators (capacity, margin, exit/entry) + fibre-to-fibre SD
  impacts.py      GHG accounting
  model.py        coupling, historical vs projection mode
  scenarios.py    policy scenarios and stress tests
  calibration.py  differential-evolution calibration
```
