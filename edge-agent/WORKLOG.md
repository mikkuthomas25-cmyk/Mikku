# Worklog

Running, dated record of the project. **Newest entry at the top.** One entry per
work session. Write it in the last 15–20 minutes of each session, even if the
session went badly — failed attempts are useful history.

**Entry template**

```
## YYYY-MM-DD (Day) — Week N, Session M · <short title>
**Who:** You / Claude / both   **Time spent:** h
**Goal:**
**Done:**
- 
**Decisions:** (what we chose and why)
**Problems / errors:** (paste the key error line, and how it was fixed)
**Results / numbers:**
**Next session:**
```

**Status board** (update when a task finishes)

| Week | Focus | Status |
|------|-------|--------|
| 1 | Toolchain (Windows): Python, Git, Build Tools, Rust, WareMax build, Ollama | in progress |
| 2 | WareMax baselines | not started |
| 3 | Edge latency · Gate 1 | not started |
| 4 | Disruptions + metrics | not started |
| 5–6 | Simulator extensions | not started |
| 7 | Scenario suite + rule baseline · Gate 2 | not started |
| 8 | Agent v0 | not started |
| 9 | Hybrid agent · Gate 3 | not started |
| 10 | Holiday buffer | not started |
| 11 | Memory (stretch) / catch-up | not started |
| 12–13 | Full experiments | not started |
| 14 | Analysis · Gate 4 | not started |
| 15–16 | Writing | not started |
| 17 | Buffer + submit (by 12 Feb) | not started |

---

## 2026-10-20 (Tue) — Week 1, Session 1 · Windows toolchain
**Who:** You (setup), Claude (plan)   **Time spent:** — (fill in)
**Goal:** native Windows toolchain: NVIDIA driver, Python, Git, C++ build tools,
Rust, a working Python virtual environment.
**Steps:** see Claude's Windows Session 1 instructions (Steps 1–9).
**Done:**
- [ ] `nvidia-smi` works in PowerShell
- [ ] Python 3.12 installed (`python --version`)
- [ ] Git installed (`git --version`); name/email configured
- [ ] Visual Studio Build Tools (C++ workload) installed
- [ ] Rust installed (`rustc --version`, `cargo --version`)
- [ ] Project repo cloned into `C:\Users\Midhu\Desktop\Warehouse`; test venv activates
- [ ] Learning-log answers written

**Decisions:**
- **Native Windows instead of WSL2/Ubuntu.** You're more comfortable on
  Windows, and WareMax officially supports it, so we lose no capability. The
  cost is some extra setup (Visual Studio Build Tools for Rust, PowerShell
  execution policy) and translating Linux-style commands in the WareMax docs.
  Claude provides PowerShell versions of every command. The reproducibility
  package will be tested on Linux later in CI or a container, not on your laptop.
- **Project folder: `C:\Users\Midhu\Desktop\Warehouse`** (your choice).
  Layout: `Warehouse\Mikku` (this repo, docs + our code in `edge-agent\`),
  `Warehouse\waremax` (simulator, pinned commit), `Warehouse\.venv` (Python).
  Caveat: if OneDrive "Desktop backup" is ever turned on, it can lock files
  mid-build. If a Rust build fails with "access denied" / "file in use", pause
  OneDrive sync first.
- **Sync rule:** Claude works in the cloud copy of the repo and can't write to
  the laptop directly. Files move through GitHub: `git pull` at the start of
  every session, `git add` / `commit` / `push` at the end.
- Effort confirmed at 2 h/day (~14 h/week). Memory (Week 11) and 7–8B ablation
  moved to stretch goals.

**Problems / errors:**
**Results / numbers:** (paste `nvidia-smi` driver + CUDA version, Python version, cargo version)
**Next session:** clone WareMax at `abaa4fd`, build the CLI, run
`waremax run --preset standard`.

---

## 2026-10-19 (Mon) — Week 0 · Planning
**Who:** both
**Done:**
- Chose Idea 1 (edge SLM agent for exception recovery) for the IEEE T-ASE
  special issue (deadline 15 Feb 2027).
- Verified WareMax: Rust core + Python Gym bindings, MIT licence. Has random
  robot failures (MTBF), battery/charging, maintenance, deadlock detection,
  and the standard dispatch heuristics. **Missing:** failures at a chosen
  time, mid-run blockages, and recovery actions beyond task assignment.
  These become the Week 5–6 extensions.
- Wrote `WORKPLAN.md` (17 weeks, 4 go/no-go gates) and `LEARNING_LOG.md`.

**Decisions:**
- The LLM chooses from simulator-validated candidate actions with
  JSON-constrained output, not free-text plans. This is needed for latency on
  a 4 GB GPU and prevents invalid actions.
- The GTX 1650 Ti (4 GB) laptop is the edge device in the paper. Main model
  class: 3B, 4-bit quantised.
- Paper claim framed as hybrid ≥ best heuristic, better on compound disruptions,
  within a latency budget. It doesn't claim the LLM beats optimisation.
