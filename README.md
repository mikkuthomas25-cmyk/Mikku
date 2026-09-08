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

## Caveats / next steps

- The base-R estimator is for the simulation engine. The paper's **illustrative
  empirical example must be reproduced with `seminr`/`cSEM`** to match canonical
  PLSpredict and satisfy the editorial panel. (`seminr` unavailable in the build
  sandbox — CRAN egress blocked — hence the from-scratch engine.)
- Sims so far cover a single reflective model. The paper needs formative/composite
  constructs, higher-order models, and more conditions.
- A plain-language overview for non-methodologists is in `ABOUT.md`.

Run: `Rscript spike/conformal_plssem_spike.R`
