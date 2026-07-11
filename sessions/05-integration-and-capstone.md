# Session 5: Integration, Best Practices & Capstone (1.5–2 hours)

**Objective**: Wire Allium into a real workflow and kick off the capstone.

## Agenda

| Time | Segment |
|------|---------|
| 0:00–0:30 | Integrating with tools, workflows, harnesses |
| 0:30–0:50 | Scaling to team/large codebases |
| 0:50–1:05 | Measuring impact |
| 1:05–1:30+ | Capstone kickoff |

## 1. Integration — the self-contained workflow (30 min)

This section teaches the repo-local version first. Remote production examples
can be discussed afterward, but they are no longer required for the course.

### The obligation pipeline

```bash
# 1. Author/update the spec
$EDITOR examples/access-grant-lifecycle.allium

# 2. Validate
allium check   examples/access-grant-lifecycle.allium
allium analyse examples/access-grant-lifecycle.allium

# 3. Generate/update obligations
allium plan examples/access-grant-lifecycle.allium \
  > examples/access-grant-lifecycle.allium.plan.json

# 4. Tag tests with obligation ids
```

```rust
// obligation id: rule-success.GrantApproved
// obligation ids: rule-success.GrantApproved + rule-success.ResourceUseAuthorized
#[test]
fn approved_grant_allows_resource_use() { ... }
```

The obligation ID is the **traceability edge**: spec construct → `plan.json`
obligation → test comment → CI.

### CI enforcement

Minimum gate:

```bash
allium check examples
find exercises -path '*/solution/*.allium' -print0 | xargs -0 -n1 allium check
```

Optional plan-drift gate for repos that commit generated plans:

```bash
for spec in examples/*.allium; do
  allium plan "$spec" > "$spec.plan.json"
done
git diff --exit-code -- '*.plan.json'
```

See `examples/ci/github-actions-allium.yml` for a copyable workflow sketch.
Discussion: what's *not* yet enforced? (`analyse` on every changed spec,
obligation-comment coverage, and weed findings are all good contribution
candidates.)

### Harness integration

- Skills run inside the agent harness; specs become loaded context, not just
  documentation.
- `allium model` output is the hook for feeding specs into a broader control
  plane, dashboard, or review bot.
- Keep deterministic gates close to probabilistic steps: whenever an agent
  edits a spec, it should run `allium check`/`analyse` before handing work
  back.

### Minimal-artifact policy

Keep artifacts lean: **specs + plans if your repo adopts plan drift checks**.
Avoid generated gate/report sprawl unless a consumer actually reads it.

## 2. Scaling to team & large codebases (20 min)

- **Modular specs with explicit scope blocks.** Each spec's `-- Includes:`
  and `-- Excludes:` lines define its boundary. If a file needs a second
  scope block inside itself, split it.
- **A product-map spec as the shared root.** Use a compact umbrella spec for
  principles, contracts, and open questions, with focused feature specs below
  it. `examples/support-ticket-routing.allium` is the local miniature.
- **Specs as backlog.** Finishing an implementation slice ≠ finishing the
  spec. Unimplemented obligations become explicit follow-on specs, not
  silent debt.
- **Decision hygiene.** `open question` + `-- Decision:` keeps the map
  current without meetings. Review diffs of the spec like code.
- **Shared libraries.** Extract recurring shapes (deny-by-default authority,
  safe projection, terminal result) into the team patterns library rather
  than copy-pasting entity clusters between specs.

## 3. Measuring impact (15 min)

Pick a small set, measure before/after adoption, revisit quarterly:

| Signal | Proxy metric |
|--------|--------------|
| Fewer behavior bugs | Bugs traced to "spec was ambiguous/missing" in retro labels |
| Faster onboarding | Time for a new dev/agent session to make a correct behavioral change |
| Better LLM outputs | Weed findings per PR (drift trend); re-elicitation frequency |
| Traceability | % of spec obligations with tagged tests |
| Toolchain health | CI failures from plan drift caught pre-merge |

Anti-metric warning: "number of specs" and "spec line count" reward sprawl —
the minimal-artifact policy exists for a reason.

## 4. Capstone project (kickoff — work spans the following 1–2 weeks)

**Brief**: [exercises/capstone.md](../exercises/capstone.md)

Team picks a real module or feature and runs the full cycle:

1. **Distill or elicit** the spec (distill if code exists, elicit if new).
2. **Implement** against it (or reconcile existing code).
3. **Weed + propagate**: alignment clean, plan generated if the repo commits
   plans, obligation-tagged tests added, CI green.
4. **Review + contribution idea**: present the spec to the team; each
   capstone must end with one written internals-level contribution proposal
   (from Session 4 Part B, refined).

Suggested scopes (small enough to finish, real enough to matter):

- A local CLI subcommand family without a spec today.
- A script with safety behavior (use `examples/code/pinned-installer.sh` as
  the practice-sized reference).
- A workflow state machine such as access request, approval, expiry, and
  revocation.
- Promote an existing informal behavior doc into a checked spec.

Pairing: same junior/senior pairs as Session 2; facilitator reviews scope
before work starts (see [facilitator guide](../facilitator-guide.md)).

## Remote production examples (optional comparison)

If the team has access, compare the local workflow with the optional remote
case studies listed in [`../examples/README.md`](../examples/README.md#optional-remote-production-examples).
Use them as "how this scales" reading, not as prerequisites.

## Resources

- [Internal examples](../examples/)
- `examples/ci/github-actions-allium.yml`
- [Capstone brief](../exercises/capstone.md)
