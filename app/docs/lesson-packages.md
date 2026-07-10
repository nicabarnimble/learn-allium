# Lesson packages

Each lesson is a versioned JSON package loaded from shared Compose resources. The package supplies authored presentation copy and deterministic evaluation requirements; Kotlin supplies lifecycle enforcement, state transitions, validation, and rendering.

## Package structure

- `formatVersion` selects the supported package schema.
- `revision` identifies the authored lesson revision used by saved learner state.
- `lesson` contains every authored lifecycle beat and learner-facing label.
- `evaluation.repair` defines deterministic repair requirements and failure feedback.
- `evaluation.transfer` defines the equivalent requirements for the transfer case.

Long-form copy uses `AuthoredText`:

```json
{
  "paragraphs": [
    "The first paragraph.",
    "The second paragraph."
  ]
}
```

Paragraphs render with a blank line between them. Newlines inside one paragraph remain single line breaks.

## Evaluation

Each `requiredFragments` entry has a stable `id` and a normalized Allium source fragment. Evaluation lowercases learner input, collapses whitespace, and requires every fragment to be present.

This is lesson-intent evaluation for a focused fragment. Parser-backed Allium validation remains a separate gate.

## Validation

Package decoding rejects:

- unsupported format versions or invalid revisions;
- missing lifecycle copy or labels;
- invalid lesson identifiers;
- empty or duplicate evaluation requirements;
- workbench starters without an Allium rule and trigger;
- unbalanced starter braces; and
- starters that already satisfy every repair requirement.

Run package and lifecycle tests with:

```bash
cd app
./gradlew :composeApp:desktopTest
```

Launch the authored package at desktop size with:

```bash
./gradlew :composeApp:run
```
