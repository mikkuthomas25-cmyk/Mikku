# India study (angle A): data map, draft v0

**Question.** EU rules (textile EPR, ESPR ecodesign, Digital Product Passport, and brand
recycled-content demand) will raise demand for recycled fibre in garments that India exports to
Europe. Can India's textile waste meet that demand, and where does supply break?
The waste comes from three sources: pre-consumer (factory), post-consumer (households) and
imported.

Status keys:
- ✅ found (secondary source, to verify against the original)
- 🟡 partial or conflicting
- ❌ not yet found

Source IDs refer to `data/sources.csv`.

## A. Demand side: what the EU rules require of Indian exporters

| Model input | Status | What we have | Next step |
|---|---|---|---|
| Value of textile and apparel exports | ✅ | US$37.8 bn (FY25), up from 35.9 bn (FY24) (TEXMIN26) | Extract the time series 2015–2025 |
| EU share of exports | ✅ | 19 % (FY24, TEXMIN25); 19.6 % (calendar 2024, CITI) | Split by product |
| Apparel exports to EU | ✅ | US$4.55 bn (FY25), 4.66 bn (FY26) (AR-EU) | Convert to tonnes using trade quantities |
| **Export volumes in tonnes, by product** | ❌ | — | UN Comtrade / Eurostat Comext, HS chapters 50–63. This is the key quantity |
| India–EU FTA | 🟡 | Ministry statement, Jan 2026, calls it "transformational" (AIR-FTA) | Verify tariff terms and entry-into-force date; affects export growth |
| ESPR textile delegated act | ✅ | Planned Q4 2027; requirements apply ≥18 months later (~2029) (EC-DPP) | Watch for recycled-content criteria |
| Ban on destroying unsold apparel | ✅ | Large firms from 19 Jul 2026 (ESPR-DESTR) | Minor channel |
| EU textile EPR (Waste Framework Directive revision) | 🟡 | Known to be adopted in 2025 with national EPR schemes required later | Search limit hit; verify the date |
| **Recycled-content demand** (legal mandate or brand targets) | ❌ | No legal mandate found so far | Brand commitments (Textile Exchange); treat as scenario ranges |

## B. Supply side: India's waste and recycling system

| Model input | Status | What we have | Next step |
|---|---|---|---|
| Total textile waste | 🟡 | 7.8 Mt/yr (FFG22) vs 7.07 Mt/yr (government mapping study, JS-MAP) | Get both primary reports and compare methods |
| Split by source | 🟡 | FFG22: 51 % post-consumer, 42 % pre-consumer, 7 % imported. Government study: 58 % post, 42 % pre | Use as a calibration pattern with a range |
| Share reused or recycled | ✅ | ~59 % locally (FFG22) | Calibration pattern |
| Panipat throughput | 🟡 | ~300 t/day in ~150 mills (TRIB) up to >500 kt/yr (ATMOS); ~60 % of national recycling (SWITCH) | Use a range; look for cluster studies |
| Import share and rules | ✅ | ~7 % imported; regulated under the Hazardous and Other Wastes Rules 2016 (MOT-CNN) | UN Comtrade HS 6309/6310 for tonnes |
| Fibre mix of waste (cotton / polyester / blends) | ❌ | — | Needed for fibre-to-fibre suitability |
| Factory cutting-waste rate | ❌ | — | Literature on garment cutting waste |
| Recycled-fibre capacity (mechanical cotton; recycled polyester) | ❌ | — | Industry reports; India is a large bottle-to-fibre recycled-polyester producer |
| Informal collection | ❌ | — | Waste-picker and kabadiwala studies |

## C. Reference case

| Item | Status |
|---|---|
| France: how an EU EPR scheme actually behaves (collection, sorting, export and reuse shares, sorter economics) | ✅ calibrated model in `../` |

## How the gaps will be handled

The methods are cross-checking, pattern matching, ranges with Sobol sensitivity analysis, and
scenarios. See the explanation given to the author. The ❌ rows in A and B are the next collection
round. Export tonnes and fibre mix matter most: without them, demand and supply cannot be put in
the same units.
