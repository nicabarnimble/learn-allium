# Capstone Project

Full Allium cycle on real code. Runs from Session 5 to Session 6 (1–2
weeks), in the same pairs as Session 2.

## Brief

Pick a real module or feature from a project you own, or use one of this
repo's internal code examples as a starter, and take it through the complete
loop:

```text
distill/elicit  →  implement/reconcile  →  weed + propagate  →  review + contribution idea
```

## Requirements

1. **Spec** (distill if the code exists, elicit if it's new):
   - ≤ ~150 lines of Allium; scope block with honest `-- Excludes:`;
   - at least one contract with named invariants;
   - at least one surface with `provides:` (triggers must be reachable);
   - `allium check` and `allium analyse` clean.
2. **Implementation** — new code implementing the spec, or existing code
   reconciled against it (drift found by `/weed` either fixed in code or
   `/tend`-ed into the spec — a conscious decision each time, recorded as
   `-- Decision:` comments).
3. **Obligations** — generate `plan.json` next to the spec if your target
   repo commits plans, and tag ≥3 obligations in tests (`// obligation id:
   ...`). Regenerate plans; never hand-edit them.
4. **CI** — add or update a deterministic gate. At minimum: `allium check`
   on the maintained spec directory. If the repo adopts plans, add a
   plan-drift check.
5. **Contribution idea** — one written proposal (Session 4 format: defect →
   triggering snippet → expected finding/change), refined from your Session
   4 draft. Presented at the Session 6 review.

## Suggested scopes

- A small CLI subcommand family without a spec today.
- A script with safety behavior (checksum, platform gate, refusal paths) —
  `examples/code/pinned-installer.sh` is the local practice target.
- A workflow state machine such as access request, approval, expiry, and
  revocation.
- Promote a behavior section from an existing prose doc into a checked spec.
- Extend one internal example with a new behavior, then update tests and
  obligations.

Facilitator reviews scope **before** work starts — see the
[facilitator guide](../facilitator-guide.md) pass bar.

## Review (Session 6 or async)

- Live spec review: facilitator + one peer from another pair, hunting
  anti-patterns for 5 minutes (Session 3 list).
- Walk one obligation end-to-end: spec construct → plan ID → tagged test →
  CI.
- Contribution proposals presented; team votes on 1–2 to submit upstream.
