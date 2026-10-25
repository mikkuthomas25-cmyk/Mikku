---
title: "Structured Interview Questionnaire: DPP Readiness among Indian Textile and Apparel Exporters (DRAFT v0.2, fsQCA design)"
subtitle: "One session per firm, 30–40 minutes, in person or by video. Draft for supervisor review; to be revised after expert review and 3–5 pretest interviews (Nov–Dec 2026)."
---

# Notes for the research team (not shown to respondents)

* **Design.** A single session per firm, with no second wave. Target 40–60 firms, sampled purposively for diversity in cluster, tier, size and integration (see the quota grid in Online Appendix C).
* **Mode.** The researcher reads the questions and records the answers. Two researchers attend where possible; one asks, the other notes.
* **Factual first.** Sections C, D, E and G are factual yes/no checklists about *current* practice. Where the respondent agrees, ask to **see one sample document** (e.g., a transaction certificate, test report or buyer data template) and tick "sighted".
* **Scale items.** Sections F and H use 1 = strongly disagree … 7 = strongly agree. "Don't know" is coded as missing.
* **Codes** (e.g., `DPR_3`) match `analysis/fsqca.R`. How each section becomes a fuzzy set is set out in Online Appendix A.
* **Ethics.** Institutional ethics approval and informed consent are required before fielding. Firm identities are confidential; case descriptions in the paper are anonymised.

---

# Introduction (read to the respondent)

> Thank you for taking part in this study on how textile and apparel exporters prepare for the European Union's new product-data rules, including the **Digital Product Passport (DPP)**. A DPP is a digital record, reached through a QR code on each product, that shows information such as fibre composition, where each production stage took place, chemicals used, environmental footprint, durability and recyclability.
>
> Most questions are simple yes/no questions about what your firm does **today**. There are no right or wrong answers. Your answers are confidential and will only be reported without naming your firm.

---

# Section A: Respondent

| Code | Question | Response |
|---|---|---|
| A1 | Your role | Owner/MD/Director; Head of compliance/sustainability; Head of merchandising/sourcing; Head of production; Other |
| A2 | Years at this firm | number |
| A3 | How involved are you in handling EU buyers' data and compliance requests? | Not involved / Somewhat / Directly responsible |

*Proceed only if A3 = Somewhat or Directly responsible.*

# Section B: Firm profile (sampling, description and robustness)

| Code | Question | Response |
|---|---|---|
| B1 | Has the firm shipped to EU buyers in the last 12 months? | Yes / No (*No → end*) |
| B2 | Permanent employees (all units) | number |
| B3 | Main product sold to EU buyers | Garments/home textiles (tier 1); Fabric (tier 2); Yarn (tier 3); Fibre (tier 4) |
| B4 | Approximate % of export turnover going to the EU | 0–100 |
| B5 | Main production location | Tiruppur/Coimbatore; Bengaluru; NCR; Ludhiana; Kolkata; Other |
| B6 | Certifications currently held (tick all) | GOTS; GRS/RCS; OCS; OEKO-TEX; bluesign; Higg FEM verified; ZDHC; Better Cotton; ISO 14001; Other (descriptive) |
| B7 | **Chain-of-custody coverage (condition `CERT`).** Approximately what % of your EU shipments in the last 12 months were covered by **transaction certificates** under GOTS, GRS, RCS or OCS? (0 if none) | 0–100%. *Ask to see one recent transaction certificate.* |

# Section C: Vertical integration (condition `VI`)

"Which of these stages does your firm perform **in its own units** (not job work)?"

| Code | Stage | In-house? |
|---|---|---|
| VI_1 | Spinning | Y/N |
| VI_2 | Weaving or knitting | Y/N |
| VI_3 | Dyeing, printing or finishing | Y/N |
| VI_4 | Cutting and sewing | Y/N |
| VI_5 | Embroidery, trims or washing | Y/N |
| VI_6 | Packing and dispatch | Y/N |

`VI` = number of Yes answers (1–6).

# Section D: Digital traceability (condition `DTC`)

"Does your firm currently…"

| Code | Practice | Y/N | Sighted |
|---|---|---|---|
| DTC_1 | …record input lots (fibre, yarn, fabric) digitally and link them to each style or batch? | | |
| DTC_2 | …hold this traceability data in one system (ERP or traceability platform), not separate spreadsheets or paper? | | |
| DTC_3 | …trace a finished product back to its input lots within one working day? | | |
| DTC_4 | …exchange product data with buyers in a structured format (portal upload, API, standard template)? | | |
| DTC_5 | …store certificates and test reports digitally, linked to the products they cover? | | |

`DTC` = number of Yes answers (0–5).

# Section E: Multi-tier data sharing (condition `MDS`)

| Code | Question | Response |
|---|---|---|
| MDS_N | How many **key** upstream suppliers (mills, dyehouses, spinners, fibre suppliers) do you buy from regularly? | number |
| MDS_1 | Of these, how many provide you with data on **material origin** (country and facility) when asked? | number |
| MDS_2 | Of these, how many provide data on **chemicals and wet-processing inputs**? | number |
| MDS_3 | Of these, how many tell you who **their own** suppliers are? | number |

`MDS` = mean of (MDS_1, MDS_2, MDS_3) ÷ MDS_N × 100, i.e. the % of key suppliers sharing data.

# Section F: Buyer pressure (`BTP`, background condition: reported descriptively and used in robustness check R4)

"In the last 12 months, our EU buyers…"

| Code | Item (1–7) |
|---|---|
| BTP_1 | …have made the provision of product-level data a condition for future orders. |
| BTP_2 | …have asked us for data about our own suppliers (mills, dyehouses, spinners). |
| BTP_3 | …have linked their data requests to EU legal requirements (the passport, the ban on destroying unsold goods, or producer responsibility). |

`BTP` = mean of the three items.

# Section G: DPP readiness (outcome `DPR`)

"If an EU buyer asked **today**, could you supply **verified data at style or batch level** on…"

| Code | Data category | Y/N | Sighted |
|---|---|---|---|
| DPR_1 | Fibre composition, including recycled content | | |
| DPR_2 | Country and facility of each processing stage | | |
| DPR_3 | Substances of concern and chemicals used | | |
| DPR_4 | Carbon and/or water footprint | | |
| DPR_5 | Durability and physical test results | | |
| DPR_6 | Care, repair and end-of-life information (recyclability, separability of trims) | | |
| DPR_7 | All of the above in a machine-readable format, within the time the buyer requires | | |

`DPR` = number of Yes answers (0–7).

# Section H: Data-appropriation concern (condition `DAC`)

| Code | Item (1–7) |
|---|---|
| DAC_1 | We worry that buyers will use the cost or process data we share to push our prices down. |
| DAC_2 | We worry that sharing the names of our suppliers will let buyers bypass us and buy directly. |
| DAC_3 | Our own suppliers hold back data because they fear how it will be used. |

`DAC` = mean of the three items. *(Asked after Section G, so that the readiness answers are not coloured by these concerns.)*

# Section I: Circular capability (outcome `CSC`)

"Does your firm currently…"

| Code | Practice | Y/N |
|---|---|---|
| CSC_1 | …collect and sort cutting waste and rejects for reuse or recycling? | |
| CSC_2 | …use recycled or regenerated fibres in at least some regular product lines? | |
| CSC_3 | …work with buyers on design-for-recycling (e.g., mono-material fabrics, removable trims)? | |
| CSC_4 | …resell, rework or donate unsold or rejected stock instead of destroying it? | |
| CSC_5 | …take part in a take-back or recycling programme run by a buyer or third party? | |

`CSC` = number of Yes answers (0–5).

# Section J: Open questions (case knowledge; notes recorded verbatim where possible)

* J1: Walk us through the last time an EU buyer asked you for product or supplier data. What happened?
* J2: What is the single biggest obstacle to providing the data EU buyers ask for?
* J3: What would make you, or your suppliers, more willing to share data?

---

# Timing

| Section | Minutes |
|---|---|
| A–B Profile | 5 |
| C–E Integration, traceability, sharing | 8–10 |
| F–I Pressure, readiness, concern, circularity | 8–10 |
| J Open questions | 10–15 |
| **Total** | **30–40** |
