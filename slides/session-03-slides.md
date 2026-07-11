# Session 3 — Slide Deck: Allium, From a Sticky Note to the Deep End

Full slides + speaker notes for the language deep dive. 36 slides,
90–120 minutes. One running example (a library book) is grown slide by slide
from a single English sentence into a checked, analysed,
obligation-generating spec. Larger shapes are then shown with the repo's own
internal examples: access grants, support ticket routing, and the pinned
installer.

**Every required snippet is local to this repo.** The running example is
[`../examples/library-lending.allium`](../examples/library-lending.allium).
The examples check and analyse clean with `allium 3.5.0`.

**Legend** — each slide has three parts:
- **SLIDE**: what's on screen.
- **SAY**: speaker script.
- **DO** (some slides): a live command to run.

Timing marks (⏱) are cumulative targets for a 100-minute run.

---

## Act I — Extremely simple (slides 1–9) · one sticky note

---

### Slide 1 — Title ⏱ 0:00

**SLIDE**

> **Allium: from a sticky note to the deep end**
> One behavior, one hour, five levels deeper each time.

**SAY**
Today has one trick, repeated: take a behavior you could write on a sticky
note, then keep asking "what exactly do you mean by that?" Every time the
sticky note can't answer, Allium gives us one more construct.

---

### Slide 2 — A behavior in English ⏱ 0:02

**SLIDE**

> *"A member can borrow a book if it's on the shelf."*
>
> Simple. Done. Ship it?

**SAY**
What happens if the book is already borrowed — error, queue, silence? Who is
recorded as having it? For how long? What happens when time runs out? Prose
hides decisions. We want a medium that argues back.

---

### Slide 3 — The same sentence in Allium ⏱ 0:05

**SLIDE**

```allium
rule BorrowBook {
    when: MemberBorrowsBook(member)
    requires: book.status = on_shelf
    ensures: book.status = borrowed
}
```

**SAY**
This is the language in miniature. *When* this happens, *provided* this is
true, *ensure* this becomes true. Trigger, guard, effect.

---

### Slide 4 — Anatomy of a rule ⏱ 0:08

**SLIDE**

```text
rule <Name> {
    when:     <the event that triggers it>
    requires: <guards — ALL must hold>
    ensures:  <effects — state changes + emitted events>
}
```

**SAY**
`requires` lines are ANDed. Case analysis means several rules with the same
trigger. `ensures` can change state or emit an event.

---

### Slide 5 — Where does `book.status` live? Entities. ⏱ 0:11

**SLIDE**

```allium
entity Book {
    status: on_shelf | borrowed
    title: String
}

entity Member {
    name: String
}
```

**SAY**
Entities are nouns with identity and state. The inline enum is a state
machine waiting to happen.

---

### Slide 6 — Second rule: a state machine appears ⏱ 0:14

**SLIDE**

```allium
rule ReturnBook {
    when: MemberReturnsBook(member)
    requires: book.status = borrowed
    ensures: book.status = on_shelf
}
```

```text
        BorrowBook ──────►
on_shelf              borrowed
        ◄────── ReturnBook
```

**SAY**
Allium v3 state machines are usually an enum state field plus one rule per
edge. The access-grant example uses the same pattern with requested, active,
denied, expired, and revoked.

---

### Slide 7 — What we did NOT say ⏱ 0:17

**SLIDE**

> Nothing about: a database · a `books` table · an HTTP endpoint · a service
> class · a button
>
> **Behavioral specification: WHAT, not HOW.**

**SAY**
The spec owns intent; the code owns mechanism. Litmus test: could we swap the
storage engine without editing this line? If no, it doesn't belong in the
spec.

---

### Slide 8 — The tool, first contact ⏱ 0:20

**SLIDE**

```bash
$ allium check examples/library-lending.allium
{ "diagnostics": [], "findings": [] }     # exit 0
```

**DO**
Run it live on `examples/library-lending.allium`.

**SAY**
After every edit, `allium check`. Fast, JSON, deterministic.

---

### Slide 9 — Act I recap ⏱ 0:22

**SLIDE**

> - `entity` — nouns with state
> - `a | b` enum field — allowed states
> - `rule` = `when` / `requires` / `ensures`
> - `allium check` — the metronome

**SAY**
With these four ideas you can already read the small examples in this repo.
The rest is what happens when reality asks more questions.

---

## Act II — The language grows with you (slides 10–18)

---

### Slide 10 — "What if the book is already out?" Case analysis. ⏱ 0:25

**SLIDE**

```allium
rule BorrowRefusedWhenTaken {
    when: MemberBorrowsBook(member)
    requires: not book.is_available
    ensures: BorrowRefused(book: book, member: member, reason: "not-on-shelf")
}
```

**SAY**
Same trigger, different guard. Refusal is explicit — not the absence of a
rule. This is the same deny-by-default shape used by the access-grant spec.

---

### Slide 11 — "Who has it?" Attribution and optional fields. ⏱ 0:29

**SLIDE**

```allium
entity Book {
    status: on_shelf | borrowed
    borrowed_by: String?
    due_at: Timestamp?
}

ensures: book.borrowed_by = member.name
ensures: book.borrowed_by = none
```

**SAY**
The `?` marks fields that don't always exist. The spec forces you to decide
the whole lifecycle of a fact, not just its creation.

---

### Slide 12 — "For how long?" Config. ⏱ 0:32

**SLIDE**

```allium
config {
    loan_period: Duration = 21.days
}

ensures: book.due_at = now + config.loan_period
```

**SAY**
Numbers with names. Durations live in `config` because policy changes should
not require rediscovering behavior.

---

### Slide 13 — "And when time runs out?" Temporal rules. ⏱ 0:36

**SLIDE**

```allium
rule BookGoesOverdue {
    when: _: Book.due_at <= now
    requires: book.status = borrowed
    ensures: BookOverdue(book: book, borrower: book.borrowed_by)
}
```

**SAY**
Time itself is the trigger. The rule emits a fact; it does not fight the
return path for the `status` field.

---

### Slide 14 — "Says who?" Where events come from: surfaces. ⏱ 0:41

**SLIDE**

```allium
surface LibraryDesk {
    facing member: Member
    context book: Book

    provides:
        MemberBorrowsBook(member)
        MemberReturnsBook(member)
            when book.status = borrowed
}
```

**DO**
Delete a `provides:` line, run `allium check`, show the diagnostic, undo.

**SAY**
A surface is a door into the system. No door means
`allium.rule.unreachableTrigger`.

---

### Slide 15 — "Who may SEE what?" Exposure. ⏱ 0:45

**SLIDE**

```allium
surface LibraryDesk {
    exposes:
        member.name
        book.status
        book.title
        book.due_at
        -- absent: book.borrowed_by
}
```

**SAY**
`exposes` is an allowlist. The support-ticket example uses this for security:
customers see safe summaries; operations sees internal triage reasons.

---

### Slide 16 — Nouns that aren't things: `value` vs `entity` ⏱ 0:48

**SLIDE**

```allium
value AuditReference {
    ref_id: String
    issued_at: Timestamp
}
```

> `entity` = identity + state; `value` = immutable data shape.

**SAY**
Declare values for data that is carried, not tracked. This keeps entities
from becoming god objects.

---

### Slide 17 — Naming predicates: derived fields ⏱ 0:51

**SLIDE**

```allium
entity Book {
    status: on_shelf | borrowed
    is_available: status = on_shelf
}

requires: not book.is_available
```

**SAY**
Derived fields keep rules legible. Sharp edge: the transition tracker doesn't
see through them for status exits, so state-changing rules should still use
direct equality guards.

---

### Slide 18 — Naming promises: contracts ⏱ 0:55

**SLIDE**

```allium
contract LendingHonesty {
    @invariant OneBorrowerAtATime
        -- A borrowed book is attributed to exactly one member until returned.

    @invariant RefusalIsExplicit
        -- Borrowing an unavailable book produces a refusal event, not silence.
}
```

**SAY**
Named invariants give reviewers, agents, and tests a stable handle for the
promise.

---

## Act III — The tool pushes back (slides 19–24)

---

### Slide 19 — The tool catches `status.noExit` ⏱ 1:00

**SLIDE**

```text
allium.status.noExit
Status 'overdue_flagged' in entity 'Book' has no observed transition out.
```

**SAY**
If you add a third `status` state but never give it a direct exit, the tool
objects. It credits exits when guards name the state directly.

---

### Slide 20 — The tool catches a conflict finding ⏱ 1:05

**SLIDE**

```json
{
  "type": "conflict",
  "rule_a": "ReturnBook",
  "rule_b": "BookGoesOverdue",
  "state": "borrowed",
  "values": { "ReturnBook": "on_shelf", "BookGoesOverdue": "overdue_flagged" }
}
```

**SAY**
If overdue also sets `status`, return and timeout can race. `analyse` finds
cross-trigger conflicts like this.

---

### Slide 21 — The fix: observe, don't race ⏱ 1:09

**SLIDE**

```allium
rule BookGoesOverdue {
    when: _: Book.due_at <= now
    requires: book.status = borrowed
    ensures: BookOverdue(book: book, borrower: book.borrowed_by)
}
```

**SAY**
Temporal facts often should be observations, not competing state transitions.
The state machine stays boring; time produces facts.

---

### Slide 22 — What stays silent ⏱ 1:14

**SLIDE**

```allium
requires: book.staet = borrowed     -- typo: no diagnostic in 3.5.0

rule ApprovePlugin       { ... ensures: plugin.state = approved }
rule ApprovePluginStrict { ... ensures: plugin.state = blocked  }
```

**DO**
Typo a field name in a `requires`, run check, show the silence, undo.

**SAY**
Green CLI ≠ correct spec. The loop has three nets: CLI, `/weed`, and human
review. Every silence is also a possible upstream diagnostic.

---

### Slide 23 — Why not full formal methods? ⏱ 1:18

**SLIDE**

> Allium sits between prose that checks **nothing** and TLA+/Alloy that many
> teams **won't write**.

**SAY**
Allium trades proofs for fast checks, explainable findings, and specs that
humans and LLMs can read and write.

---

### Slide 24 — The finished sticky note ⏱ 1:21

**SLIDE**

> `examples/library-lending.allium` — 2 entities · 4 rules · 1 temporal rule ·
> 1 surface · 1 contract · config
>
> `check` ✅ `analyse` ✅ `plan` → obligations

**DO**
Run `allium check`, `allium analyse`, and `allium plan` on the library spec.

**SAY**
The sticky note is now a machine-checked contract with traceable test
obligations.

---

## Act IV — The same shapes in local larger examples (slides 25–30)

---

### Slide 25 — Nothing new, just another domain: access grants ⏱ 1:25

**SLIDE**

```text
LIBRARY                         ACCESS GRANT
on_shelf | borrowed             requested | active | denied | expired | revoked
BorrowBook / ReturnBook         GrantApproved / GrantRevoked / GrantExpires
due_at + BookGoesOverdue        expires_at + GrantExpires
LibraryDesk provides            AccessGrantControl + ResourceUse provide
```

**SAY**
Same language, different stakes. Access grants add authority decisions and a
deny-by-default refusal path.

---

### Slide 26 — The spec as decision log ⏱ 1:29

**SLIDE**

```allium
open question "Should premium tickets bypass the standard queue?"
-- Decision: Customer-facing messages are intentionally safe summaries.
-- Decision: Operations may inspect internal triage reasons; customers may not.
```

**SAY**
Undecided things are visible, not silently absent. Decisions are diffed like
code.

---

### Slide 27 — Exposure at scale: who learns what from triage ⏱ 1:34

**SLIDE**

```text
CustomerTicketStatus             OperationsTriageInspection
  ticket_state                     ticket_state
  customer_message                 triage_reason
  audit_ref                        category, severity, assignment
```

**SAY**
Same underlying ticket, two exposure contracts. The spec separates customer-
safe projection from internal reasoning.

---

### Slide 28 — Four patterns to steal ⏱ 1:38

**SLIDE**

> 1. **Deny by default** — absence of authority becomes an explicit refusal
> 2. **Terminal result** — denied/expired/revoked do not loop back silently
> 3. **Safe projection** — requester view and operator view are separate
> 4. **Observe, don't race** — temporal facts as events

**SAY**
These patterns are reusable across product domains. Reach for them before
inventing a new shape.

---

### Slide 29 — From spec to test suite: the obligation pipeline ⏱ 1:43

**SLIDE**

```bash
allium plan examples/access-grant-lifecycle.allium \
  > examples/access-grant-lifecycle.allium.plan.json
```

```rust
// obligation id: rule-success.GrantApproved
#[test]
fn approved_grant_allows_resource_use() { /* ... */ }
```

**SAY**
Traceability becomes grep-able: spec construct → obligation → test → CI.

---

### Slide 30 — CI: the loop, mechanized ⏱ 1:46

**SLIDE**

```bash
allium check examples
find exercises -path '*/solution/*.allium' -print0 | xargs -0 -n1 allium check
```

> Optional: regenerate committed plans and fail on `git diff`.

**SAY**
CI should run the deterministic parts. The probabilistic parts — elicit,
weed, review — live with agents and humans.

---

## Act V — The deep end (slides 31–36)

---

### Slide 31 — One parser, three consumers ⏱ 1:50

**SLIDE**

```bash
allium parse examples/library-lending.allium \
  | jq '..|objects|select(.name?=="BorrowBook")'
```

> Parser → AST with byte-offset `source_span`s → diagnostics, plan, LSP.

**SAY**
When tooling behavior surprises you, trace parse → AST → passes → JSON.

---

### Slide 32 — The analysis passes, honestly mapped ⏱ 1:54

**SLIDE**

> Observed on 3.5.0:
> ✅ unused entity/field · ✅ unreachable trigger · ✅ status no-exit ·
> ✅ cross-trigger conflicts
> ❌ guard typos · ❌ same-trigger contradictions · ❌ prose invariants

**SAY**
This is not a scandal; it's a contribution map.

---

### Slide 33 — Extending it: three tiers, cheapest first ⏱ 1:58

**SLIDE**

> 1. **Markdown** — new skills, team patterns
> 2. **Rust** — new diagnostics/analysis passes
> 3. **Integration** — consume `model`/`plan` JSON in review bots or release gates
>
> Proposal format: defect → triggering snippet → expected finding JSON

**SAY**
The Session 4 proposal format is already the seed of an upstream issue.

---

### Slide 34 — The whole ladder, one slide ⏱ 2:01

**SLIDE**

> 1. Sentence
> 2. Rule
> 3. Machine: states, cases, time, doors, exposure
> 4. Contract: named invariants, obligations, tagged tests
> 5. System: decision-log specs, safe projections
> 6. Toolchain: parser → AST → passes → JSON → extension

**SAY**
Same behavior, six altitudes.

---

### Slide 35 — Now you: the refactor exercise ⏱ 2:03

**SLIDE**

> `exercises/03-spec-refactor/starter/plugin-approval.allium`
>
> Nine planted flaws. The CLI finds three. You find six.

**SAY**
Run check first, then spend human attention where the machine is blind.

---

### Slide 36 — Resources ⏱ end

**SLIDE**

> - `examples/library-lending.allium` — today's example
> - `examples/access-grant-lifecycle.allium` — authority + expiry
> - `examples/support-ticket-routing.allium` — safe projection + decisions
> - Cheat sheets: `cheatsheets/syntax.md` · `cheatsheets/cli.md`
> - Language reference (v3): juxt.github.io/allium · allium-tools repo
> - Optional remote reading: see `examples/README.md`

**SAY**
Homework before Session 4: do the refactor exercise and bring your six-flaw
list. Optional: skim remote production specs only after the local examples
are comfortable.

---

## Appendix — facilitator crib

- **Timing pressure points**: Act II slide 14 and Act III slides 19–22 are
  where a 90-minute run slips. Do not cut the live findings.
- **Demo prep**: have `examples/library-lending.allium`,
  `examples/access-grant-lifecycle.allium`, and
  `examples/support-ticket-routing.allium` open in splits; terminal with
  `PATH` including allium 3.5.0.
- **If a demo surprises you** (new CLI version, different diagnostics): say
  so and explore live. Update the deck afterward; slide 32 is version-stamped
  for exactly this reason.
