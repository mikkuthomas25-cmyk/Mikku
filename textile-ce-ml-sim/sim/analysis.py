"""Summarise experiment CSVs into tables (mean, 95% CI half-width) and paper figures."""
from __future__ import annotations

import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402
from scipy import stats  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
RES, FIG, TAB = ROOT / "results", ROOT / "results" / "figures", ROOT / "results" / "tables"

# Reference palette (dataviz skill, light mode)
CAT = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300", "#6250d6", "#e34948"]
INK, INK2, GRID = "#0b0b0b", "#52514e", "#e4e3df"
SPEC_LABEL = {"(0.95, 0.9, 0.95)": "cotton spec 95%", "(0.98, 0.9, 0.95)": "cotton spec 98%"}

plt.rcParams.update({
    "font.size": 9, "axes.edgecolor": INK2, "axes.labelcolor": INK, "xtick.color": INK2,
    "ytick.color": INK2, "axes.grid": True, "grid.color": GRID, "grid.linewidth": 0.6,
    "axes.spines.top": False, "axes.spines.right": False, "lines.linewidth": 2,
    "legend.frameon": False, "figure.dpi": 150, "savefig.bbox": "tight",
})


def seq(n, cmap="Blues", lo=0.35, hi=0.95):
    return [plt.get_cmap(cmap)(x) for x in np.linspace(lo, hi, n)]


def summarise(df, by, cols):
    g = df.groupby(by)[cols]
    m, s, n = g.mean(), g.std(ddof=1), g.count()
    hw = s * stats.t.ppf(0.975, n - 1) / np.sqrt(n)
    out = m.add_suffix("_mean").join(hw.add_suffix("_hw"))
    return out.reset_index()


KPIS = ["item_accuracy", "auto_accuracy", "coverage", "manual_overflow", "cotton_recovery",
        "COT_reject", "COT_purity", "PES_reject", "PC_reject", "f2f_share", "closed_loop_share",
        "residual_share", "manual_util"]


def fig_E1():
    df = pd.read_csv(RES / "E1_accuracy_vs_circularity.csv")
    t = summarise(df, ["cfg_spec", "cfg_sigma", "cfg_tau"], KPIS)
    t.to_csv(TAB / "E1_summary.csv", index=False)
    sigmas = sorted(t.cfg_sigma.unique())
    cols = seq(len(sigmas))
    fig, axes = plt.subplots(2, 2, figsize=(7.2, 5.4), sharex=True)
    for j, spec in enumerate(SPEC_LABEL):
        for c, sg in zip(cols, sigmas):
            d = t[(t.cfg_spec == spec) & (t.cfg_sigma == sg)].sort_values("cfg_tau")
            axes[0, j].plot(d.cfg_tau, d.auto_accuracy_mean * 100, color=c, marker="o", ms=3,
                            label=f"RMSEP {sg*100:.1f}%")
            axes[1, j].errorbar(d.cfg_tau, d.cotton_recovery_mean * 100, yerr=d.cotton_recovery_hw * 100,
                                color=c, marker="o", ms=3, capsize=2)
        axes[0, j].set_title(SPEC_LABEL[spec], color=INK)
    axes[0, 0].set_ylabel("Accuracy of auto-sorted items (%)")
    axes[1, 0].set_ylabel("Cotton recovered in accepted bales (%)")
    for a in axes[1]:
        a.set_xlabel("Confidence threshold τ")
    axes[0, 0].legend(fontsize=7, loc="lower right")
    fig.suptitle("Accuracy of auto-sorted items rises with τ; recovered cotton peaks at moderate τ, then collapses",
                 fontsize=9.5, color=INK, x=0.02, ha="left")
    fig.savefig(FIG / "F1_accuracy_vs_recovery.png")
    plt.close(fig)
    return t


def fig_E2():
    df = pd.read_csv(RES / "E2_fallback_design.csv")
    t = summarise(df, ["cfg_spec", "cfg_manual_accuracy", "cfg_manual_servers", "cfg_tau"], KPIS)
    t.to_csv(TAB / "E2_summary.csv", index=False)
    accs = sorted(t.cfg_manual_accuracy.unique())
    servers = sorted(t.cfg_manual_servers.unique())
    cols = seq(len(servers), "Oranges", 0.3, 0.95)
    fig, axes = plt.subplots(2, len(accs), figsize=(8.4, 5.2), sharex=True, sharey="row")
    for i, spec in enumerate(SPEC_LABEL):
        for j, ma in enumerate(accs):
            ax = axes[i, j]
            for c, sv in zip(cols, servers):
                d = t[(t.cfg_spec == spec) & (t.cfg_manual_accuracy == ma) & (t.cfg_manual_servers == sv)]
                d = d.sort_values("cfg_tau")
                ax.plot(d.cfg_tau, d.cotton_recovery_mean * 100, color=c, marker="o", ms=2.5,
                        label=f"{sv} manual sorters" if sv else "no manual station")
            if i == 0:
                ax.set_title(f"manual accuracy {ma:.0%}", color=INK)
            if j == 0:
                ax.set_ylabel(f"{SPEC_LABEL[spec]}\ncotton recovered (%)")
            if i == 1:
                ax.set_xlabel("Confidence threshold τ")
    axes[0, 0].legend(fontsize=6.5, loc="lower left")
    fig.savefig(FIG / "F2_fallback_design.png")
    plt.close(fig)
    return t


def fig_E3():
    df = pd.read_csv(RES / "E3_bale_spec.csv")
    df["cot_spec"] = df.cfg_spec.str.strip("()").str.split(",").str[0].astype(float)
    t = summarise(df, ["cfg_bale_kg", "cot_spec"], KPIS)
    t.to_csv(TAB / "E3_summary.csv", index=False)
    sizes = sorted(t.cfg_bale_kg.unique())
    cols = seq(len(sizes), "Greens", 0.35, 0.95)
    fig, axes = plt.subplots(1, 2, figsize=(7.2, 2.9))
    for c, b in zip(cols, sizes):
        d = t[t.cfg_bale_kg == b].sort_values("cot_spec")
        axes[0].errorbar(d.cot_spec * 100, d.COT_reject_mean * 100, yerr=d.COT_reject_hw * 100, color=c,
                         marker="o", ms=3, capsize=2, label=f"{b:.0f} kg bales")
        axes[1].plot(d.cot_spec * 100, d.cotton_recovery_mean * 100, color=c, marker="o", ms=3)
    axes[0].set_ylabel("Cotton bales rejected (%)")
    axes[1].set_ylabel("Cotton recovered (%)")
    for a in axes:
        a.set_xlabel("Recycler cotton specification (%)")
    axes[0].legend(fontsize=7)
    fig.savefig(FIG / "F3_bale_spec.png")
    plt.close(fig)
    # bale purity distribution at baseline
    return t


def fig_E4():
    df = pd.read_csv(RES / "E4_dpp.csv")
    t = summarise(df, ["cfg_spec", "cfg_sigma", "cfg_dpp_error", "cfg_dpp_share"], KPIS)
    t.to_csv(TAB / "E4_summary.csv", index=False)
    fig, axes = plt.subplots(1, 2, figsize=(7.2, 2.9), sharey=True)
    combos = [(0.035, 0.02), (0.035, 0.10), (0.066, 0.02), (0.066, 0.10)]
    for j, spec in enumerate(SPEC_LABEL):
        for c, (sg, er) in zip(CAT, combos):
            d = t[(t.cfg_spec == spec) & (t.cfg_sigma == sg) & (t.cfg_dpp_error == er)].sort_values("cfg_dpp_share")
            axes[j].errorbar(d.cfg_dpp_share * 100, d.cotton_recovery_mean * 100, yerr=d.cotton_recovery_hw * 100,
                             color=c, marker="o", ms=3, capsize=2,
                             label=f"RMSEP {sg*100:.1f}%, DPP error {er:.0%}")
        axes[j].set_title(SPEC_LABEL[spec], color=INK)
        axes[j].set_xlabel("Items carrying a readable DPP (%)")
    axes[0].set_ylabel("Cotton recovered (%)")
    axes[0].legend(fontsize=6.5)
    fig.savefig(FIG / "F4_dpp.png")
    plt.close(fig)
    return t


def fig_E5():
    df = pd.read_csv(RES / "E5_drift.csv")
    t = summarise(df, ["cfg_spec", "policy", "week"], KPIS + ["kappa"])
    t.to_csv(TAB / "E5_summary.csv", index=False)
    # cumulative over the year: mass-weighted recovered cotton is proportional to mean weekly recovery
    tot = summarise(df.groupby(["cfg_spec", "policy", "rep"])[["cotton_recovery", "f2f_share", "closed_loop_share"]]
                    .mean().reset_index(), ["cfg_spec", "policy"], ["cotton_recovery", "f2f_share", "closed_loop_share"])
    tot.to_csv(TAB / "E5_annual.csv", index=False)
    pols = list(dict.fromkeys(df.policy))
    fig, axes = plt.subplots(1, 2, figsize=(7.6, 3.0), sharey=True)
    for j, spec in enumerate(SPEC_LABEL):
        for c, pn in zip(CAT, pols):
            d = t[(t.cfg_spec == spec) & (t.policy == pn)].sort_values("week")
            axes[j].plot(d.week, d.cotton_recovery_mean * 100, color=c, lw=1.6, label=pn)
        axes[j].set_title(SPEC_LABEL[spec], color=INK)
        axes[j].set_xlabel("Week (novel-item share rises 0 → 40%)")
    axes[0].set_ylabel("Cotton recovered (%)")
    axes[1].legend(fontsize=6.5, loc="lower left")
    fig.savefig(FIG / "F5_drift.png")
    plt.close(fig)
    return t, tot


def fig_E6():
    df = pd.read_csv(RES / "E6_oat_sensitivity.csv")
    cfg_cols = [c for c in df.columns if c.startswith("cfg_") and c not in ("cfg_spec", "cfg_tau")]
    df["factor"] = "baseline"
    df["level"] = np.nan
    for c in cfg_cols:
        m = df[c].notna()
        df.loc[m, "factor"] = c[4:]
        df.loc[m, "level"] = df.loc[m, c]
    t = summarise(df, ["cfg_spec", "cfg_tau", "factor", "level"], ["cotton_recovery", "COT_reject", "f2f_share"])
    t.to_csv(TAB / "E6_summary.csv", index=False)
    fig, axes = plt.subplots(1, 2, figsize=(7.6, 3.6), sharey=True)
    for j, spec in enumerate(SPEC_LABEL):
        sub = t[(t.cfg_spec == spec) & (t.cfg_tau == 0.0)]
        base = sub[sub.factor == "baseline"].cotton_recovery_mean.iloc[0]
        rows = []
        for f, g in sub[sub.factor != "baseline"].groupby("factor"):
            g = g.sort_values("level")
            rows.append((f, g.cotton_recovery_mean.iloc[0] - base, g.cotton_recovery_mean.iloc[-1] - base,
                         g.level.iloc[0], g.level.iloc[-1]))
        rows.sort(key=lambda r: max(abs(r[1]), abs(r[2])))
        y = np.arange(len(rows))
        axes[j].barh(y, [r[1] * 100 for r in rows], color=CAT[0], height=0.6, label="low value")
        axes[j].barh(y, [r[2] * 100 for r in rows], color=CAT[1], height=0.6, label="high value")
        axes[j].set_yticks(y, [f"{r[0]} ({r[3]:g} / {r[4]:g})" for r in rows], fontsize=7)
        axes[j].axvline(0, color=INK2, lw=0.8)
        axes[j].set_title(f"{SPEC_LABEL[spec]} (base {base*100:.1f}%)", color=INK)
        axes[j].set_xlabel("Change in cotton recovered (pp)")
    axes[0].legend(fontsize=7, loc="lower left")
    fig.savefig(FIG / "F6_sensitivity.png")
    plt.close(fig)
    return t


if __name__ == "__main__":
    FIG.mkdir(parents=True, exist_ok=True)
    TAB.mkdir(parents=True, exist_ok=True)
    which = sys.argv[1:] or ["E1", "E2", "E3", "E4", "E5", "E6"]
    for e in which:
        if (RES / {"E1": "E1_accuracy_vs_circularity", "E2": "E2_fallback_design", "E3": "E3_bale_spec",
                   "E4": "E4_dpp", "E5": "E5_drift", "E6": "E6_oat_sensitivity"}[e]).with_suffix(".csv").exists():
            globals()[f"fig_{e}"]()
            print("done", e)


def paired_E1():
    """Paired (CRN) differences: recovery-optimal tau vs tau=0 and vs accuracy-optimal tau=0.99."""
    df = pd.read_csv(RES / "E1_accuracy_vs_circularity.csv")
    t = pd.read_csv(TAB / "E1_summary.csv")
    rows = []
    for (spec, sg), g in t.groupby(["cfg_spec", "cfg_sigma"]):
        best = g.loc[g.cotton_recovery_mean.idxmax(), "cfg_tau"]
        d = df[(df.cfg_spec == spec) & (df.cfg_sigma == sg)].pivot(index="seed", columns="cfg_tau",
                                                                    values="cotton_recovery")
        for ref in (0.0, 0.99):
            diff = d[best] - d[ref]
            hw = diff.std(ddof=1) * stats.t.ppf(0.975, len(diff) - 1) / np.sqrt(len(diff))
            rows.append(dict(spec=spec, sigma=sg, best_tau=best, ref_tau=ref,
                             diff_mean=diff.mean(), diff_hw=hw))
    out = pd.DataFrame(rows)
    out.to_csv(TAB / "E1_paired.csv", index=False)
    return out
