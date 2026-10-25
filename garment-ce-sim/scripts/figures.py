"""Publication figures from results/ (run after calibrate, run_scenarios, displacement, sensitivity)."""
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402

from _common import FIG, OUTCOMES, RESULTS  # noqa: E402
from gcesim.calibration import load_calibrated  # noqa: E402
from gcesim.data import Observed  # noqa: E402
from gcesim.model import run_model  # noqa: E402

plt.rcParams.update({"font.size": 9, "axes.spines.top": False, "axes.spines.right": False, "figure.dpi": 150})
PALETTE = ["#2a6f97", "#c44536", "#5b8e7d", "#e09f3e", "#7b5ea7", "#333333"]


def fig_calibration():
    obs = Observed()
    p = load_calibrated()
    runs = [run_model(p, seed=s, end=2025) for s in range(300, 320)]
    fig, axes = plt.subplots(1, 3, figsize=(10, 3))
    for ax, (var, col, title) in zip(axes, [("collected", "collected", "Collected (kt)"),
                                             ("sorted", "sorted", "Sorted (kt)"),
                                             ("placed_on_market", "new_kt", "Placed on market (kt)")]):
        arr = np.array([r[col].values for r in runs])
        yrs = runs[0].index.values
        ax.fill_between(yrs, np.quantile(arr, 0.05, 0), np.quantile(arr, 0.95, 0), color=PALETTE[0], alpha=0.25, lw=0)
        ax.plot(yrs, arr.mean(0), color=PALETTE[0], label="model (mean, 90% band)")
        d = obs.df[obs.df.variable == var]
        for use, mk, c in [("target", "o", "k"), ("target_lowweight", "o", "0.6"), ("forcing", "s", "k"),
                           ("validation", "D", PALETTE[1])]:
            dd = d[d.use == use]
            if len(dd):
                ax.scatter(dd.year, dd.value, marker=mk, s=18, color=c, zorder=3,
                           label={"validation": "2025 hold-out"}.get(use, "observed"))
        ax.set_title(title)
    h, l = axes[0].get_legend_handles_labels()
    uniq = dict(zip(l, h))
    fig.legend(uniq.values(), uniq.keys(), loc="lower center", ncol=4, frameon=False)
    fig.tight_layout(rect=(0, 0.08, 1, 1))
    fig.savefig(FIG / "fig1_calibration.png")


def fig_scenarios():
    runs = pd.read_csv(RESULTS / "scenario_runs.csv.gz")
    runs = runs[(runs.stress == "none") & (runs.year >= 2015)]
    outs = ["new_kt", "collected", "overflow_total", "virgin_fibre_kt", "ghg_total", "f2f_output"]
    fig, axes = plt.subplots(2, 3, figsize=(11, 6))
    for ax, out in zip(axes.flat, outs):
        for i, (sc, g) in enumerate(runs.groupby("scenario")):
            m = g.groupby("year")[out].mean()
            lo = g.groupby("year")[out].quantile(0.05)
            hi = g.groupby("year")[out].quantile(0.95)
            ax.plot(m.index, m.values, color=PALETTE[i], label=sc, lw=1.3)
            ax.fill_between(m.index, lo, hi, color=PALETTE[i], alpha=0.12, lw=0)
        ax.axvline(2024.5, color="0.7", ls=":", lw=0.8)
        ax.set_title(OUTCOMES[out])
    axes[0, 0].legend(frameon=False, fontsize=7)
    fig.tight_layout()
    fig.savefig(FIG / "fig2_scenarios.png")


def fig_displacement():
    d = pd.read_csv(RESULTS / "displacement_grid.csv")
    tab = d.groupby(["rebound", "sh_life_frac"]).displacement.mean().unstack()
    fig, ax = plt.subplots(figsize=(4.5, 3.5))
    im = ax.imshow(tab.values, origin="lower", cmap="viridis", aspect="auto", vmin=0, vmax=1)
    ax.set_xticks(range(len(tab.columns)), [f"{c:.1f}" for c in tab.columns])
    ax.set_yticks(range(len(tab.index)), [f"{r:.2f}" for r in tab.index])
    for i in range(tab.shape[0]):
        for j in range(tab.shape[1]):
            ax.text(j, i, f"{tab.values[i, j]:.2f}", ha="center", va="center", color="w", fontsize=8)
    ax.set_xlabel("Residual life of second-hand item (share of new)")
    ax.set_ylabel("Rebound (share of savings re-spent)")
    ax.set_title("Emergent displacement rate, 2025-2040")
    fig.colorbar(im, ax=ax, shrink=0.8)
    fig.tight_layout()
    fig.savefig(FIG / "fig3_displacement.png")


def fig_sobol():
    s = pd.read_csv(RESULTS / "sobol_indices.csv")
    outs = s.output.unique()
    fig, axes = plt.subplots(1, len(outs), figsize=(3.2 * len(outs), 4), sharey=False)
    for ax, out in zip(np.atleast_1d(axes), outs):
        g = s[s.output == out].sort_values("ST").tail(10)
        ax.barh(g.param, g.ST, xerr=g.ST_conf, color=PALETTE[0], alpha=0.8, label="total (ST)")
        ax.barh(g.param, g.S1.clip(lower=0), color=PALETTE[1], alpha=0.8, label="first order (S1)")
        ax.set_title(out, fontsize=9)
    np.atleast_1d(axes)[0].legend(frameon=False, fontsize=7)
    fig.tight_layout()
    fig.savefig(FIG / "fig4_sobol.png")


if __name__ == "__main__":
    for f in (fig_calibration, fig_scenarios, fig_displacement, fig_sobol):
        try:
            f()
            print("ok", f.__name__)
        except FileNotFoundError as e:
            print("skip", f.__name__, e)
