# PDR 0001: Story-Driven Tutoring

- **Type:** Product Decision Record
- **Status:** Accepted
- **Date:** 2026-07-10

## Context

The tutor teaches before and while it tests. Each lesson gives the learner a concrete failure, evidence to investigate, vocabulary when it becomes useful, a focused behavioural repair, and a related transfer case.

Authored lessons and deterministic checks provide the learning spine. Optional AI assistance helps the learner inspect evidence and reason about a repair without deciding mastery.

The narrative references are Interface Studies and Benn Jordan. They are references for structure and teaching relationship, not voices to imitate literally.

## Decision

The tutor will use a consistent narrative voice:

> A curious, technically serious investigator with dry humor and no patience for vague behaviour.

The voice combines two complementary influences:

- **Interface Studies provides the narrative structure:** concrete cold opens, evidence before terminology, unfolding mystery, calm precision, callbacks, and a structural conclusion deeper than the immediate repair.
- **Benn Jordan provides the teaching relationship:** first-person curiosity, dry humor, visible incorrect assumptions, question-led discovery, honest changes of direction, and a sense that learner and teacher are investigating together.

Authors apply both influences qualitatively according to the needs of each lesson.

Every foundational lesson follows this learning lifecycle:

1. **Incident** — open with an observable event and consequence, never a learning objective.
2. **Prediction** — ask the learner to commit to an interpretation before explanation.
3. **Evidence** — reveal the relevant artifact, trace, requirement, or diagnostic.
4. **Concept** — name and explain the concept only after the learner has felt its absence.
5. **Workbench** — guide one focused repair without silently replacing the learner's work.
6. **Verification** — apply deterministic checks that test lesson intent, not syntax alone.
7. **Consequence** — return to the opening incident and show how behaviour changed.
8. **Transfer** — test the same concept in a related but meaningfully different situation.

## Voice guardrails

1. Open with an event, not a syllabus statement.
2. Show the reasonable mistake before correcting it.
3. Do not introduce specialist vocabulary before the evidence creates a need for it.
4. Let evidence and consequences create tension; do not manufacture suspense.
5. Humor comes from contradictions in the system, never from mocking learners or affected people.
6. The narrator may be wrong, but must be candid about the evidence and the correction.
7. Every story detail must contribute to the learner's causal model. No decorative lore.
8. Return to the opening incident after verification.
9. Require a learner action at least every one or two presentation beats.
10. Present diagnostics as evidence, not as punishment or a school grade.

## Experience guardrails

- The experience is a paced stage, not a dashboard.
- Each beat has one primary object: story, question, evidence, explanation, work, or consequence.
- Mobile is the baseline experience; larger screens extend capability rather than merely scaling the mobile layout.
- Phone lessons favour story, prediction, evidence, and short repairs.
- Tablet and desktop may provide richer evidence and editing workspaces.
- Desktop may add local files, repositories, command-line tooling, and advanced coaching.
- Progress remains quiet and factual. Avoid confetti, streak pressure, and certification claims without human review.
- Authored narrative and deterministic evaluation form the canonical lesson. AI may coach around that spine but may not invent required behaviour or decide completion by itself.

## Testing guardrail

A passing language check does not prove mastery. Foundational lessons should distinguish:

1. language validity;
2. structural analysis;
3. lesson-intent evaluation; and
4. transfer to a related situation.

## Consequences

- Lessons require authored incident, evidence, concept, consequence, and transfer material in addition to starter and solution files.
- Content quality becomes a first-class engineering concern and must be reviewed like code.
- The engine must preserve authored pacing while allowing platform-appropriate presentation.
