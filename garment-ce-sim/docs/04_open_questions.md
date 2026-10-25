# 4. Open questions for the author (decisions that change the model)

**Q1. Placed-on-market jump after 2019.** POM rose from 648 kt (2019) to 886 kt (2024).
- Current choice: treat it as real consumption (forced in calibration).
- Alternative: assume part is improved declaration coverage (e.g. marketplaces joining), and add
  a "declared share" correction.
- Why it matters: it changes the wardrobe stock and therefore future discards.

**Q2. GHG system boundary.**
- Current choice: consumption-based for France. Exported used textiles count only as transport
  and as tonnage, with no credit for displacing new clothing abroad and no burden for disposal
  abroad.
- Alternative: an extended boundary that credits or burdens exports. This would change the
  ranking of collection versus demand-side levers.

**Q3. Scope: TLC vs garments only.**
- French data cover TLC (apparel, household linen, footwear) together, so the model uses TLC.
- Splitting out garments would need assumed shares and would weaken calibration.
- Is "garment sector" in the title acceptable with a TLC scope, or should it be reworded to
  "textile and apparel"?

**Q4. How the loi 2026-602 penalty maps to products.**
- The model applies the band (0.5–12 € until 2029, 2–19.5 € from 2030) to items with durability
  coefficient < 0.8, scaled linearly by the durability gap and capped at 50 % of the pre-tax price.
- The real amount depends on the product's environmental-cost score and on implementing decrees.
- If you have the decree or Refashion's 2026 scale, I will replace the mapping.

**Q5. Recycled-content mandate (scenario S4).**
- No adopted French or EU recycled-content obligation for textiles was found. S4 is labelled
  hypothetical (5 % in 2030, 15 % in 2035, 25 % in 2040).
- Keep, change, or drop it?

**Q6. Behavioural priors.**
- These are modelling assumptions, tested by Sobol analysis: rebound, second-hand residual life,
  price elasticity, segment shares.
- With journal access (Scite and Consensus quotas were exhausted in this session), a structured
  literature review could narrow the ranges.
- Do you have institutional database access (Scopus or Web of Science)?

**Q7. Data verification.**
- Primary PDFs (Refashion annual reports, ADEME) could not be downloaded from this environment;
  values were collected via web search.
- Can you download the Refashion/Eco TLC annual reports 2010–2024 into `data/raw/`? I will then
  verify every value and fill the gaps.
