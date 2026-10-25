# Simulation assumptions (read before interpreting any simulated result)

`simulate_survey.py` answers every item of `03_survey_instrument.md` for 50 **fictional** firms (`SIM01`–`SIM50`, column `SIMULATED = YES`). Each firm belongs to one of seven archetypes based on how Indian textile and apparel clusters are commonly organised. **The fsQCA can only recover patterns that these assumptions put into the data.** The simulation therefore tests the pipeline and rehearses the write-up; it is not evidence, and no claim about Indian exporters may rest on it.

## Archetypes and the context behind them

| Archetype (n) | Firms | Context it represents | What is assumed about readiness |
|---|---|---|---|
| Vertically integrated large exporter (9) | SIM01–09 | Large mill-to-garment groups in Bengaluru, Tiruppur/Coimbatore and NCR, with in-house dyeing and ERP, supplying large EU brands | High if digital traceability is high; data held on paper cap readiness |
| Networked SME with trusted partners (9) | SIM10–18 | Tiruppur and Bengaluru knitters with long-standing job-work dyehouses, often GOTS-certified, with strong EU (often German/Nordic) buyers | Rises with digital traceability and supplier sharing; falls as appropriation concern rises |
| Pressured, subcontractor-dependent, suspicious (10) | SIM19–28 | NCR fashion and embroidery houses and Tiruppur firms that rely on job work and serve fast-fashion buyers, wary of being bypassed | Low regardless of pressure |
| Digitised but isolated (6) | SIM29–34 | Firms that bought a traceability platform at a buyer's urging but source yarn and fabric through agents in Surat and Erode | Moderate ceiling: "systems without data" |
| Low-pressure traditional exporter (7) | SIM35–41 | Ludhiana woollens/hosiery and Kolkata/Panipat firms selling to smaller EU importers who mainly ask for OEKO-TEX and test reports | Low; rises a little with digital traceability |
| Certification-led recycler (5) | SIM42–46 | Panipat recycled-yarn home-textile firms (GRS transaction certificates) and a GOTS-chain firm | Moderate-to-high via certification paperwork even with modest digital systems |
| Upstream spinner/fabric mill (4) | SIM47–50 | Coimbatore spinners and fabric mills supplying exporters; cotton bought through traders and CCI | Moderate; farm and gin origin unknown |

## Item-level rules (to keep answers internally consistent)

* **Section C (integration):** garment makers always have cutting/sewing and packing in-house; extra stages are added in the order dyeing → knitting/weaving → embroidery → spinning. Mills and spinners follow their own stage order.
* **Checklists** follow a realistic order of difficulty, with small random swaps:
  * DTC: digital certificates < lot recording < integrated system < structured exchange < one-day trace-back
  * DPR: fibre content < test results < chemicals < facility of each stage < care/end-of-life < footprint < full machine-readable set
  * CSC: waste sale < surplus resale < recycled fibre < design-for-recycling < take-back
* **Supplier sharing (Section E):** origin data ≥ chemical data ≥ disclosure of sub-suppliers, and never more than the number of key suppliers.
* **Likert items** are scattered (SD 0.9) around a firm-level mean and clipped to 1–7.
* **Certification (item B7, v0.3):** firms holding a chain-of-custody scheme (GOTS, GRS/RCS, OCS) ship a share of EU volume under transaction certificates that depends on archetype (certification-led 70–98%, networked 40–85%, mills 25–60%, integrated 25–70%, others lower); firms without one ship 0%. Certificates are assumed to add up to about one readiness category at full coverage, and to drive readiness for certification-led firms. *This assumption creates the "borrowed" mechanism.*
* **Circularity** depends on archetype, not on readiness. Informal practices (waste sale, surplus resale) are common everywhere, and formal practices are concentrated in large and certification-led firms. *This assumption drives the decoupling result for formal circularity.*
* **Deliberate imperfection:** random noise (SD 0.6 on readiness) and archetype overlap produce contradictory and deviant cases, as in real data.
* Seed `20261201`; rerunning the script reproduces the same 50 firms exactly. (Adding item B7 in v0.3 changed the random draws, so v0.3 firms differ from v0.2.)
* Every simulated firm has at least one informal circular practice, by construction: archetypes start at one or more practices, and waste sale is the easiest item.

## What the simulation can and cannot tell you

| Can | Cannot |
|---|---|
| Show that the questionnaire → calibration → truth table → solution pipeline works | Tell you which routes to readiness actually exist in India |
| Show how many truth-table rows a 50-firm sample is likely to fill (14 of 32 here) | Support or refute P1–P4 |
| Reveal design issues (e.g., buyer pressure is too common to discriminate between firms; the overall circularity count mixes informal and formal practice) | Justify the theory in the illustrative manuscript, which must be re-derived from the real data |
| Let you rehearse the results and discussion write-up | Be cited, reported or submitted in any form |
