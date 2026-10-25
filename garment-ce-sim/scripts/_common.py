import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))
RESULTS = ROOT / "results"
FIG = RESULTS / "figures"
FIG.mkdir(parents=True, exist_ok=True)

OUTCOMES = {
    "new_kt": "New items placed on market (kt)",
    "virgin_fibre_kt": "Virgin fibre demand (kt)",
    "ghg_total": "GHG (ktCO2e)",
    "collected": "Collected (kt)",
    "sorted": "Sorted (kt)",
    "overflow_total": "Overflow: unsorted export + incineration (kt)",
    "exports_total": "Exports of used textiles (kt)",
    "residual_waste": "Household residual waste (kt)",
    "f2f_output": "Fibre-to-fibre output (kt)",
}


def add_derived(df):
    df = df.copy()
    df["overflow_total"] = df["overflow_export"] + df["overflow_incin"]
    return df
