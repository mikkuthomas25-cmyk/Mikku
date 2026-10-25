"""Greenhouse-gas accounting for one model year (ktCO2e, consumption-based for France)."""
from __future__ import annotations


def ghg(p, f: dict) -> dict:
    production = f["new_kt"] * p.ef_production
    f2f_saving = f["f2f_output"] * p.ef_production * p.fibre_stage_share * (1 - p.f2f_rel_impact)
    handling = f["collected"] * p.ef_sorting
    exported = f["unsorted_export"] + f["overflow_export"] + f["reuse_export"]
    transport = exported * p.ef_export
    burned = f["residual_waste"] + f["overflow_incin"] + f["energy_recovery"] + f["sort_residual"]
    disposal = burned * p.ef_incineration
    credit = (f["recycling_open"] + f["wipes"]) * p.ef_open_recycling_credit
    total = production - f2f_saving + handling + transport + disposal - credit
    return {
        "ghg_production": production,
        "ghg_f2f_saving": f2f_saving,
        "ghg_end_of_life": handling + transport + disposal - credit,
        "ghg_total": total,
    }
