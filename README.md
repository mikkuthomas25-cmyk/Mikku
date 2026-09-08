# Conformal Prediction Intervals for PLS-SEM

Working repository for a methodological paper targeting the *Journal of Service
Management* special section **"Partial Least Squares Structural Equation Modeling
in Service Research"** (submissions 15 Sep – 01 Dec 2026).

## Idea

PLS-SEM's predictive turn (PLSpredict; the CVPAT test) delivers **point**
predictions and average error metrics, but no **case-level prediction intervals**.
This project brings **conformal prediction** — distribution-free intervals with a
finite-sample coverage guarantee — to PLS-SEM prediction.

Novelty check (Sep 2026): conformal/distribution-free predictive inference exists
for **PLS regression** in chemometrics (Lin et al., 2022, *J. Chemometrics*,
10.1002/cem.3457) but **not** for PLS-SEM / PLS path modeling. No published prior
art found across 2015–2026.

## Feasibility spike

`spike/conformal_plssem_spike.R` is a self-contained, zero-dependency base-R
implementation of (a) a PLS-SEM estimator (Lohmöller mode-A, path-weighting),
(b) out-of-sample PLS prediction (PLSpredict-style propagation, re-estimated per
split so exchangeability holds), and (c) four interval methods:

| method | what it is |
|---|---|
| `naive_insample` | normal interval from in-sample residual sd (optimistic strawman) |
| `naive_cv` | normal interval from k-fold out-of-sample RMSE (honest baseline) |
| `split` | split (inductive) conformal |
| `normalized` | locally-weighted split conformal |
| `cvplus` | CV+ (Barber et al., 2021) — all data calibrates |
| `jackknife` | jackknife+ (Barber et al., 2021) — leave-one-out CV+ |
| Mondrian | per-segment conformal (separate quantile within each known segment) |

Data are simulated from a known population model `X1,X2 → M → Y (+ X1→Y)` with
three reflective indicators per construct; target = the outcome `Y` indicators.

## Findings (nominal coverage 0.90)

Skewed + heteroskedastic measurement noise on `Y` (the realistic service-data regime):

**Which variant works at which n** (skewed + heteroskedastic noise):

| n | method | marginal cov | width |
|---|---|---|---|
| 60 | naive_cv | 0.905 | 3.88 |
| 60 | split | **0.938** | **5.75** |
| 60 | cvplus | 0.913 | 4.01 |
| 60 | jackknife | 0.918 | 4.01 |
| 400 | naive_cv | 0.922 | 3.92 |
| 400 | cvplus | 0.904 | **3.49** |

**Heterogeneity — per-segment coverage** (two known segments, unequal error
spread; nominal per-segment = 0.90):

| method | seg1 (low-noise) | seg2 (high-noise) |
|---|---|---|
| pooled conformal | **0.990** | **0.827** |
| Mondrian conformal | 0.917 | 0.913 |

Reading:
1. **Validity confirmed** — conformal achieves nominal marginal coverage; the
   re-estimate-per-split discipline is what makes exchangeability hold.
2. **Split conformal fails at small n** (n=60: over-covers 0.94, ~50% wider) —
   its calibration set is too small. The practical problem for service-research n.
3. **CV+ and jackknife+ fix it** — near-nominal coverage at width comparable to
   the normal baseline at n=60, and **tighter** than it at large n.
4. **Efficiency under non-normality** — normal-theory intervals over-cover
   (~0.92) and are wider on skewed residuals; conformal is ~10% tighter at
   adequate n while staying calibrated.
5. **Heterogeneity** — pooled conformal is valid on average but badly
   miscalibrated per segment (over-covers the easy segment, under-covers the
   hard one); **Mondrian conformal restores ~0.90 in both**.
6. In a clean Gaussian, large-n world conformal offers no benefit (slightly
   wider) — no free lunch, and worth stating honestly.

**Contribution wedge:** distribution-free, finite-sample-valid prediction
intervals for PLS-SEM where none exist today, with **CV+/jackknife+** as the
variant that works at realistic service-research sample sizes, plus efficiency
gains under the non-normal errors typical of service data.

## Expanded Monte Carlo (`spike/montecarlo_sweep.R`)

Sample-size sweep, skewed+heteroskedastic regime, coverage (mean interval width);
Monte Carlo standard errors ≤ 0.004 throughout. Nominal coverage = 0.90.
Full grid incl. the clean-Gaussian regime in `spike/montecarlo_sweep_results.csv`.

| n | naive_cv | split | CV+ |
|---|---|---|---|
| 50 | 0.909 (3.97) | 0.917 (5.33) | 0.921 (4.30) |
| 100 | 0.912 (3.88) | 0.926 (4.61) | 0.907 (3.77) |
| 200 | 0.922 (3.91) | 0.904 (3.70) | 0.902 (3.51) |
| 400 | 0.924 (3.92) | 0.898 (3.52) | 0.901 (3.44) |
| 800 | 0.924 (3.88) | 0.899 (3.43) | 0.902 (3.44) |
| 1500 | 0.924 (3.89) | 0.901 (3.44) | 0.901 (3.41) |

Reading:
- **CV+ is the all-rounder**: near-nominal at every n and tightest-or-near-tightest.
- **split** needs n ≳ 200; at n ≤ 100 its small calibration set over-covers and
  runs 20–50% wider.
- **naive normal persistently over-covers (~0.92) under skew and never tightens
  below ~3.9** — so at adequate n, CV+ gives ~12% narrower intervals at correct
  coverage. In the clean-Gaussian regime all methods hit 0.90 and CV+ ≈ naive on
  width (no free lunch — the gains are specific to non-normal service data).

## Extensions (`spike/extensions_formative_hoc.R`)

The pluggable `fit_fun`/`pred_fun` interface lets the SAME conformal code wrap
other measurement structures. Confirmed at nominal 0.90:

| model | naive_cv | CV+ | jackknife+ |
|---|---|---|---|
| **Formative** (mode B), n=150 | 0.912 | 0.904 | 0.904 |
| **Higher-order** (disjoint two-stage), n=250 | 0.917 | 0.903 | — |

Conformal stays valid (and slightly tighter than the normal baseline) with a
formative construct and with a second-order construct estimated by the disjoint
two-stage approach (Sarstedt et al., 2019).

**Worked example** (same file): actual per-respondent 90% loyalty-item intervals
on a 1–7 scale, e.g. `predicted 4.75, interval [3.15, 6.31], actual 5.17`;
held-out coverage 0.92 over 50 respondents, mean width 3.16 points.

## seminr backend (`spike/seminr_adapter.R`)

`seminr_to_predictor()` maps a fitted `seminr` model into the predictor object the
conformal functions expect, so `cvplus()`/`split_conformal()`/`jackknife_plus()`
run on canonical seminr estimation (illustrative example: corp_rep "simple model").
Its plumbing is verified against the native engine to machine precision
(`spike/test_adapter_plumbing.R`); the seminr-specific numeric conventions are
checked at runtime by `validate_adapter()`. **Untested against a live seminr**
(CRAN blocked in the build sandbox) — run it in your R environment.

## Caveats / next steps

- The base-R estimator is the simulation engine; the paper's empirical example
  should be reproduced with `seminr` via the adapter above.
- HOC uses disjoint two-stage; extending to unobserved heterogeneity
  (FIMIX/POS segments) beyond the observed-segment Mondrian case is a further step.
- A plain-language overview for non-methodologists is in `ABOUT.md`.

Run: `Rscript spike/conformal_plssem_spike.R`
