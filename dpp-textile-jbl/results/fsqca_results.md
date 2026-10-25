# fsQCA results: SIMULATED DATA (pipeline test, not findings)

> **These results come from simulated responses (`data/SIMULATED_responses.csv`). They reflect the assumptions in `simulation/SIMULATION_ASSUMPTIONS.md` and must not be reported as evidence.**

N = 50 firms. Source file: `data/SIMULATED_responses.csv`.

## 1. Descriptives and reliability

Cronbach's α: BTP = 0.79; DAC = 0.87.

| Set | Raw mean | Raw min–max | Anchors (out / cross / in) | Share of cases > 0.5 |
|---|---|---|---|---|
| DPR | 3.40 | 0.0–7.0 | 1 / 3.5 / 6 | 0.54 |
| CSC | 2.52 | 1.0–5.0 | 0 / 2.5 / 4 | 0.46 |
| DTC | 2.96 | 0.0–5.0 | 1 / 2.5 / 4 | 0.64 |
| VI | 2.92 | 2.0–6.0 | 1 / 3.5 / 5 | 0.24 |
| MDS | 42.79 | 10.0–93.3 | 10 / 50 / 90 | 0.38 |
| BTP | 5.12 | 2.3–7.0 | 2 / 4 / 6 | 0.82 |
| DAC | 3.81 | 1.0–6.3 | 2 / 4 / 6 | 0.40 |
| CERT | 2.20 | 0.0–6.0 | 0 / 1.5 / 3 | 0.62 |
| CSCF | 0.86 | 0.0–3.0 | 0 / 1.5 / 3 | 0.22 |

## 2. Necessity analysis (threshold: consistency ≥ 0.90, RoN ≥ 0.60)

**Outcome: DPR**

| Condition | Consistency | Coverage | RoN |
|---|---|---|---|
| VI | 0.628 | 0.848 | 0.92 |
| DTC | 0.894 | 0.712 | 0.682 |
| MDS | 0.7 | 0.869 | 0.921 |
| BTP | 0.939 | 0.606 | 0.444 |
| DAC | 0.373 | 0.41 | 0.678 |
| ~VI | 0.639 | 0.494 | 0.531 |
| ~DTC | 0.285 | 0.366 | 0.718 |
| ~MDS | 0.545 | 0.443 | 0.541 |
| ~BTP | 0.244 | 0.501 | 0.864 |
| ~DAC | 0.837 | 0.743 | 0.758 |

Necessary conditions: none.

**Outcome: ~DPR**

| Condition | Consistency | Coverage | RoN |
|---|---|---|---|
| VI | 0.367 | 0.513 | 0.782 |
| DTC | 0.523 | 0.431 | 0.521 |
| MDS | 0.339 | 0.435 | 0.73 |
| BTP | 0.765 | 0.512 | 0.392 |
| DAC | 0.72 | 0.82 | 0.874 |
| ~VI | 0.891 | 0.713 | 0.666 |
| ~DTC | 0.65 | 0.864 | 0.922 |
| ~MDS | 0.898 | 0.756 | 0.729 |
| ~BTP | 0.411 | 0.875 | 0.962 |
| ~DAC | 0.483 | 0.443 | 0.592 |

Necessary conditions: none.

## 3. Sufficiency: high DPP readiness

Thresholds: raw consistency ≥ 0.80, PRI ≥ 0.70, frequency ≥ 1. Rows with cases only (remainders omitted).

| VI | DTC | MDS | BTP | DAC | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 1 | 0 | 7 | 0.991 | 0.982 | 1 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08 |
| 1 | 1 | 0 | 1 | 0 | 2 | 0.974 | 0.939 | 1 | SIM01, SIM09 |
| 0 | 1 | 1 | 1 | 0 | 9 | 0.961 | 0.915 | 1 | SIM10, SIM11, SIM12, SIM13, SIM14, SIM15, SIM16, SIM17, SIM18 |
| 0 | 1 | 1 | 1 | 1 | 1 | 0.939 | 0.767 | 1 | SIM43 |
| 0 | 0 | 1 | 1 | 0 | 1 | 0.922 | 0.744 | 1 | SIM44 |
| 0 | 1 | 0 | 0 | 0 | 1 | 0.896 | 0.503 | 0 | SIM48 |
| 1 | 0 | 0 | 1 | 0 | 1 | 0.868 | 0.563 | 0 | SIM42 |
| 0 | 0 | 1 | 0 | 0 | 1 | 0.816 | 0.298 | 0 | SIM46 |
| 0 | 1 | 0 | 1 | 0 | 5 | 0.802 | 0.509 | 0 | SIM29, SIM45, SIM47, SIM49, SIM50 |
| 0 | 1 | 0 | 1 | 1 | 7 | 0.622 | 0.266 | 0 | SIM21, SIM23, SIM30, SIM31, SIM32, SIM33, SIM34 |
| 1 | 0 | 0 | 0 | 1 | 2 | 0.622 | 0.052 | 0 | SIM35, SIM40 |
| 0 | 0 | 0 | 0 | 0 | 3 | 0.547 | 0.027 | 0 | SIM36, SIM38, SIM39 |
| 0 | 0 | 0 | 0 | 1 | 2 | 0.507 | 0.026 | 0 | SIM37, SIM41 |
| 0 | 0 | 0 | 1 | 1 | 8 | 0.380 | 0.087 | 0 | SIM19, SIM20, SIM22, SIM24, SIM25, SIM26, SIM27, SIM28 |

Rows with ≥ 1 case: 14 of 32.

**Intermediate solution for high readiness (DPR)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| MDS * BTP * ~DAC | MDS, BTP | 0.933 | 0.648 | 0.043 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08, SIM10, SIM11, SIM12, SIM13, SIM14, SIM15, SIM16, SIM17, SIM18, SIM44 | – |
| DTC * MDS * BTP | MDS, BTP | 0.956 | 0.63 | 0.025 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08, SIM10, SIM11, SIM12, SIM13, SIM14, SIM15, SIM16, SIM17, SIM18, SIM43 | – |
| VI * DTC * BTP * ~DAC | VI, DTC | 0.982 | 0.527 | 0.093 | SIM01, SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08, SIM09 | – |

Solution consistency = 0.924; solution coverage = 0.766.
Complex solution: ~VI * MDS * BTP * ~DAC + ~VI * DTC * MDS * BTP + VI * DTC * BTP * ~DAC
Parsimonious solution: MDS * BTP + VI * DTC

## 4. Sufficiency: low DPP readiness (~DPR)

| VI | DTC | MDS | BTP | DAC | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 0 | 0 | 0 | 0 | 0 | 3 | 0.988 | 0.973 | 1 | SIM36, SIM38, SIM39 |
| 0 | 0 | 0 | 0 | 1 | 2 | 0.987 | 0.974 | 1 | SIM37, SIM41 |
| 1 | 0 | 0 | 0 | 1 | 2 | 0.979 | 0.948 | 1 | SIM35, SIM40 |
| 0 | 0 | 0 | 1 | 1 | 8 | 0.941 | 0.913 | 1 | SIM19, SIM20, SIM22, SIM24, SIM25, SIM26, SIM27, SIM28 |
| 0 | 0 | 1 | 0 | 0 | 1 | 0.922 | 0.702 | 1 | SIM46 |
| 0 | 1 | 0 | 0 | 0 | 1 | 0.878 | 0.414 | 0 | SIM48 |
| 1 | 0 | 0 | 1 | 0 | 1 | 0.830 | 0.437 | 0 | SIM42 |
| 0 | 1 | 0 | 1 | 1 | 7 | 0.817 | 0.645 | 0 | SIM21, SIM23, SIM30, SIM31, SIM32, SIM33, SIM34 |
| 0 | 1 | 1 | 1 | 1 | 1 | 0.800 | 0.233 | 0 | SIM43 |
| 0 | 1 | 0 | 1 | 0 | 5 | 0.795 | 0.491 | 0 | SIM29, SIM45, SIM47, SIM49, SIM50 |
| 0 | 0 | 1 | 1 | 0 | 1 | 0.772 | 0.256 | 0 | SIM44 |
| 1 | 1 | 0 | 1 | 0 | 2 | 0.607 | 0.061 | 0 | SIM01, SIM09 |
| 0 | 1 | 1 | 1 | 0 | 9 | 0.552 | 0.019 | 0 | SIM10, SIM11, SIM12, SIM13, SIM14, SIM15, SIM16, SIM17, SIM18 |
| 1 | 1 | 1 | 1 | 0 | 7 | 0.517 | 0.018 | 0 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08 |

**Intermediate solution for low readiness (~DPR)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| ~DTC * ~MDS * DAC | ~DTC, DAC | 0.949 | 0.478 | 0.267 | SIM19, SIM20, SIM22, SIM24, SIM25, SIM26, SIM27, SIM28, SIM35, SIM37, SIM40, SIM41 | – |
| ~VI * ~DTC * ~BTP | ~DTC, ~BTP | 0.957 | 0.299 | 0.087 | SIM36, SIM37, SIM38, SIM39, SIM41 | SIM46 |

Solution consistency = 0.94; solution coverage = 0.566.
Complex solution: ~DTC * ~MDS * ~BTP * DAC + ~VI * ~DTC * ~BTP * ~DAC + ~VI * ~DTC * ~MDS * DAC
Parsimonious solution: ~DTC * DAC + ~DTC * ~BTP

## 5. Readiness and circularity (P4)

Sufficiency of DPR alone for CSC: consistency = 0.837, coverage = 0.788 (P4 expects consistency < 0.80).

| Condition | Consistency | Coverage | RoN |
|---|---|---|---|
| DPR | 0.788 | 0.837 | 0.864 |
| VI | 0.646 | 0.925 | 0.959 |
| MDS | 0.632 | 0.832 | 0.901 |
| BTP | 0.875 | 0.6 | 0.44 |
| CERT | 0.89 | 0.755 | 0.719 |
| ~DPR | 0.456 | 0.467 | 0.645 |
| ~VI | 0.619 | 0.508 | 0.538 |
| ~MDS | 0.614 | 0.53 | 0.583 |
| ~BTP | 0.3 | 0.654 | 0.902 |
| ~CERT | 0.288 | 0.39 | 0.724 |

| DPR | VI | MDS | BTP | CERT | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 0 | 1 | 1 | 3 | 1.000 | 1.000 | 1 | SIM01, SIM09, SIM42 |
| 1 | 0 | 1 | 0 | 1 | 1 | 1.000 | 1.000 | 1 | SIM46 |
| 1 | 0 | 0 | 0 | 1 | 1 | 0.995 | 0.981 | 1 | SIM48 |
| 1 | 1 | 1 | 1 | 1 | 7 | 0.981 | 0.966 | 1 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08 |
| 1 | 0 | 1 | 1 | 0 | 1 | 0.912 | 0.092 | 0 | SIM16 |
| 0 | 1 | 0 | 0 | 0 | 2 | 0.905 | 0.550 | 0 | SIM35, SIM40 |
| 1 | 0 | 0 | 1 | 1 | 4 | 0.898 | 0.759 | 1 | SIM34, SIM45, SIM47, SIM50 |
| 1 | 0 | 1 | 1 | 1 | 10 | 0.866 | 0.751 | 1 | SIM10, SIM11, SIM12, SIM13, SIM14, SIM15, SIM17, SIM18, SIM43, SIM44 |
| 0 | 0 | 0 | 1 | 1 | 5 | 0.770 | 0.434 | 0 | SIM19, SIM21, SIM30, SIM33, SIM49 |
| 0 | 0 | 0 | 0 | 0 | 5 | 0.571 | 0.014 | 0 | SIM36, SIM37, SIM38, SIM39, SIM41 |
| 0 | 0 | 0 | 1 | 0 | 11 | 0.474 | 0.006 | 0 | SIM20, SIM22, SIM23, SIM24, SIM25, SIM26, SIM27, SIM28, SIM29, SIM31, SIM32 |

**Intermediate solution for circular capability (CSC)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| DPR * CERT | DPR, CERT | 0.856 | 0.767 | 0.767 | SIM01, SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08, SIM09, SIM12, SIM13, SIM14, SIM15, SIM18, SIM42, SIM43, SIM44, SIM45, SIM46, SIM48, SIM50 | SIM10, SIM11, SIM17, SIM34, SIM47 |

Solution consistency = 0.856; solution coverage = 0.767.
Complex solution: DPR * BTP * CERT + DPR * ~VI * CERT
Parsimonious solution: DPR * CERT

### 5b. Formal circularity (CSC_2, CSC_3, CSC_5 only)

Sufficiency of DPR alone for formal circularity: consistency = 0.519, coverage = 0.878.

| DPR | VI | MDS | BTP | CERT | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 0 | 1 | 0 | 1 | 1 | 0.849 | 0.550 | 0 | SIM46 |
| 1 | 1 | 0 | 1 | 1 | 3 | 0.810 | 0.598 | 0 | SIM01, SIM09, SIM42 |
| 1 | 1 | 1 | 1 | 1 | 7 | 0.781 | 0.592 | 0 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08 |
| 1 | 0 | 0 | 0 | 1 | 1 | 0.754 | 0.297 | 0 | SIM48 |
| 1 | 0 | 0 | 1 | 1 | 4 | 0.655 | 0.337 | 0 | SIM34, SIM45, SIM47, SIM50 |
| 1 | 0 | 1 | 1 | 0 | 1 | 0.651 | 0.000 | 0 | SIM16 |
| 0 | 1 | 0 | 0 | 0 | 2 | 0.570 | 0.000 | 0 | SIM35, SIM40 |
| 1 | 0 | 1 | 1 | 1 | 10 | 0.556 | 0.250 | 0 | SIM10, SIM11, SIM12, SIM13, SIM14, SIM15, SIM17, SIM18, SIM43, SIM44 |
| 0 | 0 | 0 | 1 | 1 | 5 | 0.464 | 0.134 | 0 | SIM19, SIM21, SIM30, SIM33, SIM49 |
| 0 | 0 | 0 | 0 | 0 | 5 | 0.410 | 0.000 | 0 | SIM36, SIM37, SIM38, SIM39, SIM41 |
| 0 | 0 | 0 | 1 | 0 | 11 | 0.232 | 0.000 | 0 | SIM20, SIM22, SIM23, SIM24, SIM25, SIM26, SIM27, SIM28, SIM29, SIM31, SIM32 |

**Intermediate solution for formal circularity (CSCF)**

No configuration passed the consistency thresholds.

## 6. Robustness (high readiness)

| Variant | Intermediate solution | Consistency | Coverage |
|---|---|---|---|
| Base | MDS * BTP * ~DAC + DTC * MDS * BTP + VI * DTC * BTP * ~DAC | 0.924 | 0.766 |
| R1 anchors +10% | MDS * BTP * ~DAC + VI * DTC * BTP * ~DAC | 0.903 | 0.762 |
| R1 anchors −10% | DTC * MDS * BTP + MDS * BTP * ~DAC + VI * DTC * BTP | 0.936 | 0.818 |
| R2 consistency 0.75 | MDS * BTP * ~DAC + DTC * MDS * BTP + VI * DTC * BTP * ~DAC | 0.924 | 0.766 |
| R2 consistency 0.85 | MDS * BTP * ~DAC + DTC * MDS * BTP + VI * DTC * BTP * ~DAC | 0.924 | 0.766 |
| R3 frequency 2 | DTC * MDS * BTP * ~DAC + VI * DTC * BTP * ~DAC | 0.966 | 0.698 |
| R4 CERT replaces BTP | MDS * CERT * ~DAC + DTC * MDS * ~DAC + DTC * MDS * CERT + VI * CERT * ~DAC | 0.952 | 0.78 |
| R5 tiers 1–2 only | MDS * BTP * ~DAC + DTC * MDS * BTP + VI * DTC * BTP * ~DAC | 0.922 | 0.772 |
