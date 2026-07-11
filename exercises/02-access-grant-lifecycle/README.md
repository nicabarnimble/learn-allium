# Exercise 02 — Access Grant Lifecycle (Session 2)

Build a small end-to-end feature using **only** the Allium-driven flow.
Domain: a generic access grant — a requester may use a protected resource
only while an operator-approved, unexpired, unrevoked grant exists.

## The flow (no code before the spec is green)

1. **Spec** — complete `starter/access-grant-lifecycle.allium` (four TODOs:
   expiry, revocation, deny-by-default, contract invariants).
2. **Gate** — `allium check` and `allium analyse` both clean.
3. **Plan** — `allium plan access-grant-lifecycle.allium > access-grant-lifecycle.allium.plan.json`;
   skim the obligation IDs.
4. **Tests** — write at least three tests in your language of choice,
   tagged by obligation ID:

   ```rust
   // obligation id: rule-success.GrantExpires
   #[test]
   fn active_grant_expires_after_ttl() { /* ... */ }
   ```

5. **Implement** — the smallest thing that makes the tests pass (an
   in-memory state machine is fine; this is about the flow, not the code).
6. **Weed** — run `/weed` between your spec and implementation; fix drift.

## Behavioral requirements (what the TODOs must capture)

- An approved grant becomes `active` with an expiry `grant_ttl` from now.
- An active grant `expires` when its deadline passes (temporal rule).
- An operator can `revoke` an active grant; revocation is attributed.
- Resource use is **allowed only against an active grant**; every other
  state refuses with a reason. Deny is the default, not an error case.
- `denied`, `expired`, and `revoked` are terminal — re-access requires a new
  grant request.

## Hints

- The temporal trigger shape is in `examples/library-lending.allium`:
  `when: _: Book.due_at <= now`.
- Triggers must be **provided** by a surface (`provides:` with optional
  `when` guards) or the check reports `allium.rule.unreachableTrigger` —
  TODO(2b) exists for a reason.
- The deny rule wants the derived field: `requires: not grant.is_active`.

## Done when

- `check` + `analyse` clean, plan generated, ≥3 obligation-tagged tests
  passing, weed run clean. Bring all four artifacts to Session 3.
