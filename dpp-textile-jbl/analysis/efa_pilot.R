# Exploratory factor analysis of the NEW scales on the pilot sample (Online Appendix A.1, step 5)
#
# Usage:  Rscript efa_pilot.R path/to/pilot.csv
# Items:  DAC1-4, DPR1-7, RU1-3 (new) + BTP1-5 (adapted, discriminant anchor)
#
# NOTE: not yet executed. R was not available in the drafting environment.

suppressPackageStartupMessages(library(psych))

args <- commandArgs(trailingOnly = TRUE)
if (length(args) < 1) stop("Give the pilot CSV path, e.g. Rscript efa_pilot.R pilot.csv")
pilot <- read.csv(args[1])

items <- c(paste0("DAC", 1:4), paste0("DPR", 1:7), paste0("RU", 1:3), paste0("BTP", 1:5))
x <- na.omit(pilot[, items])
cat("Complete cases:", nrow(x), "(target >= 100)\n")

# Suitability
cat("\n== KMO (overall >= 0.70) ==\n"); print(KMO(x)$MSA)
cat("\n== Bartlett's test ==\n");       print(cortest.bartlett(cor(x), n = nrow(x)))

# Number of factors: parallel analysis (principal axis)
pa <- fa.parallel(x, fm = "pa", fa = "fa", plot = FALSE)
k <- pa$nfact
cat("\nParallel analysis suggests", k, "factors (theory expects 4: DAC, DPR, RU, BTP)\n")

# EFA: principal axis factoring, promax (oblique) rotation
efa <- fa(x, nfactors = k, fm = "pa", rotate = "promax")
cat("\n== Pattern loadings (show >= 0.30) ==\n"); print(efa$loadings, cutoff = 0.30, sort = TRUE)
cat("\n== Communalities (flag < 0.40) ==\n");      print(round(efa$communality, 2))
cat("\n== Factor correlations ==\n");              print(round(efa$Phi, 2))

# Flag items breaking the retention rules: primary loading >= 0.50, cross-loading < 0.30
L <- unclass(efa$loadings)
flag <- apply(abs(L), 1, function(r) {
  s <- sort(r, decreasing = TRUE)
  s[1] < 0.50 || (length(s) > 1 && s[2] >= 0.30)
})
cat("\nItems to review (drop one at a time, then rerun):", paste(names(flag)[flag], collapse = ", "), "\n")

# Reliability of each new scale
for (sc in list(DAC = paste0("DAC", 1:4), DPR = paste0("DPR", 1:7), RU = paste0("RU", 1:3))) {
  cat("\nalpha:", round(alpha(x[, sc])$total$raw_alpha, 2),
      " omega:", round(omega(x[, sc], nfactors = 1, plot = FALSE)$omega.tot, 2), "\n")
}
