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
| `normalized` | locally-weighted (Mondrian-style) split conformal |
| `cvplus` | CV+ (Barber et al., 2021) — all data calibrates |

Data are simulated from a known population model `X1,X2 → M → Y (+ X1→Y)` with
three reflective indicators per construct; target = the outcome `Y` indicators.

## Findings (nominal coverage 0.90)

Skewed + heteroskedastic measurement noise on `Y` (the realistic service-data regime):

| n | method | marginal cov | width |
|---|---|---|---|
| 60 | naive_cv | 0.907 | 3.91 |
| 60 | split | **0.937** | **5.84** |
| 60 | cvplus | 0.920 | 4.05 |
| 120 | naive_cv | 0.915 | 3.89 |
| 120 | cvplus | 0.909 | **3.64** |
| 400 | naive_cv | 0.921 | 3.88 |
| 400 | cvplus | 0.901 | **3.46** |

Reading:
1. **Validity confirmed** — conformal achieves nominal marginal coverage; the
   re-estimate-per-split discipline is what makes exchangeability hold.
2. **Split conformal fails at small n** (n=60: over-covers 0.94, ~50% wider) —
   its calibration set is too small. This is the practical problem for
   service-research sample sizes.
3. **CV+ fixes it** — nominal-ish coverage at width comparable to the normal
   baseline at n=60, and **tighter** than the normal baseline at n≥120.
4. **Efficiency under non-normality** — normal-theory intervals over-cover
   (~0.92) and are wider on skewed residuals; conformal (esp. CV+) is 6–11%
   tighter at adequate n while staying calibrated.
5. In a clean Gaussian, large-n world conformal offers no benefit (slightly
   wider) — no free lunch, and worth stating honestly.

**Contribution wedge:** distribution-free, finite-sample-valid prediction
intervals for PLS-SEM where none exist today, with **CV+/jackknife+** as the
variant that works at realistic service-research sample sizes, plus efficiency
gains under the non-normal errors typical of service data.

## Caveats / next steps

- The base-R estimator is for the simulation engine. The paper's **illustrative
  empirical example must be reproduced with `seminr`/`cSEM`** to match canonical
  PLSpredict and satisfy the editorial panel. (`seminr` unavailable in the build
  sandbox — CRAN egress blocked — hence the from-scratch engine.)
- Sims so far cover a single reflective model. The paper needs formative/composite
  constructs, higher-order models, more conditions, and jackknife+.
- Conditional coverage / heterogeneity (Mondrian conformal for known segments)
  is a distinct scenario still to build — ties to the special section's
  heterogeneity theme.

Run: `Rscript spike/conformal_plssem_spike.R`
