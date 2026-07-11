# Storyboard: The Notice That Would Not Stop

- **Lesson:** Temporal overdue behaviour
- **Lifecycle:** `layer/allium/lesson-lifecycle.allium`
- **Voice:** `docs/decisions/0001-story-driven-tutoring.md`
- **Platform:** `docs/decisions/0002-kmp-application-platform.md`

This is the first vertical slice. It is deliberately one paced lesson, not a catalogue, dashboard, or reusable component showcase.

## Beat 1 — Incident

**Primary object:** the consequence.

> At 08:01, Mara received an overdue notice.
>
> At 08:02, she received another.
>
> By noon, the library had sent 240 of them—one for every minute the book remained late.

The learner may only continue to investigation. No Allium vocabulary is introduced.

## Beat 2 — Prediction

**Primary object:** the learner's current model.

Ask what they would inspect first and what they expect to find. The learner must record a non-empty prediction before evidence is available.

## Beat 3 — Evidence

**Primary object:** the temporal condition and its persistence.

```allium
when: _: Book.due_at <= now
```

Pair the persistent condition with the repeated `BookOverdue` events at 08:01, 08:02, and noon. Preserve the learner's prediction nearby as a quiet comparison, not a correctness score.

## Beat 4 — Concept

**Primary object:** the new distinction.

Name **temporal guards** only now. Explain:

- the temporal trigger makes a rule eligible;
- a borrowed-state guard limits that eligibility; and
- the overdue state change makes the guard false after one successful evaluation.

## Beat 5 — Workbench

**Primary object:** one incomplete rule.

The learner edits a small Allium fragment. Verification checks for:

- the temporal trigger;
- the borrowed-state guard;
- the resulting overdue state; and
- the `BookOverdue` event that drives the notice.

Failure preserves the learner's draft and returns focused evidence. It never inserts the solution.

## Beat 6 — Consequence

**Primary object:** the opening incident after repair.

Return to Mara. One notice is produced; the date condition remains true, but the changed state makes the rule ineligible.

Conclude with the deeper structural point: the notification service was not the problem. The behavioural description was incomplete.

## Beat 7 — Transfer

**Primary object:** the same causal model in a new domain.

An access grant reaches its expiry timestamp. The learner supplies the temporal condition, active-state guard, and expired result without copying library nouns.

Completion requires deterministic transfer evaluation.

## Capability adaptation

### Compact — phone

- One beat per screen.
- Story and evidence use a single readable column.
- Workbench edits only the essential fragment.
- Primary action remains reachable above the software keyboard when possible.
- No permanent navigation or progress dashboard.

### Comfortable — tablet and browser

- Preserve the same beat order.
- Allow wider evidence and editor measures.
- The learner's prediction may sit beside evidence when width permits.
- Do not expose future beats merely because space is available.

### Workbench — desktop

- Keep story beats focused and column-limited.
- Give the editor more vertical space and full keyboard behaviour.
- Later slices may add local files, real `allium check`, and repository context.
- Additional capability must not turn every stage into a multi-pane dashboard.

## Acceptance observations

- Concept terminology is absent from incident and prediction.
- Evidence cannot be reached without a recorded prediction.
- Failed repair and transfer attempts preserve learner input.
- Passing syntax alone is insufficient.
- Consequence cannot appear before a passing repair.
- Completion cannot occur before a passing transfer.
- The same lesson state drives compact and expanded layouts.
