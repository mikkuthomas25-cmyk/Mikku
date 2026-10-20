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
| 1 | Toolchain: WSL2, Python, Rust, WareMax build, Ollama | in progress |
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

## 2026-10-20 (Tue) — Week 1, Session 1 · Linux environment on Windows
**Who:** You (setup), Claude (plan)   **Time spent:** — (fill in)
**Goal:** WSL2 + Ubuntu 24.04 that sees the GPU; Python, git, Rust installed.
**Steps:** see Claude's Session 1 instructions (Steps 1–8).
**Done:**
- [ ] `nvidia-smi` works in Windows PowerShell
- [ ] WSL2 + Ubuntu 24.04 installed (`wsl -l -v` shows VERSION 2)
- [ ] `nvidia-smi` works **inside Ubuntu**
- [ ] build tools, git, Python 3.12 installed
- [ ] Rust installed (`cargo --version`)
- [ ] git configured; `~/research` created
- [ ] Learning-log answers written

**Decisions:**
- **Work inside WSL2 (Ubuntu) rather than native Windows.** WareMax supports
  Windows but its docs list Linux as recommended. On Windows, Rust also needs
  the multi-GB Visual Studio Build Tools, so native Windows isn't less to
  install. Linux gives one consistent environment for the simulator, the local
  LLM and Python, which also makes the paper's reproducibility package simpler:
  reviewers can rerun it on Linux. WSL2 runs inside Windows; it doesn't
  replace or dual-boot Windows, and can be removed with
  `wsl --unregister Ubuntu-24.04`.
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
