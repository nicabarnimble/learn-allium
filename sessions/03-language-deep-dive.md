# Session 3: Language Deep Dive (1.5–2 hours)

**Objective**: Read and write Allium v3 fluently and understand the design
trade-offs.

> **Full slide deck available**: this session is expanded into 36 slides with
> speaker notes, live-demo scripts, and a validated running example in
> [`../slides/session-03-slides.md`](../slides/session-03-slides.md)
> (running example: [`../examples/library-lending.allium`](../examples/library-lending.allium)).
> The outline below remains the facilitator's summary view.

## Agenda

| Time | Segment |
|------|---------|
| 0:00–0:40 | Syntax breakdown (v3), construct by construct |
| 0:40–1:00 | Semantics: what the CLI actually checks |
| 1:00–1:15 | Anti-patterns |
| 1:15–1:30 | Patterns library reading |
| 1:30–2:00 | Exercise: refactor a flawed spec |

## 1. Syntax breakdown (v3) — 40 min

Teach from the internal examples, one construct at a time:
`examples/library-lending.allium`, `examples/access-grant-lifecycle.allium`,
and `examples/support-ticket-routing.allium`.

### Entities, fields, enums, conditionals

```allium
entity AccessGrant {
    grant_state: requested | active | denied | expired | revoked
    resource_name: String
    requested_by: String
    decided_by_operator: String?
    expires_at: Timestamp?

    is_active: grant_state = active
}
```

- Field types: `String`, `Boolean`, `Integer`, `Timestamp`, `Duration`,
  `List<T>`, inline enums, other `value`/`entity` types, `?` for optional.
- Conditional fields (`when`) — a field that only exists in some states:

```allium
resolution_note: String when ticket_state = closed
```

### Values vs entities

`value` = immutable data shape, no identity. `entity` = identity + state that
rules act on (`Book`, `AccessGrant`, `SupportTicket`). Use values for data
that is carried; use entities for things with lifecycle.

### Rules (`when` / `requires` / `ensures`)

```allium
rule GrantRevoked {
    when: OperatorRevokesGrant(operator)
    requires: grant.grant_state = active
    ensures: grant.grant_state = revoked
    ensures: grant.decided_by_operator = operator.name
    ensures: AccessGrantRevoked(grant: grant, operator: operator)
}
```

- Multiple rules sharing a `when` = case analysis; `requires` clauses must
  partition the cases.
- Set membership: `requires: ticket.severity in {high, critical}`.
- Temporal trigger form: `when: _: AccessGrant.expires_at <= now`.
- **Triggers must come from somewhere.** Every event a rule listens for is
  either provided by a surface or emitted by another rule's `ensures`;
  otherwise `allium check` reports `allium.rule.unreachableTrigger`.

### Transitions and invariants

Transitions in v3 are the discipline of *enum state field + one rule per
edge*. Invariants live in contracts:

```allium
contract AccessGrantAuthority {
    @invariant DenyByDefault
        -- A resource-use attempt is allowed only when an active grant exists.

    @invariant TerminalStatesStayTerminal
        -- denied, expired, and revoked grants never return to requested or active.
}
```

Each `@invariant` has a stable name (it becomes an obligation ID) and a prose
meaning. Contracts are the part of the spec that reviewers and agents quote
back.

### Surfaces, actors, contracts

A `surface` declares **exactly what a given actor may see** and which events
that actor may inject:

```allium
surface ResourceUse {
    facing requester: Requester
    context grant: AccessGrant

    exposes:
        requester.requester_id
        grant.is_active

    provides:
        RequesterUsesResource(requester)
}
```

For safe projections, compare `CustomerTicketStatus` with
`OperationsTriageInspection` in `examples/support-ticket-routing.allium`:
customers see safe summaries and audit references; operations sees internal
triage reasons.

### Config, temporal rules, exceptions/timeouts

```allium
config {
    grant_ttl: Duration = 24.hours
}
```

Timeouts = config duration + deadline field + temporal rule. The library
example shows the "observe, don't race" pattern: `BookGoesOverdue` emits a
fact instead of competing with `ReturnBook` over the `status` field.

### Spec-level scaffolding

- `-- allium: 3` version header, always first line.
- Scope block: `-- Scope:` / `-- Includes:` / `-- Excludes:` comments at the
  top. The *Excludes* list is as important as Includes.
- `open question "..."` + `-- Decision:` comments — the spec as a decision
  log. Undecided things are *visible*, not silently absent.
- `given { grant: AccessGrant }` — the ambient instances rules refer to.

## 2. Semantics & analysis — what the CLI checks (20 min)

Run these live on `examples/library-lending.allium` or
`examples/access-grant-lifecycle.allium` and show the JSON:

| Command | Checks |
|---------|--------|
| `allium check` | Structural diagnostics: malformed constructs, unused entities/fields, rules listening for triggers nothing provides |
| `allium analyse` | Everything `check` does, plus process-level passes (data flow, edge reachability, deadlock detection, conflict detection, invariant verification) |
| `allium parse` | AST as JSON (Session 4 material) |
| `allium plan` | Test obligations (Session 5 material) |
| `allium model` | Domain model extraction for downstream tools |

**Honest calibration** (validated against CLI 3.5.0, live-demo it — the
slide deck's Act III walks each case):

- *Caught*: unused entities/fields, unreachable triggers, enum statuses with
  no exit (`allium.status.noExit`), and **cross-trigger conflicts** — two
  rules with different triggers both firing in one state and setting the
  same field to different values.
- *Silent*: a typo'd field name in a `requires`, and **same-trigger** rules
  with identical guards and contradictory `ensures` (assumed to be case
  analysis). The transition tracker also only credits status exits guarded
  by direct equality — `in {}` sets and derived-field guards don't register.

Teach the CLI as a fast structural gate, not a proof engine: `/weed` and
human review carry the semantic load. Making one of the silent cases into a
diagnostic is a perfect first upstream contribution (Sessions 4/6).

## 3. Anti-patterns (15 min)

Each of these is planted in the
[refactor exercise](../exercises/03-spec-refactor/) — name them now, hunt
them next:

1. **Implementation leakage** — SQLite table names, HTTP status codes,
   function names in rules.
2. **UI in the spec** — button labels, layout, wording. Surfaces expose
   *fields*, not widgets.
3. **Prose smuggling** — a comment that carries a behavioral requirement no
   rule enforces.
4. **God entity** — one entity accreting every field; missing `value` types.
5. **Enum states with no exit/entry rules** — usually a sign the state is
   imagined, not designed.
6. **Contradiction by accretion** — a new rule added without checking
   existing guards on the same `when`.
7. **Spec as backlog-dumping** — everything `open question`, nothing
   decided.

## 4. Patterns library (15 min, reading)

Small-group reading, one exemplar each, report back in two sentences:

- **Deny-by-default authority** — `examples/access-grant-lifecycle.allium`.
- **Observe, don't race** — `examples/library-lending.allium`.
- **Safe-projection pattern** — `examples/support-ticket-routing.allium`.
- **Terminal-state pattern** — `AccessGrantAuthority.TerminalStatesStayTerminal`.
- **Decision-log pattern** — open question + `-- Decision:` entries in the
  support ticket example.

## 5. Exercise: refactor a flawed spec (30 min)

**Exercise**: [Spec refactor](../exercises/03-spec-refactor/) — a
deliberately flawed `plugin-approval.allium` containing every anti-pattern
from §3. Fix it until `allium check` and `allium analyse` are clean *and* a
partner can't find a remaining anti-pattern. A worked solution is provided.

## Resources

- Language reference (v3) — juxt.github.io/allium
- Patterns reference; parser docs in the allium-tools repo
- [Syntax cheat sheet](../cheatsheets/syntax.md)
- [Internal examples](../examples/)
- Optional remote reading after the course examples: see
  [`../examples/README.md`](../examples/README.md#optional-remote-production-examples).
