"""Hybrid SD-ABM of the French TLC (textiles, linen, footwear) system under EPR.

Agent layer: consumers (purchase, second-hand, disposal, collection habit),
brands (price, durability, recyclability), sorting operators (capacity, exit).
SD layer: material stocks and flows in kilotonnes (in-use stock, backlog,
sorted fractions, exports, fibre-to-fibre capacity) and GHG accounting.

Historical mode (year <= hist_end): new purchases are forced to observed
placed-on-market tonnage and the ultra-fast share to its assumed trajectory;
collection points, sorting support and net sorting cost follow observations.
Projection mode (year > hist_end): purchases, segment mix and design are
endogenous and driven by the scenario's policy levers.
"""
from __future__ import annotations

import numpy as np
import pandas as pd

from .brands import Brands
from .consumers import Consumers
from .data import Observed
from .impacts import ghg
from .params import Params, load_params
from .sorting import FibreToFibre, Sorters

FORCING_LAST_YEAR = {"collection_points": 2025, "sorting_support": 2026, "export_price": 2025}


class GarmentCEModel:
    def __init__(self, params: Params | None = None, scenario: dict | None = None, seed: int = 0,
                 observed: Observed | None = None, start: int = 2010, end: int = 2040, hist_end: int = 2024,
                 spinup: int = 5, sh_off_from: int | None = None):
        self.p = params if params is not None else load_params()
        self.sc = dict(scenario or {})
        self.rng = np.random.default_rng(seed)
        self.obs = observed or Observed(extra_points={"collection_points": {2010: self.p.collection_points_2010}})
        self.start, self.end, self.hist_end, self.spinup = start, end, hist_end, spinup
        self.sh_off_from = sh_off_from

        p = self.p
        self.consumers = Consumers(p, self.rng)
        self.brands = Brands(p, self.rng)
        self.sorters = Sorters(p, self.rng)
        self.f2f = FibreToFibre(p)
        self.dur_stock = p.dur_fast
        self.rec_stock = p.rec_fast
        self.uf_stock = p.uf_share_2010
        self.price_index = p.price_fast
        self.sh_supply_kt = 0.0
        self.dbar = None
        self.anchor = {}
        self.records = []

    # ---------------- exogenous drivers ---------------------------------
    def pop(self, y):
        if y <= 2024:
            return self.obs.interp("population", y)
        return self.obs.interp("population", 2024) * (1 + self.p.pop_growth_proj) ** (y - 2024)

    def forcing(self, var, y, scenario_key):
        if y <= FORCING_LAST_YEAR[var] or scenario_key not in self.sc:
            if y > FORCING_LAST_YEAR[var] and var == "collection_points":
                last = FORCING_LAST_YEAR[var]
                return self.obs.interp(var, last) * self.pop(y) / self.pop(last)
            return self.obs.interp(var, min(y, FORCING_LAST_YEAR[var]))
        path = self.sc[scenario_key]
        last = FORCING_LAST_YEAR[var]
        xs = [last] + list(path.keys())
        ys = [self.obs.interp(var, last)] + list(path.values())
        if var == "collection_points":  # scenario given as points per 1000 inhabitants
            ys = [ys[0]] + [v * self.pop(y) * 1000 for v in path.values()]
        return float(np.interp(y, xs, ys))

    def net_sorting_cost(self, y, theta):
        p = self.p
        if y <= self.hist_end:
            return self.obs.interp("net_sorting_cost", y)
        price = self.forcing("export_price", y, "export_price")
        return p.net_cost_ref - (theta * price - self.anchor["theta"] * p.export_price_ref)

    def uf_target(self, y):
        p = self.p
        return float(np.interp(y, [2010, 2024], [p.uf_share_2010, p.uf_share_2024]))

    def c_star(self, density, y):
        p = self.p
        aw = float(np.interp(y, *zip(*sorted(self.sc["awareness"].items())))) if "awareness" in self.sc else 1.0
        return p.c_max * (1 - np.exp(-p.access_kappa * density)) * aw

    def life_new(self):
        p = self.p
        return p.lifetime_new * (self.dur_stock / p.dur_ref) ** p.lifetime_dur_elast

    # ---------------- one year ------------------------------------------
    def step(self, y: int, record: bool = True):
        p, C, B = self.p, self.consumers, self.brands
        hist = y <= self.hist_end
        y_force = min(y, self.start) if not record else y  # spin-up uses start-year forcings
        pop = self.pop(y_force)
        to_kt = pop * p.kg_per_item / C.n
        sh_on = self.sh_off_from is None or y < self.sh_off_from

        points = self.forcing("collection_points", y_force, "collection_points")
        density = points / (pop * 1000)
        cstar = self.c_star(density, y_force)

        life = self.life_new()
        d_new, d_sh = C.discard(life)
        discards = d_new + d_sh
        C.adopt(y_force if record else 0)

        sh_supply_pc = self.sh_supply_kt / to_kt / C.n if sh_on else 0.0  # items per person
        price_new = self.price_index

        def purchases(dbar):
            need = C.need(dbar, discards)
            sh_dem = (need * C.sh_share(sh_on)).mean()
            frac = min(1.0, sh_supply_pc / sh_dem) if sh_dem > 0 else 0.0
            new, sh, extra = C.acquire(need, frac, price_new, sh_on)
            return need, new, sh, extra

        if hist or self.dbar is None:
            pom_pc = self.obs.interp("placed_on_market", y_force) / (p.kg_per_item * pop)
            lo, hi = 0.0, 20 * pom_pc * life + 1
            for _ in range(50):
                mid = 0.5 * (lo + hi)
                if purchases(mid)[1].mean() < pom_pc:
                    lo = mid
                else:
                    hi = mid
            self.dbar = 0.5 * (lo + hi)
        else:
            a = self.anchor
            self.dbar = (a["dbar"] * (1 + self.sc.get("demand_growth", p.demand_growth_proj)) ** (y - self.hist_end)
                         * (self.price_index / a["price"]) ** (-p.price_elasticity))
        need, new, sh, extra = purchases(self.dbar)

        demand_cseg = np.array([new[C.seg == k].sum() for k in range(3)])
        if hist:
            B.force_uf_share(self.uf_target(y_force), demand_cseg)
            B.fee = B.fees(y_force, self.sc)
            upgrades = 0
        else:
            upgrades = B.decide_upgrades(y, self.sc)
            B.fee = B.fees(y, self.sc)
        mk = B.market(demand_cseg)
        self.price_index = mk["price_index"]

        w_before = C.w_new.sum()
        C.update_wardrobe(new, sh)
        tot_new = new.sum()
        for attr, val in (("dur_stock", mk["dur_new"]), ("rec_stock", mk["rec_new"]), ("uf_stock", mk["uf_share"])):
            setattr(self, attr, (getattr(self, attr) * w_before + val * tot_new) / (w_before + tot_new))

        # disposal routes
        u = C.p2p_share(sh_on)
        p2p = u * discards
        rest = discards - p2p
        col_hh = C.collect * rest
        resid = rest - col_hh
        sh_bought_kt = sh.sum() * to_kt
        surplus_kt = max(0.0, (self.sh_supply_kt if sh_on else 0.0) - sh_bought_kt)  # unsold -> donated
        collected = col_hh.sum() * to_kt + surplus_kt
        if not sh_on:
            collected += self.sh_supply_kt  # residual pool from before switch-off goes to collection

        C.update_habit(cstar)

        support = self.forcing("sorting_support", y_force, "sorting_support")
        theta_guess = self.sorters.reuse_share(self.uf_stock)
        net_cost = self.net_sorting_cost(y_force, theta_guess)
        shock = self.sc.get("capacity_shock", {}).get(y, 1.0)
        s = self.sorters.step(collected, self.uf_stock, support, net_cost, capacity_shock=shock)

        feed = s["recycling_open"] * self.rec_stock * p.f2f_feedstock_factor
        if y > self.hist_end:
            ff = self.f2f.step(feed, tot_new * to_kt, self.sc, y)
        else:
            ff = {"f2f_output": 0.0, "f2f_capacity": self.f2f.capacity, "f2f_premium": self.f2f.premium(),
                  "f2f_required": 0.0}
        f2f_in = ff["f2f_output"] / p.f2f_yield if ff["f2f_output"] > 0 else 0.0
        s["recycling_open"] -= f2f_in
        s["sort_residual"] += f2f_in - ff["f2f_output"]

        dom_reuse = s["reuse_domestic"] if sh_on else 0.0
        if not sh_on:
            s["reuse_export"] += s["reuse_domestic"]
            s["reuse_domestic"] = 0.0
        self.sh_supply_kt = p2p.sum() * to_kt + dom_reuse

        flows = {
            "year": y, "population": pop, "collection_points": points, "density": density, "c_star": cstar,
            "dbar": self.dbar, "life_new": life,
            "new_kt": tot_new * to_kt, "sh_kt": sh_bought_kt, "rebound_kt": extra.sum() * to_kt,
            "discards_kt": discards.sum() * to_kt, "p2p_kt": p2p.sum() * to_kt,
            "collected": collected, "collected_hh": col_hh.sum() * to_kt, "sh_surplus": surplus_kt,
            "residual_waste": resid.sum() * to_kt,
            "wardrobe_kt": (C.w_new.sum() + C.w_sh.sum()) * to_kt,
            "adopter_share": C.adopter.mean(), "collect_habit": C.collect.mean(),
            "sh_buyer_share": float((1 - np.exp(-sh)).mean()),  # Poisson: P(at least one item)
            "sh_items_per_buyer": float(sh.mean() / max((1 - np.exp(-sh)).mean(), 1e-9)),
            "price_index": mk["price_index"], "dur_new": mk["dur_new"], "rec_new": mk["rec_new"],
            "uf_share": mk["uf_share"], "fee_mean": mk["fee_mean"], "brand_upgrades": upgrades,
            "fee_revenue_meur": mk["fee_mean"] * tot_new * to_kt / p.kg_per_item,
            "dur_stock": self.dur_stock, "sorting_support": support, "net_sort_cost": net_cost,
            **s, **ff,
        }
        flows["f2f_input"] = f2f_in
        flows["virgin_fibre_kt"] = max(0.0, flows["new_kt"] - ff["f2f_output"])
        flows["collection_rate"] = collected / flows["new_kt"] if flows["new_kt"] > 0 else np.nan
        flows["exports_total"] = s["unsorted_export"] + s["overflow_export"] + s["reuse_export"]
        flows.update(ghg(p, flows))
        if y == self.hist_end and record:
            self.anchor = {"dbar": self.dbar, "price": self.price_index, "theta": s["reuse_share"]}
        if record:
            self.records.append(flows)
        return flows

    def run(self) -> pd.DataFrame:
        C = self.consumers
        p = self.p
        pop0 = self.pop(self.start)
        pom0 = self.obs.interp("placed_on_market", self.start) / (p.kg_per_item * pop0)
        dens0 = self.forcing("collection_points", self.start, "collection_points") / (pop0 * 1000)
        C.init_state(pom0 * self.life_new(), self.c_star(dens0, self.start))
        for _ in range(self.spinup):
            self.step(self.start, record=False)
        self.sorters.backlog = 0.0
        for y in range(self.start, self.end + 1):
            self.step(y)
        return pd.DataFrame(self.records).set_index("year")


def run_model(params=None, scenario=None, seed=0, **kw) -> pd.DataFrame:
    return GarmentCEModel(params, scenario, seed, **kw).run()
