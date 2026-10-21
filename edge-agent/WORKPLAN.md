# Workplan — Edge-Deployable SLM Agent for Real-Time Exception Recovery

**Target:** IEEE T-ASE Special Issue *Agentic Intelligence for Materials Handling,
Warehousing, and Logistics 5.0* — submission deadline **15 Feb 2027**.
**Plan window:** Mon 19 Oct 2026 → Fri 12 Feb 2027 (17 weeks, submit 3 days early).
**Simulator:** [WareMax](https://github.com/Skelf-Research/waremax) (Rust core, Python Gym bindings, MIT).
**Hardware:** laptop with NVIDIA GTX 1650 Ti (4 GB VRAM) — this *is* the edge device in the paper.

---

## 1. What the paper will claim

> A hybrid agent — a quantised small language model that chooses among
> simulator-validated recovery actions under a hard latency budget, with a
> rule-based fallback — recovers a robotic warehouse from disruptions
> **at least as well as the best heuristic on simple exceptions and better on
> compound ones**, while running on a 4 GB consumer GPU within a bounded
> decision latency.

We do **not** claim "the LLM beats optimisation". On a single robot failure a
good rule is hard to beat. The LLM is there for compound and unusual situations;
the fallback is there so it can never make things much worse. This framing
survives the reviewer question *"why an LLM at all?"*.

### Research questions and testable hypotheses

| RQ | Question | Hypothesis we test |
|----|----------|--------------------|
| RQ1 | Is the hybrid agent ever worse than the best heuristic? | **H1 (non-inferiority):** across all scenarios, hybrid throughput loss is not significantly worse than the best heuristic. |
| RQ2 | Where does the SLM add value? | **H2:** on compound disruptions, hybrid recovers faster (lower recovery time) than the best heuristic. |
| RQ3 | Is it feasible on edge hardware? | **H3:** ≥ 95% of decisions finish within the latency budget on the 4 GB GPU. |
| RQ4 | How does it degrade? | **H4:** as LLM latency/failure rate increases, hybrid performance falls smoothly to the heuristic level, never below it. |

---

## 2. How each week works (so you learn while building)

Every week below has four parts:

- **Learn** — only the concepts needed this week, with a resource.
- **Build** — concrete tasks. *You type and run every command on your laptop.*
- **Done when** — a checkable outcome. We don't move on until it's met.
- **Explain back** — 2–3 questions you answer in your own words in
  `edge-agent/LEARNING_LOG.md`. If you can't answer them, we revisit before moving on.

**Division of work**

| You | Claude |
|-----|--------|
| Run everything locally (GPU timings must come from your laptop — my cloud container has no GPU) | Writes code skeletons, Rust patches, and analysis scripts; explains every file line by line on request |
| Write the Python glue code yourself from skeletons (agent loop, runner, metrics) | Reviews your code, debugs errors you paste |
| Keep the learning log and a results notebook | Checks results for statistical/experimental mistakes |
| Make the go/no-go calls at each gate (with my recommendation) | Drafts paper sections for you to rewrite in your voice |

**Effort:** 2 hours/day, ~14 hours/week (confirmed 20 Oct 2026). This is slightly
below what the full plan needs, so two items are now **stretch goals** that we do
only if we're ahead of schedule: memory/reflection (Week 11) and the 7–8B model
ablation. Everything else stays.

**Daily rhythm (2-hour session):** 10 min — read today's step in `WORKLOG.md`.
90 min — build/run. 20 min — write the session entry in `WORKLOG.md` and, at
week's end, the explain-back answers in `LEARNING_LOG.md`.

**Weekly rhythm:** Monday — read the week's plan and ask questions.
Friday — push code, paste results to Claude for review.

**Two logs, two purposes**
- `WORKLOG.md` — running, dated record of *what was done* each session, decisions
  made, problems hit, and the next step. This becomes the project's history and
  makes the paper's methods section easy to write.
- `LEARNING_LOG.md` — *what you understood*, in your own words, once per week.

---

## 3. Hardware reality check (GTX 1650 Ti, 4 GB)

| Model (Q4_K_M GGUF) | Approx. size | Fits fully in 4 GB? | Role in the paper |
|---------------------|-------------:|:-------------------:|-------------------|
| Qwen2.5-1.5B-Instruct | ~1.0 GB | yes | smallest model, ablation |
| Llama 3.2 3B Instruct | ~2.0 GB | yes | **main model** |
| Qwen2.5-3B-Instruct | ~1.9 GB | yes | main-model alternative |
| Phi-4-mini (3.8B) | ~2.5 GB | yes (short context) | ablation |
| 7–8B class | ~4.5–5 GB | **no** — partial CPU offload, slow | "bigger model" comparison only |

These sizes are approximate. Week 3 measures the real numbers on your laptop.

**What this means for the design**

- Decision time ≈ *prompt processing (prefill)* + *output generation*. On this
  GPU, prefill of a long prompt dominates, so **prompts must be short** (a compact
  state summary, not a dump of the whole warehouse).
- The model **chooses from a list of valid candidate actions** and answers in
  JSON enforced by a grammar. Output is a handful of tokens, never free text.
  This makes it fast and means it can't propose an impossible action.
- The latency budget gets set from Week 3 measurements. 200–500 ms may be
  achievable only with short prompts; we report whatever is true.

---

## 4. Timeline at a glance

| Phase | Weeks | Dates | Outcome | Gate |
|-------|-------|-------|---------|------|
| A. Setup & baselines | 1–2 | 19 Oct – 1 Nov | WareMax + local LLM running; normal-operation baselines | — |
| B. Edge feasibility | 3 | 2 – 8 Nov | Latency/VRAM/power table for 3–4 models | **Gate 1** |
| C. Disruptions & simulator extensions | 4–7 | 9 Nov – 6 Dec | Scenario suite S1–S6, new actions, strong rule-based baseline | **Gate 2** |
| D. Agent | 8–9 | 7 – 20 Dec | Agent v0 → hybrid agent (budget, validation, fallback) | **Gate 3** |
| — Holiday buffer | 10 | 21 – 27 Dec | Light: literature notes, catch-up | — |
| E. Memory & full experiments | 11–13 | 28 Dec – 17 Jan | Memory; full matrix with 20 seeds; ablations | — |
| F. Analysis | 14 | 18 – 24 Jan | Statistics, figures, tables | **Gate 4** |
| G. Writing | 15–16 | 25 Jan – 7 Feb | Full draft in T-ASE format | — |
| H. Buffer & submit | 17 | 8 – 12 Feb | Final checks, submit by Fri 12 Feb | — |

---

## 5. Week-by-week plan

### Week 1 (19–25 Oct) — Toolchain
**Learn:** virtual environments; basic git (clone, add, commit, push, pull); what a
*discrete-event simulation* is (time jumps from event to event).
*Resources:* [Python tutorial §12 (venv)](https://docs.python.org/3/tutorial/venv.html), [Pro Git ch. 2](https://git-scm.com/book/en/v2).

**Build**
1. Native Windows: update the NVIDIA driver; install Visual Studio Build Tools (C++ workload), which the Rust compiler needs on Windows.
2. Install Python 3.12, Git for Windows, and Rust (`rustup`, MSVC toolchain).
3. Clone WareMax, **pin the commit** (record the hash), build the CLI, run
   `waremax run --preset standard -o results/`.
4. Build the Python extension (`maturin develop ...`) and run `smoke_test.py`.
5. Install Ollama, run `llama3.2:3b`, confirm GPU use with `nvidia-smi`.

**Done when:** the WareMax preset runs, the Python smoke test passes, and the 3B model answers using the GPU.
**Explain back:** What is a seed and why must it be fixed? What does `maturin develop` actually do? Why is a discrete-event simulator fast compared with real time?

### Week 2 (26 Oct – 1 Nov) — Understand WareMax, normal-operation baselines
**Learn:** YAML; pandas basics (load CSV, group, mean); a 95% confidence interval.
*Read:* WareMax `docs/overview.md`, `simulation-model.md`, `scenario-config.md`, `dispatching-policies.md`.

**Build**
1. Write two scenario YAMLs: **small** (≈10 robots) and **medium** (≈30 robots).
2. Run all built-in dispatchers (nearest, least-busy, round-robin, auction, workload-balanced) × 10 seeds × 2 layouts.
3. Write `analysis/baseline_table.py` → table of throughput, utilisation, mean order delay with 95% CIs.

**Done when:** the baseline table exists and you can tell me which heuristic is best per layout, and why you think so.
**Explain back:** What is throughput vs. utilisation? Why 10 seeds rather than 1? Which event types does WareMax process?

### Week 3 (2–8 Nov) — Edge feasibility · **GATE 1**
**Learn:** tokens; quantisation (Q4/Q8); prefill vs. decode; KV-cache prefix reuse; JSON-constrained output.
*Resources:* [Ollama docs](https://github.com/ollama/ollama/tree/main/docs), [llama.cpp README](https://github.com/ggml-org/llama.cpp).

**Build:** `bench/latency_bench.py`. For each model in section 3 and prompt lengths {200, 500, 1000} tokens with a ~10-token JSON answer, record over 50 calls:
time to first token, total latency (median/p95), tokens/s, peak VRAM, GPU power (`nvidia-smi --query-gpu=power.draw`).

**Done when:** a latency table exists for all models.
**Gate 1:** Does a 3B model decide in ≤ ~1 s (p95) at a 500-token prompt?
- *Yes →* set the latency budget from the data and continue.
- *No →* shorten the state summary, move to the 1.5B model as main, or define the budget in simulated time. We decide together; the plan absorbs any of these.

**Explain back:** Why does a longer prompt cost more time than a longer answer here? What do you lose by quantising to 4 bits?

### Week 4 (9–15 Nov) — Disruptions with existing features + metrics
**Learn:** throughput time-series; how to define "recovered".

**Build**
1. Run existing features as disruptions: random failures (low `mtbf_hours`), battery pressure (small battery / few chargers), order surge (higher arrival rate).
2. Write `analysis/metrics.py` with **precise definitions** (agreed with me before coding):
   - *Throughput degradation* = 1 − (orders/h during disruption window ÷ pre-disruption orders/h).
   - *Recovery time* = time from disruption start until the rolling 5-min throughput returns to ≥ 95% of the pre-disruption mean and stays there 5 min.
   - Plus orders completed, deadlocks detected, robot distance travelled.
3. Plot throughput over time for one disrupted run.

**Done when:** the metric functions are unit-tested on a hand-made toy time series, and one disruption plot exists.
**Explain back:** Why "stays there 5 min" in the recovery definition? What could make this metric misleading?

### Weeks 5–6 (16–29 Nov) — Simulator extensions (Rust; Claude writes, you review and test)
WareMax lacks three things this study needs. Claude writes the patches; you run the tests and walk through the code with me until you can explain each change.

1. **Scheduled disruptions from YAML**: a `disruptions:` list — robot failure at time *t* for robot *r* with duration *d*; block node/edge from *t* to *t+d* (dynamic blockage is an open item in WareMax's roadmap); arrival-rate multiplier from *t* to *t+d* (surge).
2. **Exception decision points**: the Python env pauses when a disruption or low-battery event fires and returns a state snapshot.
3. **Recovery action hooks** from Python: reassign task, send robot to charge, reroute around blocked area, re-prioritise an order, hold/no-op — each with a validity mask.
4. **Tests**: the existing determinism test still passes; new test — same seed + same actions ⇒ identical trace with disruptions on.

**Learn:** reading (not writing) Rust; how PyO3 exposes Rust to Python; why tests matter in research code.
**Done when:** a scripted Python test blocks an aisle at t = 300 s and robots reroute; the run is reproducible.
**Explain back:** What does the validity mask prevent? Why must the extension keep the simulator deterministic?

### Week 7 (30 Nov – 6 Dec) — Scenario suite + strong rule-based baseline · **GATE 2**
**Build**
1. Scenario suite (each with fixed start, duration, severity low/high):

| ID | Disruption |
|----|-----------|
| S1 | Single robot failure |
| S2 | Multiple robot failures (20% of fleet) |
| S3 | Aisle blockage |
| S4 | Order surge (×2 arrival rate) |
| S5 | Battery-critical cluster (several robots low at once) |
| S6 | **Compound:** surge + failures + blockage overlapping |

2. **Rule-based recovery policy** (`policies/rule_recovery.py`): reassign to nearest idle, charge at threshold, reroute on block. This is the baseline the agent must beat; make it *good*.
3. `run_experiments.py`: config file → runs → one tidy CSV (one row per run).

**Done when:** all scenarios run under heuristic and rule recovery for 5 seeds.
**Gate 2:** Do the disruptions actually hurt (e.g. ≥ 10% throughput drop under the best heuristic)? If not, increase severity before building the agent — otherwise there's nothing to recover from.

### Week 8 (7–13 Dec) — Agent v0
**Learn:** prompt design; ReAct ([Yao et al. 2022](https://arxiv.org/abs/2210.03629)); structured output.
**Build** `agent/` (you write it from Claude's skeleton):
- `state_summary.py` — snapshot → ≤ ~300-token text (only what matters for this exception).
- `candidates.py` — list valid recovery actions from the action mask.
- `policy_llm.py` — prompt + JSON-schema-constrained choice + parse.
- Loop: exception → summarise → choose → execute → log (prompt, answer, latency, tokens).

**Done when:** Agent-only runs S1–S6 end to end on 3 seeds with every decision logged.
**Explain back:** Why does choosing from a list beat free-form planning here? What did the model get wrong most often in the logs?

### Week 9 (14–20 Dec) — Hybrid agent · **GATE 3**
**Build**
1. **Latency budget** with hard timeout → fallback to rule policy.
2. **Validation**: rule checks + **short look-ahead rollout** (clone the simulator state, simulate ~60 s for the top 2–3 options, pick the best). This is the "digital twin in the loop" — a key contribution for this special issue.
3. **Confidence**: agreement across k = 3 samples, or token log-probabilities; low confidence → fallback.

**Gate 3 (pilot, 5 seeds):** Is hybrid ≥ rule-based on S6 (compound) and not clearly worse elsewhere?
- *Yes →* proceed to full experiments.
- *No →* pivot options, in order: (a) richer state summary for compound cases; (b) give the LLM the rollout scores as input (LLM as tie-breaker/explainer); (c) reframe the contribution around the bounded-latency hybrid architecture and graceful degradation, with honest negative results on accuracy. All three are publishable; we pick based on the data.

### Week 10 (21–27 Dec) — Holiday buffer
Light work only: read 6–8 papers for related work (LLM agents for warehousing/MAPF, RMFS task allocation, edge LLM inference); one-paragraph note per paper in `notes/related_work.md`. Catch up on anything that slipped.

### Week 11 (28 Dec – 3 Jan) — Memory and reflection *(stretch goal)*
At 14 h/week this is done only if Gates 1–3 were passed on time. Otherwise
Week 11 becomes catch-up and an early start on the full experiments.
**Learn:** retrieval basics; Reflexion ([Shinn et al. 2023](https://arxiv.org/abs/2303.11366)).
**Build:** after each recovery, store (exception type, compact state, action, outcome). At decision time, retrieve the 2–3 most similar past episodes (start with simple feature similarity; embeddings only if needed) and add them to the prompt.
**Done when:** memory on/off can be toggled from config and both versions run.

### Weeks 12–13 (4–17 Jan) — Full experiments
**Main matrix:** 6 scenarios × 2 severities × 2 layouts × {best heuristic, rule recovery, agent-only, hybrid} × 20 seeds.
**Ablations** (on S6 + one simple scenario, medium layout, 20 seeds):
- model: 1.5B / 3B / 3.8B / 7–8B (offloaded)
- memory: on / off
- prompting: direct choice / short ReAct
- latency budget: unlimited / 1 s / 0.5 s (or values from Gate 1)
- **graceful degradation:** inject artificial LLM delay and failure rates (0–100%)

Runs go overnight with a resumable runner (results appended per run, so crashes don't lose work).

### Week 14 (18–24 Jan) — Analysis · **GATE 4**
**Learn:** paired comparisons across seeds; Welch's t-test vs. bootstrap CIs; multiple-comparison correction; how to read a non-inferiority test.
**Build:** final tables (mean ± 95% CI), recovery-time plots, latency CDF, degradation curve, sensitivity to severity.
**Gate 4:** Do the results support H1–H4? Whatever the answer, the paper reports it honestly; we adjust the claims to the evidence.

### Weeks 15–16 (25 Jan – 7 Feb) — Writing
IEEE T-ASE template. Order: Methods → Experiments → Results → Introduction → Related work → Abstract. Claude drafts sections from your results; **you rewrite them in your own words**. You must be able to defend every sentence.
Include the T-ASE "Note to Practitioners" section.

### Week 17 (8–12 Feb) — Buffer and submission
Reproducibility package (pinned WareMax commit + patches, configs, seeds, scripts), final proofreading, submit via ScholarOne by **Fri 12 Feb** under the "Topic-Based Special Issue" category.

---

## 6. Repository layout (built up week by week)

```
edge-agent/
  WORKPLAN.md          this file
  LEARNING_LOG.md      your weekly log and explain-back answers
  scenarios/           WareMax YAML scenarios (small, medium, S1–S6)
  patches/             WareMax extensions (or a fork, pinned)
  bench/               latency benchmarks
  policies/            rule-based recovery
  agent/               state summary, candidates, LLM policy, memory, hybrid
  run_experiments.py   config → CSV
  analysis/            metrics, statistics, figures
  results/             raw CSVs (large files gitignored)
  notes/               related work, decisions log
  paper/               LaTeX
```

---

## 7. Risks and what we do about them

| Risk | Early sign | Response |
|------|-----------|----------|
| LLM too slow on 4 GB GPU | Gate 1 | Shorter prompts, 1.5B model, prefix caching, simulated-time budget |
| WareMax bugs (young project, no releases) | Week 1–2 | Pin commit; fix locally; keep patches in `patches/` |
| Rust extensions take longer than 2 weeks | End of week 6 | Drop "re-prioritise" action and S5 first; keep failure + blockage + surge |
| Agent doesn't beat rules | Gate 3 | Pivot (b) or (c) in Week 9 — still a publishable contribution |
| Experiments too slow | Week 12 | Reduce to 10 seeds for ablations, keep 20 for main matrix |
| Schedule slip | Any Friday review | Cut in this order: memory (Wk 11) → 7–8B ablation → ReAct ablation → small layout |

---

## 8. Setup decisions (20 Oct 2026)

- **OS:** native **Windows** with PowerShell (WareMax officially supports
  Windows). Project folder `C:\Users\Midhu\Desktop\Warehouse`. WSL2 was considered
  and set aside (see `WORKLOG.md`, 20 Oct entry).
- **Effort:** 2 h/day, ~14 h/week.
- **Start:** Tue 20 Oct 2026.
- **WareMax:** pinned to commit `abaa4fdaa5c62b9dc32782969203e7f2d53cfbc2`.
