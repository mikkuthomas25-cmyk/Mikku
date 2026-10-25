"""Observed French TLC data: forcings (historical drivers) and calibration targets."""
from __future__ import annotations

from pathlib import Path

import numpy as np
import pandas as pd

from .params import DATA_DIR


def load_observed(path: Path | None = None) -> pd.DataFrame:
    return pd.read_csv(path or DATA_DIR / "france_tlc_observed.csv")


class Observed:
    """Interpolated access to observed series.

    Series with several values in the same year (conflicting sources) are averaged
    over rows flagged for the requested use.
    """

    def __init__(self, df: pd.DataFrame | None = None, extra_points: dict | None = None):
        self.df = load_observed() if df is None else df
        self.extra = extra_points or {}

    def points(self, variable: str, uses=("forcing", "target", "target_lowweight")) -> pd.Series:
        d = self.df[(self.df.variable == variable) & (self.df.use.isin(uses))]
        s = d.groupby("year")["value"].mean()
        for yr, v in self.extra.get(variable, {}).items():
            s.loc[yr] = v
        return s.sort_index()

    def interp(self, variable: str, year: float, uses=("forcing", "target", "target_lowweight")) -> float:
        s = self.points(variable, uses)
        if s.empty:
            raise KeyError(variable)
        return float(np.interp(year, s.index.values, s.values))  # flat extrapolation at both ends

    def targets(self, uses=("target", "target_lowweight")) -> pd.DataFrame:
        d = self.df[self.df.use.isin(uses)]
        g = d.groupby(["variable", "year"]).agg(value=("value", "mean"), use=("use", "first")).reset_index()
        return g
