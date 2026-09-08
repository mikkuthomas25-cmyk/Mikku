## Validate the seminr adapter's PLUMBING without seminr: build a mock object
## carrying seminr's slot names from a known base-R fit, run it through
## seminr_to_predictor(), and confirm predictions match the native engine.
## (Only seminr's own numeric conventions remain untested -> validate_adapter().)
options(spike_no_run = TRUE)
source("seminr_adapter.R")            # loads engine + adapter (seminr example skipped)
set.seed(1)

d  <- simulate_data(400)              # reflective X1,X2->M->Y from the engine
tr <- d[1:300,]; te <- d[301:400,]
m  <- estimate_pls(tr, mm, sm)        # native fit
indall <- unlist(mm)

## assemble a mock 'seminr' fit with seminr slot names from the native fit
OW <- matrix(0, length(indall), length(m$cons), dimnames=list(indall, m$cons))
for (c in m$cons) OW[mm[[c]], c] <- m$W[[c]]
mock <- list(
  constructs    = m$cons,
  mmMatrix      = do.call(rbind, lapply(m$cons, function(c) cbind(construct=c, measurement=mm[[c]]))),
  smMatrix      = { s<-sm; colnames(s)<-c("source","target"); s },
  outer_weights = OW,
  outer_loadings= m$L,
  path_coef     = m$B,               # [antecedent, outcome] == PATHS_FROM_ROW TRUE
  rawdata       = tr,
  mmVariables   = indall
)

p_native  <- predict_indicators(m, te, "Y")
p_adapter <- predict_indicators(seminr_to_predictor(mock), te, "Y")
cat("max |native - adapter| prediction difference:",
    format(max(abs(p_native - p_adapter)), digits=3), "\n")
cat("cor(native, adapter) for y_1:",
    round(cor(p_native[,"y_1"], p_adapter[,"y_1"]), 6), "\n")
cat(if (max(abs(p_native - p_adapter)) < 1e-8) "PLUMBING OK\n" else "MISMATCH\n")
