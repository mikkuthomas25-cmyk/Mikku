---
title: "[ILLUSTRATIVE – SIMULATED DATA – NOT FOR SUBMISSION] Who Carries the Passport? Owned, Negotiated or Borrowed Routes to Digital Product Passport Readiness in Indian Apparel Exports"
---

> **ILLUSTRATIVE VERSION.** All results below come from *simulated* responses for 50 fictional firms (`data/SIMULATED_responses.csv`; assumptions in `simulation/SIMULATION_ASSUMPTIONS.md`). They show how the paper will read. They are **not findings**. Replace the data, rerun `analysis/fsqca.py`, and rewrite the Results and Discussion from the real output.

# Abstract

The EU's Digital Product Passport (DPP) for textiles will make product-level sustainability data a condition of market access. Most of those data originate with suppliers several tiers upstream in exporting countries. Drawing on organizational information processing theory, we examine how suppliers reach upstream data: by owning it, negotiating for it, or borrowing it from chain-of-custody certification. Using fuzzy-set qualitative comparative analysis (fsQCA) of 50 Indian textile and apparel exporters, we find two core mechanisms of passport readiness: digital capture by firms that do not fear data misuse, and certification coverage. Ownership and negotiation matter as ways of removing that fear. Buyer pressure was nearly universal and did not distinguish ready from unready firms. Readiness was not sufficient for formal circular practice. We develop a theory of trusted and borrowed information capacity, with implications for brands, suppliers and EU–India policy.

*Keywords:* digital product passport; circular economy; apparel supply chains; organizational information processing theory; fsQCA; India

# Introduction

Every garment sold in the European Union will soon carry a passport. Under the Ecodesign for Sustainable Products Regulation (ESPR), a Digital Product Passport (DPP), reached through a QR code or other data carrier, will record fibre composition, origin by processing stage, substances of concern, environmental footprint, durability, repairability and recyclability (European Parliament and Council, 2024). The textile delegated act is planned for 2027. Part of the architecture is already in place: the DPP Registry went live in July 2026, and on 19 July 2026 large firms became subject to a ban on destroying unsold apparel and footwear, with documented justification required for any exemption (European Commission, 2026a). EU waste law now also mandates extended producer responsibility for clothing (European Parliament and Council, 2025).

The passport is usually discussed as the brand's problem. In practice it is the supplier's. A brand cannot report where its cotton was ginned, which dyehouse used which chemicals, or how much water a fabric consumed unless every upstream tier can produce, standardise and share those data (Jensen et al., 2023; King et al., 2023). Multi-tier visibility is notoriously weak in apparel (Villena & Gioia, 2018; Wilhelm et al., 2016), and the information burden falls most heavily on suppliers in the Global South.

India makes this tension concrete and current. After U.S. tariffs of up to 50% from August 2025, Indian exporters began moving shipments toward the EU. The EU–India free trade agreement concluded in January 2026 promises to remove the roughly 12% EU duty on Indian apparel once ratified (European Commission, 2026b; Reuters, 2026). Apparel exports to the EU reached a record of about US$4.7 billion in 2025–26, still only around 3% of the EU apparel import market. Indian suppliers are therefore turning toward a market that is about to make product-level data a condition of entry. Whether they can supply those data is a supply chain design question that must be answered now, before the mandate, while investment choices remain open.

Supply chain design rarely has one right answer. A vertically integrated mill-to-garment exporter and a small Tiruppur knitter that relies on job-work dyehouses face the same passport but start from very different structures. We therefore ask a configurational question: *Which combinations of supplier conditions lead to high passport readiness, which lead to its absence, and is readiness enough for circularity?* To make sense of a fast-moving regulatory landscape (Fawcett & Waller, 2011), we draw on organizational information processing theory (OIPT; Galbraith, 1974; Tushman & Nadler, 1978) and institutional theory (Meyer & Rowan, 1977). We analyse 2026–27 data from Indian textile and apparel exporters with fuzzy-set qualitative comparative analysis (fsQCA; Fiss, 2011; Ragin, 2008), which is designed for exactly this kind of question.

We make three contributions. First, we extend OIPT from a single-route logic of "fit" to *equifinal* routes to fit under regulation-induced, multi-tier information requirements. Second, we show when buyer pressure fails, by identifying *data-appropriation concern* as a condition that turns pressure into resistance. Third, we test whether passport readiness is sufficient for circular capability or decoupled from it. That test speaks to the connections and trade-offs between sustainability, risk and operational excellence that supply chain designers must manage.

# Theory and Propositions

## The Passport as an Information-Processing Problem

OIPT holds that organisations perform better when their *information-processing capacity* matches their *information-processing requirements* (Galbraith, 1974; Tushman & Nadler, 1978). Requirements rise with uncertainty and interdependence. Galbraith (1974) described several design responses: firms can reduce the need for information, for example by bringing interdependent activities under one roof, or raise capacity through vertical information systems or lateral relations across boundaries (Premkumar et al., 2005; Srinivasan & Swink, 2018). Crucially for our purposes, Galbraith presented these as *alternative* strategies, which is an inherently configurational idea.

Supply chain research has mostly tested OIPT with variance-based models that estimate the average net effect of each capability (e.g., Srinivasan & Swink, 2018). Supply chain design suggests otherwise: very different designs can perform equally well (Misangyi et al., 2017). Configurational theory makes this explicit through *conjunction* (conditions work in combination), *equifinality* (several combinations lead to the same outcome) and *asymmetry* (failure is not simply the reverse of success).

The DPP turns diffuse sustainability expectations into structured data fields at the level of the style or batch. For an exporter, the requirement arrives *through the buyer*: EU brands pass down data requests, codes of conduct and contractual data clauses. This **buyer-transmitted pressure** is coercive in institutional terms (DiMaggio & Powell, 1983; Zhu & Sarkis, 2007), but its content is informational. Indian apparel supply chains make it unusually demanding. Production is fragmented across small firms; dyeing, printing and embroidery are often subcontracted as job work; and yarn and fabric are bought through traders who may not disclose the mill of origin. A single garment can pass through five or more independent firms, each holding a fragment of the passport's data.

## Conditions and Configurational Propositions

We consider five conditions. **Vertical integration** is the share of processing stages (spinning to stitching) performed in-house; it *owns* the data by internalising interdependence. **Multi-tier data sharing** is the extent to which independent upstream suppliers provide timely, accurate data (Fawcett et al., 2007; Kembro et al., 2017); it *negotiates* for the data. **Chain-of-custody certification** is the share of EU shipments covered by transaction certificates under schemes such as GOTS or GRS, which document fibre content and the facility at each stage; it *borrows* the data from an institutional infrastructure. **Digital traceability** is structured, interoperable digital capture of material and process data linked to batches and styles (Agrawal et al., 2021; Kouhizadeh et al., 2021); it turns data into passport-ready form. **Data-appropriation concern** is the fear that shared data will be used to squeeze prices or bypass the supplier (King et al., 2023; Sodhi & Tang, 2019).

Buyer pressure is treated as a *background condition*. Nearly every Indian firm supplying EU brands now receives data requests, so pressure defines the setting rather than distinguishing firms within it. We measure and report it, and we reintroduce it as a condition in a robustness check.

Ownership, negotiation and borrowing are alternative ways of reaching upstream data. Galbraith (1974) would predict that any one can be enough.

> **P1 (equifinality).** High DPP readiness results from several configurations: data that are *owned* (vertical integration) or *negotiated* (multi-tier sharing without appropriation concern) combined with digital traceability, and data that are *borrowed* through chain-of-custody certification.

Systems without data are empty containers. But certification schemes already digitise much of the chain, so borrowed data may substitute for a firm's own systems.

> **P2 (substitution).** Digital traceability is not necessary for high DPP readiness when chain-of-custody certification is present.

Pressure alone should not create readiness. Where suppliers fear appropriation, requests produce selective disclosure rather than data (Fawcett et al., 2007). Because fsQCA allows causal asymmetry, the absence of readiness need not mirror its presence.

> **P3 (asymmetry).** Data-appropriation concern, in the absence of both vertical integration and certification, is sufficient for low DPP readiness.

Finally, the passport's policy logic is that information enables circularity: knowing fibre blends and chemistry makes sorting, recycling and reuse feasible (Adisorn et al., 2021; Jensen et al., 2023). Institutional theory warns, however, that firms facing coercive demands often *decouple*, adopting the visible structure while leaving practice unchanged (Meyer & Rowan, 1977). Indian clusters also run an extensive *informal* circular economy that needs no data: cutting waste is sold to recyclers in Panipat, and export surplus is resold domestically. We therefore distinguish informal from *formal*, brand-led circularity (recycled inputs, design-for-recycling, take-back).

> **P4 (two circularities).** DPP readiness is not, on its own, sufficient for formal circular practice, while informal circularity occurs regardless of readiness.

Figure 1 summarises the configurational model.

![Configurational model. Two expected routes to passport readiness (P1), digital traceability as a necessary condition (P2), a route to low readiness (P3), and the readiness–circularity test (P4).](figure1_model.png){width=100%}

# Method

## Why fsQCA

fsQCA treats each firm as a configuration of conditions and uses Boolean minimisation to identify combinations that are consistently sufficient or necessary for an outcome (Ragin, 2008; Schneider & Wagemann, 2012). It suits small-to-medium samples and combines cross-case comparison with case knowledge (Greckhamer et al., 2018).

## Sample and Data Collection

The population is Indian textile and apparel manufacturer-exporters with at least one EU buyer. Following QCA practice, we used purposive, diversity-oriented sampling rather than random sampling (Greckhamer et al., 2018), aiming for variation across clusters (Tiruppur, Bengaluru, the National Capital Region, Ludhiana and Kolkata), supply chain tiers, firm sizes and degrees of integration. Firms were approached through the Apparel Export Promotion Council, cluster associations and referrals. Between December 2026 and February 2027, trained researchers administered a structured questionnaire in person or by video call (30–40 minutes) to owners, directors, or heads of compliance, sustainability or merchandising. Each session ended with three open questions, to build the case knowledge needed to interpret configurations. The final sample comprises 50 firms (17 in Tiruppur/Coimbatore, 11 in NCR, 8 in Ludhiana, 6 in Bengaluru, 5 in Panipat and 3 elsewhere; 46 garment or home-textile makers and 4 mills; median 518 employees).

Three steps protected data quality. First, factual items were asked as concrete yes/no questions about current practice ("Can you supply, today, verified data on…?"). Where respondents agreed, researchers sighted a sample document, such as a transaction certificate, a test report or a buyer data template, for [share, from the fieldwork log; not simulated]% of firms. Second, interviews were conducted by two researchers, who resolved coding disagreements by discussion. Third, the case notes from the open questions were kept alongside the data matrix, so that each configuration could be traced back to recognisable firms. This is the case knowledge that distinguishes fsQCA from purely statistical analysis (Greckhamer et al., 2018).

## Measures and Calibration

To limit the burden of scale development and the risk of common-method bias (Podsakoff et al., 2003), four conditions and both outcomes use *factual* measures rather than attitude scales (Online Appendix A). **DPP readiness** counts the passport data categories (of seven) that the firm can supply, verified and at style or batch level, today. **Circular capability** counts circular practices in place (of five). **Digital traceability** counts digital practices in place (of five). **Vertical integration** counts the processing stages performed in-house (of six). **Multi-tier data sharing** is the share of key upstream suppliers that provide origin and chemical data. **Certification** is the share of EU shipments covered by transaction certificates, checked against a recent certificate. **Data-appropriation concern** and the background measure of **buyer pressure** are three-item, 7-point scales, averaged after reliability checks; their content was reviewed by six industry and academic experts.

We calibrated raw values into fuzzy-set membership using the direct method (Ragin, 2008), with substantively justified anchors for full membership, the crossover point and full non-membership (Table 1). For example, a firm is fully in the set of "DPP-ready" firms when it can supply six or seven categories, and fully out when it can supply one or none. Membership scores of exactly 0.5 were shifted to 0.501 (Fiss, 2011). Anchors were set from substantive knowledge, not from the sample distribution: the readiness anchors reflect the data categories that the textile DPP study expects (European Parliamentary Research Service, 2024), and the integration anchors distinguish garment-only firms from firms that also own fabric and wet processing. The expert panel reviewed all anchors before data collection, and the raw distributions are reported in Online Appendix A so that readers can judge them.

**Table 1.** Conditions, outcomes and calibration anchors.

| Set | Raw measure | Full out (0.05) | Crossover (0.5) | Full in (0.95) |
|---|---|---|---|---|
| DPP readiness (outcome) | Data categories supplied (0–7) | 1 | 3.5 | 6 |
| Circular capability (outcome) | Practices in place (0–5) | 0 | 2.5 | 4 |
| Digital traceability | Practices in place (0–5) | 1 | 2.5 | 4 |
| Vertical integration | Stages in-house (1–6) | 1 | 3.5 | 5 |
| Multi-tier data sharing | % of key suppliers sharing data | 10% | 50% | 90% |
| Chain-of-custody certification | % of EU shipments under transaction certificates | 5% | 50% | 90% |
| Data-appropriation concern | Scale mean (1–7) | 2 | 4 | 6 |
| *Buyer pressure (background; robustness only)* | Scale mean (1–7) | 2 | 4 | 6 |

## Analysis

Using the QCA package in R (Dusa, 2019), we first tested each condition for necessity (consistency ≥ 0.90; Schneider & Wagemann, 2012). We then built truth tables for high and low readiness, with a frequency threshold of one case, raw consistency ≥ 0.80 and PRI consistency ≥ 0.70 (Greckhamer et al., 2018; Ragin, 2008). We report intermediate solutions based on the directional expectations in P1–P3, distinguishing core from peripheral conditions (Fiss, 2011). For P4, we analysed circular capability with readiness included as a condition, both for all circular practices and for *formal* practices only (recycled inputs, design-for-recycling, take-back), because informal practices such as selling cutting waste need no product data. With five conditions there are 32 logically possible configurations, and a sample of this size will not cover all of them (limited diversity). We therefore report how many truth-table rows contain cases, and we justify every simplifying assumption made for the intermediate solution against theory, avoiding untenable assumptions about configurations not observed in the sample (Schneider & Wagemann, 2012). Robustness checks varied calibration anchors, the consistency threshold (0.75–0.85) and the frequency threshold, replaced certification with buyer pressure, and added buyer pressure as a sixth condition (Online Appendix B).

# Results

Cronbach's α was 0.86 for appropriation concern and 0.76 for buyer pressure. Eighteen of the 32 truth-table rows contained cases. As expected, buyer pressure was a background condition: 86% of firms were above the crossover.

**Necessity.** Digital traceability was necessary for high readiness (consistency = 0.907; RoN = 0.659), so P2 is not supported: certified firms also needed their own systems. Every firm with low readiness lacked supplier data sharing (consistency of ~MDS = 0.921).

**Sufficiency for high readiness.** Three configurations were sufficient (Table 2; solution consistency = 0.922, coverage = 0.845). The parsimonious solution reveals two core mechanisms: *trusted capture* (digital traceability without appropriation concern; H1) and *borrowed data* (certification coverage; H2, H3). H1 covered integrated firms and networked SMEs alike (raw coverage = 0.781). Vertical integration and multi-tier sharing appeared only as peripheral conditions, alongside certification. P1 is partly supported. Owned, negotiated and borrowed access all lead to readiness, but ownership and negotiation work through the same mechanism: removing the fear that data will be misused.

**Sufficiency for low readiness.** Two configurations explained low readiness (consistency = 0.919, coverage = 0.837). L1 combines missing traceability with no certification. L2 combines appropriation concern with neither integration nor certification, supporting P3.

**Readiness and circularity.** Readiness alone was not sufficient for circular capability (consistency = 0.752), and still less for formal circular practice (0.496). Formal circularity appeared only where integration and certification coincided, in two Panipat recyclers (VI•~MDS•CERT). Informal practices, such as selling cutting waste or reselling surplus, appeared in every simulated firm, partly by construction. P4 is supported.

**Robustness.** The two mechanisms survived changes to anchors, thresholds and sample (Online Appendix B). With a frequency threshold of two, the single-case integration-plus-certification row dropped out. Adding buyer pressure as a sixth condition left the solution unchanged apart from adding pressure to each configuration: it accompanies every route but explains none.

**Case illustration.** A networked knitter explained that its dyehouses "gave everything" once "the buyer agreed not to contact our dyehouse directly". A subcontractor-dependent NCR exporter gave "partial information only" because "the buyer will go direct next season". A Panipat recycler noted that "GRS needs transaction certificates at every step, so recycled content and origin are already documented". *(Quotes are from simulated case notes.)*

**Table 2.** Configurations for high and low DPP readiness (SIMULATED). ● = core present; • = peripheral present; ⊗ = core absent; ⊘ = peripheral absent; blank = does not matter.

| Condition | H1 Trusted capture | H2 Borrowed + negotiated | H3 Borrowed + owned | L1 No capture, no certificates | L2 Suspicious and unsupported |
|---|---|---|---|---|---|
| Vertical integration | | | • | | ⊗ |
| Digital traceability | ● | • | | ⊗ | |
| Multi-tier data sharing | | • | | ⊘ | ⊘ |
| Chain-of-custody certification | | ● | ● | ⊗ | ⊗ |
| Data-appropriation concern | ⊗ | | | | ● |
| Consistency | 0.930 | 0.999 | 0.970 | 0.970 | 0.912 |
| Raw coverage | 0.781 | 0.424 | 0.387 | 0.558 | 0.691 |
| Unique coverage | 0.341 | 0.023 | 0.038 | 0.146 | 0.279 |
| Solution consistency / coverage | 0.922 / 0.845 | | | 0.919 / 0.837 | |

# Discussion: Trusted or Borrowed

From the supplier side the DPP looks chaotic: almost every exporter faces the same pressure, yet similar-looking firms end up on opposite sides of the readiness line. Our configurations make sense of this through three ideas.

**First, readiness depends on trusted or borrowed access to data, not on structure.** We expected ownership, negotiation and borrowing to form three routes. The data point to two mechanisms. In *trusted capture*, a firm digitises data it can reach without fearing misuse. Ownership achieves this trivially, since a firm does not fear its own data, while negotiation achieves it only where appropriation concern is low. In *borrowed capacity*, certification schemes supply documented chain data that the firm did not have to collect. This refines Galbraith (1974). Lateral capacity is conditional on appropriation safety, and capacity can be held in institutional infrastructure outside the firm. Borrowed data still had to be captured in-house, however: digital traceability remained necessary.

**Second, when pressure is everywhere, it explains nothing.** Buyer pressure accompanied every configuration, successful or not. What separated success from failure was appropriation concern and the absence of any data source. This moves the focus of supply chain transparency research (Sodhi & Tang, 2019) from how much brands ask to how suppliers expect the data to be used (Fawcett et al., 2007).

**Third, there are two circularities, and the passport sees only one.** Indian clusters run an informal circular economy driven by cost rather than data: cutting waste flows from Tiruppur to Panipat, and surplus is resold domestically. Formal circularity was decoupled from readiness (Meyer & Rowan, 1977) and appeared only in integrated, certified recyclers. A passport designed for brand-led loops may leave existing informal loops invisible while creating few new ones.

## Managerial Implications

**Key insights for practitioners.**

* **Trust first, then technology.** Firms with software but fearful or unwilling suppliers stayed below the readiness line.
* **Brands: limit data use and say so.** Written assurance that supplier data will not be used to bypass or squeeze suppliers turns negotiation into a working route.
* **Treat certificates as data assets.** Transaction certificates already hold much of what the passport needs, but they must still be captured digitally.
* **Don't confuse readiness with circularity.** Passport data did not create recycled inputs or take-back on their own.

## Policy Implications

The passport's technical design should recognise certification data, so that suppliers do not collect it twice. Cluster associations in Tiruppur, Ludhiana and Panipat could run shared platforms that extend trusted capture to small firms. Enforceable data-use safeguards, in the passport's access rights or the EU–India agreement, would reduce the appropriation concern behind most failures. And the passport should record recycled content from informal waste flows, not only from brand-run take-back.

## Limitations and Future Research

fsQCA identifies set relations, not net effects, and our purposive sample of 50 firms supports analytical rather than statistical generalisation. Future research should test the two mechanisms with larger samples once the passport is mandatory, and in Bangladesh, Vietnam and Türkiye.

# Conclusion

The Digital Product Passport is a supply chain design decision disguised as a compliance requirement. Suppliers become passport-ready when they can capture data without fearing its misuse, or when they can borrow it from certification, not because buyers push harder. If the passport is to serve circularity and not only compliance, brands and policymakers must build trust, recognise existing certification infrastructure, and see the circular economy that Indian clusters already run.

# References

Adisorn, T., Tholen, L., & Götz, T. (2021). Towards a digital product passport fit for contributing to a circular economy. *Energies, 14*(8), 2289. https://doi.org/10.3390/en14082289

Agrawal, T. K., Kumar, V., Pal, R., Wang, L., & Chen, Y. (2021). Blockchain-based framework for supply chain traceability: A case example of textile and clothing industry. *Computers & Industrial Engineering, 154*, 107130. https://doi.org/10.1016/j.cie.2021.107130

DiMaggio, P. J., & Powell, W. W. (1983). The iron cage revisited: Institutional isomorphism and collective rationality in organizational fields. *American Sociological Review, 48*(2), 147–160.

Dusa, A. (2019). *QCA with R: A comprehensive resource*. Springer.

European Commission. (2026a). *Commission Delegated Regulation (EU) 2026/296 on derogations from the prohibition of the destruction of unsold consumer products*. Official Journal of the European Union.

European Commission. (2026b, January 27). *EU and India conclude negotiations for a free trade agreement* [Press release].

European Parliament and Council. (2024). Regulation (EU) 2024/1781 establishing a framework for the setting of ecodesign requirements for sustainable products. *Official Journal of the European Union*, L 2024/1781.

European Parliament and Council. (2025). Directive (EU) 2025/1892 amending Directive 2008/98/EC on waste. *Official Journal of the European Union*.

European Parliamentary Research Service. (2024). *Digital product passport for the textile sector* (PE 757.808). Panel for the Future of Science and Technology.

Fawcett, S. E., Osterhaus, P., Magnan, G. M., Brau, J. C., & McCarter, M. W. (2007). Information sharing and supply chain performance: The role of connectivity and willingness. *Supply Chain Management: An International Journal, 12*(5), 358–368.

Fawcett, S. E., & Waller, M. A. (2011). Making sense out of chaos? Why theory is relevant to supply chain research. *Journal of Business Logistics, 32*(1), 1–5.

Fiss, P. C. (2011). Building better causal theories: A fuzzy set approach to typologies in organization research. *Academy of Management Journal, 54*(2), 393–420.

Galbraith, J. R. (1974). Organization design: An information processing view. *Interfaces, 4*(3), 28–36.

Greckhamer, T., Furnari, S., Fiss, P. C., & Aguilera, R. V. (2018). Studying configurations with qualitative comparative analysis: Best practices in strategy and organization research. *Strategic Organization, 16*(4), 482–495.

Gualandris, J., Longoni, A., Luzzini, D., & Pagell, M. (2021). The association between supply chain structure and transparency: A large-scale empirical study. *Journal of Operations Management, 67*(7), 803–827.

Jensen, S. F., Kristensen, J. H., Adamsen, S., Christensen, A., & Waehrens, B. V. (2023). Digital product passports for a circular economy: Data needs for product life cycle decision-making. *Sustainable Production and Consumption, 37*, 242–255. https://doi.org/10.1016/j.spc.2023.02.021

Kembro, J., Näslund, D., & Olhager, J. (2017). Information sharing across multiple supply chain tiers: A Delphi study on antecedents. *International Journal of Production Economics, 193*, 77–86.

King, M. R. N., Timms, P. D., & Mountney, S. (2023). A proposed universal definition of a Digital Product Passport Ecosystem (DPPE): Worldviews, discrete capabilities, stakeholder requirements and concerns. *Journal of Cleaner Production, 384*, 135538. https://doi.org/10.1016/j.jclepro.2022.135538

Kouhizadeh, M., Saberi, S., & Sarkis, J. (2021). Blockchain technology and the sustainable supply chain: Theoretically exploring adoption barriers. *International Journal of Production Economics, 231*, 107831.

Meyer, J. W., & Rowan, B. (1977). Institutionalized organizations: Formal structure as myth and ceremony. *American Journal of Sociology, 83*(2), 340–363.

Misangyi, V. F., Greckhamer, T., Furnari, S., Fiss, P. C., Crilly, D., & Aguilera, R. (2017). Embracing causal complexity: The emergence of a neo-configurational perspective. *Journal of Management, 43*(1), 255–282.

Montecchi, M., Plangger, K., & West, D. C. (2021). Supply chain transparency: A bibliometric review and research agenda. *International Journal of Production Economics, 238*, 108152.

Podsakoff, P. M., MacKenzie, S. B., Lee, J.-Y., & Podsakoff, N. P. (2003). Common method biases in behavioral research: A critical review of the literature and recommended remedies. *Journal of Applied Psychology, 88*(5), 879–903.

Premkumar, G., Ramamurthy, K., & Saunders, C. S. (2005). Information processing view of organizations: An exploratory examination of fit in the context of interorganizational relationships. *Journal of Management Information Systems, 22*(1), 257–294.

Ragin, C. C. (2008). *Redesigning social inquiry: Fuzzy sets and beyond*. University of Chicago Press.

Reuters. (2026, January). India's textile exporters pin hopes on EU deal after US tariff blow. *Reuters*.

Schneider, C. Q., & Wagemann, C. (2012). *Set-theoretic methods for the social sciences: A guide to qualitative comparative analysis*. Cambridge University Press.

Sodhi, M. S., & Tang, C. S. (2019). Research opportunities in supply chain transparency. *Production and Operations Management, 28*(12), 2946–2959.

Srinivasan, R., & Swink, M. (2018). An investigation of visibility and flexibility as complements to supply chain analytics: An organizational information processing theory perspective. *Production and Operations Management, 27*(10), 1849–1867. https://doi.org/10.1111/poms.12746

Tushman, M. L., & Nadler, D. A. (1978). Information processing as an integrating concept in organizational design. *Academy of Management Review, 3*(3), 613–624.

Villena, V. H., & Gioia, D. A. (2018). On the riskiness of lower-tier suppliers: Managing sustainability in supply networks. *Journal of Operations Management, 64*, 65–87.

Wilhelm, M. M., Blome, C., Bhakoo, V., & Paulraj, A. (2016). Sustainability in multi-tier supply chains: Understanding the double agency role of the first-tier supplier. *Journal of Operations Management, 41*, 42–60.

Zhu, Q., & Sarkis, J. (2007). The moderating effects of institutional pressures on emergent green supply chain practices and performance. *International Journal of Production Research, 45*(18–19), 4333–4355.
