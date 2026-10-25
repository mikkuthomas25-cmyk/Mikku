# Literature matrix (supervisor request, review round 1)

Columns follow the supervisor's matrix. Cell codes:

- **Y** — the feature is modelled/analysed, per the evidence level shown.
- **N(abs)** — not mentioned in title/abstract/summary. **This is NOT evidence of absence**:
  the full text has not been read. Every N(abs) must be checked in full text before the
  gap statement is finalised.
- **?** — could not be determined from the available record.

Evidence level: **A** = abstract/bibliographic record only (all rows at present);
**F** = full text read (none yet — blocked by network policy; see README).

| Stream | Source (docs/literature.md id) | ML sorting accuracy | Human–AI delegation / reject option | Queue / manual capacity | Bale (lot) purity | Recycler acceptance | Evidence |
|---|---|---|---|---|---|---|---|
| Textile classification | M1 Riba et al. 2020 | Y | N(abs) | N(abs) | N(abs) | N(abs) | A |
| Textile classification | M4 Li et al. 2023 | Y | N(abs) | N(abs) | N(abs) | N(abs) | A |
| Textile classification | M5 Analyst 2021 (cellulose HSI) | Y | N(abs) | N(abs) | N(abs) | N(abs) | A |
| Textile classification | M6 Rijken 2026 (thesis) | Y | N(abs) | N(abs) | N(abs) | N(abs) | A |
| Textile quantification | Q1 Stipanovic et al. 2025 | Y (RMSEP) | N(abs) | N(abs) | N(abs) | ? ("10% accuracy threshold in industrial sorting") | A |
| Textile quantification | Q1b Lilek et al. 2026 | Y (RMSEP) | N(abs) | N(abs) | N(abs) | N(abs) | A |
| Textile sorting review | M2 Faghih et al. 2025 | Y | ? | ? | ? | ? | A |
| Textile sorting review | MDPI *Technologies* 2026 (PRISMA, pre-processing) | ? | ? | ? | ? | ? | A — **must read** |
| Textile AI review | AI in textile value chain review 2026 (ScienceDirect, title not confirmed) | Y | ? | ? | ? | ? ("reuse/resale sensitive to misclassification") | A — **must read** |
| Textile contaminants | ORNL / AIChE contaminant investigation (textile-to-textile) | Y (CV/FTIR/ML) | N(abs) | N(abs) | **Y** (per-garment minimum content for stream to meet target purity) | **Y** (recycler target purity) | A — **closest textile work; must read** |
| Textile recycling simulation | S1 Zammori et al. (ABM collection) | N(abs) | N(abs) | N(abs) (collection logistics) | N(abs) | N(abs) | A |
| Textile recycling simulation | S2 Valtere et al. 2025 (SD) | N(abs) | N(abs) | sorting capacity (aggregate) | N(abs) | N(abs) | A |
| Textile recycling simulation | S3 Windsor 2019 (DES) | N(abs) | N(abs) | ? | ? | ? | A — **must read** |
| Plastics SBS simulation | S5 TU Delft Monte Carlo of false calls | Y (error types) | N(abs) | N(abs) | Y (product grade) | N(abs) | A |
| Plastics SBS simulation | S6 VTT/Leoben 2026 DES | Y (sorting step) | N(abs) | throughput | Y (purity/recovery) | N(abs) | A |
| Plastics SBS | S7 DETRITUS 2020 | Y (detection vs discharge errors) | N(abs) | occupancy | Y | N(abs) | A |
| Plastics SBS | S8 *Polymers* 2023 operating point | Y | N(abs) | N(abs) | Y | N(abs) | A |
| Human–AI decision systems | T1 Chow 1970; T2 Geifman & El-Yaniv 2017 | Y | **Y** (reject option) | N (abstention cost abstract) | — | — | A |
| Human–AI decision systems | H1 Alves et al. (2024) Cost-sensitive learning to defer to multiple experts with workload constraints (DeCCaF), *TMLR* | Y | **Y** (learning to defer) | **Y** (per-expert capacity limits; constraint programming assignment) | N (fraud detection, no material aggregation) | N | A |
| Human–AI decision systems | H2 "Sample efficient learning of predictors that complement humans" (arXiv 2207.09584) | Y | **Y** (joint classifier–rejector) | N(abs) | N | N | A |
| Human–AI sorting (waste) | H3 recAIcle project (Pro2Future / FFG, Austria): human-guided sensor-based sorting; worker removals act as labels | ? | **Y** (human–machine integration) | ? | ? | ? | A (project page) |
| OM: quality-uncertain returns | O1 Galbreth & Blackburn (2006) Optimal acquisition and sorting policies for remanufacturing, *POM* 15(3) 384–392; (2010) *POM* 19(1) 61–69 | — | N | N(abs) | — | grading/disposition under condition uncertainty | A |
| OM: quality-uncertain returns | O2 Karamouzian et al. — grading-based acquisition, M/G/1 priority queue | — | N | **Y** (queue) | — | **Y** (remanufacture vs scrap by grade) | A |
| OM: quality-uncertain returns | O3 UWM dissertation — capacitated reverse logistics with quality threshold (CTMC) | — | N | **Y** (capacity) | — | **Y** (acceptance threshold) | A |

## Gaps in this matrix that a reviewer would hit first

1. **Learning to defer — RESOLVED (partly).** Capacity-constrained deferral to human
   experts exists in ML (H1, DeCCaF, TMLR 2024). Therefore capacity-aware delegation
   *per se* is **not** a contribution of this paper. Our policy B is a simple
   instance of that idea; the contribution is what happens to it when outcomes are
   aggregated into bales and judged by a recycler, and when part of the contamination
   is generated after the decision (discharge, manual errors).
2. **OM on quality-uncertain returns — PARTLY CHECKED.** Grading/disposition and
   capacitated acceptance-threshold models exist for remanufacturing (O1–O3). They
   treat items or cores individually; none found aggregates items into lots judged on
   mean purity. Lot-level acceptance sampling literature still not searched.
3. **Full text of the ORNL/AIChE textile contaminant paper** — the closest textile work.

## Gap statement status

Current safe wording (supported by matrix at evidence level A):

> We found no textile-sorting study that links a confidence-based delegation policy
> and finite manual capacity to bale-level purity and recycler acceptance.

The stronger wording proposed by the supervisor ("Existing research has not adequately
characterised how confidence-based human–AI delegation interacts with manual processing
capacity and downstream bale acceptance…") is **not yet supported** and should not be
used until items 1–3 above are checked in full text.
