# The Allium Loop — One Page

```text
            intent                          code
              │                               │
              ▼                               ▼
         ┌─────────┐                    ┌──────────┐
         │ /elicit │  ── gather ──      │ /distill │
         │ forward │     context        │ backward │
         └────┬────┘                    └────┬─────┘
              │            spec              │
              └──────────►(.allium)◄─────────┘
                             │
                             ▼  take action
                       ┌────────────┐
                       │ /propagate │──► tests (plan.json + obligation-
                       │            │    tagged tests), implementation
                       └─────┬──────┘
                             │
                             ▼  verify
              ┌──────────────────────────────┐
              │ /weed        spec ↔ code     │
              │ allium check    structure    │
              │ allium analyse  semantics    │
              └─────────────┬────────────────┘
                            │
                            ▼  iterate
                       ┌─────────┐
                       │  /tend  │──► behavior changes hit the
                       └─────────┘    spec FIRST, then re-propagate
```

## Entry points — you don't always start at the top

| Situation | Enter at |
|-----------|----------|
| New feature idea | `/elicit` |
| Legacy code, no spec | `/distill` |
| Bug report / "code does X, should do Y" | `/weed` |
| Requirement changed | `/tend` |
| Not sure | `/allium` (routes for you) |

## The one rule

**Every probabilistic step gets a deterministic gate.** After any skill
touches a spec: `allium check` + `allium analyse`. After any code change
against a spec: `/weed` + tests. Green gates or it didn't happen.

## Team pipeline

1. Edit `.allium` → 2. `check` + `analyse` clean → 3. `allium plan` →
`plan.json` generated/committed if the repo adopts plans → 4. tests tagged
with obligation IDs → 5. CI runs deterministic gates and, optionally,
regenerates plans to fail on drift.

Local practice targets: `examples/access-grant-lifecycle.allium` for the
spec-to-tests path and `examples/code/pinned-installer.sh` for distill.
