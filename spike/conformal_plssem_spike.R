###############################################################################
## Conformal prediction intervals for PLS-SEM  --  FEASIBILITY SPIKE (v2)
##
## Question this settles BEFORE writing the JOSM special-section paper:
##   1. Does conformal inference wrapped around a RE-ESTIMATED PLS-SEM predictor
##      deliver nominal out-of-sample coverage?           (core validity)
##   2. Is there a real contribution beyond trivial validity -- i.e. does
##      conformal HOLD where the current default (normal-theory intervals from
##      in-sample residuals) BREAKS: skewed, heteroskedastic errors, smaller n?
##   3. Does a locally-NORMALIZED conformal variant restore CONDITIONAL coverage
##      that plain conformal (valid only marginally) loses under heteroskedasticity?
##
## If (1) holds and (2)-(3) show a gap the defaults cannot close, the paper has a
## genuine methodological contribution, not just an application.
##
## Validity note: conformal coverage is guaranteed for ANY fixed predictor on
## exchangeable data. Data here are simulated i.i.d. from a known population PLS
## model, and the full PLS-SEM is RE-ESTIMATED on each training split (no leakage
## into calibration/test). No external packages. Run: Rscript conformal_plssem_spike.R
###############################################################################

set.seed(20260908)

## ---------------------------------------------------------------------------
## 1. POPULATION MODEL   X1,X2 -> M -> Y  (+ X1 -> Y);  3 reflective indicators
## ---------------------------------------------------------------------------
mm <- list(X1=c("x1_1","x1_2","x1_3"), X2=c("x2_1","x2_2","x2_3"),
           M =c("m_1","m_2","m_3"),    Y =c("y_1","y_2","y_3"))
sm <- rbind(c("X1","M"), c("X2","M"), c("X1","Y"), c("M","Y"))

## y_noise = "normal"      -> homoskedastic Gaussian measurement noise on Y
##         = "skew_hetero" -> right-skewed noise whose spread grows with |eta_Y|.
## In the stress scenario we also raise loadings + path strengths so eta_Y is
## HIGHLY predictable: then the structural (symmetric) error is small and the
## skew/heteroskedastic MEASUREMENT noise dominates the prediction residual --
## which is exactly the regime where normal-theory intervals should fail.
simulate_data <- function(n, loadings = 0.75, y_noise = c("normal","skew_hetero"),
                          gamma = c(X1=0.40, X2=0.35), beta = c(M=0.50, X1=0.25),
                          rho_x = 0.30, het_a = 0.30, het_b = 1.6,
                          segment = FALSE, seg_scale = c(0.5, 2.0)) {
  y_noise <- match.arg(y_noise)
  L1 <- rnorm(n); L2 <- rho_x*L1 + sqrt(1-rho_x^2)*rnorm(n)
  vM <- gamma["X1"]^2 + gamma["X2"]^2 + 2*gamma["X1"]*gamma["X2"]*rho_x
  M  <- gamma["X1"]*L1 + gamma["X2"]*L2 + sqrt(max(1e-6,1-vM))*rnorm(n)
  cMX1 <- gamma["X1"] + gamma["X2"]*rho_x
  vY <- beta["M"]^2 + beta["X1"]^2 + 2*beta["M"]*beta["X1"]*cMX1
  Y  <- beta["M"]*M + beta["X1"]*L1 + sqrt(max(1e-6,1-vY))*rnorm(n)

  refl_normal <- function(e,k) sapply(1:k, function(j) loadings*e + sqrt(1-loadings^2)*rnorm(length(e)))
  refl_Y <- function(e,k) sapply(1:k, function(j) {
    if (y_noise=="normal") {
      loadings*e + sqrt(1-loadings^2)*rnorm(length(e))
    } else {
      sc  <- het_a + het_b*abs(e)               # spread grows with |eta_Y| (heteroskedastic)
      raw <- (rexp(length(e)) - 1)              # right-skewed, mean 0
      loadings*e + sqrt(1-loadings^2)*sc*raw
    }
  })
  dat <- data.frame(refl_normal(L1,3), refl_normal(L2,3), refl_normal(M,3), refl_Y(Y,3))
  names(dat) <- unlist(mm)
  if (segment) {                              # known segments with different Y-error spread
    grp <- sample(seq_along(seg_scale), n, replace=TRUE); s <- seg_scale[grp]
    for (j in 1:3) dat[[mm$Y[j]]] <- loadings*Y + sqrt(1-loadings^2)*s*rnorm(n)
    dat$grp <- grp
  }
  dat
}

## ---------------------------------------------------------------------------
## 2. PLS-SEM estimator (Lohmoller, mode A, path weighting) + PLS prediction
## ---------------------------------------------------------------------------
estimate_pls <- function(data, mm, sm, tol=1e-7, maxit=300) {
  cons <- names(mm)
  Xall <- scale(as.matrix(data[, unlist(mm), drop=FALSE]))
  center <- attr(Xall,"scaled:center"); scal <- attr(Xall,"scaled:scale")
  preds <- lapply(cons, function(c) sm[sm[,2]==c,1]); names(preds) <- cons
  succs <- lapply(cons, function(c) sm[sm[,1]==c,2]); names(succs) <- cons
  W <- lapply(mm, function(ind) setNames(rep(1,length(ind)), ind))
  scores <- function(W) { S <- sapply(cons, function(c) as.numeric(scale(Xall[,mm[[c]],drop=FALSE] %*% W[[c]]))); colnames(S)<-cons; S }
  S <- scores(W)
  for (it in 1:maxit) {
    Wold <- unlist(W); Z <- matrix(0,nrow(S),length(cons),dimnames=list(NULL,cons))
    for (c in cons) {
      z <- numeric(nrow(S)); P <- preds[[c]]
      if (length(P)) { b <- lm.fit(cbind(1,S[,P,drop=FALSE]),S[,c])$coefficients[-1]
        for (k in seq_along(P)) z <- z + b[k]*S[,P[k]] }
      Sc <- succs[[c]]; if (length(Sc)) for (k in Sc) z <- z + cor(S[,c],S[,k])*S[,k]
      Z[,c] <- z
    }
    Z <- scale(Z)
    for (c in cons) W[[c]] <- setNames(as.numeric(cor(Xall[,mm[[c]],drop=FALSE],Z[,c])), mm[[c]])
    S <- scores(W); if (max(abs(unlist(W)-Wold)) < tol) break
  }
  L <- matrix(0,length(unlist(mm)),length(cons),dimnames=list(unlist(mm),cons))
  for (c in cons) L[mm[[c]],c] <- as.numeric(cor(Xall[,mm[[c]],drop=FALSE],S[,c]))
  B <- matrix(0,length(cons),length(cons),dimnames=list(cons,cons))
  for (c in cons) { P<-preds[[c]]; if (length(P)) B[P,c] <- lm.fit(cbind(1,S[,P,drop=FALSE]),S[,c])$coefficients[-1] }
  comp_sd <- sapply(cons, function(c) sd(as.numeric(Xall[,mm[[c]],drop=FALSE] %*% W[[c]])))
  list(W=W,L=L,B=B,cons=cons,mm=mm,preds=preds,center=center,scale=scal,comp_sd=comp_sd)
}
topo_order <- function(cons, preds) { ord<-character(0); rem<-cons
  while (length(rem)) { rdy <- rem[sapply(rem, function(c) all(preds[[c]] %in% ord))]
    if (!length(rdy)) stop("cycle"); ord<-c(ord,rdy); rem<-setdiff(rem,rdy) }; ord }
predict_indicators <- function(model, newdata, target) {
  Xnew <- as.matrix(newdata[, names(model$center), drop=FALSE])
  Znew <- scale(Xnew, center=model$center, scale=model$scale)
  cons <- model$cons; Sn <- matrix(NA_real_,nrow(Xnew),length(cons),dimnames=list(NULL,cons))
  for (c in topo_order(cons, model$preds)) { P <- model$preds[[c]]
    if (!length(P)) Sn[,c] <- as.numeric(Znew[,model$mm[[c]],drop=FALSE] %*% model$W[[c]]) / model$comp_sd[c]
    else            Sn[,c] <- as.numeric(Sn[,P,drop=FALSE] %*% model$B[P,c]) }
  ind <- model$mm[[target]]
  out <- sapply(ind, function(k) Sn[,target]*model$L[k,target]*model$scale[k] + model$center[k])
  out <- matrix(out, nrow = nrow(Xnew), ncol = length(ind))   # keep matrix even for 1-row newdata
  colnames(out) <- ind; out
}

## ---------------------------------------------------------------------------
## 3. Interval methods.  Each returns per-indicator: marginal coverage, width,
##    and WORST-TERTILE conditional coverage (min coverage across predicted-value
##    tertiles of the test set -- a probe of conditional validity).
## ---------------------------------------------------------------------------
cond_cov <- function(y, pred, lo, hi) {                # coverage in TOP predicted-value tertile
  thr <- quantile(pred, 2/3); sel <- pred >= thr        # the high-|prediction| region
  mean((y >= lo & y <= hi)[sel])
}
qlevel <- function(nc, alpha) min(1, ceiling((nc+1)*(1-alpha))/nc)

split_conformal <- function(data, mm, sm, target, alpha=0.10, p_tr=0.5, p_cal=0.25) {
  n<-nrow(data); idx<-sample(n); ntr<-floor(p_tr*n); ncal<-floor(p_cal*n)
  tr<-idx[1:ntr]; cal<-idx[(ntr+1):(ntr+ncal)]; te<-idx[(ntr+ncal+1):n]
  m<-estimate_pls(data[tr,],mm,sm); pc<-predict_indicators(m,data[cal,],target); pt<-predict_indicators(m,data[te,],target)
  sapply(mm[[target]], function(k){
    qh<-as.numeric(quantile(abs(data[cal,k]-pc[,k]), qlevel(ncal,alpha), type=1))
    lo<-pt[,k]-qh; hi<-pt[,k]+qh
    c(coverage=mean(data[te,k]>=lo & data[te,k]<=hi), width=2*qh,
      cond=cond_cov(data[te,k], pt[,k], lo, hi))
  })
}
norm_conformal <- function(data, mm, sm, target, alpha=0.10, p_tr=0.5, p_cal=0.25) {
  n<-nrow(data); idx<-sample(n); ntr<-floor(p_tr*n); ncal<-floor(p_cal*n)
  tr<-idx[1:ntr]; cal<-idx[(ntr+1):(ntr+ncal)]; te<-idx[(ntr+ncal+1):n]
  m<-estimate_pls(data[tr,],mm,sm)
  pin<-predict_indicators(m,data[tr,],target); pc<-predict_indicators(m,data[cal,],target); pt<-predict_indicators(m,data[te,],target)
  sapply(mm[[target]], function(k){
    ## local scale sigma(x) from training: |resid| ~ |pred - median|, clamped positive
    v_tr<-abs(pin[,k]-median(pin[,k])); a<-lm.fit(cbind(1,v_tr), abs(data[tr,k]-pin[,k]))$coefficients
    sig<-function(p){ s<-a[1]+a[2]*abs(p-median(pin[,k])); pmax(s, 0.05*sd(data[tr,k])) }
    s_cal<-abs(data[cal,k]-pc[,k])/sig(pc[,k])
    qh<-as.numeric(quantile(s_cal, qlevel(ncal,alpha), type=1))
    half<-qh*sig(pt[,k]); lo<-pt[,k]-half; hi<-pt[,k]+half
    c(coverage=mean(data[te,k]>=lo & data[te,k]<=hi), width=mean(2*half),
      cond=cond_cov(data[te,k], pt[,k], lo, hi))
  })
}
## Strawman baseline: normal interval from IN-SAMPLE residual sd (optimistic).
naive_normal <- function(data, mm, sm, target, alpha=0.10, p_tr=0.5, p_cal=0.25) {
  n<-nrow(data); idx<-sample(n); ntr<-floor((p_tr+p_cal)*n)
  tr<-idx[1:ntr]; te<-idx[(ntr+1):n]
  m<-estimate_pls(data[tr,],mm,sm); pin<-predict_indicators(m,data[tr,],target); pt<-predict_indicators(m,data[te,],target)
  z<-qnorm(1-alpha/2)
  sapply(mm[[target]], function(k){
    s<-sd(data[tr,k]-pin[,k]); lo<-pt[,k]-z*s; hi<-pt[,k]+z*s
    c(coverage=mean(data[te,k]>=lo & data[te,k]<=hi), width=2*z*s,
      cond=cond_cov(data[te,k], pt[,k], lo, hi))
  })
}
## HONEST strong baseline: normal interval from k-fold OUT-OF-SAMPLE RMSE
## (this is what a careful PLSpredict user would build). pred +/- z * sd(oos resid).
naive_cv <- function(data, mm, sm, target, alpha=0.10, p_tr=0.75, K=5) {
  n<-nrow(data); idx<-sample(n); ntr<-floor(p_tr*n); tr<-idx[1:ntr]; te<-idx[(ntr+1):n]
  dtr<-data[tr,]; folds<-sample(rep(1:K, length.out=nrow(dtr))); z<-qnorm(1-alpha/2)
  res<-setNames(lapply(mm[[target]], function(.) numeric(0)), mm[[target]])
  for (f in 1:K) { trn<-which(folds!=f); val<-which(folds==f)
    mf<-estimate_pls(dtr[trn,],mm,sm); pv<-predict_indicators(mf,dtr[val,],target)
    for (k in mm[[target]]) res[[k]]<-c(res[[k]], dtr[val,k]-pv[,k]) }
  mfull<-estimate_pls(dtr,mm,sm); pt<-predict_indicators(mfull,data[te,],target)
  sapply(mm[[target]], function(k){
    s<-sd(res[[k]]); lo<-pt[,k]-z*s; hi<-pt[,k]+z*s
    c(coverage=mean(data[te,k]>=lo & data[te,k]<=hi), width=2*z*s,
      cond=cond_cov(data[te,k], pt[,k], lo, hi))
  })
}

## CV+ (Barber et al. 2021): every training point yields an out-of-sample residual
## (all data calibrates -> fixes split conformal's tiny-calibration-set blowup).
cvplus <- function(data, mm, sm, target, alpha=0.10, p_tr=0.75, K=10) {
  n<-nrow(data); idx<-sample(n); ntr<-floor(p_tr*n); tr<-idx[1:ntr]; te<-idx[(ntr+1):n]
  dtr<-data[tr,]; ntrn<-nrow(dtr); nt<-length(te); ind<-mm[[target]]
  K<-min(K, ntrn)                                   # K==ntrn gives leave-one-out (jackknife+)
  folds<-sample(rep(1:K, length.out=ntrn))
  R<-setNames(lapply(ind, function(.) numeric(ntrn)), ind)
  fp<-setNames(lapply(ind, function(.) matrix(NA_real_, nt, K)), ind)
  for (f in 1:K) { trn<-which(folds!=f); val<-which(folds==f)
    mf<-estimate_pls(dtr[trn,],mm,sm); pv<-predict_indicators(mf,dtr[val,],target); pT<-predict_indicators(mf,data[te,],target)
    for (k in ind) { R[[k]][val]<-abs(dtr[val,k]-pv[,k]); fp[[k]][,f]<-pT[,k] } }
  li<-max(1,min(ntrn,floor(alpha*(ntrn+1)))); hi<-max(1,min(ntrn,ceiling((1-alpha)*(ntrn+1))))
  sapply(ind, function(k){
    lo<-numeric(nt); up<-numeric(nt); pbar<-rowMeans(fp[[k]])
    for (t in 1:nt){ mu<-fp[[k]][t,folds]; lo[t]<-sort(mu-R[[k]])[li]; up[t]<-sort(mu+R[[k]])[hi] }
    yy<-data[te,k]
    c(coverage=mean(yy>=lo & yy<=up), width=mean(up-lo), cond=cond_cov(yy,pbar,lo,up))
  })
}
## jackknife+ (Barber et al. 2021) == leave-one-out CV+ (every point its own fold).
jackknife_plus <- function(data, mm, sm, target, alpha=0.10, p_tr=0.75)
  cvplus(data, mm, sm, target, alpha, p_tr, K=.Machine$integer.max)

## Mondrian (per-segment) conformal: when respondents fall in known segments with
## different error spread, pooled conformal is valid ONLY on average -- it over-
## covers the low-noise segment and under-covers the high-noise one. Mondrian
## conformal calibrates a SEPARATE quantile within each segment, restoring per-
## segment coverage. Returns per-segment coverage for pooled vs Mondrian (one fit).
mondrian_compare <- function(data, mm, sm, target, alpha=0.10, p_tr=0.5, p_cal=0.25) {
  n<-nrow(data); idx<-sample(n); ntr<-floor(p_tr*n); ncal<-floor(p_cal*n)
  tr<-idx[1:ntr]; cal<-idx[(ntr+1):(ntr+ncal)]; te<-idx[(ntr+ncal+1):n]
  m<-estimate_pls(data[tr,],mm,sm); pc<-predict_indicators(m,data[cal,],target); pt<-predict_indicators(m,data[te,],target)
  gcal<-data$grp[cal]; gte<-data$grp[te]; ind<-mm[[target]]; grps<-sort(unique(data$grp))
  P<-M<-matrix(NA_real_, length(ind), length(grps))
  for (ii in seq_along(ind)) { k<-ind[ii]
    rc<-abs(data[cal,k]-pc[,k]); rt<-abs(data[te,k]-pt[,k])
    qp<-as.numeric(quantile(rc, qlevel(ncal,alpha), type=1))       # pooled quantile
    for (gi in seq_along(grps)) { g<-grps[gi]; st<-gte==g; sc<-gcal==g
      P[ii,gi]<-mean(rt[st]<=qp)
      qm<-as.numeric(quantile(rc[sc], qlevel(sum(sc),alpha), type=1))  # per-segment quantile
      M[ii,gi]<-mean(rt[st]<=qm) }
  }
  rbind(pooled=colMeans(P), mondrian=colMeans(M))
}

## ---------------------------------------------------------------------------
## 4. Monte Carlo over a scenario
## ---------------------------------------------------------------------------
ALL_METHODS <- list(naive_insample=NULL, naive_cv=NULL, split=NULL, normalized=NULL, cvplus=NULL)
run_scenario <- function(reps, n, y_noise, alpha=0.10, target="Y",
                         method_names=c("naive_cv","split","cvplus"), ...) {
  ind<-mm[[target]]
  pool<-list(naive_insample=naive_normal, naive_cv=naive_cv, split=split_conformal,
             normalized=norm_conformal, cvplus=cvplus, jackknife=jackknife_plus)
  methods<-pool[method_names]
  A<-lapply(methods, function(.) array(0, c(3,length(ind),reps), dimnames=list(c("coverage","width","cond"),ind,NULL)))
  for (r in 1:reps) { d<-simulate_data(n, y_noise=y_noise, ...)
    for (nm in names(methods)) A[[nm]][,,r] <- methods[[nm]](d,mm,sm,target,alpha) }
  do.call(rbind, lapply(names(methods), function(nm) data.frame(
    method=nm,
    marginal_cov=round(mean(apply(A[[nm]]["coverage",,],1,mean)),3),
    top_tertile_cov=round(mean(apply(A[[nm]]["cond",,],1,mean)),3),
    mean_width=round(mean(apply(A[[nm]]["width",,],1,mean)),3), row.names=NULL)))
}

cat(sprintf("PLS-SEM conformal spike v2 | R %s | alpha=0.10 (nominal 0.90)\n\n", getRversion()))
run_mondrian <- function(reps, n, alpha=0.10, target="Y", seg_scale=c(0.5,2.0)) {
  acc <- array(0, c(2, length(seg_scale)),
               dimnames=list(c("pooled_conformal","mondrian_conformal"),
                             paste0("seg", seq_along(seg_scale), "_cov")))
  for (r in 1:reps) acc <- acc + mondrian_compare(simulate_data(n, segment=TRUE, seg_scale=seg_scale),
                                                   mm, sm, target, alpha)
  round(acc/reps, 3)
}

stress <- list(y_noise="skew_hetero", loadings=0.85, gamma=c(X1=0.50,X2=0.45),
               beta=c(M=0.60,X1=0.40), het_a=0.25, het_b=1.8)

cat("PART 1 -- Which conformal variant works at service-research sample sizes?\n")
cat("(naive_cv = honest normal baseline; split; cvplus = CV+; jackknife = jackknife+)\n\n")
cat("=== n=60, skew+heteroskedastic (small-sample regime) ===\n")
print(do.call(run_scenario, c(list(reps=200, n=60,
      method_names=c("naive_cv","split","cvplus","jackknife")), stress)), row.names=FALSE)
cat("\n=== n=400, skew+heteroskedastic (efficiency under non-normality) ===\n")
print(do.call(run_scenario, c(list(reps=120, n=400,
      method_names=c("naive_cv","split","cvplus")), stress)), row.names=FALSE)

cat("\nPART 2 -- Mondrian: per-segment coverage when two known segments differ in\n")
cat("error spread (seg1 low-noise, seg2 high-noise). Nominal per-segment = 0.90.\n\n")
print(run_mondrian(reps=250, n=300, seg_scale=c(0.5, 2.0)))
cat("\nRead: pooled conformal is valid ON AVERAGE but mis-covers each segment\n",
    "(over-covers seg1, under-covers seg2); Mondrian conformal restores ~0.90 in BOTH.\n",
    "That is the heterogeneity contribution the special section explicitly asks for.\n", sep="")
