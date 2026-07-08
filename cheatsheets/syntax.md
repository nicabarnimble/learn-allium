# Allium v3 Syntax Cheat Sheet

One page. Examples are adapted from this repo's internal specs:
`examples/library-lending.allium`, `examples/access-grant-lifecycle.allium`,
and `examples/support-ticket-routing.allium`.

## File scaffolding

```allium
-- allium: 3                       ← version header, always line 1
-- my-spec.allium

-- Scope: one sentence
-- Includes:
--   - what this spec covers
-- Excludes:
--   - what deliberately lives elsewhere

config {
    grant_ttl: Duration = 24.hours
}

given {
    grant: AccessGrant             ← ambient instances rules refer to
}
```

## Entities & values

```allium
value AuditReference {             ← immutable data shape, no identity
    ref_id: String
    issued_at: Timestamp
}

entity AccessGrant {               ← identity + state, rules act on it
    grant_state: requested | active | denied | expired | revoked
    resource_name: String
    requested_by: String
    decided_by_operator: String?   ← optional field
    expires_at: Timestamp?

    is_active: grant_state = active
}
```

Conditional field (exists only in some states):

```allium
resolution_note: String when ticket_state = closed
```

Types: `String` `Boolean` `Integer` `Timestamp` `Duration` `List<T>` inline
enums (`a | b | c`), other value/entity types, `?` optional.

## Rules

```allium
rule GrantRevoked {
    when: OperatorRevokesGrant(operator)          ← trigger event
    requires: grant.grant_state = active          ← guards (all must hold)
    ensures: grant.grant_state = revoked          ← state effect
    ensures: grant.decided_by_operator = operator.name
    ensures: AccessGrantRevoked(grant: grant, operator: operator)  ← emitted event
}
```

- Case analysis = several rules sharing one `when`, with `requires` clauses
  partitioning the cases.
- Temporal trigger (timeout/expiry):
  `when: _: AccessGrant.expires_at <= now`
- Negation: `requires: not grant.is_active`
- Set membership: `requires: ticket.severity in {high, critical}`
- Every trigger a rule listens for must be **provided** by a surface or
  emitted by another rule's `ensures`, or check reports
  `allium.rule.unreachableTrigger`.
- Guard **state-changing** rules with direct equality (`status = x`): the
  transition tracker doesn't credit `in {}` or derived-field guards, and an
  uncredited state exit can trigger `allium.status.noExit`.
- Temporal rules that could race a state field should **observe a fact
  instead** (emit an event, don't set the field) — see
  `examples/library-lending.allium`'s `BookGoesOverdue`.

## Surfaces (who may see what, and where events come from)

```allium
surface AccessGrantControl {
    facing operator: Operator            ← an entity, or a declared actor
    context grant: AccessGrant

    exposes:                             ← ONLY these fields are visible
        operator.name
        grant.grant_state
        grant.resource_name

    provides:                            ← how events enter the system
        OperatorApprovesGrant(operator)
            when grant.grant_state = requested
        OperatorRevokesGrant(operator)
            when grant.grant_state = active
}
```

Sensitive reasoning gets its own operations-facing surface (see
`CustomerTicketStatus` vs `OperationsTriageInspection` in
`examples/support-ticket-routing.allium`). Named actors are declared
separately when identity matters:

```allium
actor ResultConsumer {
    within: Result
    identified_by: result.audit_ref
}
```

## Contracts & invariants

```allium
contract AccessGrantAuthority {
    @invariant DenyByDefault
        -- A resource-use attempt is allowed only when an active grant exists.

    @invariant ExpiredGrantsDoNotAuthorize
        -- Once expires_at passes, the grant leaves active and can no longer allow use.
}
```

`@invariant` names are stable IDs — they become obligations.

## Decisions & open questions

```allium
open question "Should premium tickets bypass the standard queue?"
-- Decision: Customer-facing messages are safe summaries.
-- Principle: Ranking can prioritize only among already-authorized options.
```

Undecided things stay visible; decisions are logged next to them.

## Top-level declarations (full v3 list, from the parser)

`entity` `rule` `enum` `value` `config` `surface` `actor` `given`
`default` `variant` `deferred` `use` `open question` `contract` `invariant`

## Smells (see Session 3 anti-patterns)

- Table names / status codes / function names in rules → implementation leak
- Button labels in surfaces → UI leak
- Behavioral requirement in a comment with no rule → prose smuggling
- Enum state no rule enters or leaves → imagined state
- Same `when`+`requires`, contradictory `ensures` → conflict by accretion
