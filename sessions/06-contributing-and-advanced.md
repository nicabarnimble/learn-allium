# Session 6 (Optional): Contributing & Advanced Topics (1.5 hours)

**Objective**: Turn proficient users into upstream contributors and handle
the sharp edges of larger-scale use.

## Agenda

| Time | Segment |
|------|---------|
| 0:00–0:25 | Open-source contribution workflow |
| 0:25–0:50 | Custom skills & agents |
| 0:50–1:10 | Performance & edge cases with larger specs |
| 1:10–1:30 | Future directions + capstone contribution review |

## 1. Open-source workflow (25 min)

- Repos: **allium** (language, skills, docs) and **allium-tools** (CLI,
  parser, LSP, Tree-sitter). Read each repo's `AGENTS.md` and contributing
  guide before opening anything.
- Contribution ladder, lowest friction first:
  1. **Docs/patterns** — fix a pattern, document a workflow you use here
     (the obligation-tagging pipeline is shareable).
  2. **Skills** — improve a prompt guardrail you've seen fail.
  3. **Diagnostics** — a new check/analyse rule (Session 4 Part B proposals).
  4. **Parser/LSP** — grammar and tooling changes.
- Issue → discussion → PR etiquette: lead with the spec snippet that
  motivates the change and the finding JSON you expect (same format as the
  Session 4 exercise — that write-up *is* the issue body).
- Local norms still apply: pin tool versions in CI when reproducibility
  matters, and avoid artifact sprawl.

## 2. Custom skills & agents (25 min)

- Anatomy of a team-local skill: procedure + guardrails + deterministic gate
  (`allium check` in the loop). Store team skills next to the code they
  serve.
- Live build (group): a `/weed-ci` variant — an agent instruction file that,
  on a PR, loads changed `.allium` files + touched implementation modules,
  runs weed, and posts findings. Wire it to the obligation-comment convention
  so it can flag *untagged* obligations.
- Composition: skills calling CLI calling skills — where to put the
  deterministic gates so autonomous runs stay honest.
- Review-bot angle: `allium plan` and `allium model` are JSON contracts that
  can feed dashboards, PR comments, or release gates without changing the
  language.

## 3. Performance & edge cases with larger specs (20 min)

Use `examples/support-ticket-routing.allium` as the local subject, then
compare with any remote production spec the team has access to:

- **Analysis cost** grows with the rule-interaction graph, not line count —
  many small independent contracts are cheap; one entity with many
  interacting rules is not.
- **When to split**: a spec that needs its own scope block *inside* itself is
  two specs. Split along `-- Excludes:` seams.
- **Enum explosion**: closed `kind`/`reason` sets are the honest way to model
  taxonomies, but review their diffs carefully — every added variant is a
  behavioral promise.
- **Plan JSON churn**: large specs make large `plan.json` diffs; keep one
  plan per spec (never hand-edit) and let regeneration own the refresh so
  drift detection stays trustworthy.
- **Skill context limits**: agents can't load huge specs plus code; this is
  another force pushing toward modular specs + a compact map.

## 4. Future directions (10 min, discussion)

- **Formal verification ties** — Allium deliberately stops short of full
  formal methods (Session 3); the open question is exporting to
  model-checkable form (state graph → TLA+/Alloy) for the few contracts that
  warrant it, without changing the authoring experience.
- **Deeper harness integration** — specs as first-class runtime evidence:
  release gates consuming plan obligations, control planes consuming
  `allium model`, review bots explaining behavioral diffs.
- **Spec-aware review** — LSP/CI surfacing *behavioral* diffs ("this PR
  removes an invariant") instead of purely textual ones.

## 5. Capstone contribution review (20 min)

Each pair presents their capstone contribution proposal (5 min each): defect
→ motivating snippet → expected finding/change. The team votes on one or two
to actually submit upstream; facilitator assigns shepherds.

## Resources

- allium / allium-tools contributing guides and `AGENTS.md`
- Session 4 exercise write-ups (the proposal seeds)
- [Facilitator guide — assessment](../facilitator-guide.md#assessment)
- [Internal examples](../examples/)
