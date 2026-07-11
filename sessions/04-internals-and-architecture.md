# Session 4: Internals & Architecture (2 hours)

**Objective**: Understand *how* Allium works so the team can debug, extend,
and contribute — not just consume.

## Agenda

| Time | Segment |
|------|---------|
| 0:00–0:20 | Component map: skills, CLI, editor integrations |
| 0:20–0:45 | How skills work |
| 0:45–1:15 | Parser & validation pipeline |
| 1:15–1:35 | Extensibility |
| 1:35–2:00 | Exercise: trace a spec through the CLI + propose an extension |

## 1. Component map (20 min)

Allium is three cooperating layers with the **spec file as the only shared
contract** between them:

```text
┌─────────────────────────────────────────────────────────┐
│ Skills / agents (Markdown instructions an LLM follows)  │
│   /allium /elicit /distill /propagate /weed /tend       │
│   → read & write .allium files, invoke the CLI          │
├─────────────────────────────────────────────────────────┤
│ CLI (Rust: allium-cli)                                  │
│   check | analyse | parse | plan | model                │
│   → deterministic parsing + structural/semantic analysis│
├─────────────────────────────────────────────────────────┤
│ Editor integrations                                     │
│   LSP, Tree-sitter grammar, diagnostics, autofix        │
│   → same parser core, surfaced at edit time             │
└─────────────────────────────────────────────────────────┘
```

Design consequence worth dwelling on: the LLM parts (skills) are
**probabilistic**, the CLI is **deterministic**. The loop works because every
probabilistic step is followed by a deterministic gate — an agent can
hallucinate a rule, but `allium check`/`analyse` will not hallucinate a pass.

## 2. How skills work (25 min)

Open the installed skill files and read them as artifacts:

- **Markdown-based instructions LLMs follow.** A skill is prompt engineering
  under version control: role framing, procedure, output format, guardrails.
  There is no skill "runtime" — the harness loads the Markdown into context.
- **Routing** — the `/allium` entry point classifies the situation (new
  intent? existing code? drift? change request?) and hands off to
  elicit/distill/weed/tend.
- **Prompt-engineering patterns to spot while reading:**
  - Forced interrogation (elicit *must* surface `open question`s rather than
    invent answers).
  - Output validation loops ("run `allium check`; if diagnostics, fix and
    re-run") — the deterministic gate embedded in the instructions.
  - Anti-injection framing (spec content is data, not instructions).
- **Autonomous agents** — because skills are files, they compose into larger
  flows (e.g. a CI agent that runs weed on every PR). This is the hook for
  Session 5's harness discussion.

**Live reading**: pull up the distill skill and annotate its structure as a
group — where's the procedure? where are the guardrails? what would you
change?

## 3. Parser & validation (30 min)

Reference: parser code and docs in the **allium-tools** repo.

- **Recursive descent** — one parse function per construct
  (entity/value/rule/contract/surface/config/given), hand-written rather
  than grammar-generated, so diagnostics can be precise and recovery
  deliberate.
- **AST** — see it directly:

  ```bash
  allium parse examples/access-grant-lifecycle.allium | jq '.' | less
  ```

  Walk one rule from source text to its AST node: trigger, guard
  expressions, effect list, source spans. Note `source_span` byte offsets —
  the same spans appear in `plan.json` obligations and LSP diagnostics (one
  parser, three consumers).

- **Validation passes** (roughly in order):
  1. Parse → syntax diagnostics.
  2. Name/type resolution — fields, enum literals, and references are
     resolved as far as the current tool supports.
  3. Structural checks (`check`) — construct-level well-formedness, unused
     declarations, trigger reachability.
  4. Process analysis (`analyse`) — data flow tracing, edge reachability,
     deadlock detection, conflict detection, and invariant verification.
     Recall Session 3's calibration: several semantic defect classes still
     pass silently in 3.5.0 — reading this layer's source to see what each
     pass actually covers is exactly the kind of contribution reconnaissance
     this session is for.
- Output is **JSON everywhere** — `diagnostics` (structural) vs `findings`
  (semantic). Machine-readable by design: CI, editors, and agents consume the
  same output. `examples/ci/github-actions-allium.yml` sketches a minimal
  consumer.

## 4. Extensibility (20 min)

Three tiers, cheapest first:

1. **New skills or patterns** — Markdown only, no Rust. A team-specific
   pattern can ship as a skill fragment or a patterns-library entry. Lowest
   bar to contribute.
2. **Custom analysis rules** — Rust, in the analysis layer. Model: a pass
   consumes the AST/state graph and emits findings JSON. Candidate ideas from
   the course examples:
   - warn when a field referenced in `requires`/`ensures` was never declared;
   - flag same-trigger rules with identical guards and conflicting state
     effects;
   - warn when a surface exposes a field named `internal_*` to a customer
     actor.
3. **Harness integration** — feed `allium model` / `plan` output into a
   control plane, review bot, or CI dashboard. The JSON output contract is
   the integration surface — no CLI changes needed.

## 5. Exercise: trace + propose (25 min)

**Part A — trace** (pairs): take your Session 2 access-grant spec and follow
it through the pipeline, capturing each stage:

```bash
cd exercises/02-access-grant-lifecycle/solution
allium parse   access-grant-lifecycle.allium > /tmp/ast.json
allium check   access-grant-lifecycle.allium
allium analyse access-grant-lifecycle.allium
allium plan    access-grant-lifecycle.allium | jq -r '.obligations[].id'
```

For one rule, write down: its AST shape, which analyse findings *could* fire
on it, and which obligations `plan` derived from it.

**Part B — propose** (individual, written): one paragraph proposing a small
extension — a new diagnostic, analysis rule, or pattern. Must include: the
defect it catches, a spec snippet that triggers it, and the finding JSON
you'd expect. Best proposals become Session 6 / capstone contribution
candidates.

## Resources

- allium-tools repo: docs, AGENTS.md, parser code, contributing guide
- `allium help <command>` for each pipeline stage
- [CLI cheat sheet](../cheatsheets/cli.md)
