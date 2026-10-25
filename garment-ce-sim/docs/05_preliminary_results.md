# 5. Preliminary results (model v0.1 — not yet publication-grade)

These numbers come from calibrated parameters (`results/calibration.json`), averaged over 20
seeds (scenarios) or 10 seeds (displacement, law-cap test). They are conditional on the
assumption-flagged parameters in `data/parameters.csv` and on the open questions in
`04_open_questions.md`.

## 5.1 Calibration and validation (Fig. 1)

- **Collected tonnage, 2014–2024:** NRMSE 4.4 %. Every year is within ±4 % except 2014 (+9 %).
- **Sorted tonnage:** within ±5 % for 2022–2024. 2019 is −11 %, and sources disagree for that
  year (196 vs 187 kt).
- **Second-hand:**
  - domestic sales: 64.8 vs 62.4 kt (2024);
  - share of consumers buying second-hand: 0.43 vs 0.38 (2023);
  - items per buyer: 7.9 vs 7.3 (2023).
- **Operator count** (not fitted): 63 vs 66–67 (2022–23).
- **2025 hold-out:**

  | Variable | Error |
  |---|---|
  | Sorted | −0.5 % |
  | Placed on market | −4 % |
  | Used-textile exports | +7 % |
  | Collected | +12 % |
  | Second-hand sales | +12 % |

  The collection miss matches an event the model does not include: Le Relais (more than 60 % of
  capacity) halted collection in summer 2025. The 2025 shortfall in placed-on-market tonnage
  reflects the BAU demand-growth assumption of 1 %/yr.
- **Calibrated values worth discussing:**
  - collection points in 2010 ≈ 11,900, which has no source (Q7);
  - capacity margin sensitivity ≈ 0, meaning sorting capacity tracked inflow regardless of margins
    before 2024. This is consistent with operators surviving negative margins (social-economy
    model).

## 5.2 Scenarios, cumulative 2025–2040, difference vs BAU (Fig. 2; `results/scenario_delta_vs_bau.csv`)

BAU totals: new items 13.95 Mt, virgin fibre 13.9 Mt, GHG 290 MtCO2e.

| Scenario | New items | Virgin fibre | GHG | Overflow | Exports | Residual waste |
|---|---|---|---|---|---|---|
| S1 Anti-fast-fashion law | −1.65 Mt (−11.8 %) | −1.65 Mt | −34.1 Mt (−11.8 %) | −18 kt | −271 kt | −813 kt |
| S2 Collection push (60 % target) | ≈0 | ≈0 | −1.9 Mt (−0.6 %) | **+188 kt** | **+736 kt** | −1,207 kt |
| S3 Sorting + recycling support | −24 kt | −54 kt | −0.5 Mt | −7 kt | ≈0 | −12 kt |
| S4 Recycled-content mandate (hyp.) | 0 | −31 kt | +0.05 Mt | 0 | 0 | 0 |
| S5 Combined | −1.64 Mt | −1.70 Mt (−12.2 %) | −35.6 Mt (−12.3 %) | +79 kt | +382 kt | −1,861 kt |

The rankings are unchanged under an export-price collapse (X1) and under 3 %/yr demand growth (X2).

## 5.3 Hypotheses

- **H1 (displacement < 1): supported.** Displacement emerges at **0.47** in the central case. Across
  the rebound × residual-life grid it ranges from 0.12 to 0.98, and the GSA 90 % interval is
  0.20–0.78. That brackets the empirical 0.28 (WRAP) to 0.60–0.85 (Farrant et al. 2010). Sobol:
  residual life of second-hand items explains about 66 % of variance, rebound about 24 %
  (Fig. 3, 4).
- **H2 (collection without sorting creates overflow): supported.**
  - Raising collection to about 52 % of POM moves tonnage out of residual waste (−1.2 Mt).
  - But most of it goes to exports (+0.74 Mt) and overflow (+0.19 Mt) during the capacity ramp-up.
  - Virgin fibre demand does not change.
- **H3 (law works mainly through segment mix): not supported.**
  - The law cuts new-item tonnage mainly by pushing brands above the 0.8 durability threshold:
    the durability index rises from 0.81 to 0.90 and longer life means fewer replacements.
  - The ultra-fast share falls only from 15 % to 12 %.
  - The 50 % price cap barely matters (−11.4 % with cap vs −11.9 % without), and even a half-size
    penalty gives −10.2 %. The effect saturates once the penalty exceeds the upgrade cost.
  - So the policy-relevant uncertainty is **whether durability scores translate into longer use**.
    Sobol on S5's effect: price elasticity ST 0.39, durability→lifetime elasticity ST 0.26.
- **H4 (demand-side levers beat end-of-life levers): supported.**
  - The law delivers about 18× the GHG reduction of the collection push, and about 70× that of
    sorting and recycling support.
  - Domestic fibre-to-fibre recycling stays below about 20 kt/yr by 2040. It is limited by capacity
    ramp-up and by recyclable feedstock (mono-material share of discards).
  - Under central GHG factors, diverting open-loop recycling to fibre-to-fibre is roughly neutral
    for GHG. This is sensitive to `fibre_stage_share` and `f2f_rel_impact`.

## 5.4 Robustness (Sobol, N=128, 21 parameters; `results/sobol_indices.csv`)

- The combined-policy reduction in virgin fibre is positive in every sample: 1.3–3.1 Mt (90 % interval).
- BAU virgin fibre is dominated by the demand-growth assumption (ST 0.86).
- BAU GHG is dominated by the production emission factor (ST 0.88).
  → **Report policy results as differences vs BAU**, which are far more robust than absolute levels.

## 5.5 Caveats before writing the paper

1. All data values were collected via web search. Verify them against primary PDFs (Q7).
2. Behavioural parameters are assumptions within ranges, so results are conditional (Q6).
3. The penalty mapping is simplified (Q4). The mandate scenario is hypothetical (Q5).
4. GHG uses a France-consumption boundary. Exported used textiles carry no impacts or credits abroad (Q2).
5. The model has no explicit space and an annual time step. The AnyLogic port can add both.
