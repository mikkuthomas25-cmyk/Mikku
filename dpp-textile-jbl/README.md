# Who Carries the Passport? DPP readiness in Indian apparel exports

Submission package for the **Journal of Business Logistics** Special Topic Forum
*To the Point! Supply Chain Management Design: Seeking Research that Resonates* (2026/27).

| File | What it is | Status |
|---|---|---|
| `01_abstract.md` / `.docx` | 500-word STF abstract (due **31 Dec 2026**) | Ready for supervisor review (495 words incl. title and keywords; one labelled paragraph per question in the call) |
| `02_manuscript.md` / `.docx` | Full paper draft (window **1–30 Apr 2027**) | Theory, hypotheses, method and discussion drafted; **results are placeholders** |
| `03_survey_instrument.md` / `.docx` | Two-wave questionnaire, draft v0.1 | Needs expert review, card sort and pilot |
| `04_online_appendix.md` / `.docx` | Measures, analysis plan, timeline | Draft |
| `05_cfp_compliance.md` / `.docx` | Requirement-by-requirement check against the call | Current |
| `figure1_model.svg` / `.png` | Figure 1 (research model) | Final |
| `analysis/efa_pilot.R` | EFA of the new scales on the pilot sample (psych) | **Not yet executed** |
| `analysis/pls_model.R` | seminr PLS-SEM pipeline + lavaan CFA robustness check (synthetic-data mode for testing) | **Not yet executed**; R was unavailable while drafting |

Rebuild the Word files with: `bash build_docx.sh`

## What the call requires, and how this package meets it

| STF requirement | How it is met |
|---|---|
| Empirical, with data from 2026/2027 | Two-wave survey, Feb–Mar 2027 |
| 3,500–4,000 words including references | 3,789 words (body 2,784 + references 1,005), with the results written as fill-in-the-blank sentences. Recount after filling them in |
| Germane, current, provocative | Destruction ban (July 2026), DPP Registry live (July 2026), EU–India FTA (Jan 2026), US tariffs. "Traceability without circularity?" competing hypotheses |
| Theory to make sense of chaos | Organizational information processing theory + institutional decoupling |
| STF topic bullets | Sustainability vs. operational trade-offs; global footprint and geopolitics; public policy; cross-boundary collaboration; technology |
| Abstract content: what, why, lens, method, data, impact | Each is a labelled paragraph in `01_abstract.md` |

## Honesty notes (read before submitting)

1. **No data have been collected.** Every `[ ]` and `[N]` in the manuscript must be filled from the real 2027 survey. The R script's synthetic mode exists only to test the code.
2. **Citation verification log.** I checked these online during drafting: Jensen et al. (2023); King et al. (2023); Srinivasan & Swink (2018); the ESPR/DPP timeline (textile delegated act planned for 2027, Registry live July 2026); the destruction ban (19 July 2026, Delegated Reg. (EU) 2026/296); Directive (EU) 2025/1892; the EU–India FTA (concluded 27 Jan 2026); the India export figures. **All other references come from my knowledge of well-established literature and must be checked against the originals** (volume, pages, exact title) before submission. Exact titles still to confirm: Delegated Regulation 2026/296, the Commission's FTA press release, and the Reuters article. Fawcett & Waller (2011) JBL 32(1) also needs its pages confirmed.
3. "Adapted" scale items are reworded for the DPP context. Compare them with the source papers before fielding.
4. JBL's previous *To the Point!* papers could not be opened from this environment (the Wiley site was blocked). Read two or three of them to calibrate tone and length.
