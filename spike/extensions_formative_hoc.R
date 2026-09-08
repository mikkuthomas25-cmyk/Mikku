###############################################################################
## Extensions: conformal prediction intervals for PLS-SEM with
##   (1) a FORMATIVE (mode B) construct, and
##   (2) a HIGHER-ORDER construct (disjoint two-stage),
## plus a WORKED EXAMPLE printing actual per-respondent loyalty intervals.
##
## Reuses the engine + conformal methods from conformal_plssem_spike.R via the
## pluggable fit_fun/pred_fun interface (no duplicated conformal code).
## Run:  Rscript extensions_formative_hoc.R
###############################################################################

options(spike_no_run = TRUE)                     # source engine, do not run its driver
source("conformal_plssem_spike.R")
set.seed(20260908)

## ===========================================================================
## (1) FORMATIVE construct.  F (mode B, 3 formative indicators) and X2 (reflective)
##     -> M -> Y (+ F -> Y). Target = Y's reflective indicators.
## ===========================================================================
mm_f <- list(F=c("f1","f2","f3"), X2=c("x2_1","x2_2","x2_3"),
             M=c("m_1","m_2","m_3"), Y=c("y_1","y_2","y_3"))
sm_f <- rbind(c("F","M"), c("X2","M"), c("F","Y"), c("M","Y"))
fit_f <- function(d) estimate_pls(d, mm_f, sm_f, modes=c(F="B"))

sim_formative <- function(n, loadings=0.75, skew=TRUE) {
  Sig <- matrix(0.4, 3, 3); diag(Sig) <- 1                 # correlated formative indicators
  Fx  <- matrix(rnorm(n*3), n) %*% chol(Sig); colnames(Fx) <- c("f1","f2","f3")
  Ftrue <- as.numeric(scale(Fx %*% c(0.5,0.4,0.3)))        # construct is CAUSED by indicators
  L2 <- rnorm(n)
  M  <- 0.45*Ftrue + 0.35*L2 + sqrt(1-0.45^2-0.35^2)*rnorm(n)
  Y  <- 0.50*M + 0.25*Ftrue + sqrt(max(1e-6,1-0.5^2-0.25^2))*rnorm(n)
  refl <- function(e) sapply(1:3, function(j) loadings*e + sqrt(1-loadings^2)*rnorm(length(e)))
  reflY<- function(e) sapply(1:3, function(j){ s<-if(skew) 0.3+1.5*abs(e) else 1
    r<-if(skew) rexp(length(e))-1 else rnorm(length(e)); loadings*e + sqrt(1-loadings^2)*s*r })
  d <- data.frame(Fx, refl(L2), refl(M), reflY(Y))
  names(d) <- unlist(mm_f); d
}

## ===========================================================================
## (2) HIGHER-ORDER construct via disjoint TWO-STAGE (Sarstedt et al. 2019).
##     HOC is a 2nd-order construct with two reflective lower-order components
##     LOC1 (a1..a3), LOC2 (b1..b3); HOC -> Y (reflective). Target = Y.
##     Stage 1: estimate LOCs -> get LOC scores.  Stage 2: HOC measured by the
##     two LOC scores -> Y.  Prediction propagates through both stages.
## ===========================================================================
ycols <- c("y_1","y_2","y_3")
fit_hoc <- function(d) {
  mm1 <- list(LOC1=c("a1","a2","a3"), LOC2=c("b1","b2","b3"), Y=ycols)
  m1  <- estimate_pls(d, mm1, rbind(c("LOC1","Y"), c("LOC2","Y")))
  S1  <- lv_scores_newdata(m1, d)
  d2  <- data.frame(LOC1=S1[,"LOC1"], LOC2=S1[,"LOC2"], d[, ycols])
  m2  <- estimate_pls(d2, list(HOC=c("LOC1","LOC2"), Y=ycols), rbind(c("HOC","Y")))
  list(m1=m1, m2=m2)
}
pred_hoc <- function(model, newdata, target="Y") {
  S1 <- lv_scores_newdata(model$m1, newdata)
  d2 <- data.frame(LOC1=S1[,"LOC1"], LOC2=S1[,"LOC2"], newdata[, ycols])
  predict_indicators(model$m2, d2, "Y")
}
mm_hoc <- list(Y=ycols)                             # cvplus only needs target indicators here

sim_hoc <- function(n, loadings=0.80, skew=TRUE) {
  HOC <- rnorm(n)
  LOC1<- 0.85*HOC + sqrt(1-0.85^2)*rnorm(n); LOC2<- 0.85*HOC + sqrt(1-0.85^2)*rnorm(n)
  Y   <- 0.55*HOC + sqrt(1-0.55^2)*rnorm(n)
  refl<- function(e) sapply(1:3, function(j) loadings*e + sqrt(1-loadings^2)*rnorm(length(e)))
  reflY<-function(e) sapply(1:3, function(j){ s<-if(skew) 0.3+1.5*abs(e) else 1
    r<-if(skew) rexp(length(e))-1 else rnorm(length(e)); loadings*e + sqrt(1-loadings^2)*s*r })
  d <- data.frame(refl(LOC1), refl(LOC2), reflY(Y))
  names(d) <- c("a1","a2","a3","b1","b2","b3", ycols); d
}

## small Monte Carlo coverage helper (marginal coverage + width, averaged over indicators)
mc_cov <- function(reps, simfun, methods, alpha=0.10) {
  agg <- setNames(lapply(names(methods), function(.) c(0,0)), names(methods))
  for (r in 1:reps) { d <- simfun()
    for (nm in names(methods)) { M <- methods[[nm]](d)
      agg[[nm]] <- agg[[nm]] + c(mean(M["coverage",]), mean(M["width",])) } }
  do.call(rbind, lapply(names(methods), function(nm) data.frame(method=nm,
    marginal_cov=round(agg[[nm]][1]/reps,3), mean_width=round(agg[[nm]][2]/reps,3), row.names=NULL)))
}

cat(sprintf("Extensions | R %s | nominal 0.90\n\n", getRversion()))

cat("=== (1) FORMATIVE construct (mode B), n=150, skewed noise ===\n")
print(mc_cov(150, function() sim_formative(150), list(
  naive_cv  = function(d) naive_cv (d, mm_f, sm_f, "Y", fit_fun=fit_f),
  cvplus    = function(d) cvplus   (d, mm_f, sm_f, "Y", fit_fun=fit_f),
  jackknife = function(d) jackknife_plus(d, mm_f, sm_f, "Y", fit_fun=fit_f))), row.names=FALSE)

cat("\n=== (2) HIGHER-ORDER construct (disjoint two-stage), n=250, skewed noise ===\n")
print(mc_cov(120, function() sim_hoc(250), list(
  naive_cv = function(d) naive_cv(d, mm_hoc, NULL, "Y", fit_fun=fit_hoc, pred_fun=pred_hoc),
  cvplus   = function(d) cvplus  (d, mm_hoc, NULL, "Y", fit_fun=fit_hoc, pred_fun=pred_hoc))), row.names=FALSE)

## ===========================================================================
## WORKED EXAMPLE -- actual per-respondent 90% prediction intervals (CV+),
## higher-order model, one dataset, loyalty item rescaled to a 1-7 survey scale.
## ===========================================================================
case_intervals <- function(train, test, target, indicator, alpha=0.10, K=10, fit_fun, pred_fun) {
  ntrn<-nrow(train); folds<-sample(rep(1:K, length.out=ntrn))
  R<-numeric(ntrn); fp<-matrix(NA_real_, nrow(test), K)
  for (f in 1:K){ trn<-which(folds!=f); val<-which(folds==f)
    mf<-fit_fun(train[trn,]); pv<-pred_fun(mf,train[val,],target); pT<-pred_fun(mf,test,target)
    R[val]<-abs(train[val,indicator]-pv[,indicator]); fp[,f]<-pT[,indicator] }
  li<-max(1,min(ntrn,floor(alpha*(ntrn+1)))); hi<-max(1,min(ntrn,ceiling((1-alpha)*(ntrn+1))))
  lo<-up<-numeric(nrow(test)); pbar<-rowMeans(fp)
  for (t in 1:nrow(test)){ mu<-fp[t,folds]; lo[t]<-sort(mu-R)[li]; up[t]<-sort(mu+R)[hi] }
  clip<-function(x) pmin(7, pmax(1, x))
  data.frame(pred=round(clip(pbar),2), lo=round(clip(lo),2), hi=round(clip(up),2),
             actual=round(test[,indicator],2))
}

cat("\n=== WORKED EXAMPLE: per-respondent loyalty prediction intervals (HOC model) ===\n")
d <- sim_hoc(300)
for (k in ycols) d[[k]] <- pmin(7, pmax(1, round(4.5 + 0.9*d[[k]], 2)))   # -> 1-7 survey scale
idx <- sample(nrow(d)); tr <- d[idx[1:250],]; te <- d[idx[251:300],]
ci  <- case_intervals(tr, te, "Y", "y_1", alpha=0.10, K=10, fit_fun=fit_hoc, pred_fun=pred_hoc)
cat("Loyalty item y_1 (1-7). Each row is one held-out respondent:\n")
print(head(ci, 6), row.names=FALSE)
cat(sprintf("\nHeld-out coverage of the 90%% intervals over all %d test respondents: %.2f\n",
            nrow(te), mean(te$y_1 >= ci$lo & te$y_1 <= ci$hi)))
cat(sprintf("Mean interval width: %.2f points on the 1-7 scale.\n", mean(ci$hi - ci$lo)))
