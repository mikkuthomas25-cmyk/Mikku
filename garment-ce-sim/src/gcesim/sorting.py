"""Sorting operators (agents) inside the system-dynamics material layer.

Each operator holds sorting capacity. Collected tonnage that is not exported
unsorted joins a backlog; operators sort up to capacity, the rest waits in
storage and overflows (export unsorted or incineration) once storage is full.
Desired capacity follows the inflow and is modulated by the operating margin
(EPR sorting support minus net sorting cost), with a first-order delay.
Operators with sustained losses exit.
"""
from __future__ import annotations

import numpy as np


class Sorters:
    def __init__(self, p, rng: np.random.Generator):
        self.p = p
        self.rng = rng
        n = p.n_sorters
        share = np.empty(n)
        share[0] = p.big_sorter_share  # largest network (Le Relais)
        rest = rng.dirichlet(np.ones(n - 1)) * (1 - p.big_sorter_share)
        share[1:] = rest
        self.capacity = share * p.sort_capacity_2010
        self.cost_mult = rng.normal(1.0, 0.15, n).clip(0.6, 1.5)
        self.cost_mult[0] = 1.0
        self.active = np.ones(n, dtype=bool)
        self.loss_years = np.zeros(n)
        self.backlog = 0.0
        self.inflow_smooth = None

    @property
    def total_capacity(self) -> float:
        return float(self.capacity[self.active].sum())

    def reuse_share(self, uf_discard_share: float) -> float:
        p = self.p
        return float(np.clip(p.reuse_share_base - p.reuse_quality_slope * uf_discard_share, 0.2, 0.8))

    def step(self, collected_kt: float, uf_discard_share: float, support: float, net_cost: float,
             unsorted_share: float | None = None, capacity_shock: float = 1.0) -> dict:
        p = self.p
        e = p.unsorted_export_share if unsorted_share is None else unsorted_share
        unsorted_export = e * collected_kt
        to_sort = (1 - e) * collected_kt + self.backlog
        cap = self.total_capacity * capacity_shock
        sorted_kt = min(to_sort, cap)
        backlog = to_sort - sorted_kt
        storage = p.storage_frac * cap
        overflow = max(0.0, backlog - storage)
        self.backlog = backlog - overflow
        overflow_export = p.overflow_export_share * overflow
        overflow_incin = overflow - overflow_export

        theta = self.reuse_share(uf_discard_share)
        reuse = theta * sorted_kt
        reuse_dom = p.domestic_reuse_frac * reuse
        nonreuse = sorted_kt - reuse
        recycling = p.recycling_frac_nonreuse * nonreuse
        wipes = p.wipes_frac_nonreuse * nonreuse
        energy = p.energy_frac_nonreuse * nonreuse
        residual = nonreuse - recycling - wipes - energy

        # capacity dynamics (agent level)
        inflow = (1 - e) * collected_kt
        self.inflow_smooth = inflow if self.inflow_smooth is None else 0.5 * self.inflow_smooth + 0.5 * inflow
        margin = support - net_cost * self.cost_mult
        cap_share = self.capacity / max(self.capacity[self.active].sum(), 1e-9)
        desired = cap_share * self.inflow_smooth * np.clip(1 + p.capacity_margin_sens * margin, 0.5, 1.2)
        self.capacity += (desired - self.capacity) / p.capacity_adj_time
        self.capacity = np.maximum(self.capacity, 0.0)
        # exit after sustained heavy losses; entry into vacant slots when the average margin is positive
        self.loss_years = np.where(margin < -p.exit_margin, self.loss_years + 1, 0)
        exit_prob = np.where(self.loss_years >= 3, p.exit_prob, 0.0)
        exit_prob[0] = 0.0  # the dominant network is modelled as a capacity shock instead
        exits = self.active & (self.rng.random(len(exit_prob)) < exit_prob)
        self.active &= ~exits
        self.capacity[exits] = 0.0
        avg_margin = float(np.mean(margin[self.active])) if self.active.any() else 0.0
        if avg_margin > 0:
            vacant = ~self.active & (self.rng.random(len(self.active)) < p.entry_prob)
            small = self.capacity[1:][self.active[1:]]
            self.capacity[vacant] = small.mean() if small.size else 1.0
            self.active |= vacant
            self.loss_years[vacant] = 0

        return {
            "unsorted_export": unsorted_export,
            "sorted": sorted_kt,
            "backlog": self.backlog,
            "overflow_export": overflow_export,
            "overflow_incin": overflow_incin,
            "reuse_share": theta,
            "reuse_domestic": reuse_dom,
            "reuse_export": reuse - reuse_dom,
            "recycling_open": recycling,
            "wipes": wipes,
            "energy_recovery": energy,
            "sort_residual": residual,
            "sort_capacity": cap,
            "sort_margin": float(np.mean(margin[self.active])) if self.active.any() else 0.0,
            "n_sorters": int(self.active.sum()),
        }


class FibreToFibre:
    """System-dynamics stock of fibre-to-fibre recycling capacity with learning."""

    def __init__(self, p):
        self.p = p
        self.capacity = p.f2f_capacity_2024
        self.pipeline = 0.0
        self.cum = max(p.f2f_capacity_2024, 1e-3)
        self.cum0 = self.cum

    def premium(self) -> float:
        p = self.p
        b = -np.log2(1 - p.f2f_learning_rate)
        return p.f2f_cost_premium_0 * (self.cum / self.cum0) ** (-b)

    def step(self, feedstock_kt: float, new_mass_kt: float, sc: dict, year: int) -> dict:
        p = self.p
        out = min(self.capacity, feedstock_kt * p.f2f_yield)
        self.cum += out
        mandate = sc.get("recycled_content", {})
        req_share = float(np.interp(year, list(mandate.keys()), list(mandate.values()))) if mandate else 0.0
        required = req_share * new_mass_kt
        incentive = sc.get("recycled_bonus_frac", 0.0) + (1.0 if required > out else 0.0) * min(1.0, req_share * 10)
        drive = 1 / (1 + np.exp(-8 * (incentive - self.premium())))
        investment = p.f2f_max_growth * drive * max(self.capacity, 1.0)
        completed = self.pipeline / p.f2f_build_delay
        self.pipeline += investment - completed
        self.capacity += completed
        return {"f2f_output": out, "f2f_capacity": self.capacity, "f2f_premium": self.premium(),
                "f2f_required": required}
