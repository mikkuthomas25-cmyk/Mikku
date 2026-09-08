# What this project is — in plain terms

## The one-line version

Business researchers use a popular method called **PLS-SEM** to predict things
like "how loyal will this customer be?" Today it gives a **single guessed number**.
This project adds an honest **"give or take" range** around that guess — a range
you can actually trust, even when the data is messy or small.

## An everyday analogy

A weather app is more useful when it says **"18°C, give or take 3°"** than just
**"18°C"** — the range tells you how sure to be and whether to carry a jacket.

Right now, PLS-SEM is like an app that only ever says "18°C". It never tells you
the "give or take". We're adding a trustworthy "give or take" to every prediction.

## Why that matters

Managers make real decisions from these predictions — who might quit, how
satisfied a customer will be, which users to target. A single number hides how
uncertain the guess is. A **reliable range** says "we're confident it lands
between here and here," which is what a decision actually needs.

## Why it isn't already solved

- PLS-SEM is everywhere in service, marketing, and management research, but its
  prediction tools only produce single numbers — **no honest per-person range**.
- The usual statistical shortcut for a range assumes the data is nicely
  bell-shaped and the sample is large. **Real service survey data often isn't** —
  it's lopsided, and samples are small.

We borrow a modern statistics idea called **conformal prediction**, whose ranges
come with a **mathematical guarantee** that holds *without* assuming bell-shaped
data. Nobody has brought it to PLS-SEM before.

## What we found so far (feasibility is proven)

We built a working version and stress-tested it:

1. **It works** — the ranges cover the truth about as often as promised (a "90%
   range" contains the real answer ~90% of the time).
2. **The obvious version breaks on small samples** — the simplest recipe makes
   the range far too wide when you only have ~60 respondents (common in this field).
3. **A smarter recipe fixes it** — methods called **CV+** and **jackknife+** give
   honest, appropriately-tight ranges even at small samples.
4. **Different customer groups** — if two groups are unequally predictable, one
   overall range quietly cheats each of them (too wide for one, too narrow for the
   other). A **per-group ("Mondrian") version** keeps every group's range honest.
5. Honest limit: when data *is* clean, large, and bell-shaped, our range offers no
   advantage over the old shortcut — the gains show up exactly in the messy, small,
   uneven conditions real service data lives in.

## Who this helps

- **Researchers** who use PLS-SEM and want defensible predictions, not just guesses.
- **Managers** relying on those models, who need to know how much to trust a number.

## Where it's going

This is a feasibility study for a methodology paper aimed at the *Journal of
Service Management* special section on PLS-SEM in service research. The code that
produced the findings above lives in `spike/conformal_plssem_spike.R`
(run it with `Rscript spike/conformal_plssem_spike.R`). The technical write-up is
in `README.md`.
