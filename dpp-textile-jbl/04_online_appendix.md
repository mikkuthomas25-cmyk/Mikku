---
title: "Online Appendix: Who Carries the Passport?"
subtitle: "Appendix A (measures and calibration), B (fsQCA protocol and robustness), C (sampling and timeline). Appendices do not count toward the STF word limit."
---

# Appendix A: Measures and calibration

## A.1 Sets, raw measures and anchors

| Set (code) | Role | Raw measure | Full out | Crossover | Full in | Anchor rationale |
|---|---|---|---|---|---|---|
| DPP readiness (DPR) | Outcome 1 | Data categories supplied today (0–7) | 1 | 3.5 | 6 | Six or seven categories ≈ the textile DPP data set expected by EPRS (2024); one or none = no capability |
| Circular capability (CSC) | Outcome 2 | Practices in place (0–5) | 0 | 2.5 | 4 | Four or more practices = circularity built into routine; none = linear |
| Formal circularity (CSCF) | Outcome 2b | CSC_2, CSC_3, CSC_5 in place (0–3) | 0 | 1.5 | 3 | Separates brand-led circular practice from informal waste sale and surplus resale, which need no product data |
| Digital traceability (DTC) | Condition | Digital practices (0–5) | 1 | 2.5 | 4 | Four or more = integrated digital records; one or none = paper/spreadsheet |
| Vertical integration (VI) | Condition | Stages in-house (1–6) | 1 | 3.5 | 5 | Five or more = mill-to-garment; one = garment-only or trading |
| Multi-tier data sharing (MDS) | Condition | % of key suppliers sharing data | 10 | 50 | 90 | Most suppliers sharing vs. almost none |
| Chain-of-custody certification (CERT) | Condition ("borrowed" data) | % of EU shipments under GOTS/GRS/RCS/OCS transaction certificates (item B7) | 5 | 50 | 90 | Majority of EU volume certified vs. essentially none; certificate sighted |
| Data-appropriation concern (DAC) | Condition | Mean of 3 items (1–7) | 2 | 4 | 6 | As above |
| Buyer pressure (BTP) | Background condition; robustness R4 and R7 | Mean of 3 items (1–7) | 2 | 4 | 6 | Scale verbal anchors: "disagree" (2), neutral (4), "agree" (6). Expected to be near-universal |
| Firm size (SIZE) | Condition for circularity only | Employees | 50 | 250 | 1000 | Formal circular programmes are expected to need scale |

Calibration uses the direct method (logistic transformation; Ragin, 2008): `QCA::calibrate(x, type = "fuzzy", thresholds = "e=…, c=…, i=…")`. Scores of exactly 0.5 are set to 0.501. Raw distributions (histograms) and the share of cases above each crossover are reported in Table A2, so that readers can judge whether each anchor separates cases meaningfully.

## A.2 Measurement quality without large-sample factor analysis

fsQCA does not use latent-variable estimation, and with 40–60 firms an EFA or CFA would not be stable. Measurement quality is established instead by:

1. **Content validity:** an expert panel (5–6 exporters, brand sourcing managers and academics) reviews every item and anchor (Nov 2026).
2. **Factual items:** six of the eight sets are checklists, shares of current practice or counts, not attitudes, so they don't depend on latent constructs. Sample documents are sighted where possible, and the share verified is reported.
3. **Short scales (BTP, DAC):** Cronbach's α and inter-item correlations are reported. If α < 0.70, the strongest single item is used and this is reported.
4. **Researcher triangulation:** the two-researcher protocol and the open-question notes are used to check implausible answer patterns.

# Appendix B: fsQCA protocol

## B.1 Necessity analysis (run first)

* Each condition and its negation is tested for high and low readiness, and for high and low circular capability
* A condition counts as necessary if consistency ≥ 0.90 and coverage is not trivial (relevance of necessity, RoN ≥ 0.60) (Schneider & Wagemann, 2012)
* Optional: necessary condition analysis (NCA) is reported alongside for necessity *in degree*
* Buyer pressure is expected to be near-universal among EU suppliers, so its RoN is reported explicitly. A condition present in almost all cases can pass the consistency test while being trivial

## B.2 Sufficiency analysis

| Step | Setting |
|---|---|
| Truth table | 5 conditions → 32 rows; the number of rows with ≥ 1 case is reported |
| Frequency threshold | 1 case (small-N); robustness at 2 |
| Raw consistency | ≥ 0.80 |
| PRI consistency | ≥ 0.70 (rows with high raw but low PRI are treated as contradictory and coded 0) |
| Solutions | Conservative, intermediate (reported) and parsimonious; core conditions = in both the parsimonious and intermediate solutions (Fiss, 2011) |
| Conditions | VI, DTC, MDS, CERT, DAC (BTP is a background condition) |
| Directional expectations (high DPR) | DTC present; VI present; MDS present; CERT present; DAC absent |
| Directional expectations (low DPR) | DTC absent; VI absent; MDS absent; CERT absent; DAC present |
| Untenable assumptions | Checked with `SetMethods`; no simplifying assumption may contradict P2 (necessity of DTC) |
| Outcome 2 (CSC and CSCF) | Run for all circular practices and for formal practices only. Conditions: DPR, VI, MDS, CERT, SIZE. P4 is supported if DPR alone is not sufficient for formal circularity (consistency < 0.80), while informal practices occur regardless of readiness |

## B.3 Robustness (Oana & Schneider-style checks)

| Code | Check |
|---|---|
| R1 | Calibration anchors shifted ±10% of the range |
| R2 | Raw consistency threshold 0.75 and 0.85 |
| R3 | Frequency threshold 2 |
| R4 | BTP substituted for CERT |
| R5 | Excluding tier-3/4 firms (yarn and fibre suppliers) |
| R6 | DPR recomputed without DPR_7 (format/time item; anchors 1 / 3 / 5 of 6) |
| R7 | BTP added as a sixth condition (64 rows) |

Solutions are compared on fit and on whether the same configurations recur (subset/superset relations reported).

## B.4 Common-method considerations

A single respondent answers everything in one session. Mitigation: factual measures for six of eight sets; document sighting; ordering that separates the readiness questions from the concern items; and assured anonymity.

## B.5 Software

Primary: `analysis/fsqca.py` (Python implementation of this protocol). Cross-check: R with the `QCA` (Dusa, 2019) and `SetMethods` packages (`analysis/fsqca.R`), or fsQCA 4.x.

# Appendix C: Sampling and timeline

## C.1 Quota grid (purposive diversity sampling)

Target 50 firms (minimum 40). The aim is to cover the configurations that theory expects, not to estimate population averages.

| Dimension | Categories | Minimum per category |
|---|---|---|
| Cluster | Tiruppur/Coimbatore; NCR; Bengaluru; Ludhiana; Panipat (certified recyclers); Kolkata/other | 5 |
| Tier | Finished goods; intermediate (fabric/yarn) | 12 |
| Integration | Garment-only (VI ≤ 2); partly integrated (3–4); integrated (≥ 5) | 10 |
| Size | < 250 employees; ≥ 250 employees | 15 |

## C.2 Timeline

| Date | Milestone | Firms |
|---|---|---|
| Nov 2026 | Ethics approval; expert review of items and anchors; 3–5 pretest interviews; contact AEPC and cluster associations | – |
| Dec 2026 | Fieldwork starts | ~10 |
| 31 Dec 2026 | **Abstract submitted** | – |
| Jan 2027 | Fieldwork continues; 15 Jan STF feedback; end-Jan workshop | ~20 |
| Feb 2027 | Fieldwork closes | ~20 (cumulative 50) |
| Mar 2027 | Calibration, necessity and sufficiency analyses, robustness, case vignettes | – |
| Apr 2027 | Writing; **submit within 1–30 April** | – |

That is about **4–5 interviews per week** over 11 weeks, which two researchers can manage with association referrals.
