# 3. Data

All inputs are in three CSV files:

| File | Contents |
|---|---|
| `data/sources.csv` | Every source, with ID, citation, URL and what was taken from it |
| `data/france_tlc_observed.csv` | Observed French series. `use` = `forcing` (historical driver), `target` (calibration), `validation` (held out: 2025), `alternative` (conflicting value kept for transparency) |
| `data/parameters.csv` | Every model parameter, with value, range, unit, source and role: `fixed`, `calibrate`, `gsa` (global sensitivity analysis) or `structural` |

## How the data were collected, and the caveat

From this environment, the primary hosts (ademe.fr, refashion.fr, oecd.org) could not be
downloaded directly. Figures were therefore collected through web search of:
- Refashion and Eco TLC annual reports;
- ADEME publications;
- government audits (IGEDD, CGE);
- trade press quoting those reports.

Each value carries a confidence flag. **Before submission, every `high`/`medium` value must be
checked against the original PDF.** Do this first for Refashion's annual reports 2010–2024, which
should also fill the gaps in the series.

## Known data issues

1. **Structural break in placed-on-market tonnage.** It rose from 648 kt (2019) to 886 kt (2024),
   +37 % in five years. Unit counts rose from 2.6 to 3.48 bn. Part of this is likely better
   declaration coverage (marketplaces) rather than consumption growth. The model takes POM as given
   in calibration. See Q1.
2. **Conflicting values:**
   - 2023 collected: 259.6 kt (ARec) vs 267.9 kt (ADEME). The mean is used.
   - 2024 sorted: 206.1 kt (Refashion) vs 197 kt (ADEME). Refashion is used.
   - 2019 sorted: 196.1 kt vs 187.2 kt.
3. **Gaps.**
   - No sourced values for collection points before 2014. A 2010 value of 25,000 is assumed.
   - Sorting support 2019–2022 is held at 82.5 €/t.
   - Net sorting cost 2015–2023 is interpolated.
4. **Mixed denominators.** Collection "rates" are reported against POM by Refashion (about 32 %)
   and against a different base by ADEME (36.5 %). The model reports collected/POM.

## Calibration targets (2009–2024)

| Variable | Years |
|---|---|
| Collected (kt) | 2009 (low weight), 2014, 2018, 2019, 2021, 2022, 2023, 2024 |
| Sorted (kt) | 2019, 2022, 2023, 2024 |
| Reuse share of sorted | 2019, 2024 |
| Domestic second-hand sales (kt) | 2024 |
| Share of consumers buying second-hand; items per buyer | 2023 |

## Hold-out validation (2025)

| Variable | Value |
|---|---|
| Placed on market | 917.6 kt |
| Collected | 272.5 kt |
| Sorted | 216.3 kt |
| Domestic second-hand sales | 65.4 kt |
| Used-textile exports | 172 kt |
