"""Parameter registry: values come from data/parameters.csv, never hard-coded."""
from __future__ import annotations

from pathlib import Path

import pandas as pd

DATA_DIR = Path(__file__).resolve().parents[2] / "data"


class Params(dict):
    """Dict with attribute access. Keys are parameter names from the registry."""

    def __getattr__(self, name):
        try:
            return self[name]
        except KeyError as e:
            raise AttributeError(name) from e

    def __setattr__(self, name, value):
        self[name] = value

    def copy(self) -> "Params":
        return Params(self)


def load_registry(path: Path | None = None) -> pd.DataFrame:
    return pd.read_csv(path or DATA_DIR / "parameters.csv")


def load_params(overrides: dict | None = None, path: Path | None = None) -> Params:
    reg = load_registry(path)
    p = Params()
    for row in reg.itertuples():
        v = float(row.value)
        p[row.name] = int(v) if row.role == "structural" or row.name.endswith("_start") else v
    for k, v in (overrides or {}).items():
        if k not in p:
            raise KeyError(f"Unknown parameter override: {k}")
        p[k] = v
    return p


def bounds(role: str | tuple = ("gsa", "calibrate"), path: Path | None = None) -> pd.DataFrame:
    """Parameters with a given role and their [low, high] ranges."""
    reg = load_registry(path)
    roles = (role,) if isinstance(role, str) else role
    return reg[reg.role.isin(roles)][["name", "value", "low", "high", "unit", "source_id"]].reset_index(drop=True)
