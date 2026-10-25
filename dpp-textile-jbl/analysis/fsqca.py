"""fsQCA engine and analysis for "Who Carries the Passport?".

Python implementation of the protocol in 04_online_appendix.md (Appendix B), so the
analysis runs without R. The R script (fsqca.R) is the cross-check for the final paper.

Usage:
    python3 fsqca.py                                   # uses ../data/SIMULATED_responses.csv
    python3 fsqca.py path/to/real_responses.csv        # same column layout as the questionnaire

Writes ../results/fsqca_results.md and ../results/calibrated_sets.csv.
"""
import itertools
import math
import os
import sys

import pandas as pd

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")

# ---- Calibration anchors (Online Appendix A.1): (full out, crossover, full in) -----
ANCHORS = {
    "DPR": (1, 3.5, 6), "CSC": (0, 2.5, 4), "DTC": (1, 2.5, 4), "VI": (1, 3.5, 5),
    "MDS": (10, 50, 90), "BTP": (2, 4, 6), "DAC": (2, 4, 6), "CERT": (0, 1.5, 3),
    # Formal circularity: recycled inputs, design-for-recycling, take-back (CSC_2, CSC_3, CSC_5)
    "CSCF": (0, 1.5, 3),
}


def build_raw(d):
    yes = lambda cols: (d[cols] == "Y").sum(axis=1)
    return pd.DataFrame({
        "firm": d["firm"],
        "VI": yes([f"VI_{i}" for i in range(1, 7)]),
        "DTC": yes([f"DTC_{i}" for i in range(1, 6)]),
        "MDS": 100 * d[["MDS_1", "MDS_2", "MDS_3"]].mean(axis=1) / d["MDS_N"],
        "BTP": d[["BTP_1", "BTP_2", "BTP_3"]].mean(axis=1),
        "DAC": d[["DAC_1", "DAC_2", "DAC_3"]].mean(axis=1),
        "DPR": yes([f"DPR_{i}" for i in range(1, 8)]),
        "CSC": yes([f"CSC_{i}" for i in range(1, 6)]),
        "CSCF": yes(["CSC_2", "CSC_3", "CSC_5"]),
        "CERT": d["CERT"],
        "TIER": d["B3"],
        "cluster": d["B5"],
        "archetype": d.get("archetype", pd.Series([""] * len(d))),
    })


def cronbach_alpha(items):
    k = items.shape[1]
    return k / (k - 1) * (1 - items.var(axis=0, ddof=1).sum() / items.sum(axis=1).var(ddof=1))


def calibrate(x, e, c, i):
    """Ragin's direct method: log-odds of +3 at full membership, -3 at full non-membership."""
    def one(v):
        lo = 3 * (v - c) / (i - c) if v >= c else 3 * (v - c) / (c - e)
        m = math.exp(lo) / (1 + math.exp(lo))
        return 0.501 if abs(m - 0.5) < 1e-9 else round(m, 3)
    return x.astype(float).map(one)


def calibrate_all(raw, shift=0.0):
    fs = pd.DataFrame(index=raw["firm"])
    for v, (e, c, i) in ANCHORS.items():
        s = shift * (i - e)
        fs[v] = calibrate(raw[v], e + s, c + s, i + s).values
    return fs


# ---- Set-theoretic measures ----------------------------------------------------------
def lit(fs, name):
    return 1 - fs[name[1:]] if name.startswith("~") else fs[name]


def term_membership(fs, term):
    """term: tuple of literals, e.g. ('VI', 'DTC', '~DAC')."""
    m = pd.Series(1.0, index=fs.index)
    for t in term:
        m = pd.concat([m, lit(fs, t)], axis=1).min(axis=1)
    return m


def consistency(x, y):
    return (pd.concat([x, y], axis=1).min(axis=1).sum() / x.sum()) if x.sum() else float("nan")


def coverage(x, y):
    return pd.concat([x, y], axis=1).min(axis=1).sum() / y.sum()


def pri(x, y):
    xy = pd.concat([x, y], axis=1).min(axis=1)
    xyny = pd.concat([x, y, 1 - y], axis=1).min(axis=1)
    den = x.sum() - xyny.sum()
    return (xy.sum() - xyny.sum()) / den if den else float("nan")


def necessity(fs, conds, outcome):
    y = lit(fs, outcome)
    rows = []
    for c in conds + ["~" + c for c in conds]:
        x = lit(fs, c)
        xy = pd.concat([x, y], axis=1).min(axis=1)
        cons = xy.sum() / y.sum()
        cov = xy.sum() / x.sum()
        ron = (1 - x).sum() / (1 - xy).sum()
        rows.append((c, round(cons, 3), round(cov, 3), round(ron, 3)))
    return pd.DataFrame(rows, columns=["condition", "consistency", "coverage", "RoN"])


# ---- Truth table -------------------------------------------------------------------------
def truth_table(fs, conds, outcome, incl=0.80, pri_cut=0.70, ncut=1):
    y = lit(fs, outcome)
    rows = []
    for bits in itertools.product([1, 0], repeat=len(conds)):
        term = tuple(c if b else "~" + c for c, b in zip(conds, bits))
        m = term_membership(fs, term)
        cases = list(m[m > 0.5].index)
        n = len(cases)
        inc = consistency(m, y)
        p = pri(m, y)
        if n >= ncut:
            out = 1 if (inc >= incl and p >= pri_cut) else 0
        else:
            out = None  # logical remainder
        rows.append(dict(bits=bits, n=n, incl=inc, PRI=p, OUT=out, cases=cases))
    return rows


# ---- Quine-McCluskey -----------------------------------------------------------------------
def _combine(a, b):
    diff = [i for i, (x, y) in enumerate(zip(a, b)) if x != y]
    if len(diff) == 1 and "-" not in (a[diff[0]], b[diff[0]]):
        return a[:diff[0]] + ("-",) + a[diff[0] + 1:]
    return None


def prime_implicants(minterms, dontcares):
    terms = set(tuple(map(str, t)) for t in minterms + dontcares)
    primes = set()
    while terms:
        used, nxt = set(), set()
        tl = sorted(terms)
        for a, b in itertools.combinations(tl, 2):
            c = _combine(a, b)
            if c:
                nxt.add(c)
                used.update([a, b])
        primes |= terms - used
        terms = nxt
    return primes


def covers(imp, mt):
    return all(i == "-" or i == str(m) for i, m in zip(imp, mt))


def minimal_cover(primes, minterms):
    """All minimum-size sets of prime implicants covering the minterms (exact, small problems)."""
    primes = [p for p in primes if any(covers(p, m) for m in minterms)]
    for k in range(1, len(primes) + 1):
        sols = [s for s in itertools.combinations(sorted(primes), k)
                if all(any(covers(p, m) for p in s) for m in minterms)]
        if sols:
            # tie-break: fewest literals
            best = min(sum(c != "-" for p in s for c in p) for s in sols)
            return [s for s in sols if sum(c != "-" for p in s for c in p) == best]
    return [()]


def imp_to_term(imp, conds):
    return tuple(c if v == "1" else "~" + c for c, v in zip(conds, imp) if v != "-")


def minimise(tt, conds, include_remainders=False, remainder_filter=None):
    pos = [r["bits"] for r in tt if r["OUT"] == 1]
    if not pos:
        return []
    dc = []
    if include_remainders:
        dc = [r["bits"] for r in tt if r["OUT"] is None and (remainder_filter is None or remainder_filter(r["bits"]))]
    primes = prime_implicants(pos, dc)
    sols = minimal_cover(primes, pos)
    return [[imp_to_term(p, conds) for p in s] for s in sols]


def intermediate(tt, conds, dir_exp):
    """Intermediate solution using easy counterfactuals only.

    dir_exp maps condition -> 1 (presence expected to contribute), 0 (absence expected), None.
    A remainder may be used if, for every condition where it differs from some positive row
    it is being merged with, the remainder's value matches the directional expectation.
    Implemented the standard way: a complex term's literal is dropped when it is not in the
    corresponding parsimonious term and its value runs against the directional expectation.
    """
    complex_sol = minimise(tt, conds)
    pars_sol = minimise(tt, conds, include_remainders=True)
    if not complex_sol:
        return [], [], []
    comp, pars = complex_sol[0], pars_sol[0]
    out = []
    for term in comp:
        # parsimonious implicant(s) that this complex term is a subset of
        sup = [p for p in pars if all(l in term for l in p)]
        pterm = sup[0] if sup else ()
        new = []
        for l in term:
            name = l.lstrip("~")
            val = 0 if l.startswith("~") else 1
            exp = dir_exp.get(name)
            if l in pterm or exp is None or exp == val:
                new.append(l)
        new = tuple(new)
        if new not in out:
            out.append(new)
    # remove terms subsumed by more general terms
    out = [t for t in out if not any(o != t and set(o) <= set(t) for o in out)]
    return comp, pars, out


def describe_solution(fs, terms, outcome, pars_terms):
    y = lit(fs, outcome)
    rows = []
    mems = [term_membership(fs, t) for t in terms]
    for t, m in zip(terms, mems):
        others = [mm for tt, mm in zip(terms, mems) if tt != t]
        other_max = pd.concat(others, axis=1).max(axis=1) if others else pd.Series(0.0, index=fs.index)
        raw_cov = coverage(m, y)
        uniq = raw_cov - coverage(other_max, y) if others else raw_cov
        if others:
            sol_all = pd.concat(mems, axis=1).max(axis=1)
            uniq = coverage(sol_all, y) - coverage(other_max, y)
        # core = literals of the parsimonious term(s) contained in this configuration (Fiss, 2011)
        subsuming = [p for p in pars_terms if set(p) <= set(t)]
        core = [l for l in t if any(l in p for p in subsuming)]
        cases = list(m[(m > 0.5) & (y > 0.5)].index)
        deviant = list(m[(m > 0.5) & (y < 0.5)].index)
        rows.append(dict(term=" * ".join(t), core=core, consistency=round(consistency(m, y), 3),
                         raw_coverage=round(raw_cov, 3), unique_coverage=round(uniq, 3),
                         typical_cases=cases, deviant_cases=deviant))
    sol = pd.concat(mems, axis=1).max(axis=1) if mems else pd.Series(0.0, index=fs.index)
    return rows, round(consistency(sol, y), 3), round(coverage(sol, y), 3)


DIR_HIGH = {"VI": 1, "DTC": 1, "MDS": 1, "BTP": 1, "DAC": 0, "CERT": 1}
DIR_LOW = {"VI": 0, "DTC": 0, "MDS": 0, "BTP": 1, "DAC": 1, "CERT": 0}
DIR_CSC = {"DPR": 1, "VI": 1, "MDS": 1, "BTP": 1, "CERT": 1}


def run(fs, conds, outcome, dir_exp, incl=0.80, pri_cut=0.70, ncut=1):
    tt = truth_table(fs, conds, outcome, incl, pri_cut, ncut)
    comp, pars, inter = intermediate(tt, conds, dir_exp)
    desc, scons, scov = describe_solution(fs, inter, outcome, pars) if inter else ([], None, None)
    return dict(tt=tt, complex=comp, pars=pars, inter=inter, desc=desc, sol_cons=scons, sol_cov=scov)


def fmt_tt(tt, conds):
    lines = ["| " + " | ".join(conds) + " | n | consistency | PRI | OUT | cases |",
             "|" + "---|" * (len(conds) + 5)]
    for r in sorted(tt, key=lambda r: (-(r["n"] > 0), -(r["incl"] if r["n"] else 0))):
        if r["n"] == 0:
            continue
        out = "–" if r["OUT"] is None else str(r["OUT"])
        lines.append("| " + " | ".join(map(str, r["bits"])) +
                     f" | {r['n']} | {r['incl']:.3f} | {r['PRI']:.3f} | {out} | {', '.join(r['cases'])} |")
    return "\n".join(lines)


def fmt_solution(res, title):
    s = [f"**{title}**", ""]
    if not res["inter"]:
        return "\n".join(s + ["No configuration passed the consistency thresholds.", ""])
    s += ["| Configuration | Core conditions | Consistency | Raw coverage | Unique coverage | Typical cases | Deviant cases |",
          "|---|---|---|---|---|---|---|"]
    for r in res["desc"]:
        s.append(f"| {r['term']} | {', '.join(r['core']) or '–'} | {r['consistency']} | {r['raw_coverage']} | "
                 f"{r['unique_coverage']} | {', '.join(r['typical_cases'])} | {', '.join(r['deviant_cases']) or '–'} |")
    s += ["", f"Solution consistency = {res['sol_cons']}; solution coverage = {res['sol_cov']}.",
          f"Complex solution: {' + '.join(' * '.join(t) for t in res['complex'])}",
          f"Parsimonious solution: {' + '.join(' * '.join(t) for t in res['pars'])}", ""]
    return "\n".join(s)


def main():
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "data", "SIMULATED_responses.csv")
    d = pd.read_csv(path)
    simulated = "SIMULATED" in d.columns and (d["SIMULATED"] == "YES").all()
    raw = build_raw(d)
    fs = calibrate_all(raw)
    os.makedirs(os.path.join(ROOT, "results"), exist_ok=True)
    out = pd.concat([raw.set_index("firm"), fs.add_suffix("_fs")], axis=1)
    out.to_csv(os.path.join(ROOT, "results", "calibrated_sets.csv"))

    conds = ["VI", "DTC", "MDS", "BTP", "DAC"]
    md = []
    if simulated:
        md += ["# fsQCA results: SIMULATED DATA (pipeline test, not findings)", "",
               "> **These results come from simulated responses (`data/SIMULATED_responses.csv`). "
               "They reflect the assumptions in `simulation/SIMULATION_ASSUMPTIONS.md` and must not be reported as evidence.**", ""]
    else:
        md += ["# fsQCA results", ""]

    md += [f"N = {len(fs)} firms. Source file: `{os.path.relpath(path, ROOT)}`.", "",
           "## 1. Descriptives and reliability", "",
           f"Cronbach's α: BTP = {cronbach_alpha(d[['BTP_1', 'BTP_2', 'BTP_3']]):.2f}; "
           f"DAC = {cronbach_alpha(d[['DAC_1', 'DAC_2', 'DAC_3']]):.2f}.", "",
           "| Set | Raw mean | Raw min–max | Anchors (out / cross / in) | Share of cases > 0.5 |", "|---|---|---|---|---|"]
    for v, (e, c, i) in ANCHORS.items():
        md.append(f"| {v} | {raw[v].mean():.2f} | {raw[v].min():.1f}–{raw[v].max():.1f} | {e} / {c} / {i} | {(fs[v] > 0.5).mean():.2f} |")

    md += ["", "## 2. Necessity analysis (threshold: consistency ≥ 0.90, RoN ≥ 0.60)", ""]
    for o in ["DPR", "~DPR"]:
        nec = necessity(fs, conds, o)
        md += [f"**Outcome: {o}**", "", "| Condition | Consistency | Coverage | RoN |", "|---|---|---|---|"]
        md += [f"| {r.condition} | {r.consistency} | {r.coverage} | {r.RoN} |" for r in nec.itertuples()]
        passed = nec[(nec.consistency >= 0.90) & (nec.RoN >= 0.60)].condition.tolist()
        md += ["", f"Necessary conditions: {', '.join(passed) if passed else 'none'}.", ""]

    high = run(fs, conds, "DPR", DIR_HIGH)
    low = run(fs, conds, "~DPR", DIR_LOW)
    md += ["## 3. Sufficiency: high DPP readiness", "",
           "Thresholds: raw consistency ≥ 0.80, PRI ≥ 0.70, frequency ≥ 1. Rows with cases only (remainders omitted).", "",
           fmt_tt(high["tt"], conds), "",
           f"Rows with ≥ 1 case: {sum(r['n'] >= 1 for r in high['tt'])} of 32.", "",
           fmt_solution(high, "Intermediate solution for high readiness (DPR)"),
           "## 4. Sufficiency: low DPP readiness (~DPR)", "",
           fmt_tt(low["tt"], conds), "",
           fmt_solution(low, "Intermediate solution for low readiness (~DPR)")]

    # P4: readiness and circularity
    dpr_csc = consistency(fs["DPR"], fs["CSC"])
    csc_conds = ["DPR", "VI", "MDS", "BTP", "CERT"]
    circ = run(fs, csc_conds, "CSC", DIR_CSC)
    nec_csc = necessity(fs, csc_conds, "CSC")
    md += ["## 5. Readiness and circularity (P4)", "",
           f"Sufficiency of DPR alone for CSC: consistency = {dpr_csc:.3f}, coverage = {coverage(fs['DPR'], fs['CSC']):.3f} "
           f"(P4 expects consistency < 0.80).", "",
           "| Condition | Consistency | Coverage | RoN |", "|---|---|---|---|"]
    md += [f"| {r.condition} | {r.consistency} | {r.coverage} | {r.RoN} |" for r in nec_csc.itertuples()]
    md += ["", fmt_tt(circ["tt"], csc_conds), "", fmt_solution(circ, "Intermediate solution for circular capability (CSC)")]

    # Formal circularity only (excludes waste sale and surplus resale, which need no data)
    circf = run(fs, csc_conds, "CSCF", DIR_CSC)
    md += ["### 5b. Formal circularity (CSC_2, CSC_3, CSC_5 only)", "",
           f"Sufficiency of DPR alone for formal circularity: consistency = {consistency(fs['DPR'], fs['CSCF']):.3f}, "
           f"coverage = {coverage(fs['DPR'], fs['CSCF']):.3f}.", "",
           fmt_tt(circf["tt"], csc_conds), "", fmt_solution(circf, "Intermediate solution for formal circularity (CSCF)")]

    # Robustness
    variants = {
        "Base": run(fs, conds, "DPR", DIR_HIGH),
        "R1 anchors +10%": run(calibrate_all(raw, 0.10), conds, "DPR", DIR_HIGH),
        "R1 anchors −10%": run(calibrate_all(raw, -0.10), conds, "DPR", DIR_HIGH),
        "R2 consistency 0.75": run(fs, conds, "DPR", DIR_HIGH, incl=0.75),
        "R2 consistency 0.85": run(fs, conds, "DPR", DIR_HIGH, incl=0.85),
        "R3 frequency 2": run(fs, conds, "DPR", DIR_HIGH, ncut=2),
        "R4 CERT replaces BTP": run(fs, ["VI", "DTC", "MDS", "CERT", "DAC"], "DPR", DIR_HIGH),
        "R5 tiers 1–2 only": run(fs[raw.set_index("firm")["TIER"] <= 2], conds, "DPR", DIR_HIGH),
    }
    md += ["## 6. Robustness (high readiness)", "",
           "| Variant | Intermediate solution | Consistency | Coverage |", "|---|---|---|---|"]
    for k, r in variants.items():
        sol = " + ".join(" * ".join(t) for t in r["inter"]) or "none"
        md.append(f"| {k} | {sol} | {r['sol_cons']} | {r['sol_cov']} |")

    with open(os.path.join(ROOT, "results", "fsqca_results.md"), "w", encoding="utf-8") as f:
        f.write("\n".join(md) + "\n")
    print("\n".join(md))


if __name__ == "__main__":
    main()
