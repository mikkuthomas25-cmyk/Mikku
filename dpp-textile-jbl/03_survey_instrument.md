---
title: "Survey Instrument: DPP Readiness among Indian Textile and Apparel Exporters (DRAFT v0.1)"
subtitle: "Two-wave key-informant survey. This draft is for supervisor review and will be revised after expert interviews and the pilot (January 2027)."
---

# Notes for the research team (not shown to respondents)

* **Waves.** Wave 1 (Feb 2027) covers sections A–F. Wave 2 (Mar 2027, 3–4 weeks later, same firm and ideally the same informant) covers sections G–J. The firm ID links the two waves.
* **Scale.** Unless stated otherwise, items use 1 = strongly disagree … 7 = strongly agree, with a separate "don't know" option that is coded as missing.
* **Reference period.** "The last 12 months" unless stated otherwise.
* **Item codes** (e.g., `BTP1`) match the variable names in `analysis/pls_model.R`.
* **Sources.** "Adapted from" means wording is reworded for the DPP context and must be checked against the original paper before finalising. "New" items need content validation (expert review + card sort).
* **Ethics.** Institutional ethics approval and informed consent are required before fielding. Firm-level responses are confidential and reported only in aggregate.
* **Languages.** English master, with Hindi and Tamil versions produced by translation and back-translation.

---

# Introduction (shown to respondents)

> Thank you for taking part in this study on how textile and apparel exporters prepare for the European Union's new product-data requirements, including the **Digital Product Passport (DPP)**. A DPP is a digital record, reached through a QR code on each product, that shows information such as fibre composition, where each production stage took place, chemicals used, environmental footprint, durability and recyclability. EU rules will require it for clothing and textiles in the coming years.
>
> The survey takes about 15 minutes. There are no right or wrong answers. Your responses are confidential and will be reported only in aggregate. Please answer for **your firm (the legal entity that exports)**.

---

# WAVE 1

## Section A: Screening and informant

| Code | Question | Response |
|---|---|---|
| SCR1 | Has your firm shipped goods to buyers in the European Union in the last 12 months? | Yes / No (*No → end*) |
| SCR2 | What is your role? | Owner/MD/Director; Head of compliance/sustainability; Head of merchandising/sourcing; Head of production/operations; Other (specify) |
| SCR3 | How many years have you worked at this firm? | number |
| KNOW1 | I am knowledgeable about my firm's dealings with EU buyers on sustainability and product data. | 1–7 |
| INV1 | I am personally involved in responding to buyers' data and compliance requests. | 1–7 |

*Exclude informants with KNOW1 or INV1 ≤ 3, or tenure under 1 year.*

## Section B: Firm profile (controls)

| Code | Question | Response |
|---|---|---|
| SIZE | Number of permanent employees (all units) | number (log-transformed in analysis) |
| AGE_EXP | Number of years your firm has been exporting | number |
| TIER | What does your firm mainly sell to EU customers? | Finished garments/home textiles (tier 1); Fabric (tier 2); Yarn (tier 3); Fibre (tier 4); Other |
| VI1–VI6 | Which stages do you perform **in-house**? (tick all) | Spinning; Weaving/Knitting; Dyeing/Printing/Finishing; Cutting & sewing; Trims/Embroidery; Packing/Logistics. The count of ticks gives `VI` |
| CERT | Which certifications does your firm currently hold? (tick all) | GOTS; GRS/RCS; OEKO-TEX (Standard 100/STeP); bluesign; Higg FEM verified; SA8000/SLCP; ZDHC; Better Cotton member; ISO 14001; Other. The count gives `CERT` |
| EUSHARE | Approximately what % of your export turnover goes to the EU? | 0–100 |
| BUYCON | What % of your EU turnover comes from your largest EU buyer? | 0–100 |
| OWN | Ownership | Indian private; Indian listed; Foreign/JV; Other |
| CLUSTER | Main production location | Tiruppur/Coimbatore; Bengaluru; NCR (Delhi/Noida/Gurugram); Ludhiana; Kolkata; Mumbai/Surat; Jaipur; Other |

## Section C: Buyer-transmitted regulatory information pressure (BTP)
*Construct: the extent to which EU buyers pass EU product-data requirements on to the firm. Adapted from Zhu and Sarkis (2007) institutional pressure items.*

"In the last 12 months, our EU buyers…"

| Code | Item |
|---|---|
| BTP1 | …have asked us for more detailed product-level sustainability data than before. |
| BTP2 | …have told us that data we provide will be used for EU legal requirements (e.g., the Digital Product Passport, the ban on destroying unsold goods, or extended producer responsibility). |
| BTP3 | …have made the provision of product data a condition for future orders. |
| BTP4 | …have asked us for data about our own suppliers (mills, dyehouses, spinners). |
| BTP5 | …have added data-provision clauses to contracts, codes of conduct or supplier manuals. |

## Section D: Data-appropriation concern (DAC)
*Construct: the firm's concern that product and supply chain data it shares will be used against its interests. New scale, informed by King et al. (2023) on stakeholder concerns about data sharing.*

| Code | Item |
|---|---|
| DAC1 | We worry that buyers will use the cost or process data we share to push our prices down. |
| DAC2 | We worry that sharing the names of our suppliers will allow buyers to bypass us and buy directly. |
| DAC3 | We are concerned that product data shared through digital passports could reach our competitors. |
| DAC4 | Our own suppliers are reluctant to share data because they fear how it will be used. |

## Section E: Digital traceability capability (DTC)
*Construct: the extent to which material and process data are captured in structured, interoperable digital form linked to batches and styles. Adapted from supply visibility measures (Srinivasan & Swink, 2018) and traceability literature (Agrawal et al., 2021).*

| Code | Item |
|---|---|
| DTC1 | We record material inputs (fibre, yarn, fabric lots) digitally and link them to each production batch or style. |
| DTC2 | Our traceability data are held in an integrated system (e.g., ERP or a traceability platform), not in separate spreadsheets or paper files. |
| DTC3 | We can trace a finished product back to its input material lots within one working day. |
| DTC4 | Our product data use standard identifiers or formats that can be exchanged with buyers' systems (e.g., GS1/GTIN, API, structured file upload). |
| DTC5 | Certificates and test reports are stored digitally and linked to the products they cover. |

## Section F: Multi-tier data sharing (MDS)
*Construct: the extent to which upstream suppliers (tier 2 and beyond) provide timely, accurate and complete data to the firm. Adapted from Fawcett et al. (2007) and Kembro et al. (2017).*

"Our main suppliers (fabric mills, dyehouses, spinners, fibre suppliers)…"

| Code | Item |
|---|---|
| MDS1 | …share data on material origin and composition with us on time. |
| MDS2 | …share data on chemicals and wet-processing inputs with us. |
| MDS3 | …share environmental data (energy, water, emissions) for the products we buy. |
| MDS4 | …tell us who *their* suppliers are when we ask. |
| MDS5 | …provide data that we can trust without re-checking. |

## Section F2: Perceived regulatory uncertainty (RU), control

| Code | Item |
|---|---|
| RU1 | It is unclear to us exactly which data the EU Digital Product Passport will require. |
| RU2 | It is unclear to us when the EU product-data requirements will apply to our products. |
| RU3 | Different EU buyers ask us for different, inconsistent data. |

## Section F3: Marker variable (MV), for common-method testing
*Theoretically unrelated to the model's constructs.*

| Code | Item |
|---|---|
| MV1 | I prefer to receive work communication by phone rather than by email. |
| MV2 | I usually read the news in print rather than online. |
| MV3 | I prefer meetings in person rather than by video. |

---

# WAVE 2

## Section G: DPP readiness (DPR)
*Construct: the firm's ability to supply, at style or batch level, the data categories expected in an EU textile DPP. New scale; categories derived from ESPR Art. 7 and Annex III, the EPRS (2024) textile DPP study, and Jensen et al. (2023).*

"If an EU buyer asked today, we could provide **verified data at style or batch level** on…"

| Code | Item |
|---|---|
| DPR1 | …fibre composition, including recycled content. |
| DPR2 | …the country and facility of each processing stage (spinning, weaving/knitting, wet processing, cutting & sewing). |
| DPR3 | …substances of concern and chemicals used in processing. |
| DPR4 | …carbon and water footprint of the product. |
| DPR5 | …durability and physical test results (e.g., pilling, colour fastness, seam strength). |
| DPR6 | …care, repair and end-of-life information (e.g., recyclability, separability of trims). |
| DPR7 | …all of the above in a machine-readable format, within the time our buyer requires. |

## Section H: Circular supply chain capability (CSC)
*Construct: the firm's ability to keep materials in use across its supply chain. Adapted from circular economy practices in textiles (Jia et al., 2020).*

| Code | Item |
|---|---|
| CSC1 | We systematically collect and sort pre-consumer waste (cutting waste, rejects) for reuse or recycling. |
| CSC2 | We use a meaningful share of recycled or regenerated fibres in our products. |
| CSC3 | We work with buyers on design-for-recycling (e.g., mono-material fabrics, removable trims). |
| CSC4 | We have arrangements to resell, rework or donate unsold or rejected stock instead of destroying it. |
| CSC5 | We take part in take-back or recycling programmes run by buyers or third parties. |

## Section I: EU export performance (EXP)
*Construct: the firm's performance with EU buyers relative to its main competitors over the last 12 months.*

"Compared with our main competitors, in the last 12 months…"

| Code | Item |
|---|---|
| EXP1 | …our order volume from EU buyers has grown faster. |
| EXP2 | …EU buyers have given us a larger share of their business. |
| EXP3 | …we have won new EU buyers more easily. |
| EXP4 | …the prices or margins we obtain from EU buyers have been better. |
| EUSHARE_CHG | (Objective triangulation) The change in the EU share of your export turnover compared with the previous financial year: decreased >10 points / decreased 1–10 / no change / increased 1–10 / increased >10 |

## Section J: Open questions (optional)

* OPEN1: What is the single biggest obstacle to providing the product data EU buyers ask for?
* OPEN2: What support (from buyers, industry associations or government) would help most?
* Would you like a summary of the findings? (email)

---

# Item count and timing

| Wave | Reflective items | Profile/control items | Estimated time |
|---|---|---|---|
| 1 | BTP 5, DAC 4, DTC 5, MDS 5, RU 3, MV 3 = 25 | ~12 | 12–14 min |
| 2 | DPR 7, CSC 5, EXP 4 = 16 | 2 | 6–8 min |
