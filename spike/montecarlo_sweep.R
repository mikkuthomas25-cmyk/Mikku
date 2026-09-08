###############################################################################
## Expanded Monte Carlo: coverage & interval width across a wide range of sample
## sizes, with Monte Carlo standard errors, for the paper's simulation table.
## Two regimes x six sample sizes x three interval methods.
## Run:  Rscript spike/montecarlo_sweep.R   (writes spike/montecarlo_sweep_results.csv)
###############################################################################
options(spike_no_run = TRUE)
source("/home/user/Mikku/spike/conformal_plssem_spike.R")
set.seed(20260908)

regimes <- list(
  skew_hetero = list(y_noise="skew_hetero", loadings=0.85, gamma=c(X1=0.50,X2=0.45),
                     beta=c(M=0.60,X1=0.40), het_a=0.25, het_b=1.8),
  clean       = list(y_noise="normal")
)
methods <- list(
  naive_cv = function(d) naive_cv(d, mm, sm, "Y"),        # honest normal baseline (k-fold RMSE)
  split    = function(d) split_conformal(d, mm, sm, "Y"), # split conformal
  cvplus   = function(d) cvplus(d, mm, sm, "Y")           # CV+
)
grid_n   <- c(50, 100, 200, 400, 800, 1500)
reps_for <- function(n) if (n<=100) 400 else if (n<=200) 300 else if (n<=400) 200 else if (n<=800) 120 else 80

run_cell <- function(reps, n, rp, mfun) {
  covs <- wids <- numeric(reps)
  for (r in 1:reps) { d <- do.call(simulate_data, c(list(n=n), rp)); M <- mfun(d)
    covs[r] <- mean(M["coverage",]); wids[r] <- mean(M["width",]) }
  c(coverage=mean(covs), cov_se=sd(covs)/sqrt(reps), width=mean(wids))
}

rows <- list(); t0 <- Sys.time()
for (rg in names(regimes)) for (n in grid_n) {
  reps <- reps_for(n)
  for (nm in names(methods)) {
    s <- run_cell(reps, n, regimes[[rg]], methods[[nm]])
    rows[[length(rows)+1]] <- data.frame(regime=rg, n=n, reps=reps, method=nm,
      coverage=round(s["coverage"],3), cov_se=round(s["cov_se"],3), width=round(s["width"],2),
      row.names=NULL)
    cat(sprintf("done: %-11s n=%-4d %-9s cov=%.3f (se %.3f) width=%.2f\n",
                rg, n, nm, s["coverage"], s["cov_se"], s["width"]))
  }
}
res <- do.call(rbind, rows)
cat("\n================ SUMMARY (nominal coverage = 0.90) ================\n")
print(res, row.names=FALSE)
write.csv(res, "/home/user/Mikku/spike/montecarlo_sweep_results.csv", row.names=FALSE)
cat(sprintf("\nWrote spike/montecarlo_sweep_results.csv | elapsed %.1f min\n",
            as.numeric(difftime(Sys.time(), t0, units="mins"))))
