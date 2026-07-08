# Allium CLI Cheat Sheet

All commands emit JSON (`diagnostics` = structural, `findings` = semantic).
Course material validated against `allium 3.5.0`.

## Install

```bash
brew tap juxt/allium && brew install allium   # macOS
cargo install --locked allium-cli
allium --version
```

## The five commands

```bash
allium check   <spec|dir>    # validate structure; empty diagnostics = pass
allium analyse <spec>        # reachability, deadlocks, conflicts, data flow
allium parse   <spec>        # AST as JSON
allium plan    <spec>        # derive test obligations
allium model   <spec>        # extract domain model for downstream tools
allium help <command>        # per-command help
```

## Daily one-liners

```bash
# validate the internal example bundle
allium check examples

# full analysis of one spec, findings only
allium analyse examples/access-grant-lifecycle.allium | jq '.findings'

# regenerate a plan (never hand-edit plan.json)
allium plan examples/access-grant-lifecycle.allium \
  > examples/access-grant-lifecycle.allium.plan.json

# list obligation IDs for test tagging
allium plan examples/access-grant-lifecycle.allium | jq -r '.obligations[].id'

# inspect one rule's AST
allium parse examples/access-grant-lifecycle.allium \
  | jq '.. | objects | select(.name? == "GrantExpires")'
```

## Obligation tagging

```rust
// obligation id: rule-success.GrantApproved
// obligation ids: rule-success.GrantApproved + rule-success.ResourceUseAuthorized
#[test]
fn approved_grant_allows_resource_use() { /* ... */ }
```

## CI gates

Minimum gate:

```bash
allium check examples
find exercises -path '*/solution/*.allium' -print0 | xargs -0 -n1 allium check
```

Optional plan-drift gate (for repos that commit generated plans):

```bash
for spec in examples/*.allium; do
  allium plan "$spec" > "$spec.plan.json"
done
git diff --exit-code -- '*.plan.json'
```

See [`examples/ci/github-actions-allium.yml`](../examples/ci/github-actions-allium.yml)
for a copyable workflow sketch.

## Reading output

| Field | Meaning |
|-------|---------|
| `diagnostics: []` | structurally valid |
| `findings: []` | no process-level issues |
| `severity` | `error` / `warning` / `info` — any diagnostic makes check exit 1 |
| `source_span {start,end}` | byte offsets into the spec — same spans in parse/plan/LSP |
| `obligations[].id` | stable ID: `<category>.<construct>[.n]` — goes in the test comment |
| `obligations[].category` | e.g. `entity_fields`, `rule-success`, `rule-failure`, `temporal`, `surface-provides` |

Diagnostics you'll actually meet:

| Code | Meaning |
|------|---------|
| `allium.rule.unreachableTrigger` | rule listens for an event no surface provides / no rule emits |
| `allium.entity.unused` | entity declared, never referenced |
| `allium.field.unused` | field declared, never referenced |
| `allium.status.noExit` | enum status has no rule transitioning out of it |

`analyse` findings you'll actually meet: `"type": "conflict"` — two rules
with **different triggers** can both fire in the same state and set the same
field to different values (a race, found mechanically). The idiomatic fix is
shown in `examples/library-lending.allium`: let the temporal rule **observe a
fact instead of racing the state field**.

## Know the net's exact shape (validated on 3.5.0)

Caught: structural closure (`unreachableTrigger`), unused declarations,
missing status exits, cross-trigger conflicts on one state.

Silent: a **typo'd field name** in `requires`/`ensures`; **same-trigger**
rules with identical guards and contradictory `ensures` (treated as case
analysis). Also note the transition tracker only credits a status exit when
the guard names the state **directly** (`status = x`) — `in {a, b}` sets and
derived-field guards don't register, so guard state-changing rules with
direct equality. Green CLI ≠ correct spec — that's what `/weed` and review
are for. Each silence is a good first upstream contribution.
