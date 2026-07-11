# Facilitator Notes — Alliumlings

## Teaching rhythm

Use the trainer slowly. The goal is not to finish quickly; it is to let learners feel the loop:

```text
read one behavior → make one edit → run deterministic gate → explain what changed
```

Suggested order for each exercise:

1. Read the exercise file.
2. Press `Enter` before editing.
3. Ask: “What is the checker/analyser telling us?”
4. Use `2` for a hint if needed.
5. Use `3` to generate a Pi prompt only after the learner has tried to interpret the diagnostic.
6. Learner edits the spec manually.
7. Press `Enter` again and explain the change.
8. Optional: use `7` to show save-triggered rechecks if learners are editing in another terminal.
9. Move on with `4` only after the exercise passes.

## Where students usually get stuck

- **They write implementation details.** Nudge them back to behavior: no endpoints, classes, tables, buttons, or command flags.
- **They forget the trigger source.** Remind them that events come from `surface provides:` or another rule’s emitted trigger.
- **They confuse `requires` and `ensures`.** `requires` is the precondition/case; `ensures` is the promised outcome.
- **They expect green check to mean complete behavior.** It means structurally valid. Completeness still needs review, `/weed`, and tests.
- **They treat time like a user action.** Temporal observations use `_:` triggers, for example `when: _: Book.due_at <= now`.
- **They let the agent edit too early.** Use “Explain slowly; do not edit yet” before asking for a smallest edit.

## Isolation expectations

Every run is disposable and lives under `.tutorial/runs/`. If a learner gets lost, use reset (`6`) or delete the run directory and launch again.

Default Alliumlings does not launch Pi or tmux. Optional Pi helper modes still use the isolation guard to block agent file access outside the lesson workspace and restrict shell commands to deterministic Allium checks.

## Collection options

For homework review, ask learners to submit one of:

- the edited `.tutorial/runs/alliumlings-<timestamp>/workspace/exercises/*.allium` files;
- the whole `.tutorial/runs/alliumlings-<timestamp>/` directory zipped;
- if using Pi, a session export from inside Pi using `/export alliumlings.html`.
