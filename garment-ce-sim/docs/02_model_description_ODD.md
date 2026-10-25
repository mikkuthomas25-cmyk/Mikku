# 2. Model description (ODD protocol, Grimm et al. 2020 structure)

## 2.1 Purpose and patterns

The model has two purposes:
1. Evaluate how French TLC EPR levers affect new-item tonnage, virgin fibre demand, end-of-life
   routing and GHG over 2025–2040.
2. Estimate the system-level displacement rate of second-hand consumption.

Patterns used for calibration and validation:
- Rise of collected tonnage (about 129 kt in 2009 to 289 kt in 2024).
- Sorted tonnage plateauing at about 190 kt (2019–2023) and then rising.
- Reuse share of sorted output of about 57 %.
- Domestic second-hand sales of about 62 kt (2024), and 38 % of consumers buying second-hand (2023).

## 2.2 Entities, state variables and scales

| Entity | Number | State variables |
|---|---|---|
| Consumer agent | `n_agents` (3,000); each represents population/n people | segment (trend / mainstream / conscious); purchase intensity; resale-platform `adopter` flag and `potential`; collection habit *c* ∈ [0,1]; wardrobe of new items `w_new` and second-hand items `w_sh` (items per person); 8 network neighbours |
| Brand agent | 4 segments × 10 | segment (ultra-fast UF, fast, mid, premium); base price; durability coefficient (0–1, legal threshold 0.8); recyclability (0–1); cumulative unit-cost increase; fee per item |
| Sorting operator | 67 (REF23KPI) | capacity (kt/yr); cost efficiency multiplier; active flag; consecutive loss years. Operator 0 is the dominant network with 60 % of capacity (TI25) |
| SD stocks | — | sorting backlog (kt); fibre-to-fibre (F2F) capacity and construction pipeline (kt/yr); cumulative F2F output (learning); in-use durability, recyclability and UF-share indices |

The time step is one year. The model runs 2010–2040, with 5 spin-up years at 2010 conditions.
The spatial scale is France (no explicit space).

## 2.3 Process overview and scheduling (each year)

1. **Exogenous drivers.** Population; collection points (observed through 2025, then the
   scenario); collection attractor `c* = c_max (1 − exp(−κ·density)) × awareness`.
2. **Discard.** Each consumer discards `w_new (1 − e^{−1/L})` and `w_sh (1 − e^{−1/(φL)})`, with
   `L = lifetime_new (dur_stock / dur_ref)^elasticity`.
3. **Adoption.** Bass diffusion on the network:
   `P(adopt) = 1 − exp(−(p + q · adopters_nbrs/potential))` for agents with `potential`, from 2012.
4. **Need.** `need_i = max(0, discards_i + (D_i − W_i)/τ)`, with desired wardrobe
   `D_i = intensity_i × D̄(t)`.
5. **Acquisition.**
   - Second-hand demand is `need_i × s_i`. `s_i` depends on adoption and segment, and is scaled
     down if domestic second-hand supply (last year's peer-to-peer and domestic-reuse output) is short.
   - Rebound: a share `rebound` of the money saved, `(p_new − p_sh)` per item, buys extra new items.
   - So `new_i = need_i − sh_i + rebound × sh_i (p_new − p_sh)/p_new`.
   - **Historical mode:** `D̄(t)` is solved by bisection so that Σ new items = observed
     placed-on-market (POM) tonnage.
   - **Projection mode:** `D̄(t) = D̄_2024 (1+g)^{t−2024} (P_t/P_2024)^{−ε}`.
6. **Brand market.**
   - Consumer segments choose brands by multinomial logit:
     `U_b = ASC_b − λ ln(price_b) + δ_c · dur_b`, where only conscious consumers weight durability.
   - Price is `price0 + cost_up + pass_through × fee`.
   - Historically, the UF constant is shifted so that the UF share follows its 2010→2024 path.
   - From 2025, each brand may upgrade durability (+0.1) or recyclability (+0.2). The probability
     is a logit in (fee saving − unit cost)/unit cost.
7. **Fees.**
   - Base fee, minus a durability bonus if dur ≥ 0.8 (from 2023).
   - Plus a recyclability malus if rec < 0.5 (from 2025).
   - Plus, if the law scenario is on, a loi 2026-602 penalty for dur < 0.8: band [0.5, 12] €
     (2027–29) and [2, 19.5] € (2030+), scaled linearly with (0.8 − dur)/0.8 and capped at 50 % of
     the pre-tax price. The linear mapping is an assumption; see Q4.
8. **Disposal.** Discards go to peer-to-peer resale (share `u_i`), separate collection (`c_i` of
   the rest), or residual waste. Unsold second-hand supply is donated to collection.
9. **Habit update.** `c_i += α_access (c*·m_seg − c_i) + α_norm (mean_nbr c − c_i) + ε`.
10. **Sorting.**
    - A share `e` (20.9 %, OCC20) of collected tonnage is exported unsorted.
    - The rest joins the backlog. Operators sort up to capacity; backlog beyond 15 % of capacity
      overflows to unsorted export or incineration.
    - Sorted output: reuse share `θ = θ0 − slope × UF share of discards`. Reuse splits into domestic
      and export. The non-reuse part splits into recycling, wipes, energy recovery and residual
      (2024 proportions, REF24).
11. **Sorter economics.**
    - Margin per operator = support − net cost × efficiency.
    - Net cost: observed 2012–2024 (IGEDD, CGE25). From 2025 it is endogenous:
      `net_cost = 123 − (θ·P_export − θ_2024·513)`.
    - Desired capacity = smoothed inflow × clip(1 + sens × margin, 0.5, 1.2), reached with delay τ_K.
    - Operators with 3+ consecutive loss years exit with probability 0.3 per year.
12. **Fibre-to-fibre recycling (SD).**
    - Feedstock = open-loop recycling grade × recyclability of in-use stock × suitability.
    - Output = min(capacity, feedstock × yield).
    - Investment rate rises logistically with (incentive − cost premium). The premium falls with a
      learning curve.
    - Output replaces virgin fibre.
13. **GHG accounting** (ktCO2e):
    - production of new items, minus fibre-stage savings from F2F;
    - plus collection and sorting, export transport, and incineration;
    - minus the open-loop recycling credit.

## 2.4 Design concepts

- **Emergence.** The displacement rate, overflow tonnage, the market mix under fees, and collection
  rates all emerge.
- **Adaptation.** Brands upgrade design under fees. Sorters adjust capacity or exit under margins.
- **Sensing.** Consumers sense their neighbours' collection habits and platform adoption.
  Sorters sense their own margin.
- **Interaction.**
  - Consumers interact through the network (indirect) and the second-hand market (supply constraint).
  - Brands and consumers interact through prices.
  - Consumers interact with sorters through collected tonnage and quality (UF share lowers reuse share).
- **Stochasticity.** Agent attributes, network, adoption, habit noise, brand upgrades and sorter
  exits. Results are reported over many seeds.
- **Observation.** Annual flows (kt), shares, GHG, and paired counterfactual runs for displacement.

## 2.5 Initialisation

- Wardrobes start at the 2010 steady state (`D̄ = POM_2010 per capita × L`).
- Collection habits start at the 2010 attractor.
- Sorting capacity in 2010 is a calibrated parameter.
- UF share in 2010 is 2 % (assumption).
- Five spin-up years are run at 2010 conditions.

## 2.6 Input data

See `03_data.md`, `data/france_tlc_observed.csv` and `data/parameters.csv`.

## 2.7 Submodels: key equations

They are listed in 2.3 and implemented one-to-one in:
- `src/gcesim/consumers.py`
- `src/gcesim/brands.py`
- `src/gcesim/sorting.py`
- `src/gcesim/impacts.py`
- `src/gcesim/model.py`

## 2.8 Mass balance

Tested every year in `tests/test_model.py`:
- discards = peer-to-peer + collected + residual;
- collected = unsorted export + sorted + overflow + Δbacklog;
- sorted = Σ output fractions (including F2F output and losses).

## 2.9 Porting to AnyLogic (next phase)

| Python | AnyLogic |
|---|---|
| `Consumers` | Agent type `Consumer`; population with a small-world network |
| `Brands` | Agent type `Brand` |
| `Sorters` | Agent type `SortingOperator` |
| Backlog, F2F capacity, durability indices | SD stocks in Main |
| `scenarios.py` | Experiments: parameter variation, Monte Carlo, and calibration using the Python-fitted values as start |

The Python model stays the reference implementation. AnyLogic results should reproduce it within
Monte Carlo noise.
