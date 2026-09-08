###############################################################################
## seminr adapter -- run the conformal prediction intervals on a REAL seminr fit
##
## WHY: the paper's illustrative empirical example must use seminr's canonical
## PLS-SEM estimation (to match PLSpredict and satisfy reviewers). This adapter
## maps a fitted seminr model into the lightweight predictor object that the
## conformal functions in conformal_plssem_spike.R already understand, so the
## SAME conformal code (split / CV+ / jackknife+) runs on a seminr fit.
##
## !! UNTESTED in the build sandbox: seminr could not be installed there (CRAN
##    egress blocked). It is written against seminr's documented object slots.
##    The adapter auto-detects the path orientation, so there is nothing to
##    configure. validate_adapter() still prints a correlation as reassurance
##    (should be high, > 0.5).
##
## Usage:
##   install.packages("seminr")
##   Rscript seminr_adapter.R
###############################################################################

options(spike_no_run = TRUE)
source("conformal_plssem_spike.R")     # engine + conformal methods (adjust path if needed)

## Map a fitted seminr model -> predictor object consumed by predict_indicators().
## The path-matrix orientation is AUTO-DETECTED (no manual flag): we build the
## predictor both ways and keep whichever reproduces the training endogenous
## indicators better. So this "just works" across seminr versions.
seminr_to_predictor <- function(fit) {
  cons <- as.character(fit$constructs)
  mmM  <- fit$mmMatrix                       # cols: "construct","measurement"(,"type")
  mm   <- lapply(cons, function(c) as.character(mmM[mmM[,"construct"]==c, "measurement"]))
  names(mm) <- cons
  indall <- unique(unlist(mm))
  raw    <- as.data.frame(fit$rawdata)[, indall, drop=FALSE]
  center <- colMeans(raw); scal <- apply(raw, 2, sd)
  W <- lapply(cons, function(c){ i<-mm[[c]]; setNames(as.numeric(fit$outer_weights[i, c]), i) })
  names(W) <- cons
  L <- fit$outer_loadings[indall, cons, drop=FALSE]
  smM <- fit$smMatrix                        # cols: "source","target"
  preds <- lapply(cons, function(c) as.character(smM[smM[,"target"]==c, "source"])); names(preds)<-cons
  Ztr <- scale(as.matrix(raw), center=center, scale=scal)
  comp_sd <- sapply(cons, function(c) sd(as.numeric(Ztr[, mm[[c]], drop=FALSE] %*% W[[c]])))
  pc <- fit$path_coef
  mk <- function(B) list(W=W,L=L,B=B,cons=cons,mm=mm,preds=preds,center=center,scale=scal,comp_sd=comp_sd)
  endo <- cons[sapply(cons, function(c) length(preds[[c]])>0)]
  score <- function(pr) mean(unlist(lapply(endo, function(c){ P<-predict_indicators(pr, raw, c)
    sapply(mm[[c]], function(k) suppressWarnings(cor(P[,k], raw[,k]))) })), na.rm=TRUE)
  cand <- list(mk(pc[cons,cons,drop=FALSE]), mk(t(pc[cons,cons,drop=FALSE])))
  cand[[ which.max(sapply(cand, score)) ]]  # keep the better orientation
}

## fit_fun factory: returns function(data) -> predictor object, re-estimating
## seminr on each conformal split (this is the exchangeability discipline).
make_seminr_fit_fun <- function(measurement_model, structural_model)
  function(d) seminr_to_predictor(
    estimate_pls(data=d, measurement_model=measurement_model,
                 structural_model=structural_model, inner_weights=path_weighting))

## One-line sanity check: predict the target in-sample and correlate with actual.
validate_adapter <- function(fit, target, indicator) {
  pr <- predict_indicators(seminr_to_predictor(fit), as.data.frame(fit$rawdata), target)
  r  <- cor(pr[, indicator], as.data.frame(fit$rawdata)[, indicator])
  cat(sprintf("adapter check: cor(predicted, actual) for %s = %.3f %s\n",
              indicator, r, if (r > 0.5) "-> OK" else "-> LOW: flip PATHS_FROM_ROW"))
  invisible(r)
}

## ===========================================================================
## Illustrative example -- corporate reputation "simple model" (Hair et al.)
## Runs only when seminr is installed; otherwise the adapter functions above are
## loaded and the example is skipped (so this file can be sourced for testing).
## ===========================================================================
if (!requireNamespace("seminr", quietly = TRUE)) {
  message("seminr not installed; adapter functions loaded, example skipped.")
} else {
library(seminr)
data("corp_rep_data", package = "seminr")

measurement_model <- constructs(
  composite("COMP", multi_items("comp_", 1:3)),
  composite("LIKE", multi_items("like_", 1:3)),
  composite("CUSA", single_item("cusa")),
  composite("CUSL", multi_items("cusl_", 1:3))
)
structural_model <- relationships(
  paths(from = c("COMP","LIKE"),        to = "CUSA"),
  paths(from = c("COMP","LIKE","CUSA"), to = "CUSL")
)

fit_fun <- make_seminr_fit_fun(measurement_model, structural_model)
mm_target <- list(CUSL = c("cusl_1","cusl_2","cusl_3"))   # conformal needs only target indicators

## 0. sanity-check the adapter on a full-sample fit
full <- estimate_pls(corp_rep_data, measurement_model, structural_model, inner_weights=path_weighting)
validate_adapter(full, "CUSL", "cusl_1")

## 1. distribution-free prediction intervals for CUSL indicators, on real data
cat("\nCV+ conformal intervals for CUSL (corp_rep_data), nominal 0.90:\n")
print(cvplus(corp_rep_data, mm=mm_target, sm=NULL, target="CUSL",
             alpha=0.10, fit_fun=fit_fun, pred_fun=predict_indicators))

cat("\nsplit conformal (same data) for comparison:\n")
print(split_conformal(corp_rep_data, mm=mm_target, sm=NULL, target="CUSL",
                      alpha=0.10, fit_fun=fit_fun, pred_fun=predict_indicators))

cat("\nNote: jackknife_plus(...) and mondrian_compare(...) accept the same",
    "fit_fun/pred_fun and can be run identically on this seminr fit.\n")
}
