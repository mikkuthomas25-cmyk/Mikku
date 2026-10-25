# Accuracy is not circularity: a simulation study of how machine-learning sorting decisions propagate to fibre-to-fibre recycling of post-consumer textiles

*Draft v0.1 — prepared for supervisor review. Author list, affiliations and AI-use
disclosure to be completed by the human author(s).*

> **Verification flags.** Statements marked **[V-press]** rest on secondary or trade-press
> reporting and must be checked against the primary source before submission; see
> `docs/literature.md`. Parameters marked *assumed* have no empirical source and are
> varied in the sensitivity analysis.

---

## Abstract

Fibre-to-fibre recycling is the step on which a circular textile economy depends, and it
depends in turn on sorting: chemical and mechanical recyclers accept feedstock only if it
meets tight fibre-purity specifications. Near-infrared (NIR) spectroscopy combined with
machine learning (ML) is the leading technology for automated fibre sorting, and the
literature evaluates it almost exclusively by item-level classification accuracy on
laboratory sample sets. We argue that item-level accuracy is the wrong yardstick for a
circular-economy technology and build a stochastic, item-level discrete-event simulation
that links the ML sorting decision to the outcomes that matter to the recycling chain:
recycler acceptance of bales and the share of fibre that actually closes the loop. The
model represents the composition of post-consumer textile streams, an NIR quantification
error calibrated to published root-mean-square errors of prediction (1–6.6%), a
selective-classification (reject-option) policy, a finite-capacity and error-prone manual
re-sort station, mechanical discharge errors, digital product passport (DPP) data, baling,
and recycler quality control. Six experiments with common random numbers and 20–30
replications per configuration show that
(i) raising the confidence threshold monotonically increases the accuracy of auto-sorted
items while **reducing** recovered cotton, because abstained items overload a manual
fallback that is less accurate than the sensor;
(ii) a recycler specification of 98% instead of 95% cotton turns a contamination problem
that is invisible at item level into large bale-rejection rates, and larger bales sharpen
this into an acceptance cliff;
(iii) DPP data raise recovery mainly by rescuing items the sensor cannot read, but only if
the DPP's own error rate is low; and
(iv) under drift, abstention acquires a second value—it generates labels that let the
classifier recover—so the best operating point depends on the planning horizon.
**[Numbers to be filled from results.]** The results imply that ML sorting systems for
textile circularity should be specified, tuned and regulated by system-level metrics—
accepted-bale yield at a stated recycler specification—rather than by classification
accuracy.

**Keywords:** circular economy; textile recycling; fibre-to-fibre; machine learning;
selective classification; near-infrared spectroscopy; discrete-event simulation; digital
product passport; sorting.

---

## 1. Introduction

The textile system is one of the least circular material systems in the economy. Less
than 1% of the material used to produce clothing is recycled into new clothing
(Ellen MacArthur Foundation, 2017) **[V-press]**, and the European Union alone generates
about 12.6 million tonnes of textile waste a year, of which only around 22% of
post-consumer textiles are collected separately for reuse or recycling (European
Commission, 2023) **[V-press]**. Policy is now forcing the issue. Separate collection of
textiles has been mandatory in EU Member States since 1 January 2025, and Directive (EU)
2025/1892 requires Member States to set up extended producer responsibility (EPR) schemes
for textiles, under which producers bear the costs of collection, sorting and recycling,
by April 2028. The Ecodesign for Sustainable Products Regulation (EU) 2024/1781 adds a
Digital Product Passport (DPP) that is expected to carry, among other information, the
fibre composition of apparel products.

Collection alone does not close a loop. Once collected, non-rewearable textiles must be
sorted into fractions that recyclers will accept. A large sorting study across six
European countries estimated that 74% of low-value post-consumer textiles could in
principle be fibre-to-fibre recycled, yet only about 2% currently are (Fashion for Good &
Circle Economy, 2022) **[V-press]**. The bottleneck is the quality of sorted feedstock.
Chemical pulping of cellulose, for example, has been reported to require inputs of about
98% cotton **[V-press]**, and chemical recycling of synthetic fibres needs polyester or
polyamide contents of roughly 80–90% to be economic **[V-press]**. Manual sorting relies
largely on care labels, which are unreliable: 41% of garments scanned in a Dutch study
**[V-press]** and 37% of garments tested in a 2025 coordinated EU market-surveillance
action **[V-press]** had inaccurate fibre-composition labels, with blends faring worst.

Automated sorting with NIR spectroscopy and machine learning is the technology expected to
fix this (Faghih et al., 2025). Its research literature, however, evaluates it almost
exclusively with item-level classification accuracy on controlled sample sets, where
accuracies between about 90% and 100% are routinely reported (Riba et al., 2020; Li et
al., 2023). Two observations suggest that this yardstick is insufficient. First,
classifiers deployed on real streams perform worse than in the laboratory: quantification
errors of 3.1–6.6% have been reported for post-consumer cotton/polyester blends
(Stipanovic et al., 2025), against below 1% for benchtop instruments on controlled
samples (Lilek et al., 2026). Second, and more fundamentally, a recycler does not accept
or reject *items*; it accepts or rejects *bales*, by their aggregate purity. Whether an
ML error matters therefore depends on what kind of item was misrouted, where it went,
what happened to items the classifier was unsure about, how large the bale is, and how
strict the specification is. None of these is visible in a confusion matrix.

Simulation is the natural tool to connect the two levels, and it has a long history in
circular-economy research. In the textile domain, however, simulation studies have
focused on collection logistics and citizen behaviour (agent-based models of household
textile collection; Zammori et al.) and on aggregate policy dynamics (system dynamics
models comparing sorting capacity with consumption reduction; Valtere et al., 2025), with
sorting represented as a capacity or a fixed split ratio. The only work that propagates
sensor errors to product grade concerns plastic packaging, where Monte Carlo and
discrete-event models of sensor-based sorting have been fitted to experimental data
(TU Delft study; VTT/Leoben, 2026). These models do not represent the features that make
textile sorting distinctive—blends that sit close to class boundaries, minor elastane
content, unusable spectra of carbon-black-dyed items, unreliable labels—nor the ML
decision policy itself.

This paper addresses that gap. We ask:

- **RQ1.** How does the ML operating point—specifically the confidence threshold of a
  selective classifier—affect recycler acceptance and closed-loop yield, and does the
  operating point that maximises item-level accuracy also maximise circularity?
- **RQ2.** Under which conditions of manual-fallback capacity and accuracy does
  abstaining from automatic decisions help?
- **RQ3.** How do recycler specifications and bale size shape the translation of
  item-level errors into bale rejection?
- **RQ4.** How much do DPP composition data add, and how does their value depend on
  sensor quality and on DPP data quality?
- **RQ5.** When the incoming stream drifts, how do labelling and retraining policies
  interact with the operating point over time?

Our contributions are (1) an open, verified, item-level discrete-event simulation model
of ML-assisted textile sorting that couples a measurement-error sensor model and a
selective-classification policy with a queueing model of manual re-sorting and a
bale-acceptance model of the recycler; (2) evidence that item-level accuracy and system
circularity can move in opposite directions; and (3) design and policy implications for
specifying ML sorting systems in EPR-funded sorting infrastructure.

## 2. Background

### 2.1 Fibre-to-fibre recycling and its feedstock requirements

Reviewing 41 life-cycle studies, Sandin and Peters (2018) concluded that reuse and
recycling generally reduce environmental impact relative to incineration and landfill,
with the benefit hinging on how much virgin production the recovered material displaces.
In the waste hierarchy, fibre-to-fibre recycling sits above open-loop recycling
(downcycling into insulation, wipers or fillers) because it substitutes virgin fibre in
the same application. Two families of processes exist. *Mechanical* recycling shreds
fabric back to fibre; it preserves the fibre's chemistry but shortens it, and it needs
colour- and composition-sorted input. *Chemical* recycling dissolves or depolymerises the
fibre—cellulose to pulp for regenerated fibres, polyester to monomers or oligomers—and
can tolerate colour but is sensitive to foreign fibres, elastane and finishes. In the
Sorting for Circularity Europe analysis, 21% of the low-value material was judged
suitable for mechanical and 53% for chemical recycling (Fashion for Good & Circle
Economy, 2022) **[V-press]**.

What makes these routes viable or not is the purity of their feedstock. Reported
thresholds include about 98% cotton for a commercial cellulose-pulping process and a
claimed tolerance to about 80% cellulose content for another **[V-press]**, and 80–90%
PET or PA for chemical recycling of synthetics to be economic **[V-press]**. The same
trade-press source reports bales sold as pure cotton containing 20–40% polyester
**[V-press]**, illustrating why sorting quality, not collection volume, is the binding
constraint. The February 2024 bankruptcy of the operator of the first commercial-scale
textile-to-pulp plant **[V-press]** is a reminder that the viability of fibre-to-fibre
recycling is economic as well as technical. Minor components matter as well:
elastane, present in about one in five non-rewearable single-material items
**[V-press]**, is a contaminant for several fibre-to-fibre processes even at 1–5%.

### 2.2 NIR and machine learning for textile sorting

NIR spectroscopy identifies fibres through overtone and combination bands of C–H, O–H
and N–H bonds. Two kinds of ML model are used on the spectra. *Classification* models
assign an item to a discrete fibre class (support vector machines, k-nearest neighbours,
random forests, one-dimensional convolutional networks), with reported accuracies
typically between 90% and 100% on laboratory sets—for example 100% on unknown samples
with multivariate methods (Riba et al., 2020) and 96.6% on 232 held-out fabrics with a
one-dimensional CNN over 12 classes including blends (Li et al., 2023). *Quantification*
models regress the mass fraction of a fibre, typically cotton in cotton/polyester blends,
and report a root-mean-square error of prediction (RMSEP): below 1% for benchtop
instruments on blends from a single starting material and about 1.2–1.6% for handheld and
imaging sensors (Lilek et al., 2026), but 3.1% for a handheld instrument and 6.6% for a
lab-scale NIR sorter on real post-consumer textiles (Stipanovic et al., 2025).

Three textile-specific difficulties recur. Blends whose minor fibre is 10% or less tend to
be classified as the pure majority fibre (*Textiles* mini-review, 2026); elastane
detection is markedly weaker on the samples that actually contain elastane (83.3%
accuracy versus 92.6% overall in one thesis-scale study; Rijken, 2026); and textiles dyed
with carbon black, such as black denim, absorb so much NIR radiation that their spectra
cannot be interpreted and are usually diverted to a separate fraction **[V-press]**. A
further warning comes from plastics, where a CNN with about 95% offline accuracy achieved
43.5% sorting accuracy in a live trial (Liège MSc thesis). Systematic reviews identify blend
detection and the scarcity of representative data as the main open problems (Faghih et
al., 2025), and open spectral datasets of known-provenance and post-consumer textiles have
only recently appeared (NIST NIR-SORT; OpenTextile-NIR). Industrial NIR sorting lines
operate at roughly 0.9–1.2 t/h for one established system and up to 4.5 t/h claimed for
another **[V-press]**.

Across this literature, the unit of evaluation is the item (or the pixel), and the metric
is accuracy or RMSEP. What happens to the misclassified items downstream is not
analysed.

### 2.3 Selective classification

A selective classifier may abstain when it is not confident enough. Chow (1970) showed
that the error-minimising rule rejects an input when its largest posterior class
probability falls below a threshold, and Geifman and El-Yaniv (2017) operationalised the
resulting risk–coverage trade-off for deep networks. In the ML literature the cost of an
abstention is abstract. In a sorting plant it is concrete: an abstained garment either
goes to a residual fraction (lost to the loop) or to a manual station with finite
capacity and its own error rate. Our model makes that cost explicit.

### 2.4 Simulation of circular textile systems and of sensor-based sorting

Simulation studies of circular textile systems concentrate upstream and in the
aggregate. Agent-based models compare standard and innovative household textile
collection systems and show that citizens' interactions strongly influence collection
efficiency, with follow-up hybrid models testing bin numbers, placement and routing
(Zammori, Romagnoli, Mercogliano and co-authors, Univ. Parma). A system dynamics model
for Latvia concluded that reducing clothing consumption is more effective than expanding
sorting capacity (Valtere et al., 2025). A discrete-event model of a textile recycling
system was used to study interactions between system components (Univ. Windsor, 2019),
and interactive tools such as CircSim and Valoramix represent R-strategies and material
flows **[V-press]**. In all of these, sorting is a capacity or a fixed split ratio: no ML
decision, error structure or recycler acceptance is represented.

The closest methodological precedent is in plastics. A TU Delft study measured the
probability of each type of NIR false call for 13 plastic classes and propagated them by
Monte Carlo to product grade, estimating about 85% average product grade; a VTT and
Montanuniversität Leoben model of a sensor-based sorting unit built on discrete-event
principles and fitted to experimental throughput and composition data reproduced
multi-step sorting experiments accurately (Waste Management Bulletin, 2026); regression
models have been used to find the operating point that achieves a target purity and
recovery (*Polymers*, 2023); and the distinction between detection errors and mechanical
discharge errors, with purity, recovery and yield as the performance measures, is well
established (DETRITUS, 2020). On the textile side, an engineering analysis that
deconstructed a bale of post-consumer clothing derived the minimum per-garment primary
content needed for an output stream to meet recyclers' target purity (Oak Ridge National
Laboratory; AIChE) **[TO-READ]**. This work points to the right unit—the bale—but does
not simulate a sorting policy.

We therefore position our model as an extension of sensor-based-sorting simulation from
plastics to textiles, with three additions: textile-specific item heterogeneity (near-
boundary blends, elastane, unusable spectra, unreliable labels); the ML decision policy
(selective classification with a capacity-limited human fallback); and the recycler's
bale-acceptance decision, together with DPP data and classifier drift.

## 3. Model

The model is described following the purpose–entities–process structure of the ODD
protocol, adapted to a discrete-event model. Source code, tests and parameter sources are
in the accompanying repository (`sim/model.py`, `tests/`, `docs/parameters.md`).

### 3.1 Purpose and scope

The model estimates how ML sorting decisions at a single automated sorting line
determine (a) the purity and acceptance of bales delivered to fibre-to-fibre recyclers
and (b) the share of input mass and of available cotton that is recovered in accepted
bales. Collection, pre-sorting for reuse, and the recycling processes themselves are
outside the scope; the input is the stream of non-rewearable post-consumer textiles
arriving at the automated line.

### 3.2 Entities and state variables

*Garments* have a mass (lognormal, mean 0.35 kg, CV 0.6; *assumed*), an arrival time
(determined by a line throughput of 1000 kg/h, in the range reported for industrial
systems **[V-press]**), a true mass composition over four fibre groups (cotton,
polyester, elastane, other), and flags for carbon-black/dark colour, DPP availability,
and membership of a "novel" sub-population (used in the drift experiment). Item
categories follow the stream composition of Fashion for Good & Circle Economy (2022):
42% mono-cotton, 12% polycotton, 20% other blends, with the remaining 26% split into
mono-polyester (16%, *assumed*) and other mono-materials (10%, *assumed*). One in five
mono-material items contains elastane (1–5% in cotton, 1–10% in polyester)
**[V-press]**. Polycotton blends take cotton fractions of 90, 80, 60, 50 or 35% (*assumed*
distribution), deliberately including near-pure 90/10 blends.

The *true sort class* of an item follows a composition rule: COT if cotton ≥ 95%; PES if
polyester ≥ 95%; PC (polycotton) if cotton + polyester ≥ 95%; otherwise OTH. Output
lines COT, PES and PC are baled; OTH and all discarded items form the residual fraction.

### 3.3 Process overview

Each replication simulates one week (40 h) of operation, ≈114,000 garments and 40 t:

1. **Sensing.** The NIR+ML model estimates the cotton and polyester mass fractions with
   unbiased Gaussian error of standard deviation σ (the RMSEP). The predicted class is the
   composition rule applied to the estimate. σ ranges from 1% (benchtop, controlled
   samples; Lilek et al., 2026) to 6.6% (lab-scale sorter on post-consumer blends;
   Stipanovic et al., 2025); the baseline is 3.5%. The confidence score is the share of
   64 resamples drawn around the estimate with the same error model that fall in the
   predicted class—an uncertainty-aware plug-in estimate of the posterior probability of
   the predicted class. Dark/carbon-black items (5%, *assumed*) return no usable spectrum,
   are detected by their low signal and are routed to the residual fraction, as reported
   for industrial NIR textile sorters **[V-press]**.
2. **Selective routing.** If confidence ≥ τ the item is routed automatically to its
   predicted line; otherwise it is sent to manual re-sorting. τ = 0 means no abstention.
3. **DPP override.** If the item carries a readable DPP, its declared composition
   overrides the sensor. A DPP declares the wrong class with probability 2% (*assumed*;
   10% in E4).
4. **Mechanical discharge.** A machine-routed item lands in a random wrong bin with
   probability 2% (*assumed*), representing the discharge-error pathway documented for
   sensor-based sorting (DETRITUS, 2020).
5. **Manual re-sort station.** Abstained items join a first-come-first-served queue with
   c sorters (baseline 2) and a waiting room of K = 20 items; arrivals that find the room
   full overflow to the residual fraction. Service times are lognormal (mean 20 s, CV
   0.5; *assumed*). A manual sorter assigns the correct class with probability 0.85
   (*assumed*, bracketed by the 37–41% label-inaccuracy rates, many of which do not change
   the sort class) and otherwise the most plausible wrong class. The station is simulated
   as a G/G/c/c+K queue in event order.
6. **Baling and acceptance.** Each output line is cut into bales of 200 kg (*assumed*) in
   arrival order. Bale purity is the mass share of the target fibre(s). The recycler
   measures purity with Gaussian error (SD 0.5 percentage points; *assumed*) and accepts
   the bale if the measurement meets the specification: 95% or 98% cotton for COT,
   90% polyester for PES, 95% cotton+polyester for PC. The trailing incomplete bale of
   each line is carried over to the next period; to avoid a horizon artefact that
   would grow with bale size, its mass is credited at the line's observed acceptance
   rate in the replication.

### 3.4 Outputs

- *Item accuracy*: share of sensed items whose predicted class equals the true class;
  *auto accuracy*: the same for automatically routed items only (the "selective risk"
  complement reported in ML studies); *coverage*: share routed automatically.
- *Bale rejection rate* and mean *bale purity* per line.
- *Cotton recovery*: cotton mass in accepted COT bales divided by the cotton mass in
  items whose true class is COT.
- *Fibre-to-fibre share*: mass in accepted COT and PES bales over input mass;
  *closed-loop share*: the same including accepted PC bales.
- Manual-station utilisation and overflow.

### 3.5 Verification and validation

Verification tests (`tests/test_model.py`, 11 tests, all passing) check: the class rule
on hand-constructed compositions; mass conservation and arrival ordering; that a
noiseless sensor is perfectly accurate and fully confident; that accuracy decreases
monotonically in σ; that a perfect system yields only accepted bales; that bale purity
equals its mass-weighted definition; that the queue reproduces the Erlang-B blocking
probability of an M/M/c/c system and the utilisation of an M/M/1 system; that full,
error-free DPP coverage bypasses the sensor; and reproducibility under common random
numbers.

*Validation status.* The sensor model reproduces the range of item-level accuracies
reported in the literature (≈80–99% across the σ range; Section 5.1). Operational
validation against plant data was not possible: no public data on bale acceptance at
textile recyclers were found. The model is therefore used for comparative, not
predictive, analysis, which is the standard use of simulation when a system is being
designed rather than operated. An empirical replacement of the parametric sensor by a
classifier trained on the NIST NIR-SORT spectra is planned (Section 6.4).

### 3.6 Experimental design and output analysis

All experiments use common random numbers (replication r uses the same seed in every
configuration). A pilot of 10 replications gave standard deviations up to 0.04 for
cotton recovery and 0.05 for bale rejection; we use 30 replications (20 in E2 and drift
trajectories) so that 95% confidence-interval half-widths are about 1.5–2 percentage
points or less (Law, 2015). Table 1 summarises the experiments.

| Exp. | Question | Factors (levels) | Reps |
|---|---|---|---|
| E1 | RQ1 | σ (1, 2, 3.5, 5, 6.6%) × τ (0, .5, .6, .7, .8, .9, .95, .99) × COT spec (95, 98%) | 30 |
| E2 | RQ2 | τ (8) × manual sorters (0, 1, 2, 4, 8) × manual accuracy (.75, .85, .95) × spec (2) | 20 |
| E3 | RQ3 | bale size (50, 100, 200, 500, 1000 kg) × COT spec (95–99%) | 30 |
| E4 | RQ4 | DPP share (0–100%) × σ (3.5, 6.6%) × DPP error (2, 10%) × spec (2) | 30 |
| E5 | RQ5 | 5 labelling/retraining policies × spec (2); 52 weekly periods | 10 trajectories |
| E6 | robustness | one-at-a-time low/high for 11 parameters × spec (2) × τ (0, 0.8) | 30 |

## 4. Results

All values are means over replications with 95% confidence-interval half-widths;
differences between policies are paired across common random numbers. Summary tables are
in `results/tables/`.

### 4.1 Item-level accuracy and system circularity diverge (E1)

Across the RMSEP range of 1–6.6%, the sensor model produces item-level accuracies of
98.7% to 80.3% (Table 2), which brackets the accuracies reported for laboratory and
industrial settings in Section 2.2. Without abstention (τ = 0) and with a 95% cotton
specification, cotton recovery falls from 91.6% to 70.7% across that range. No cotton bale
is rejected at any σ: misclassified items near the 95% class boundary contaminate bales
only marginally, and mean bale purity stays at 98.4–98.7%. At this specification,
sensor error costs *yield* (cotton routed to the wrong line or the residual), not
*acceptance*.

Under a 98% specification the picture changes. The same bale purities now fall
close to the threshold, and 13.6% of cotton bales are rejected even with the best sensor
(σ = 1%), rising to 28.7% at σ = 6.6%. Improving the sensor from 2% to 1% RMSEP
raises recovery by only 1.3 points (77.9% → 79.2%), because at that point the residual
contamination comes from sources that are not the classifier: mechanical discharge
errors and elastane in "cotton" garments (quantified in E6).

Figure 1 shows the central result. Raising the confidence threshold τ increases the
accuracy of auto-sorted items monotonically at every σ—exactly the risk–coverage
behaviour that selective-classification studies report—but cotton recovery is
non-monotone: it rises for moderate τ and collapses beyond it. With σ = 3.5% and a 95%
specification, the recovery-maximising threshold is τ = 0.6 (recovery 88.4%, +5.8
points over τ = 0, paired CI ±0.05), while the accuracy-maximising threshold τ = 0.99
delivers 98.8% auto-accuracy and only 31.4% recovery, 57.0 points less than the
optimum. Under the 98% specification the collapse is sharper, because items that the
overloaded manual station does handle are re-sorted with an 85% class accuracy; their
errors push bale rejection from 15.6% at τ = 0 to 69.8% at τ = 0.99, and recovery
falls to 9.5%. The recovery-optimal threshold decreases as the sensor gets worse
(0.8 at σ ≤ 2%, 0.5–0.6 at σ ≥ 3.5% for the 95% spec) and, for the 98% spec and good
sensors, is τ = 0: abstention does not pay at all.

The mechanism is visible in the manual-station statistics. At σ = 3.5% the station
receives 4% of items at τ = 0.5, 16% at τ = 0.7 and 58% at τ = 0.99; with two sorters
it starts to overflow at τ ≈ 0.7 (20% of abstained items overflow at τ = 0.7, 78% at
τ = 0.99), and overflow goes to the residual. **The operating point that a classification benchmark would select
is among the worst for circularity.**

[Sections 4.2–4.6 and 5–7 to be written from E2–E6 results.]

## References (draft — see docs/literature.md for verification status)

- Chow, C.K. (1970). On optimum recognition error and reject tradeoff. *IEEE Transactions on Information Theory*, 16(1), 41–46.
- Directive (EU) 2025/1892 of the European Parliament and of the Council of 10 September 2025 amending Directive 2008/98/EC on waste. *OJ L*, 26.9.2025.
- Ellen MacArthur Foundation (2017). *A New Textiles Economy: Redesigning Fashion's Future*.
- European Commission (2023). Proposal for a targeted revision of the Waste Framework Directive (press release IP/23/3635).
- Faghih, S., Saki, Z., & Moore, M. (2025). AI-enabled textile waste sorting. *Sustainability*, 17(10), 4264.
- Fashion for Good & Circle Economy (2022). *Sorting for Circularity Europe*.
- Geifman, Y., & El-Yaniv, R. (2017). Selective classification for deep neural networks. *Advances in Neural Information Processing Systems 30*, 4878–4887.
- Law, A.M. (2015). *Simulation Modeling and Analysis* (5th ed.). McGraw-Hill.
- Li et al. (2023). [1D-CNN NIR waste textile sorting]. *Spectroscopy and Spectral Analysis*, 43(7), 2139.
- Lilek et al. (2026). [NIR cotton quantification instrument comparison]. *Applied Sciences*, 16(7), 3242. doi:10.3390/app16073242
- Regulation (EU) 2024/1781 (Ecodesign for Sustainable Products Regulation).
- Riba, J.-R., Cantero, R., Canals, T., & Puig, R. (2020). Circular economy of post-consumer textile waste: Classification through infrared spectroscopy. *Journal of Cleaner Production*, 272, 123011. doi:10.1016/j.jclepro.2020.123011
- Sandin, G., & Peters, G.M. (2018). Environmental impact of textile reuse and recycling – A review. *Journal of Cleaner Production*, 184, 353–365. doi:10.1016/j.jclepro.2018.02.266
- Stipanovic, H., et al. (2025). Quantifying cotton content in post-consumer polyester/cotton blend textiles. *Recycling*, 10(4), 152. doi:10.3390/recycling10040152
- Valtere et al. (2025). What drives the circular economy? Textile sorting or consumption reduction. Riga Technical University.
- VTT / Montanuniversität Leoben (2026). Simulating process parameters of sensor-based sorting for plastic packaging waste: Methodology and validation. *Waste Management Bulletin*, 4(2), 100299.
- [Remaining entries in docs/literature.md.]
