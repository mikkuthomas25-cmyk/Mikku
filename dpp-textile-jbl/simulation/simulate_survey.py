"""Simulated responses to 03_survey_instrument.md for 50 Indian textile/garment exporters.

*** SIMULATED DATA. NOT REAL FIRMS. NEVER REPORT AS FINDINGS. ***

Purpose: test the fsQCA pipeline, rehearse the analysis and show how the paper reads,
until the real 2026-27 interview data replace this file (same column layout).

Every firm is drawn from one of seven archetypes grounded in how Indian textile and
apparel clusters are organised (see SIMULATION_ASSUMPTIONS.md). Item-level answers are
generated so that they are internally consistent: e.g. stages ticked in Section C
match the firm's tier, digital practices follow a realistic order of difficulty,
and supplier counts in Section E never exceed the number of key suppliers.

Because the archetypes encode our own expectations, any configuration recovered from
these data reflects those assumptions. That is why this is a pipeline test, not evidence.

Usage: python3 simulate_survey.py  ->  ../data/SIMULATED_responses.csv / .xlsx

v0.3: CERT is the % of EU shipments under chain-of-custody transaction certificates (item B7).
"""
import csv
import os
import random
from datetime import date, timedelta

SEED = 20261201
rng = random.Random(SEED)

OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data")

# Order of difficulty (easiest first), used to decide which checklist items are "Yes"
DTC_ORDER = ["DTC_5", "DTC_1", "DTC_2", "DTC_4", "DTC_3"]  # digital certificates are easiest; 1-day trace-back hardest
DPR_ORDER = ["DPR_1", "DPR_5", "DPR_3", "DPR_2", "DPR_6", "DPR_4", "DPR_7"]  # fibre content easiest; machine-readable full set hardest
CSC_ORDER = ["CSC_1", "CSC_4", "CSC_2", "CSC_3", "CSC_5"]  # waste sale & surplus resale common; take-back rare

CERT_POOL = ["GOTS", "GRS/RCS", "OCS", "OEKO-TEX", "bluesign", "Higg FEM verified", "ZDHC", "Better Cotton", "ISO 14001"]
COC = {"GOTS", "GRS/RCS", "OCS"}  # chain-of-custody schemes that issue transaction certificates
# Share of EU shipments under transaction certificates, for firms holding a chain-of-custody scheme
TC_SHARE = {"integrated_large": (25, 70), "networked_sme": (40, 85), "pressured_suspicious": (5, 30),
            "digitised_isolated": (5, 35), "low_pressure_traditional": (0, 15), "certification_led": (70, 98),
            "upstream_mill": (25, 60)}

# --- Archetypes -------------------------------------------------------------------
# Each value is a (low, high) range drawn uniformly, or a list to choose from.
ARCHETYPES = {
    "integrated_large": dict(
        n=9, label="Vertically integrated large exporter",
        clusters=["Bengaluru", "Tiruppur/Coimbatore", "Tiruppur/Coimbatore", "NCR", "Bengaluru"],
        tier=1, employees=(1500, 25000), eu_share=(30, 70), vi=(4, 6), dtc=(3, 5),
        mds=(35, 75), btp=(5.0, 6.7), dac=(1.7, 3.7), certs=(3, 6),
        dpr_rule="integrated", csc=(3, 5),
        notes=[
            ("Our brand customer's sustainability team asked for fibre-to-garment mapping for the spring range; we filled their template from our ERP in two days.",
             "Cotton origin before the spinning mill. We buy some yarn from traders and the gin is not known.",
             "A common format. Every brand sends a different template."),
            ("The buyer audited our dyehouse and asked for chemical inventory per style. Our own dyehouse made it easy.",
             "Trims and accessories suppliers do not give any data.",
             "If the passport format is fixed, we can automate it."),
            ("They wanted carbon per garment. We have Higg FEM for the facility but not per product.",
             "Product-level footprint. Facility data we have.",
             "Training for our IT team on the EU data format."),
        ]),
    "networked_sme": dict(
        n=9, label="Networked SME with trusted long-term partners",
        clusters=["Tiruppur/Coimbatore", "Tiruppur/Coimbatore", "Tiruppur/Coimbatore", "Bengaluru", "Ludhiana"],
        tier=1, employees=(120, 900), eu_share=(45, 90), vi=(2, 3), dtc=(3, 4),
        mds=(60, 95), btp=(4.8, 6.5), dac=(1.8, 3.4), certs=(1, 3),
        dpr_rule="networked", csc=(1, 3),
        notes=[
            ("Our German buyer asked for the dyehouse and mill names. We have worked with the same two dyehouses for twelve years, so they gave everything.",
             "Keeping the data updated when we change a job-work unit in peak season.",
             "The buyer agreed not to contact our dyehouse directly. That made our partners comfortable."),
            ("GOTS transaction certificates already give us the chain up to the ginner. We just scan and upload.",
             "Footprint per product. Our suppliers do not measure it.",
             "A Tiruppur common platform through the association, so small units can also upload."),
            ("Buyer sent a supplier-mapping form. Our knitting and dyeing partners filled it on WhatsApp and we entered it.",
             "Machine-readable format. We do everything in Excel.",
             "Some cost sharing for software."),
        ]),
    "pressured_suspicious": dict(
        n=10, label="Pressured, subcontractor-dependent, suspicious",
        clusters=["NCR", "NCR", "NCR", "Tiruppur/Coimbatore", "Ludhiana"],
        tier=1, employees=(80, 700), eu_share=(40, 85), vi=(2, 2), dtc=(1, 3),
        mds=(5, 40), btp=(5.0, 6.8), dac=(4.8, 6.6), certs=(0, 2),
        dpr_rule="suspicious", csc=(1, 2),
        notes=[
            ("The buyer asked for names of our embroidery and dyeing job-workers. We gave partial information only.",
             "If we give our job-worker names, the buyer will go direct next season. It has happened to others in Noida.",
             "A written assurance that supplier data is only for compliance."),
            ("They asked for chemical lists. Our dyeing job-worker said the chemicals come from local dealers and refused to share.",
             "Job-work units are small and do not keep records.",
             "Price. Every new request is more cost and the FOB keeps falling."),
            ("Fast-fashion buyer wants everything in 48 hours with a new template each time.",
             "Too many templates, no extra price.",
             "Long-term orders. Then suppliers will trust us and we will trust the buyer."),
        ]),
    "digitised_isolated": dict(
        n=6, label="Digitised but isolated from suppliers",
        clusters=["NCR", "Tiruppur/Coimbatore", "Bengaluru", "Ludhiana"],
        tier=1, employees=(200, 1500), eu_share=(35, 75), vi=(2, 3), dtc=(4, 5),
        mds=(10, 35), btp=(5.0, 6.5), dac=(3.6, 5.6), certs=(1, 3),
        dpr_rule="isolated", csc=(1, 3),
        notes=[
            ("We bought a traceability platform last year because the buyer recommended it. Our fabric suppliers never log in.",
             "The software is ready but the data is not there.",
             "Make it compulsory for mills, not only for garment makers."),
            ("Our ERP has lot tracking, but the yarn comes through agents in Surat and Erode who will not reveal the mill.",
             "Agents and traders hide the source.",
             "Direct sourcing from mills, but that needs bigger volumes."),
        ]),
    "low_pressure_traditional": dict(
        n=7, label="Low-pressure traditional exporter",
        clusters=["Ludhiana", "Ludhiana", "Kolkata/other", "Kolkata/other", "Panipat"],
        tier=1, employees=(60, 600), eu_share=(10, 35), vi=(2, 4), dtc=(0, 2),
        mds=(15, 55), btp=(2.0, 3.8), dac=(3.0, 5.2), certs=(0, 1),
        dpr_rule="low_pressure", csc=(1, 3),
        notes=[
            ("Our EU buyers are importers and wholesalers in Poland and Spain. They only ask for OEKO-TEX and the test report.",
             "No one has asked yet. We will see when it is compulsory.",
             "Clear information from AEPC on what is required and when."),
            ("Woollen knitwear to small EU importers. They want price and delivery, not data.",
             "We do not have staff for compliance.",
             "Government scheme for software and training."),
        ]),
    "certification_led": dict(
        n=5, label="Certification-led recycler",
        clusters=["Panipat", "Panipat", "Panipat", "Tiruppur/Coimbatore"],
        tier=1, employees=(150, 1200), eu_share=(35, 70), vi=(3, 4), dtc=(2, 3),
        mds=(40, 65), btp=(4.0, 5.8), dac=(3.0, 4.4), certs=(3, 5),
        dpr_rule="certification", csc=(4, 5),
        notes=[
            ("GRS needs transaction certificates at every step, so recycled content and origin are already documented.",
             "Chemical data for recycled fibre. The original dyes are unknown.",
             "Recognition of GRS data in the passport so we do not duplicate."),
            ("We make blankets and rugs from recycled yarn. Waste comes from Tiruppur cutting rooms and imported clothing.",
             "Footprint data. Nobody has asked us to measure it per product.",
             "Better prices for recycled products."),
        ]),
    "upstream_mill": dict(
        n=4, label="Upstream spinner/fabric mill",
        clusters=["Tiruppur/Coimbatore", "Tiruppur/Coimbatore", "Kolkata/other", "NCR"],
        tier=None, employees=(400, 4000), eu_share=(15, 45), vi=(2, 3), dtc=(3, 4),
        mds=(15, 40), btp=(3.5, 5.0), dac=(2.5, 4.2), certs=(2, 4),
        dpr_rule="mill", csc=(2, 3),
        notes=[
            ("Garment exporters forward the brand's request to us. We give lot-wise fibre and origin from our ERP.",
             "Cotton bought through traders and the Cotton Corporation. The farm or gin is not always known.",
             "Traceable cotton programmes at the ginning level."),
            ("We supply fabric to Tiruppur exporters for EU brands. Chemical data we give; who our yarn mill is, we do not like to disclose.",
             "Customers may bypass us and buy direct from the yarn mill.",
             "Confidentiality agreements."),
        ]),
}


def u(lo, hi):
    return rng.uniform(lo, hi)


def ri(lo, hi):
    return rng.randint(lo, hi)


def likert_items(mean, k=3, spread=0.9):
    """k Likert items (1-7) scattered around a firm-level mean, clipped and rounded."""
    return [int(min(7, max(1, round(mean + rng.gauss(0, spread))))) for _ in range(k)]


def checklist(order, n_yes, jitter=0.25):
    """Mark n_yes items Yes, mostly the easiest ones, occasionally swapping adjacent items."""
    items = list(order)
    for i in range(len(items) - 1):
        if rng.random() < jitter:
            items[i], items[i + 1] = items[i + 1], items[i]
    yes = set(items[:n_yes])
    return {c: ("Y" if c in yes else "N") for c in order}


def stages(tier, vi):
    """Section C stages consistent with tier and the number of in-house stages."""
    if tier == 1:  # garment maker: cutting/sewing and packing always in-house
        base = ["VI_4", "VI_6"]
        extras = ["VI_3", "VI_2", "VI_5", "VI_1"]  # dyeing, knitting/weaving, embroidery, spinning
    elif tier == 2:  # fabric mill
        base = ["VI_2", "VI_6"]
        extras = ["VI_3", "VI_1", "VI_5", "VI_4"]
    else:  # spinner
        base = ["VI_1", "VI_6"]
        extras = ["VI_2", "VI_3", "VI_5", "VI_4"]
    chosen = set(base + extras[: max(0, vi - len(base))])
    return {f"VI_{i}": ("Y" if f"VI_{i}" in chosen else "N") for i in range(1, 7)}


def readiness(rule, dtc, vi, mds, dac, certs, tc):
    """Number of passport data categories the firm can supply (0-7).

    Encodes the simulation's assumptions (see SIMULATION_ASSUMPTIONS.md), including
    deliberate exceptions so that the data are not perfectly consistent.
    """
    if rule == "integrated":
        base = 3 + 0.5 * dtc + 0.3 * (vi - 4)
        if dtc <= 2:          # integration without digital capture: data exist on paper only
            base = 3
    elif rule == "networked":
        base = 2.4 + 0.6 * dtc + 0.02 * (mds - 60) - 0.4 * max(0, dac - 3)
    elif rule == "suspicious":
        base = 0.5 + 0.4 * dtc
    elif rule == "isolated":
        base = 1.5 + 0.3 * dtc       # systems without upstream data: "empty containers"
    elif rule == "low_pressure":
        base = 0.6 + 0.6 * dtc
    elif rule == "certification":
        base = 2.6 + 0.03 * tc       # transaction certificates substitute for in-house systems
    else:  # mill
        base = 2.2 + 0.4 * dtc
    # Assumption ("borrowed" data): transaction certificates document fibre content and the
    # facility of each stage, adding up to about one data category for full TC coverage.
    if rule != "certification":
        base += 0.012 * tc
    return int(min(7, max(0, round(base + rng.gauss(0, 0.6)))))


def build_firms():
    firms = []
    fid = 0
    start = date(2026, 12, 1)
    for key, a in ARCHETYPES.items():
        for j in range(a["n"]):
            fid += 1
            cluster = a["clusters"][j % len(a["clusters"])]
            tier = a["tier"] if a["tier"] else (3 if j % 2 == 0 else 2)
            vi = ri(*a["vi"]) if tier == 1 else ri(2, 3)
            dtc = ri(*a["dtc"])
            mds_pct = u(*a["mds"])
            btp_mean = u(*a["btp"])
            dac_mean = u(*a["dac"])
            ncert = ri(*a["certs"])
            certs = rng.sample(CERT_POOL, ncert)
            if key == "certification_led" and "GRS/RCS" not in certs:
                certs[0] = "GRS/RCS"
            if key == "networked_sme" and cluster.startswith("Tiruppur") and "GOTS" not in certs and certs:
                certs[0] = "GOTS"

            mds_n = ri(4, 18)
            share = mds_pct / 100
            origin = min(mds_n, max(0, round(mds_n * min(1, share + 0.10))))
            chem = min(origin, max(0, round(mds_n * share)))
            who = min(chem, max(0, round(mds_n * max(0, share - 0.15))))

            tc = ri(*TC_SHARE[key]) if COC & set(certs) else 0
            dpr_n = readiness(a["dpr_rule"], dtc, vi, mds_pct, dac_mean, ncert, tc)
            csc_n = ri(*a["csc"])
            note = a["notes"][j % len(a["notes"])]

            row = {
                "firm": f"SIM{fid:02d}",
                "SIMULATED": "YES",
                "archetype": a["label"],
                "interview_date": str(start + timedelta(days=int(fid * 1.7))),
                "A1": rng.choice(["Owner/MD/Director", "Head of compliance/sustainability",
                                  "Head of merchandising/sourcing"]),
                "A2": ri(3, 22),
                "A3": rng.choice(["Directly responsible", "Directly responsible", "Somewhat"]),
                "B1": "Yes",
                "B2": ri(*a["employees"]),
                "B3": tier,
                "B4": ri(*a["eu_share"]),
                "B5": cluster,
                "B6": "; ".join(certs),
                "B7": tc,
            }
            row.update(stages(tier, vi))
            row.update(checklist(DTC_ORDER, dtc))
            row.update({"MDS_N": mds_n, "MDS_1": origin, "MDS_2": chem, "MDS_3": who})
            row.update(dict(zip(["BTP_1", "BTP_2", "BTP_3"], likert_items(btp_mean))))
            row.update(checklist(DPR_ORDER, dpr_n, jitter=0.15))
            row.update(dict(zip(["DAC_1", "DAC_2", "DAC_3"], likert_items(dac_mean))))
            row.update(checklist(CSC_ORDER, csc_n, jitter=0.2))
            row.update({"J1": note[0], "J2": note[1], "J3": note[2]})
            firms.append(row)
    return firms


def main():
    firms = build_firms()
    os.makedirs(OUT_DIR, exist_ok=True)
    path = os.path.join(OUT_DIR, "SIMULATED_responses.csv")
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(firms[0].keys()))
        w.writeheader()
        w.writerows(firms)
    try:
        import pandas as pd
        pd.read_csv(path).to_excel(os.path.join(OUT_DIR, "SIMULATED_responses.xlsx"), index=False,
                                   sheet_name="SIMULATED - not real")
    except Exception as e:  # Excel copy is a convenience only
        print("xlsx not written:", e)
    print(f"Wrote {len(firms)} SIMULATED firms to {path}")


if __name__ == "__main__":
    main()
