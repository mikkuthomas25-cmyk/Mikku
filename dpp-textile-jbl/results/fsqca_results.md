# fsQCA results: SIMULATED DATA (pipeline test, not findings)

> **These results come from simulated responses (`data/SIMULATED_responses.csv`). They reflect the assumptions in `simulation/SIMULATION_ASSUMPTIONS.md` and must not be reported as evidence.**

N = 50 firms. Source file: `data/SIMULATED_responses.csv`.

## 1. Descriptives and reliability

Cronbach's α: BTP = 0.76; DAC = 0.86.

| Set | Raw mean | Raw min–max | Anchors (out / cross / in) | Share of cases > 0.5 |
|---|---|---|---|---|
| DPR | 3.50 | 0.0–7.0 | 1 / 3.5 / 6 | 0.56 |
| CSC | 2.50 | 1.0–5.0 | 0 / 2.5 / 4 | 0.48 |
| DTC | 3.04 | 0.0–5.0 | 1 / 2.5 / 4 | 0.70 |
| VI | 3.00 | 2.0–6.0 | 1 / 3.5 / 5 | 0.34 |
| MDS | 41.80 | 11.9–83.3 | 10 / 50 / 90 | 0.36 |
| BTP | 5.23 | 2.0–7.0 | 2 / 4 / 6 | 0.86 |
| DAC | 3.95 | 1.7–6.7 | 2 / 4 / 6 | 0.56 |
| CERT | 28.90 | 0.0–89.0 | 5 / 50 / 90 | 0.28 |
| SIZE | 2746.34 | 85.0–21250.0 | 50 / 250 / 1000 | 0.84 |
| CSCF | 0.92 | 0.0–3.0 | 0 / 1.5 / 3 | 0.20 |

## 2. Necessity analysis (threshold: consistency ≥ 0.90, RoN ≥ 0.60)

**Outcome: DPR**

| Condition | Consistency | Coverage | RoN |
|---|---|---|---|
| VI | 0.625 | 0.793 | 0.875 |
| DTC | 0.907 | 0.721 | 0.659 |
| MDS | 0.684 | 0.903 | 0.941 |
| CERT | 0.551 | 0.936 | 0.973 |
| DAC | 0.417 | 0.451 | 0.666 |
| ~VI | 0.633 | 0.551 | 0.605 |
| ~DTC | 0.247 | 0.364 | 0.745 |
| ~MDS | 0.537 | 0.456 | 0.542 |
| ~CERT | 0.612 | 0.454 | 0.444 |
| ~DAC | 0.817 | 0.808 | 0.826 |

Necessary conditions: DTC.

**Outcome: ~DPR**

| Condition | Consistency | Coverage | RoN |
|---|---|---|---|
| VI | 0.45 | 0.534 | 0.758 |
| DTC | 0.539 | 0.401 | 0.474 |
| MDS | 0.315 | 0.389 | 0.718 |
| CERT | 0.214 | 0.341 | 0.776 |
| DAC | 0.792 | 0.802 | 0.847 |
| ~VI | 0.826 | 0.673 | 0.678 |
| ~DTC | 0.626 | 0.864 | 0.931 |
| ~MDS | 0.921 | 0.732 | 0.706 |
| ~CERT | 0.96 | 0.667 | 0.567 |
| ~DAC | 0.457 | 0.423 | 0.613 |

Necessary conditions: ~MDS.

## 3. Sufficiency: high DPP readiness

Thresholds: raw consistency ≥ 0.80, PRI ≥ 0.70, frequency ≥ 1. Rows with cases only (remainders omitted).

| VI | DTC | MDS | CERT | DAC | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 1 | 1 | 0 | 3 | 1.000 | 1.000 | 1 | SIM02, SIM05, SIM43 |
| 1 | 1 | 0 | 1 | 0 | 1 | 1.000 | 1.000 | 1 | SIM03 |
| 0 | 1 | 1 | 1 | 0 | 4 | 1.000 | 1.000 | 1 | SIM11, SIM12, SIM13, SIM17 |
| 0 | 1 | 1 | 1 | 1 | 2 | 0.997 | 0.991 | 1 | SIM15, SIM16 |
| 0 | 1 | 0 | 1 | 0 | 1 | 0.996 | 0.985 | 1 | SIM46 |
| 1 | 1 | 0 | 1 | 1 | 1 | 0.992 | 0.958 | 1 | SIM45 |
| 1 | 0 | 1 | 1 | 1 | 1 | 0.990 | 0.948 | 1 | SIM42 |
| 1 | 0 | 0 | 1 | 1 | 1 | 0.984 | 0.925 | 1 | SIM44 |
| 1 | 1 | 1 | 0 | 0 | 3 | 0.966 | 0.930 | 1 | SIM04, SIM06, SIM09 |
| 0 | 1 | 1 | 0 | 0 | 3 | 0.938 | 0.849 | 1 | SIM10, SIM14, SIM18 |
| 0 | 1 | 0 | 0 | 0 | 3 | 0.925 | 0.757 | 1 | SIM47, SIM49, SIM50 |
| 1 | 1 | 1 | 0 | 1 | 2 | 0.892 | 0.660 | 0 | SIM07, SIM08 |
| 1 | 1 | 0 | 0 | 1 | 1 | 0.879 | 0.514 | 0 | SIM01 |
| 0 | 0 | 0 | 0 | 0 | 1 | 0.635 | 0.158 | 0 | SIM37 |
| 1 | 0 | 0 | 0 | 1 | 1 | 0.629 | 0.111 | 0 | SIM36 |
| 1 | 0 | 0 | 0 | 0 | 3 | 0.591 | 0.121 | 0 | SIM35, SIM38, SIM39 |
| 0 | 1 | 0 | 0 | 1 | 11 | 0.553 | 0.154 | 0 | SIM22, SIM25, SIM26, SIM28, SIM29, SIM30, SIM31, SIM32, SIM33, SIM34, SIM48 |
| 0 | 0 | 0 | 0 | 1 | 8 | 0.327 | 0.035 | 0 | SIM19, SIM20, SIM21, SIM23, SIM24, SIM27, SIM40, SIM41 |

Rows with ≥ 1 case: 18 of 32.

**Intermediate solution for high readiness (DPR)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| DTC * ~DAC | DTC, ~DAC | 0.93 | 0.781 | 0.341 | SIM02, SIM03, SIM04, SIM05, SIM06, SIM09, SIM10, SIM11, SIM12, SIM13, SIM14, SIM17, SIM18, SIM43, SIM46, SIM47, SIM49, SIM50 | – |
| DTC * MDS * CERT | CERT | 0.999 | 0.424 | 0.023 | SIM02, SIM05, SIM11, SIM12, SIM13, SIM15, SIM16, SIM17, SIM43 | – |
| VI * CERT | CERT | 0.97 | 0.387 | 0.038 | SIM02, SIM03, SIM05, SIM42, SIM43, SIM44, SIM45 | – |

Solution consistency = 0.922; solution coverage = 0.845.
Complex solution: DTC * MDS * ~DAC + ~VI * DTC * ~DAC + ~VI * DTC * MDS * CERT + VI * ~DTC * CERT * DAC + VI * DTC * ~MDS * CERT
Parsimonious solution: CERT + DTC * ~DAC

## 4. Sufficiency: low DPP readiness (~DPR)

| VI | DTC | MDS | CERT | DAC | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 0 | 0 | 0 | 0 | 1 | 8 | 0.976 | 0.965 | 1 | SIM19, SIM20, SIM21, SIM23, SIM24, SIM27, SIM40, SIM41 |
| 1 | 0 | 0 | 0 | 1 | 1 | 0.954 | 0.889 | 1 | SIM36 |
| 1 | 0 | 0 | 0 | 0 | 3 | 0.944 | 0.879 | 1 | SIM35, SIM38, SIM39 |
| 0 | 0 | 0 | 0 | 0 | 1 | 0.932 | 0.842 | 1 | SIM37 |
| 1 | 1 | 0 | 0 | 1 | 1 | 0.872 | 0.486 | 0 | SIM01 |
| 0 | 1 | 0 | 0 | 1 | 11 | 0.870 | 0.755 | 1 | SIM22, SIM25, SIM26, SIM28, SIM29, SIM30, SIM31, SIM32, SIM33, SIM34, SIM48 |
| 1 | 0 | 1 | 1 | 1 | 1 | 0.826 | 0.052 | 0 | SIM42 |
| 1 | 1 | 0 | 1 | 1 | 1 | 0.825 | 0.042 | 0 | SIM45 |
| 1 | 0 | 0 | 1 | 1 | 1 | 0.801 | 0.069 | 0 | SIM44 |
| 1 | 1 | 1 | 0 | 1 | 2 | 0.791 | 0.340 | 0 | SIM07, SIM08 |
| 0 | 1 | 0 | 0 | 0 | 3 | 0.767 | 0.243 | 0 | SIM47, SIM49, SIM50 |
| 0 | 1 | 0 | 1 | 0 | 1 | 0.731 | 0.015 | 0 | SIM46 |
| 0 | 1 | 1 | 1 | 1 | 2 | 0.709 | 0.009 | 0 | SIM15, SIM16 |
| 1 | 1 | 0 | 1 | 0 | 1 | 0.676 | 0.000 | 0 | SIM03 |
| 0 | 1 | 1 | 0 | 0 | 3 | 0.643 | 0.135 | 0 | SIM10, SIM14, SIM18 |
| 1 | 1 | 1 | 1 | 0 | 3 | 0.544 | 0.000 | 0 | SIM02, SIM05, SIM43 |
| 1 | 1 | 1 | 0 | 0 | 3 | 0.543 | 0.070 | 0 | SIM04, SIM06, SIM09 |
| 0 | 1 | 1 | 1 | 0 | 4 | 0.486 | 0.000 | 0 | SIM11, SIM12, SIM13, SIM17 |

**Intermediate solution for low readiness (~DPR)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| ~DTC * ~MDS * ~CERT | ~DTC, ~CERT | 0.97 | 0.558 | 0.146 | SIM19, SIM20, SIM21, SIM23, SIM24, SIM27, SIM35, SIM36, SIM37, SIM38, SIM39, SIM40, SIM41 | – |
| ~VI * ~MDS * ~CERT * DAC | ~VI, ~CERT, DAC | 0.912 | 0.691 | 0.279 | SIM19, SIM20, SIM21, SIM22, SIM23, SIM24, SIM25, SIM26, SIM27, SIM28, SIM29, SIM31, SIM32, SIM33, SIM34, SIM40, SIM41 | SIM30, SIM48 |

Solution consistency = 0.919; solution coverage = 0.837.
Complex solution: ~DTC * ~MDS * ~CERT + ~VI * ~MDS * ~CERT * DAC
Parsimonious solution: ~DTC * ~CERT + ~VI * ~CERT * DAC

## 5. Readiness and circularity (P4)

Sufficiency of DPR alone for CSC: consistency = 0.752, coverage = 0.751 (P4 expects consistency < 0.80).

| Condition | Consistency | Coverage | RoN |
|---|---|---|---|
| DPR | 0.751 | 0.752 | 0.791 |
| VI | 0.715 | 0.908 | 0.941 |
| MDS | 0.615 | 0.813 | 0.893 |
| CERT | 0.509 | 0.865 | 0.944 |
| SIZE | 0.897 | 0.646 | 0.526 |
| ~DPR | 0.454 | 0.485 | 0.675 |
| ~VI | 0.582 | 0.507 | 0.582 |
| ~MDS | 0.629 | 0.534 | 0.58 |
| ~CERT | 0.657 | 0.488 | 0.461 |
| ~SIZE | 0.322 | 0.59 | 0.861 |

| DPR | VI | MDS | CERT | SIZE | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 0 | 1 | 1 | 3 | 0.996 | 0.991 | 1 | SIM03, SIM44, SIM45 |
| 1 | 0 | 0 | 1 | 1 | 1 | 0.983 | 0.955 | 1 | SIM46 |
| 1 | 1 | 1 | 0 | 1 | 5 | 0.973 | 0.942 | 1 | SIM04, SIM06, SIM07, SIM08, SIM09 |
| 1 | 1 | 1 | 1 | 1 | 4 | 0.965 | 0.934 | 1 | SIM02, SIM05, SIM42, SIM43 |
| 1 | 1 | 0 | 0 | 1 | 1 | 0.943 | 0.813 | 1 | SIM01 |
| 0 | 1 | 0 | 0 | 1 | 4 | 0.896 | 0.649 | 0 | SIM35, SIM36, SIM38, SIM39 |
| 1 | 0 | 1 | 0 | 1 | 1 | 0.872 | 0.555 | 0 | SIM14 |
| 1 | 0 | 1 | 1 | 1 | 5 | 0.856 | 0.729 | 1 | SIM11, SIM12, SIM13, SIM15, SIM16 |
| 1 | 0 | 1 | 1 | 0 | 1 | 0.845 | 0.611 | 0 | SIM17 |
| 1 | 0 | 0 | 0 | 1 | 5 | 0.790 | 0.460 | 0 | SIM30, SIM47, SIM48, SIM49, SIM50 |
| 1 | 0 | 1 | 0 | 0 | 2 | 0.786 | 0.332 | 0 | SIM10, SIM18 |
| 0 | 0 | 0 | 0 | 1 | 13 | 0.592 | 0.193 | 0 | SIM20, SIM22, SIM24, SIM25, SIM26, SIM27, SIM29, SIM31, SIM32, SIM33, SIM34, SIM37, SIM41 |
| 0 | 0 | 0 | 0 | 0 | 5 | 0.585 | 0.123 | 0 | SIM19, SIM21, SIM23, SIM28, SIM40 |

**Intermediate solution for circular capability (CSC)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| DPR * CERT * SIZE | CERT, SIZE | 0.894 | 0.45 | 0.089 | SIM02, SIM03, SIM05, SIM11, SIM13, SIM16, SIM42, SIM43, SIM44, SIM45, SIM46 | SIM12, SIM15 |
| DPR * VI * SIZE | DPR, VI | 0.941 | 0.572 | 0.212 | SIM01, SIM02, SIM03, SIM04, SIM05, SIM06, SIM07, SIM08, SIM09, SIM42, SIM43, SIM44, SIM45 | – |

Solution consistency = 0.894; solution coverage = 0.662.
Complex solution: DPR * CERT * SIZE + DPR * VI * SIZE
Parsimonious solution: CERT * SIZE + DPR * VI

### 5b. Formal circularity (CSC_2, CSC_3, CSC_5 only)

Sufficiency of DPR alone for formal circularity: consistency = 0.496, coverage = 0.856.

| DPR | VI | MDS | CERT | SIZE | n | consistency | PRI | OUT | cases |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | 0 | 1 | 1 | 3 | 0.872 | 0.724 | 1 | SIM03, SIM44, SIM45 |
| 1 | 0 | 0 | 1 | 1 | 1 | 0.870 | 0.648 | 0 | SIM46 |
| 1 | 1 | 1 | 1 | 1 | 4 | 0.837 | 0.675 | 0 | SIM02, SIM05, SIM42, SIM43 |
| 1 | 1 | 1 | 0 | 1 | 5 | 0.750 | 0.482 | 0 | SIM04, SIM06, SIM07, SIM08, SIM09 |
| 1 | 1 | 0 | 0 | 1 | 1 | 0.744 | 0.337 | 0 | SIM01 |
| 1 | 0 | 1 | 0 | 1 | 1 | 0.720 | 0.233 | 0 | SIM14 |
| 1 | 0 | 1 | 1 | 0 | 1 | 0.693 | 0.329 | 0 | SIM17 |
| 1 | 0 | 1 | 1 | 1 | 5 | 0.644 | 0.306 | 0 | SIM11, SIM12, SIM13, SIM15, SIM16 |
| 1 | 0 | 1 | 0 | 0 | 2 | 0.554 | 0.050 | 0 | SIM10, SIM18 |
| 1 | 0 | 0 | 0 | 1 | 5 | 0.553 | 0.102 | 0 | SIM30, SIM47, SIM48, SIM49, SIM50 |
| 0 | 1 | 0 | 0 | 1 | 4 | 0.544 | 0.070 | 0 | SIM35, SIM36, SIM38, SIM39 |
| 0 | 0 | 0 | 0 | 1 | 13 | 0.372 | 0.035 | 0 | SIM20, SIM22, SIM24, SIM25, SIM26, SIM27, SIM29, SIM31, SIM32, SIM33, SIM34, SIM37, SIM41 |
| 0 | 0 | 0 | 0 | 0 | 5 | 0.314 | 0.016 | 0 | SIM19, SIM21, SIM23, SIM28, SIM40 |

**Intermediate solution for formal circularity (CSCF)**

| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |
|---|---|---|---|---|---|---|
| DPR * VI * ~MDS * CERT * SIZE | VI, ~MDS, CERT | 0.872 | 0.445 | 0.445 | SIM44, SIM45 | SIM03 |

Solution consistency = 0.872; solution coverage = 0.445.
Complex solution: DPR * VI * ~MDS * CERT * SIZE
Parsimonious solution: VI * ~MDS * CERT

## 6. Robustness (high readiness)

| Variant | Intermediate solution | Consistency | Coverage |
|---|---|---|---|
| Base | DTC * ~DAC + DTC * MDS * CERT + VI * CERT | 0.922 | 0.845 |
| R1 anchors +10% | DTC * CERT * ~DAC + DTC * MDS * ~DAC + VI * CERT + VI * DTC * ~DAC | 0.947 | 0.77 |
| R1 anchors −10% | DTC * MDS * ~DAC + DTC * CERT * ~DAC + DTC * MDS * CERT + VI * CERT + VI * DTC * MDS | 0.947 | 0.773 |
| R2 consistency 0.75 | DTC * ~DAC + DTC * MDS * CERT + VI * CERT | 0.922 | 0.845 |
| R2 consistency 0.85 | DTC * ~DAC + DTC * MDS * CERT + VI * CERT | 0.922 | 0.845 |
| R3 frequency 2 | DTC * ~DAC + DTC * MDS * CERT | 0.931 | 0.806 |
| R4 BTP replaces CERT | DTC * BTP * ~DAC + VI * DTC * MDS * BTP | 0.94 | 0.779 |
| R5 tiers 1–2 only | DTC * CERT * ~DAC + DTC * MDS * ~DAC + DTC * MDS * CERT + VI * CERT | 0.953 | 0.748 |
| R6 DPR without DPR_7 | DTC * ~DAC + DTC * MDS * CERT + VI * CERT + VI * DTC | 0.96 | 0.81 |
| R7 BTP added (6 conditions) | DTC * ~DAC * BTP + DTC * MDS * CERT * BTP + VI * CERT * BTP | 0.943 | 0.82 |
