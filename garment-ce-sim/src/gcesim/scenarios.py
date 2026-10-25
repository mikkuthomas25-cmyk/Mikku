"""Policy scenarios and stress tests (projection period 2025-2040).

Keys understood by the model:
  sorting_support      {year: EUR/t}             after last observed year (2026)
  collection_points    {year: points per 1000 inhabitants}
  export_price         {year: EUR/t}
  awareness            {year: multiplier on collection attractor}
  ff_law               bool, loi n° 2026-602 durability penalty
  law_mult             scale on the penalty band
  law_cap              apply the 50 % of pre-tax price cap (default True)
  bonus_mult/malus_mult  scale on existing eco-modulation bonus/malus
  recycled_bonus_frac  recycled-content bonus as fraction of virgin fibre price
  recycled_content     {year: required recycled share of new mass}
  demand_growth        trend growth of desired wardrobe
  capacity_shock       {year: multiplier on sorting capacity}
"""
from __future__ import annotations

S0 = {
    "name": "BAU",
    "sorting_support": {2027: 268, 2040: 268},
}

SCENARIOS = {
    "S0_BAU": S0,
    "S1_FF_LAW": {**S0, "name": "Anti-fast-fashion law", "ff_law": True},
    "S2_COLLECTION": {**S0, "name": "Collection push", "collection_points": {2028: 1.0, 2040: 1.0},
                      "awareness": {2025: 1.0, 2030: 1.2, 2040: 1.2}},
    "S3_SORTING_F2F": {**S0, "name": "Sorting + recycling support", "sorting_support": {2027: 304, 2040: 304},
                       "recycled_bonus_frac": 0.3, "bonus_mult": 2.0},
    "S4_RC_MANDATE": {**S0, "name": "Recycled-content mandate (hypothetical)",
                      "recycled_content": {2029: 0.0, 2030: 0.05, 2035: 0.15, 2040: 0.25}},
}
SCENARIOS["S5_COMBINED"] = {
    **SCENARIOS["S1_FF_LAW"], **{k: v for k, v in SCENARIOS["S2_COLLECTION"].items() if k != "name"},
    **{k: v for k, v in SCENARIOS["S3_SORTING_F2F"].items() if k != "name"},
    "recycled_content": SCENARIOS["S4_RC_MANDATE"]["recycled_content"], "name": "Combined",
}

STRESS = {
    "none": {},
    "X1_EXPORT_COLLAPSE": {"export_price": {2028: 250, 2040: 250}},
    "X2_DEMAND_GROWTH": {"demand_growth": 0.03},
}


def combine(scenario: str, stress: str = "none") -> dict:
    sc = dict(SCENARIOS[scenario])
    sc.update(STRESS[stress])
    return sc
