# Literature log and verification status

**How sources were checked (read this first).** The session's network policy blocked
direct access to publisher sites, arXiv, PMC and the NIST data repository, and the
Consensus and scite tools were unavailable (quota / subscription). Every entry below was
located through web search and cross-checked against at least one bibliographic record
(publisher page, institutional repository, or indexing record) **as returned by the
search engine**. Full texts were *not* read unless stated. Status codes:

- **V-meta** — title, authors, venue, year (and DOI where given) confirmed from a
  bibliographic record; claims taken from abstract/summary only.
- **V-press** — figure confirmed only through secondary/trade-press coverage; must be
  checked against the primary document before submission.
- **TO-READ** — must be read in full by the supervisor/author before the claim is kept.

Nothing here should go into the final submission without the supervisor opening the
primary source. Claims we rely on numerically are flagged in `paper/manuscript.md`.

---

## 1. Problem framing: textile circularity

| # | Source | Status | What we use it for |
|---|---|---|---|
| L1 | Ellen MacArthur Foundation (2017). *A New Textiles Economy: Redesigning Fashion's Future.* | V-press | "<1% of material used for clothing is recycled into new clothing"; one garbage truck of textiles landfilled or burned per second. (The commonly quoted 73% landfill/incineration figure was **not** confirmed — do not use.) |
| L2 | Directive (EU) 2025/1892 of 10 Sept 2025 amending Directive 2008/98/EC (OJ 26 Sept 2025). | V-meta (law-firm and EP summaries) | Mandatory textile EPR; schemes by 17 Apr 2028; separate textile collection obligation since 1 Jan 2025. |
| L3 | European Commission (2023) proposal for targeted WFD revision (IP/23/3635). | V-press | EU generates 12.6 Mt textile waste/yr; ~22% of post-consumer textiles collected separately. |
| L4 | Regulation (EU) 2024/1781 (ESPR) and ESPR Working Plan 2025–2030. | V-press | Textiles/apparel a priority group; Digital Product Passport (DPP); textile delegated act expected ~2027. Date to be re-checked. |
| L5 | Fashion for Good & Circle Economy (2022). *Sorting for Circularity Europe.* | V-press | Six countries; 74% (494 kt/yr) of low-value post-consumer textiles suitable for closing the loop; cotton 42%, blends 32% (polycotton 12%); 21% suitable for mechanical, 53% for chemical recycling; ~2% currently goes to fibre-to-fibre. |
| L6 | Circle Economy / Fibersort study (#whatsinmyclothes, 2020). | V-press | Labels misleading in 41% of cases; largest deviations in cotton–polyester blends. |
| L7 | EU JACOP 2025 coordinated market-surveillance action on fibre labels (summarised by EU Transition Pathways / Intertek, 2026). | V-press | 37% of 132 garments mislabelled; natural+artificial blends 64% failure. |
| L8 | Sandin, G. & Peters, G.M. (2018). Environmental impact of textile reuse and recycling – A review. *J. Cleaner Production* 184, 353–365. doi:10.1016/j.jclepro.2018.02.266 | V-meta | Reuse/recycling generally beat incineration/landfill; benefits hinge on displacement of virgin production; call for research on cascade systems. |

## 2. Fibre identification and ML sorting

| # | Source | Status | Use |
|---|---|---|---|
| M1 | Riba, J.-R., Cantero, R., Canals, T., Puig, R. (2020). Circular economy of post-consumer textile waste: Classification through infrared spectroscopy. *J. Cleaner Production* 272, 123011. doi:10.1016/j.jclepro.2020.123011 | V-meta | Lab-scale IR + multivariate classifiers; reports 100% accuracy on unknown samples (small, controlled set). Shows the "lab accuracy" end of the spectrum. |
| M2 | Faghih, S., Saki, Z., Moore, M. (2025). AI-Enabled Textile Waste Sorting (systematic review). *Sustainability* 17(10), 4264. | V-meta (RePEc record) | Field state: ML/DL/CV integrated with NIR; gaps = blend detection, data availability. |
| M3 | Mini-review: Recent development on sorting of textile waste by fibre type for recycling. *Textiles* (MDPI) 6(1):28 (2026). | V-meta | Blends with minor fibre ≤10% misclassified as pure. Motivates our composition-distance sensor model. |
| M4 | Li et al. (2023), 1D-CNN on NIR for waste textile sorting, *Spectroscopy and Spectral Analysis* 43(7), 2139. | V-meta | 12 classes incl. blends; 96.6% test accuracy on 232 samples. |
| M5 | RSC *Analyst* (2021) NIR imaging of cellulose fibres (doi:10.1039/d1an01794b). | V-meta | 100% TPR in CV but ~80–90% pixel accuracy on independent test set — offline vs. independent gap. |
| M6 | Rijken (2026), MSc thesis, Univ. Groningen. | V-meta (thesis) | Weighted-KNN composition accuracy 91.7%; elastane detector 92.6% overall but 83.3% on elastane-present samples. Basis for elastane-detector sensitivity. Thesis-grade source: flag in paper. |
| M7 | AUTSORT project (EU) | V-press | NIR recognition 90–95% depending on textile type, 13 fractions. |
| M8 | Tomra AUTOSORT for textiles; Valvan Fibersort | V-press (vendor) | Throughput ~0.9–1.2 t/h (Fibersort), up to 4.5 t/h (Tomra claim). Used only for order-of-magnitude line throughput. |
| M9 | NIST NIR-SORT dataset (v1.0 2024, v2.0 2026), doi:10.18434/mds2-3325; Scientific Data descriptor (2026), "A spectroscopic dataset for known provenance and post-consumer textiles". | V-meta | 113 fabrics; benchtop 833–2500 nm + two handheld instruments 1550–1950 nm; ~7 replicates. **Planned empirical classifier source — download blocked in this session.** |
| M10 | OpenTextile-NIR (VTT, 2026; PMC12925456). | V-meta | 71 post-industrial samples, SWIR hyperspectral; alternative empirical source. |
| M11 | Liège MSc thesis (matheo.uliege.be 2268.2/20911). | V-meta (thesis) | CNN ~95% offline accuracy vs 43.5% live sorting accuracy — offline/online gap (plastics). |

## 3. Selective classification / reject option (ML theory we use)

| # | Source | Status | Use |
|---|---|---|---|
| T1 | Chow, C.K. (1970). On optimum recognition error and reject tradeoff. *IEEE Trans. Information Theory* 16(1), 41–46. | V-meta (DOI not yet confirmed; believed 10.1109/TIT.1970.1054406 — CHECK) | Reject rule: reject when max posterior < threshold. Our τ policy. |
| T2 | Geifman, Y. & El-Yaniv, R. (2017). Selective classification for deep neural networks. *NeurIPS 30*, 4878–4887. | V-meta | Risk–coverage trade-off; we evaluate risk–coverage at system level instead of item level. |

## 4. Simulation of textile CE and of sensor-based sorting

| # | Source | Status | Use |
|---|---|---|---|
| S1 | Zammori, F., Romagnoli, G., Mercogliano, M. — agent-based models of household textile collection (Univ. Parma, several papers 2024–2026). | V-meta | Simulation in textile CE concentrates on **collection**, not sorting quality. |
| S2 | Valtere et al. (2025), Riga Technical Univ. — system dynamics: "What drives the circular economy? Textile sorting or consumption reduction". | V-meta | SD models treat sorting as aggregate capacity, no classifier error. |
| S3 | Univ. Windsor major paper (2019) — DES of a textile recycling system. | V-meta | Prior DES, no ML. |
| S4 | DITF CircSim; SOLSTICE Valoramix. | V-press | Tools model R-strategies/material flows, not classifier error. |
| S5 | TU Delft repository study — Monte Carlo of NIR false calls, 13 plastic classes. | V-meta (author list TO-READ) | Closest analogue (plastics): error types → Monte Carlo product grade ~85%. |
| S6 | VTT/Leoben (2026). Simulating process parameters of sensor-based sorting for plastic packaging waste: methodology and validation. *Waste Management Bulletin* 4(2), 100299. | V-meta (authors TO-READ) | DES of SBS unit fitted to experiments (plastics). |
| S7 | DETRITUS (2020) vol. 9, 59–67, sensor-based sorting error pathways and purity/recovery definitions. | V-meta | Purity, recovery, yield definitions; detection vs. discharge errors. |
| S8 | *Polymers* (Oct 2023) regression models for SBS operating point. | V-meta | Operating-point optimisation for purity/recovery (plastics). |
| S9 | Law, A.M. (2015). *Simulation Modeling and Analysis*, 5th ed., McGraw-Hill. | Standard text | Replication/CI methodology, CRN. |

## 5. Recycler feedstock specifications (parameter sources — weakest evidence)

| # | Source | Status | Use |
|---|---|---|---|
| R1 | Inside Denim feature on Circulose / Infinited Fiber. | V-press | Circulose input bar ~98% cotton; Infinited Fiber claims tolerance to ~80% cellulose; "pure cotton" bales found with 20–40% polyester. |
| R2 | Ecologic Institute (2022) study on textile recycling (for EU). | V-press | Chemical recycling of synthetics needs ~80–90% PET/PA input purity for economics. |
| R3 | Fibersort elastane finding (Fashion for Good elastane project coverage). | V-press | Elastane in ~1 in 5 non-rewearable single-material items; 1–5% w/w in cotton, up to 20% in PES/PA. |


## 6. Sensor quantification error (parameter sources for σ)

| # | Source | Status | Use |
|---|---|---|---|
| Q1 | Stipanovic, H. et al. (2025). Quantifying cotton content in post-consumer polyester/cotton blend textiles [NIR sorter and handheld spectrometer]. *Recycling* 10(4), 152. doi:10.3390/recycling10040152 | V-meta (abstract) | RMSEP 6.6% (bias −0.9%) lab-scale NIR sorter; 3.1% (bias −0.6%) handheld; industrial "10% accuracy" convention; worse below 35% cotton. Upper end of σ range. |
| Q1b | Lilek et al. (2026). [Instrument classes and analysis strategies for NIR cotton quantification in textile sorting]. *Applied Sciences* 16(7), 3242. doi:10.3390/app16073242 | V-meta (title TO-CONFIRM) | Benchtop RMSEP <1.0%; handheld/imaging ~1.2–1.6% on blends from one starting material. Lower end of σ range. |
| Q2 | Sagitto (NIR spectrometer vendor) blog post on carbon black. | V-press (vendor) | Carbon-black-dyed textiles (e.g. black denim) yield spectra too uninformative for ML models; usually assigned to a separate category. Basis for dark-item routing; prevalence is an assumption. |

---

## Gap statement (derived from the above)

1. ML-for-textile-sorting papers (M1–M7) report **item-level accuracy** on small lab sets.
2. Textile CE simulation papers (S1–S4) model **collection logistics or aggregate flows**,
   with sorting treated as a capacity or a fixed split ratio.
3. The only work that propagates classifier errors to product grade (S5–S8) is for
   **plastic packaging**, does not represent textile-specific issues (blends near class
   boundaries, elastane, unreliable labels), and does not model ML decision policy
   (confidence thresholds/reject option), recycler bale acceptance, or DPP data.

**No study found** links ML sorting decisions (operating point, reject option, label/DPP
fusion, drift and retraining) to **bale-level acceptance and closed-loop yield** in
fibre-to-fibre textile recycling. This is the gap the paper addresses.
