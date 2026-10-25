# Model parameters and their provenance

Source codes refer to `docs/literature.md`. **ASSUMPTION** = no empirical source found in
this session; every such parameter is varied in E6 (one-at-a-time sensitivity) or is
itself an experimental factor.

| Parameter | Baseline | Range explored | Source / justification |
|---|---|---|---|
| Share mono-cotton items | 0.42 | — | L5 (cotton 42% of analysed low-value post-consumer textiles) |
| Share polycotton blends | 0.12 | — | L5 (polycotton 12%) |
| Share other blends | 0.20 | — | L5 (blends 32% in total → 32 − 12) |
| Share mono-PES | 0.16 | 0.10–0.22 (E6) | ASSUMPTION: split of remaining 26% between PES and other mono |
| Share other mono (wool, viscose, PA, acrylic) | 0.10 | via normalisation | ASSUMPTION |
| Polycotton cotton fractions / probs | {0.9,0.8,0.6,0.5,0.35} / {.15,.15,.25,.20,.25} | — | ASSUMPTION (covers 90/10 "near-pure" blends that M3 reports are misclassified as pure) |
| Elastane prevalence in mono items | 0.20 | 0.10–0.40 (E6) | R3 (~1 in 5 non-rewearable single-material items) |
| Elastane content | cotton U(1,5)%, PES U(1,10)% | — | R3 (1–5% in cotton; up to 20% in PES/PA, capped at 10%) |
| Garment mass | lognormal, mean 0.35 kg, CV 0.6 | mean 0.25–0.50 (E6) | ASSUMPTION |
| Line throughput | 1000 kg/h | — | M8 (Fibersort ~900–1200 kg/h) |
| Horizon per replication | 40 h (1 week, 1 shift) | — | ASSUMPTION; trailing partial bale (~1% of mass) excluded consistently |
| Sensor RMSEP σ (cotton, PES fraction) | 0.035 | 0.01–0.066 (E1) | Q1: <1% benchtop lab; 1.2–1.6% handheld/imaging (Applied Sciences 2026); 3.1% handheld and 6.6% lab-scale sorter on post-consumer textiles (Stipanovic et al. 2025, Montanuniversität Leoben) |
| Dark / carbon-black items | 5%, routed to residual | 0–15% (E6) | Q2: vendor report that carbon-black-dyed textiles (e.g. black denim) give unusable NIR spectra; share is ASSUMPTION |
| Confidence draws | 64 | — | Numerical; confidence resolution 1/64 |
| Mechanical discharge error | 2% | 0–5% (E6) | ASSUMPTION; error pathway documented for sensor-based sorting (S7) |
| DPP data error | 2% | 2–10% (E4) | ASSUMPTION |
| Manual station servers / buffer | 2 / 20 | 0–8 servers (E2) | ASSUMPTION |
| Manual service time | 20 s, CV 0.5 (lognormal) | 10–40 s (E6) | ASSUMPTION |
| Manual sort accuracy (class level) | 0.85 | 0.75–0.95 (E2, E6) | ASSUMPTION, bracketed by label inaccuracy rates of 37% (L7) and 41% (L6), which include percentage deviations that do not always change the sort class |
| Bale mass | 200 kg | 50–1000 kg (E3) | ASSUMPTION (export bales ~45–55 kg; wiper bales 100–200 kg; balers up to 600 kg; no recycler-feedstock standard found) |
| Cotton-line spec (cotton mass fraction) | 0.95 and 0.98 | 0.95–0.99 (E3) | R1 (Circulose input bar ~98% cotton, trade press); 0.95 = ASSUMPTION for a more tolerant process |
| PES-line spec | 0.90 | — | R2 (80–90% PET/PA for chemical recycling economics) |
| Polycotton-line spec (cotton+PES) | 0.95 | — | ASSUMPTION (blend-separation processes) |
| QC measurement SD | 0.005 | 0–0.01 (E6) | ASSUMPTION |

## Drift experiment (E5) — illustrative assumptions

| Parameter | Value | Note |
|---|---|---|
| Novel-item share | rises linearly 0 → 40% over 52 weeks | ASSUMPTION (new blends, finishes, recycled-content fibres) |
| Initial noise multiplier on novel items κ₀ | 2.0 | ASSUMPTION |
| Learning curve | κ(N) = 1 + (κ₀ − 1)(1 + 0.85N/500)^−0.5 | Power-law learning curve form (sample-size/accuracy relations); constants ASSUMPTION |
| Label source | novel items served by the manual station (and audit sample) | Mechanism under study |
