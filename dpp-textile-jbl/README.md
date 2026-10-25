# Who Carries the Passport? DPP readiness in Indian apparel exports (fsQCA design)

Submission package for the **Journal of Business Logistics** Special Topic Forum
*To the Point! Supply Chain Management Design: Seeking Research that Resonates* (2026/27).

**Design (v0.2):** fuzzy-set qualitative comparative analysis (fsQCA) of 40–60 Indian textile and apparel exporters, using structured interviews from December 2026 to February 2027. This replaces the earlier PLS-SEM design (v0.1, in git history), which needed 200+ firms in two waves plus scale development.

| File | What it is | Status |
|---|---|---|
| `01_abstract.md` / `.docx` | 500-word STF abstract (due **31 Dec 2026**) | Ready for supervisor review (493 words incl. title and keywords; one labelled paragraph per question in the call) |
| `02_manuscript.md` / `.docx` | Full paper draft (window **1–30 Apr 2027**) | Theory, propositions, method and discussion drafted; **results are placeholders** |
| `03_survey_instrument.md` / `.docx` | Structured interview questionnaire (single session, 30–40 min) | Needs expert review and 3–5 pretests (Nov 2026) |
| `04_online_appendix.md` / `.docx` | Calibration anchors, fsQCA protocol, robustness, quota grid, timeline | Draft |
| `05_cfp_compliance.md` / `.docx` | Requirement-by-requirement check against the call | Current |
| `figure1_model.svg` / `.png` | Figure 1 (configurational model) | Final |
| `analysis/fsqca.R` | QCA-package pipeline: calibration, necessity, sufficiency, robustness (synthetic-data mode for testing) | **Not yet executed**; R was unavailable while drafting |

Rebuild the Word files with: `bash build_docx.sh`

## What the call requires, and how this package meets it

| STF requirement | How it is met |
|---|---|
| Empirical, with data from 2026/2027 | Structured interviews, Dec 2026 – Feb 2027 |
| 3,500–4,000 words including references | 3,654 words (body 2,758 + references 896), with the results written as fill-in-the-blank sentences. Recount after filling them in |
| Germane, current, provocative | Destruction ban (July 2026), DPP Registry live (July 2026), EU–India FTA (Jan 2026), US tariffs. "Traceability without circularity?" (P4) |
| Theory to make sense of chaos | Information processing theory read configurationally (Galbraith's alternative design strategies) + institutional decoupling |
| STF topic bullets | Sustainability vs. operational trade-offs; global footprint and geopolitics; public policy; cross-boundary collaboration; technology |
| Abstract content: what, why, lens, method, data, impact | Each is a labelled paragraph in `01_abstract.md` |

## Honesty notes (read before submitting)

1. **No data have been collected.** Every `[ ]` and `[N]` in the manuscript must be filled from the real 2026–27 interviews. The R script's synthetic mode exists only to test the code.
2. **Citation verification log.** Checked online during drafting: Jensen et al. (2023); King et al. (2023); Srinivasan & Swink (2018); the ESPR/DPP timeline; the destruction ban (Delegated Reg. (EU) 2026/296); Directive (EU) 2025/1892; the EU–India FTA; the India export figures. **All other references, including the fsQCA methods sources (Ragin 2008; Fiss 2011; Greckhamer et al. 2018; Misangyi et al. 2017; Schneider & Wagemann 2012; Dusa 2019), come from my knowledge of established literature and must be checked against the originals before submission.** Exact titles still to confirm: Delegated Regulation 2026/296, the Commission's FTA press release, and the Reuters article.
3. Calibration anchors are set from theory, before data collection, and must be reviewed by the expert panel. Don't tune them afterwards to improve the results; robustness check R1 shows sensitivity instead.
4. JBL's previous *To the Point!* papers could not be opened from this environment. Read two or three of them to calibrate tone and length, and check whether the journal has published fsQCA studies you can cite as precedent.
