# Facilitator Guide

How to run this curriculum well. Audience: whoever is teaching or organizing
the sessions.

## Principles

- **Progressive hands-on.** Every session ends with coding time. If a session
  runs long, cut lecture, never the exercise.
- **Pairing.** Junior + senior pairs for the distill/elicit exercises and the
  capstone. Seniors carry codebase context; juniors carry fresh eyes —
  elicitation benefits from both. Keep pairs stable across sessions so
  capstone pairs already have a working rhythm.
- **Self-contained anchor.** Use this repo's internal examples first:
  `library-lending`, `access-grant-lifecycle`, `pinned-installer`, and
  `support-ticket-routing`. Remote production specs can enrich discussion,
  but they should not be prerequisites.
- **Deterministic gates, always.** Model the habit yourself: every demo ends
  with `allium check`/`analyse` on screen, green.

## Logistics

- Cadence: weekly or twice-weekly; capstone spans the 1–2 weeks after Session
  5.
- Room setup: projector for demos, everyone with a laptop, this repo cloned,
  and the Allium CLI installed. Verify installs at the *start* of Session 1,
  not the end.
- Session 6 is optional; run it if at least half the team finished a
  capstone.

## Per-session facilitator notes

| Session | Watch out for |
|---------|---------------|
| 1 | Install failures eat the hour. Have the cargo fallback ready (`cargo install --locked allium-cli`) and pair anyone blocked. |
| 2 | Pairs over-polishing the elicited spec. Timebox: check-clean beats beautiful. The break-it/weed-it segment is the retention moment — don't cut it. |
| 3 | The syntax segment can become a lecture wall. Keep flipping to local specs; make attendees find the construct in `examples/support-ticket-routing.allium` themselves. |
| 4 | Depth mismatch: Rust-comfortable attendees want parser internals, others want skills. The split agenda (skills §2 vs parser §3) is deliberate — you can weight it per audience. |
| 5 | Capstone scoping. Reject scopes that are too big in the room, before work starts. A good capstone is one spec ≤ ~150 lines + a handful of tagged tests. |
| 6 | Keep contribution proposals concrete: snippet + expected finding JSON, or it's a wish, not a proposal. |

## Materials to share (before Session 1)

- This directory (curriculum, cheat sheets, exercises, internal examples).
- A curated branch/fork with organization-specific examples if teaching
  outside this repo.
- The spec-driven engineering demo video (optional pre-watch).
- Links: main README, https://juxt.github.io/allium/, language reference
  (v3), patterns reference.

## Assessment

Completion requires both:

1. **Spec review (pass/revise).** The capstone spec, reviewed live by one
   facilitator + one peer from another pair. Pass bar:
   - `allium check` and `allium analyse` clean;
   - scope block with honest Excludes;
   - no anti-patterns from Session 3 (reviewer hunts for 5 minutes);
   - at least one contract with named invariants;
   - `plan.json` generated/committed if the target repo adopts plans and ≥3
     obligations tagged in tests.
2. **Small contribution (one of):**
   - fix or add a pattern in the patterns library;
   - a new diagnostic/analysis rule proposal written to the Session 4
     standard (defect + triggering snippet + expected finding), or an
     implemented one;
   - document a workflow (e.g. write up the obligation-tagging pipeline for
     the allium docs);
   - a team-local skill improvement with a before/after transcript.

Record outcomes wherever the team tracks training; a one-line note in the
capstone PR description is enough.

## Adapting the examples to another stack

The session structure is stack-agnostic. To retarget (e.g. web/frontend
team): swap the internal examples for a UI-adjacent state machine
(session/auth flow), a code-distill script, and one larger safe-projection
example. Keep the local examples in this repo as the baseline so the course
remains runnable without external repos.
