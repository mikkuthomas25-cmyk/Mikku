"""Calibrate the model to French TLC data and write results/calibration.json."""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "src"))

from gcesim.calibration import calibrate, fit_stats, target_table  # noqa: E402
from gcesim.calibration import load_calibrated  # noqa: E402
from gcesim.model import run_model  # noqa: E402

if __name__ == "__main__":
    res = calibrate(out=ROOT / "results" / "calibration.json", workers=-1)
    print(res)
    p = load_calibrated()
    df = run_model(p, seed=3, end=2025)  # seed 3 not used in fitting
    fs = fit_stats(df, target_table())
    fs.to_csv(ROOT / "results" / "calibration_fit.csv", index=False)
    print(fs.round(3).to_string())
