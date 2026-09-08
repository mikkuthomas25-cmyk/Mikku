# How to run this (quick guide)

All scripts live in the `spike/` folder and are run from inside it. They use only
base R — **except** `seminr_adapter.R`, which needs the `seminr` package.

## Setup (once)

```r
install.packages("seminr")   # only needed for seminr_adapter.R
```

```bash
cd spike
```

## The one that matters: real-data result

```bash
Rscript seminr_adapter.R
```

This fits the corporate-reputation model with `seminr` and prints real
prediction intervals. Nothing to configure — the adapter auto-detects everything.
Look for two things in the output:

1. a line like `adapter check: cor(predicted, actual) for cusl_1 = 0.8xx -> OK`
2. a table `CV+ conformal intervals for CUSL`

Send me those two and I'll fold the empirical result into the writeup.

## The rest (optional — reproduce the simulations)

Run any of these from `spike/` (no packages needed):

```bash
Rscript conformal_plssem_spike.R        # core study (~2 min)
Rscript extensions_formative_hoc.R      # formative + higher-order + worked example (~4 min)
Rscript montecarlo_sweep.R              # full n=50..1500 table -> writes a CSV (~9 min)
Rscript test_adapter_plumbing.R         # checks the seminr adapter mapping (seconds)
```

That's it. If `seminr_adapter.R` errors on `install.packages`, your R just needs
a CRAN mirror set (`chooseCRANmirror()` once), nothing else.
```
