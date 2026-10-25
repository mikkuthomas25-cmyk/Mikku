# fsQCA analysis for "Who Carries the Passport?" (JBL STF To the Point! 2027)
#
# Usage:
#   Rscript fsqca.R                        # runs on SYNTHETIC data (pipeline check only)
#   Rscript fsqca.R path/to/interviews.csv # real data: one row per firm, item columns as in 03_survey_instrument.md
#
# SYNTHETIC DATA ARE FOR TESTING THE CODE ONLY. They must never be reported as results.
#
# NOTE: written against the QCA package (>= 3.x) but not yet executed. R was not
# available in the drafting environment, so run it once and fix any API drift.

suppressPackageStartupMessages(library(QCA))
set.seed(2027)

# ---- 1. Data: raw measures ---------------------------------------------------

simulate_firms <- function(n = 50) {
  VI  <- sample(1:6, n, replace = TRUE)
  DTC <- rbinom(n, 5, 0.45)
  MDS <- round(runif(n, 0, 100))
  BTP <- round(runif(n, 1, 7), 1)
  DAC <- round(runif(n, 1, 7), 1)
  # Readiness rises with DTC and with either integration or sharing (illustrative only)
  lat <- 0.5 * DTC + 0.5 * pmax(VI - 1, MDS / 20) - 0.2 * pmax(DAC - 4, 0) + rnorm(n, 0, 0.8)
  DPR <- pmin(7, pmax(0, round(lat)))
  CSC <- pmin(5, pmax(0, round(0.3 * DPR + 0.3 * VI + rnorm(n, 0, 1))))
  CERT <- round(runif(n, 0, 95))          # % of EU shipments under transaction certificates
  SIZE <- round(exp(rnorm(n, 6, 1)))
  TIER <- sample(1:4, n, replace = TRUE, prob = c(0.6, 0.25, 0.1, 0.05))
  CSCF <- pmin(3, pmax(0, CSC - 2))
  data.frame(firm = sprintf("F%02d", 1:n), VI, DTC, MDS, BTP, DAC, DPR, CSC, CSCF, CERT, SIZE, TIER)
}

build_raw <- function(d) {
  # Turn questionnaire items into the raw measures defined in Online Appendix A
  data.frame(
    firm = d$firm,
    VI   = rowSums(d[, paste0("VI_", 1:6)] == "Y"),
    DTC  = rowSums(d[, paste0("DTC_", 1:5)] == "Y"),
    MDS  = 100 * rowMeans(d[, paste0("MDS_", 1:3)]) / d$MDS_N,
    BTP  = rowMeans(d[, paste0("BTP_", 1:3)], na.rm = TRUE),
    DAC  = rowMeans(d[, paste0("DAC_", 1:3)], na.rm = TRUE),
    DPR  = rowSums(d[, paste0("DPR_", 1:7)] == "Y"),
    CSC  = rowSums(d[, paste0("CSC_", 1:5)] == "Y"),
    CSCF = rowSums(d[, c("CSC_2", "CSC_3", "CSC_5")] == "Y"),
    CERT = d$B7,                                  # % of EU shipments under transaction certificates
    SIZE = d$B2,
    TIER = d$B3                                   # 1 = garments ... 4 = fibre
  )
}

args <- commandArgs(trailingOnly = TRUE)
if (length(args) >= 1) {
  raw <- build_raw(read.csv(args[1], stringsAsFactors = FALSE))
  synthetic <- FALSE
  cat("Alpha BTP:", round(psych::alpha(read.csv(args[1])[, paste0("BTP_", 1:3)])$total$raw_alpha, 2), "\n")
  cat("Alpha DAC:", round(psych::alpha(read.csv(args[1])[, paste0("DAC_", 1:3)])$total$raw_alpha, 2), "\n")
} else {
  raw <- simulate_firms()
  synthetic <- TRUE
  message("*** Running on SYNTHETIC data: pipeline check only, not results ***")
}

# ---- 2. Calibration (direct method; anchors from Online Appendix A.1) --------

anchors <- list(
  DPR  = c(e = 1,  c = 3.5, i = 6),
  CSC  = c(e = 0,  c = 2.5, i = 4),
  DTC  = c(e = 1,  c = 2.5, i = 4),
  VI   = c(e = 1,  c = 3.5, i = 5),
  MDS  = c(e = 10, c = 50,  i = 90),
  BTP  = c(e = 2,  c = 4,   i = 6),
  DAC  = c(e = 2,  c = 4,   i = 6),
  CERT = c(e = 5,  c = 50,  i = 90),
  SIZE = c(e = 50, c = 250, i = 1000),
  CSCF = c(e = 0,  c = 1.5, i = 3)
)

calibrate_all <- function(raw, anchors, shift = 0) {
  out <- data.frame(row.names = raw$firm)
  for (v in names(anchors)) {
    a <- anchors[[v]]
    a <- a + shift * (a["i"] - a["e"])          # robustness R1: shift all anchors
    m <- calibrate(raw[[v]], type = "fuzzy",
                   thresholds = sprintf("e=%g, c=%g, i=%g", a["e"], a["c"], a["i"]))
    m[m == 0.5] <- 0.501                        # avoid exact crossover (Fiss, 2011)
    out[[v]] <- round(m, 3)
  }
  out
}

fs <- calibrate_all(raw, anchors)
cat("\n== Share of cases above crossover ==\n"); print(round(colMeans(fs > 0.5), 2))

conds <- "VI, DTC, MDS, CERT, DAC"   # BTP is a background condition (robustness R4, R7)

# ---- 3. Necessity (P2) -------------------------------------------------------

cat("\n== Necessity for DPR (consistency >= .90, RoN >= .60) ==\n")
print(pof("VI + DTC + MDS + CERT + DAC + ~VI + ~DTC + ~MDS + ~CERT + ~DAC",
          outcome = "DPR", data = fs, relation = "necessity"))
cat("\n== Necessity for ~DPR ==\n")
print(pof("VI + DTC + MDS + CERT + DAC + ~VI + ~DTC + ~MDS + ~CERT + ~DAC",
          outcome = "~DPR", data = fs, relation = "necessity"))

# ---- 4. Sufficiency: high and low readiness (P1, P3) -------------------------

run_suff <- function(fs, outcome, dir_exp, incl = 0.80, pri = 0.70, ncut = 1, conds = "VI, DTC, MDS, CERT, DAC") {
  tt <- truthTable(fs, outcome = outcome, conditions = conds,
                   incl.cut = incl, pri.cut = pri, n.cut = ncut,
                   complete = TRUE, show.cases = TRUE, sort.by = c("OUT", "incl"))
  list(
    tt  = tt,
    con = minimize(tt, details = TRUE),                                   # conservative
    par = minimize(tt, include = "?", details = TRUE),                    # parsimonious
    int = minimize(tt, include = "?", dir.exp = dir_exp, details = TRUE)  # intermediate (reported)
  )
}

high <- run_suff(fs, "DPR",  dir_exp = "VI, DTC, MDS, CERT, ~DAC")
low  <- run_suff(fs, "~DPR", dir_exp = "~VI, ~DTC, ~MDS, ~CERT, DAC")

cat("\n== Truth table: high DPR ==\n"); print(high$tt)
cat("\nRows with >= 1 case:", sum(high$tt$tt$n >= 1), "of 32\n")
cat("\n== Intermediate solution: high DPR ==\n"); print(high$int)
cat("\n== Parsimonious solution: high DPR (core conditions) ==\n"); print(high$par)
cat("\n== Intermediate solution: low DPR ==\n"); print(low$int)
cat("\n== Parsimonious solution: low DPR ==\n"); print(low$par)

# ---- 5. Readiness and circularity (P4) ---------------------------------------

cat("\n== Is DPR alone sufficient for CSC? (P4 expects consistency < .80) ==\n")
print(pof("DPR", outcome = "CSC", data = fs, relation = "sufficiency"))
circ <- run_suff(fs, "CSC", dir_exp = "DPR, VI, MDS, CERT, SIZE", conds = "DPR, VI, MDS, CERT, SIZE")
circf <- run_suff(fs, "CSCF", dir_exp = "DPR, VI, MDS, CERT, SIZE", conds = "DPR, VI, MDS, CERT, SIZE")
cat("\n== Intermediate solution: formal circularity (CSCF) ==\n"); print(circf$int)
cat("\n== Intermediate solution: high CSC ==\n"); print(circ$int)

# ---- 6. Robustness (Online Appendix B.3) -------------------------------------

summarise_solution <- function(sol) {
  ic <- sol$IC$sol.incl.cov
  if (is.null(ic)) return(c(incl = NA, cov = NA))
  c(incl = round(ic[1, "inclS"], 3), cov = round(ic[1, "covS"], 3))
}

rob <- list(
  base   = high$int,
  R1_up  = run_suff(calibrate_all(raw, anchors, shift = 0.10),  "DPR", "VI, DTC, MDS, CERT, ~DAC")$int,
  R1_dn  = run_suff(calibrate_all(raw, anchors, shift = -0.10), "DPR", "VI, DTC, MDS, CERT, ~DAC")$int,
  R2_75  = run_suff(fs, "DPR", "VI, DTC, MDS, CERT, ~DAC", incl = 0.75)$int,
  R2_85  = run_suff(fs, "DPR", "VI, DTC, MDS, CERT, ~DAC", incl = 0.85)$int,
  R3_n2  = run_suff(fs, "DPR", "VI, DTC, MDS, CERT, ~DAC", ncut = 2)$int,
  R4_btp = run_suff(fs, "DPR", "VI, DTC, MDS, BTP, ~DAC", conds = "VI, DTC, MDS, BTP, DAC")$int,
  R7_btp6 = run_suff(fs, "DPR", "VI, DTC, MDS, CERT, ~DAC, BTP", conds = "VI, DTC, MDS, CERT, DAC, BTP")$int,
  R5_tier12 = run_suff(fs[raw$TIER <= 2, ], "DPR", "VI, DTC, MDS, CERT, ~DAC")$int
)
cat("\n== Robustness: solution consistency / coverage ==\n")
print(t(sapply(rob, function(s) tryCatch(summarise_solution(s), error = function(e) c(incl = NA, cov = NA)))))
for (nm in names(rob)) { cat("\n--", nm, "--\n"); tryCatch(print(rob[[nm]]$solution), error = function(e) cat("no solution\n")) }

if (synthetic) message("\n*** Reminder: SYNTHETIC data. Do not report these numbers. ***")
