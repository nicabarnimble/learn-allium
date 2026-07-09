# Facilitator Notes — Allium Tutor

## Teaching rhythm

Use the tutor slowly. The goal is not to finish quickly; it is to let learners feel the loop:

```text
read one behavior → make one edit → run deterministic gate → explain what changed
```

For Lesson 01, do not let learners ask the agent to solve immediately. Suggested order:

1. Read the lesson.
2. Run `allium check` before editing.
3. Ask: “What is the checker telling us?”
4. Ask Pi to explain the TODO, but not edit.
5. Learner edits the rule manually.
6. Run `allium check` and `allium analyse`.
7. Only then compare the solution.

## Where students usually get stuck

- **They write implementation details.** Nudge them back to behavior: no endpoints, classes, tables, buttons, or command flags.
- **They forget the trigger source.** Remind them that events come from `surface provides:` or another rule’s emitted trigger.
- **They confuse `requires` and `ensures`.** `requires` is the precondition/case; `ensures` is the promised outcome.
- **They expect green check to mean complete behavior.** It means structurally valid. Completeness still needs review, `/weed`, and tests.
- **They let the agent edit too early.** Use “Explain slowly; do not edit yet” before asking for a smallest edit.

## Isolation expectations

Every run is disposable and lives under `.tutorial/runs/`. If a learner gets lost, use the tutor’s reset action or delete the run directory and launch again.

The isolation guard should block agent file access outside the lesson workspace and restrict shell commands to deterministic Allium checks. If a learner needs broader access, stop and discuss why before disabling guardrails.

## Collection options

For homework review, ask learners to submit one of:

- the edited `workspace/library-starter.allium` file;
- the whole `.tutorial/runs/lesson-01-<timestamp>/` directory zipped;
- a Pi session export from inside Pi using `/export lesson-01.html`.
