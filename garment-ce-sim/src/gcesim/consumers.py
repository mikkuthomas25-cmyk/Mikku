"""Consumer agents (households, one agent = population / n_agents people).

Agents are held as a structure of arrays for speed. Each agent has a segment, a
purchase intensity, a resale-platform adoption state (Bass diffusion on a
small-world network), a separate-collection habit, and a wardrobe split into
items bought new and items bought second-hand. Quantities are items per person.
"""
from __future__ import annotations

import numpy as np

TREND, MAIN, CONSC = 0, 1, 2


def small_world(n: int, k: int, p: float, rng: np.random.Generator) -> np.ndarray:
    """Watts-Strogatz neighbour list with fixed out-degree k (shape n x k)."""
    half = k // 2
    offsets = np.concatenate([np.arange(1, half + 1), -np.arange(1, half + 1)])
    nbrs = (np.arange(n)[:, None] + offsets[None, :]) % n
    rewire = rng.random(nbrs.shape) < p
    targets = rng.integers(0, n, size=nbrs.shape)
    targets = np.where(targets == np.arange(n)[:, None], (targets + 1) % n, targets)
    return np.where(rewire, targets, nbrs)


class Consumers:
    def __init__(self, p, rng: np.random.Generator):
        n = p.n_agents
        self.n = n
        self.p = p
        self.rng = rng
        self.seg = rng.choice(3, size=n, p=[p.seg_share_trend, p.seg_share_main, p.seg_share_consc])
        sig = p.purchase_sigma
        x = rng.lognormal(-sig**2 / 2, sig, size=n)
        self.intensity = x / x.mean() * np.where(self.seg == TREND, 1.3, np.where(self.seg == CONSC, 0.8, 1.0))
        self.intensity /= self.intensity.mean()
        self.nbrs = small_world(n, p.network_k, p.network_p, rng)
        self.adopter = np.zeros(n, dtype=bool)
        self.potential = rng.random(n) < p.bass_potential  # can ever adopt resale platforms
        self.collect = np.zeros(n)
        self.w_new = np.zeros(n)
        self.w_sh = np.zeros(n)
        self.sh_mult = np.select([self.seg == TREND, self.seg == CONSC], [p.seg_sh_mult_trend, p.seg_sh_mult_consc], 1.0)
        self.col_mult = np.select(
            [self.seg == TREND, self.seg == CONSC], [p.seg_collect_mult_trend, p.seg_collect_mult_consc], 1.0
        )

    # --- initialisation -------------------------------------------------
    def init_state(self, dbar: float, c_star: float):
        self.w_new = self.intensity * dbar
        self.w_sh = np.zeros(self.n)
        self.collect = np.clip(c_star * self.col_mult, 0, 1)

    # --- behaviour --------------------------------------------------------
    def sh_share(self, enabled: bool = True) -> np.ndarray:
        if not enabled:
            return np.zeros(self.n)
        p = self.p
        base = np.where(self.adopter, p.sh_share_adopter, p.sh_share_base)
        return np.clip(base * self.sh_mult, 0.0, 0.9)

    def p2p_share(self, enabled: bool = True) -> np.ndarray:
        if not enabled:
            return np.zeros(self.n)
        p = self.p
        return np.clip(np.where(self.adopter, p.p2p_share_adopter, p.p2p_share_base) * self.sh_mult, 0.0, 0.9)

    def discard(self, life_new: float) -> tuple[np.ndarray, np.ndarray]:
        """Exponential outflow over one year; second-hand items have shorter residual life."""
        d_new = self.w_new * (1 - np.exp(-1.0 / life_new))
        d_sh = self.w_sh * (1 - np.exp(-1.0 / (self.p.sh_life_frac * life_new)))
        self.w_new -= d_new
        self.w_sh -= d_sh
        return d_new, d_sh

    def adopt(self, year: int):
        p = self.p
        if year < p.bass_start:
            return
        frac = self.adopter[self.nbrs].mean(axis=1) / max(self.p.bass_potential, 1e-9)
        prob = 1 - np.exp(-(p.bass_p + p.bass_q * frac))
        self.adopter |= self.potential & (self.rng.random(self.n) < prob)

    def need(self, dbar: float, discards: np.ndarray) -> np.ndarray:
        """Desired acquisitions: replace discards and close wardrobe gap."""
        gap = self.intensity * dbar - (self.w_new + self.w_sh)
        return np.maximum(0.0, discards + gap / self.p.wardrobe_adj_time)

    def acquire(self, need, sh_frac_supply, price_new, sh_enabled=True):
        """Split acquisitions into second-hand and new items, including rebound.

        sh_frac_supply scales second-hand demand down when supply is short.
        Money saved on second-hand purchases is partly re-spent on new items.
        """
        p = self.p
        sh = need * self.sh_share(sh_enabled) * sh_frac_supply
        saving = sh * max(price_new - p.price_sh, 0.0)
        extra_new = p.rebound * saving / price_new
        new = need - sh + extra_new
        return new, sh, extra_new

    def update_wardrobe(self, new, sh):
        self.w_new += new
        self.w_sh += sh

    def update_habit(self, c_star: float):
        p = self.p
        nbr = self.collect[self.nbrs].mean(axis=1)
        target = np.clip(c_star * self.col_mult, 0, 1)
        noise = self.rng.normal(0, p.habit_sigma, self.n)
        self.collect = np.clip(
            self.collect + p.alpha_access * (target - self.collect) + p.alpha_norm * (nbr - self.collect) + noise, 0, 1
        )
