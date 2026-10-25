# 1. Research design

## Working title

**Does textile EPR close the loop? A calibrated hybrid system-dynamics / agent-based
simulation of policy, consumer rebound and sorting capacity in France's TLC sector.**

## Why France

France has the world's longest-running textile EPR scheme for apparel, linen and footwear
(TLC). It started with Eco TLC in 2009 and is now run by Refashion. That gives about 15 years
of observed data to calibrate against:
- tonnage placed on the market;
- tonnage collected and sorted;
- sorted output fractions;
- collection points;
- sorting support per tonne.

See `03_data.md`. France is also changing its rules mid-stream, which turns it into a natural
policy laboratory:

| Change | Year | Source |
|---|---|---|
| Recyclability malus becomes mandatory | 2025 | REFFEE25 |
| Sorting support raised from 156 → 223 → 268 €/t | 2025–2026 | FN25, AE26 |
| Sorting crisis: saturation from summer 2024; Le Relais (>60 % of capacity) halts collection in summer 2025 | 2024–2025 | REF6M, TI25 |
| **Loi n° 2026-602 (8 July 2026)**: penalty per item for products with durability coefficient < 0.8, rising to about 19.5 € per item from 2030 | 2026 | LAW26 |
| 60 % collection target | 2028 | RECY25 |

None of these interventions can be evaluated empirically yet. Their outcomes come from
interactions between consumers, brands and sorters, delayed by product lifetimes and capacity
build-up. That is exactly the case for simulation.

## Problem statement

EPR is meant to internalise end-of-life costs and push design towards durability and
recyclability. In France, collection has risen from about 2 to about 4.3 kg per inhabitant
(OCC20B, REF24). But the tonnage placed on the market grew faster, from about 600 kt (2015) to
886 kt (2024). Sorting capacity saturated, export prices for used clothing fell (471 €/t in
2025, −8.2 %), and the material-recycling share of sorted output is only about 24 % (REF24).

Whether the new levers reduce **net virgin fibre demand and life-cycle GHG**, or only move
tonnage around, depends on three mechanisms that are poorly quantified:

1. **Displacement and rebound.** How many new items does one second-hand purchase replace? The
   literature ranges from about 28 % (WRAP) to 60–85 % (Farrant et al. 2010). Displacement
   cannot be observed at system level.
2. **Capacity mismatch.** When collection grows faster than sorting capacity and export demand,
   the surplus overflows to unsorted export or incineration.
3. **Design response.** Fees and penalties change brands' durability and recyclability only if
   the fee saving is large relative to unit cost. The fast-fashion penalty is capped at 50 % of
   the pre-tax price.

## Research questions

- **RQ1 (displacement).** Given French price levels and resale-platform diffusion, what system-level
  displacement rate emerges from second-hand purchasing? How sensitive is it to rebound spending
  and to the shorter residual life of second-hand items?
- **RQ2 (capacity).** Under the 2025–2026 sorting-support increases and the 60 % collection target,
  does sorting capacity keep pace? How much tonnage overflows to unsorted export or incineration,
  and what happens if export markets collapse?
- **RQ3 (design and the 2026 law).** Does the durability penalty of loi 2026-602, together with
  existing eco-modulation, shift the market mix and durability enough to reduce new-item tonnage?
  Does the 50 % price cap blunt it?
- **RQ4 (policy mix).** Which combinations of levers (fast-fashion penalty, collection push,
  sorting and recycling support, recycled-content mandate) robustly reduce cumulative virgin
  fibre demand and GHG over 2025–2040 across the uncertainty space?

## Hypotheses (falsifiable within the model)

- **H1.** The emergent displacement rate is well below 1. With central rebound and residual-life
  assumptions, it falls in the empirical 0.3–0.85 band.
- **H2.** Collection-push policies without matching sorting capacity raise overflow (unsorted export
  plus incineration) more than they raise reuse and recycling.
- **H3.** The fast-fashion penalty reduces new-item tonnage mainly through the segment mix (price),
  not through durability upgrades. Its effect is bounded by the 50 % price cap.
- **H4.** Demand-side levers (penalty, durability) reduce virgin fibre demand more than
  end-of-life levers (collection, sorting, fibre-to-fibre recycling) by 2040. This would echo the
  EU-level SD finding in CEJ25.

## Method in one paragraph

A hybrid model:
- **Agent-based layer.** About 3,000 consumer agents on a small-world network: purchase,
  second-hand, disposal, collection habit, and Bass diffusion of resale platforms. 40 brand agents
  in four segments: logit market shares, plus durability and recyclability upgrade decisions under
  fees. 67 sorting-operator agents, including one dominant network: capacity, margin and exit.
- **System-dynamics layer.** Material stocks and flows in kt (wardrobe, sorting backlog, sorted
  fractions, exports, fibre-to-fibre capacity with learning), plus GHG accounting.
- **Calibration and validation.**
  - Historical drivers, 2010–2024: purchases are forced to observed placed-on-market tonnage, and
    collection points, sorting support and net sorting cost follow observations.
  - Calibration: differential evolution with common random numbers, fitted to collected and
    sorted tonnage, second-hand sales and reuse shares.
  - Hold-out validation: 2025 data (placed on market, collected, sorted, second-hand sales,
    exports).
- **Experiments.** Six scenarios × stress tests × seeds. Sobol global sensitivity analysis.
  Displacement estimated by paired runs with the second-hand channel switched off.

## Expected contribution

1. The first calibrated simulation of a real textile EPR scheme that links consumer rebound,
   brand design response and sorting-capacity dynamics. Existing work treats them separately:
   - static material-flow analysis or LCA;
   - an EU-level SD model without agents (CEJ25);
   - an ABM of reuse and rebound without the EPR or sorting system (ZAND26).
2. An ex-ante evaluation of France's 2026 anti-fast-fashion law.
3. An estimate of displacement as an *emergent* quantity with uncertainty bounds, not an assumed input.
4. Open, reproducible code with an ODD protocol, ready to port to AnyLogic for visualisation.

## Limitations to state up front

- Many behavioural parameters are assumptions. They are listed with ranges in
  `data/parameters.csv` and tested by global sensitivity analysis.
- The jump in placed-on-market tonnage after 2019 may partly reflect better declaration coverage
  (more producers and marketplaces declaring), not only consumption growth. See open question Q1.
- GHG factors come from secondary sources and must be replaced by Base Empreinte / Ecoinvent
  values before publication.
- Accounting is consumption-based for France. Impacts of exported used textiles abroad are
  reported as flows, not as impacts.
