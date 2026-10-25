# PLS-SEM analysis for "Who Carries the Passport?" (JBL STF To the Point! 2027)
#
# Usage:
#   Rscript pls_model.R                       # runs on SYNTHETIC data (pipeline check only)
#   Rscript pls_model.R path/to/survey.csv    # runs on the real merged wave-1 + wave-2 file
#
# Variable names follow 03_survey_instrument.md.
# SYNTHETIC DATA ARE FOR TESTING THE CODE ONLY. They must never be reported as results.
#
# NOTE: written against the seminr API (>= 2.3) but not yet executed. R was not
# available in the drafting environment, so run it once and fix any API drift.

suppressPackageStartupMessages({
  library(seminr)
  library(MASS)
})

set.seed(2027)

# ---- 1. Data -----------------------------------------------------------------

simulate_survey <- function(n = 220) {
  # Latent scores with modest effects, then 7-point items. Purely illustrative.
  BTP <- rnorm(n)
  DAC <- rnorm(n)
  RU  <- rnorm(n)
  DTC <- 0.40 * BTP + rnorm(n, sd = sqrt(1 - 0.40^2))
  MDS <- 0.30 * BTP - 0.20 * BTP * DAC - 0.15 * DAC + rnorm(n, sd = 0.9)
  DPR <- 0.35 * DTC + 0.30 * MDS + 0.10 * DTC * MDS + rnorm(n, sd = 0.8)
  CSC <- 0.25 * DPR + 0.10 * BTP + rnorm(n, sd = 0.95)
  EXP <- 0.20 * DPR + rnorm(n, sd = 0.97)
  MV  <- rnorm(n)

  to_likert <- function(x) pmin(7, pmax(1, round(4 + 1.2 * x)))
  make_items <- function(latent, prefix, k, loading = 0.8) {
    items <- sapply(seq_len(k), function(i)
      to_likert(loading * scale(latent)[, 1] + rnorm(n, sd = sqrt(1 - loading^2))))
    colnames(items) <- paste0(prefix, seq_len(k))
    items
  }

  data.frame(
    make_items(BTP, "BTP", 5), make_items(DAC, "DAC", 4),
    make_items(DTC, "DTC", 5), make_items(MDS, "MDS", 5),
    make_items(DPR, "DPR", 7), make_items(CSC, "CSC", 5),
    make_items(EXP, "EXP", 4), make_items(RU, "RU", 3),
    make_items(MV, "MV", 3),
    SIZE    = log(round(exp(rnorm(n, 5.5, 1)))),
    TIER1   = rbinom(n, 1, 0.7),           # 1 = finished goods, 0 = intermediate
    VI      = sample(1:6, n, replace = TRUE),
    CERT    = rpois(n, 2),
    EUSHARE = round(runif(n, 5, 90))
  )
}

args <- commandArgs(trailingOnly = TRUE)
if (length(args) >= 1) {
  survey <- read.csv(args[1])
  survey$SIZE  <- log(survey$SIZE)           # raw employee count in the CSV
  survey$TIER1 <- as.integer(survey$TIER == 1)
  synthetic <- FALSE
} else {
  survey <- simulate_survey()
  synthetic <- TRUE
  message("*** Running on SYNTHETIC data: pipeline check only, not results ***")
}

# ---- 2. Measurement model ----------------------------------------------------
# Reflective constructs are estimated as mode A composites (standard PLS-SEM).

mm <- constructs(
  composite("BTP", multi_items("BTP", 1:5)),
  composite("DAC", multi_items("DAC", 1:4)),
  composite("DTC", multi_items("DTC", 1:5)),
  composite("MDS", multi_items("MDS", 1:5)),
  composite("DPR", multi_items("DPR", 1:7)),
  composite("CSC", multi_items("CSC", 1:5)),
  composite("EXP", multi_items("EXP", 1:4)),
  composite("RU",  multi_items("RU", 1:3)),
  composite("SIZE",    single_item("SIZE")),
  composite("TIER1",   single_item("TIER1")),
  composite("VI",      single_item("VI")),
  composite("CERT",    single_item("CERT")),
  composite("EUSHARE", single_item("EUSHARE")),
  interaction_term(iv = "BTP", moderator = "DAC", method = two_stage),
  interaction_term(iv = "DTC", moderator = "MDS", method = two_stage)
)

# ---- 3. Structural model (H1-H6 + controls) ----------------------------------

controls <- c("SIZE", "TIER1", "VI", "CERT", "EUSHARE", "RU")

sm <- relationships(
  paths(from = c("BTP", "SIZE", "CERT"), to = "DTC"),                       # H1a
  paths(from = c("BTP", "DAC", "BTP*DAC", "TIER1", "VI"), to = "MDS"),       # H1b, H2
  paths(from = c("DTC", "MDS", "DTC*MDS", controls), to = "DPR"),            # H3a, H3b, H4
  paths(from = c("DPR", "BTP", "SIZE", "VI"), to = "CSC"),                   # H5a/H5b (BTP direct = decoupling test)
  paths(from = c("DPR", "SIZE", "EUSHARE"), to = "EXP")                      # H6
)

# ---- 4. Estimate -------------------------------------------------------------

pls <- estimate_pls(data = survey, measurement_model = mm, structural_model = sm)
pls_sum <- summary(pls)

cat("\n== Reliability (alpha, rhoC, AVE, rhoA) ==\n"); print(pls_sum$reliability)
cat("\n== Outer loadings ==\n");                     print(pls_sum$loadings)
cat("\n== HTMT ==\n");                               print(pls_sum$validity$htmt)
cat("\n== Inner VIF ==\n");                          print(pls_sum$vif_antecedents)
cat("\n== Paths, R2 ==\n");                          print(pls_sum$paths)
cat("\n== f2 ==\n");                                 print(pls_sum$fSquare)

# ---- 5. Bootstrap ------------------------------------------------------------

nboot <- if (synthetic) 500 else 10000
boot <- bootstrap_model(seminr_model = pls, nboot = nboot, seed = 2027)
boot_sum <- summary(boot, alpha = 0.05)
cat("\n== Bootstrapped paths ==\n"); print(boot_sum$bootstrapped_paths)
cat("\n== Bootstrapped HTMT ==\n");  print(boot_sum$bootstrapped_HTMT)

# Specific indirect effects
for (p in list(c("BTP", "DTC", "DPR"), c("BTP", "MDS", "DPR"))) {
  cat("\nIndirect", paste(p, collapse = " -> "), "\n")
  print(specific_effect_significance(boot, from = p[1], through = p[2], to = p[3], alpha = 0.05))
}

# ---- 6. Full-collinearity VIF (Kock, 2015) -----------------------------------

scores <- as.data.frame(pls$construct_scores[, c("BTP", "DAC", "DTC", "MDS", "DPR", "CSC", "EXP")])
full_vif <- sapply(names(scores), function(v) {
  r2 <- summary(lm(scores[[v]] ~ ., data = scores[, setdiff(names(scores), v), drop = FALSE]))$r.squared
  1 / (1 - r2)
})
cat("\n== Full-collinearity VIFs (flag > 3.3) ==\n"); print(round(full_vif, 2))

# ---- 7. Prediction (PLSpredict) ----------------------------------------------

# predict_pls may not support interaction terms; if it fails, re-estimate the
# main-effects model (without BTP*DAC and DTC*MDS) and run PLSpredict on that.
pred <- tryCatch(
  predict_pls(model = pls, technique = predict_DA, noFolds = 10, reps = 10),
  error = function(e) { message("PLSpredict failed: ", conditionMessage(e)); NULL }
)
if (!is.null(pred)) { cat("\n== PLSpredict ==\n"); print(summary(pred)) }

if (synthetic) message("\n*** Reminder: SYNTHETIC data. Do not report these numbers. ***")
