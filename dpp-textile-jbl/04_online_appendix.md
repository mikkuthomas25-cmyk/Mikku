---
title: "Online Appendix: Who Carries the Passport?"
subtitle: "Appendix A (measures), B (analysis plan), C (data-collection protocol). Appendices do not count toward the STF word limit."
---

# Appendix A: Constructs, definitions and sources

| Construct (code) | Definition | Items | Role | Source |
|---|---|---|---|---|
| Buyer-transmitted regulatory information pressure (BTP) | How far EU buyers pass EU product-data requirements on to the supplier | 5 | Exogenous (information-processing requirement) | Adapted from Zhu & Sarkis (2007) |
| Data-appropriation concern (DAC) | Concern that shared data will be used against the firm's interests | 4 | Moderator of BTP → MDS | New; informed by King et al. (2023) |
| Digital traceability capability (DTC) | Structured, interoperable digital capture of material and process data | 5 | Vertical information-processing capacity | Adapted from Srinivasan & Swink (2018); Agrawal et al. (2021) |
| Multi-tier data sharing (MDS) | Timely, accurate data from tier-2+ suppliers | 5 | Lateral information-processing capacity | Adapted from Fawcett et al. (2007); Kembro et al. (2017) |
| DPP readiness (DPR) | Ability to supply passport-grade data at style or batch level | 7 | Focal outcome (fit) | New; ESPR, EPRS (2024), Jensen et al. (2023) |
| Circular SC capability (CSC) | Ability to keep materials in use across the supply chain | 5 | Outcome (H5a/H5b) | Adapted from Jia et al. (2020) |
| EU export performance (EXP) | Performance with EU buyers relative to competitors | 4 (+1 objective) | Outcome (H6) | Perceptual export-performance items |
| Regulatory uncertainty (RU) | Ambiguity about what the DPP will require, and when | 3 | Control | New |
| Marker (MV) | Communication-media preference | 3 | CMV test | Marker-variable approach |

The full wording is in `03_survey_instrument.md`.

## A.1 Scale development for new constructs (DAC, DPR, RU)

1. **Item generation (Nov–Dec 2026):** items are drawn from the ESPR text, the EPRS (2024) study, Jensen et al.'s (2023) data clusters and King et al. (2023).
2. **Expert review (Dec 2026–Jan 2027):** 8–10 interviews with exporters' compliance heads, brand sourcing managers, an AEPC representative and two academics. Experts rate each item's relevance on a 4-point scale, and we compute item-level and scale-level content validity indices (target I-CVI ≥ 0.78).
3. **Card sort:** 6–8 judges assign items to construct definitions. Items are kept if placed correctly by at least 75% of judges.
4. **Cognitive pretest (early Jan 2027):** 8–10 respondents talk through each item ("think aloud") to check clarity and completion time.
5. **Pilot survey with EFA (Jan 2027):** at least **100 firms** (about 7 per item for the 14 new items), drawn at random from the sampling frame and excluded from the main sample. EFA is run on the new items (DAC, DPR, RU) together with one adapted scale (BTP) as a discriminant anchor:
   * Suitability: KMO ≥ 0.70; Bartlett's test p < 0.05
   * Extraction: principal axis factoring with oblique (promax) rotation, since the constructs are expected to correlate
   * Number of factors: parallel analysis, cross-checked with the scree plot (not eigenvalue > 1 alone)
   * Retention: primary loading ≥ 0.50, cross-loadings < 0.30, communality ≥ 0.40; items are dropped one at a time and the EFA rerun
   * Reliability of the retained items: Cronbach's α and ω ≥ 0.70
6. **Fallback if the pilot reaches fewer than 100 firms:** randomly split the main sample. Run the EFA on one half and the CFA on the other, and report both. This costs power, so the main target should rise to 300 firms if this route is used.

*Why EFA only for the new scales:* the adapted scales have established factor structures. Running EFA on them again adds little and uses words. Their structure is tested confirmatorily (B.2).

# Appendix B: Analysis plan

## B.1 Power

G*Power, linear multiple regression (R² deviation from zero), with the largest number of predictors pointing at one construct = 10 (DTC, MDS, DTC×MDS + 7 controls). For f² = 0.15, α = 0.05 and power = 0.80, the minimum n is 118. The inverse-square-root method (Hair et al., 2019) with p_min ≈ 0.20 gives n ≈ 155. **We target at least 200 matched responses.**

## B.2 Measurement model (reflective)

* Indicator loadings ≥ 0.708 (0.40–0.70 retained if CR and AVE are acceptable)
* Composite reliability ρ_A and ρ_C between 0.70 and 0.95
* AVE ≥ 0.50
* HTMT < 0.85 (conservative) and bootstrap CI excluding 1 (Henseler et al., 2015)

## B.2a Covariance-based CFA (robustness for reviewers)

The full measurement model (all multi-item constructs, freely correlated) is estimated as a CFA in `lavaan` with robust maximum likelihood (MLR), because Likert data are rarely normal. Reported in Table B1:

* Global fit: χ²/df ≤ 3; CFI and TLI ≥ 0.90 (≥ 0.95 good); RMSEA ≤ 0.08 with 90% CI; SRMR ≤ 0.08
* Standardised loadings ≥ 0.50 (ideally ≥ 0.70), CR ≥ 0.70, AVE ≥ 0.50; Fornell–Larcker and HTMT for discriminant validity
* Comparison with a one-factor model (Δχ² test), as an additional common-method check
* Modification indices are reported but used only for theoretically justified changes, never to chase fit

If the CFA and PLS confirmatory composite analysis disagree (e.g., an item loads well as a composite but poorly as a factor), the item is flagged and the results are reported with and without it.

## B.3 Common-method variance

* Procedural: temporal separation of predictors and outcomes, anonymity, different scale anchors for EXP, and the order of items mixed across constructs
* Statistical: full-collinearity VIF < 3.3 (Kock, 2015); a marker-variable partial correlation adjustment; Harman single-factor test reported only for completeness

## B.4 Structural model

* Collinearity: inner VIF < 3
* Paths: 10,000 bootstrap samples, percentile or bias-corrected 95% CIs, one-tailed for directional hypotheses
* Interactions H2 (BTP × DAC → MDS) and H4 (DTC × MDS → DPR) use the two-stage approach and are reported with simple-slope plots at ±1 SD
* R², f², and Q²_predict with PLSpredict (Shmueli et al., 2019; 10 folds, 10 repetitions), compared against a linear-model benchmark
* **H5a versus H5b:** BTP → CSC is added as a direct path. H5a is supported if DPR → CSC is positive and its CI excludes 0. H5b (decoupling) is supported if the CI includes 0 while BTP → DPR (indirect) is positive. We also report an equivalence test (TOST, bounds ±0.10), so that "no effect" is not just "non-significant".
* Mediation: indirect effects BTP → DTC/MDS → DPR → CSC/EXP, using bootstrapped specific indirect effects

## B.5 Robustness

1. Excluding tier-3/4 firms (yarn and fibre), whose passport role differs
2. Multi-group analysis (MICOM first): large vs. small firms; knitwear (Tiruppur) vs. woven clusters
3. Replacing perceptual EXP with the objective EUSHARE_CHG (ordinal)
4. Endogeneity: Gaussian copula test on the DPR → EXP path
5. Non-response: early vs. late respondents (Armstrong & Overton, 1977); respondents vs. non-respondents on directory size and region

## B.6 Software

R with the `seminr` package, or SmartPLS 4. Analysis code is available from the authors on request.

# Appendix C: Data-collection protocol and timeline

| Date | Milestone |
|---|---|
| 31 Dec 2026 | Abstract submitted to the JBL STF |
| Dec 2026 – Jan 2027 | Ethics approval; expert interviews; card sort; translation; cognitive pretest |
| 15 Jan 2027 | STF feedback on abstract; revise the model and instrument |
| 10–31 Jan 2027 | Pilot survey (n ≥ 100) and EFA of the new scales; finalise items |
| End Jan 2027 | STF online developmental workshop |
| 1–28 Feb 2027 | Wave 1 fielding (email + phone follow-up through AEPC and cluster associations; 3 reminders) |
| 1–25 Mar 2027 | Wave 2 fielding (matched firms) |
| 26 Mar – 15 Apr 2027 | Data cleaning, PLS-SEM, robustness |
| 15–25 Apr 2027 | Write results and discussion, then internal review |
| By 30 Apr 2027 | Full paper submitted (window 1–30 April 2027) |

**Contact strategy.** Pre-notification letter co-signed by an industry association if possible; personalised email with a link; phone follow-up by trained enumerators; a benchmark report offered as an incentive.
