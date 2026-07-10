# Allium Training Curriculum

A complete, self-contained training program that takes a team from Allium
beginners to proficient users and contributors. The curriculum is anchored in
this repository's own examples first; remote production specs are optional
comparative reading.

## Product direction

Allium Tutor teaches through an incident, prediction, evidence, concept,
repair, deterministic verification, consequence, and transfer case. The
application uses Kotlin Multiplatform and Compose Multiplatform, with mobile as
the baseline and desktop adding richer workbench capabilities.

Canonical guardrails:

- [PDR 0001: Story-Driven Tutoring](docs/decisions/0001-story-driven-tutoring.md)
- [ADR 0002: Kotlin Multiplatform Application Platform](docs/decisions/0002-kmp-application-platform.md)
- [Lesson lifecycle specification](layer/allium/lesson-lifecycle.allium)
- [Temporal-overdue vertical-slice storyboard](docs/storyboards/temporal-overdue.md)

The KMP application lives in [`app/`](app/). Run its shared tests with
`cd app && ./gradlew :composeApp:desktopTest`, or launch the desktop slice with
`cd app && ./gradlew :composeApp:run`.

The Allium specification remains implementation-independent. Platform choices
belong in ADRs; observable learner behaviour belongs in `layer/allium/`.

## Training Goals

- **Basics** — install Allium, use the core skills, and write/read small
  Allium specs.
- **Deep dive** — understand the language design, parser/CLI pipeline,
  analysis limits, and extension points.
- **Proficiency** — integrate Allium into normal development workflows with
  checked specs, generated obligations, tagged tests, and reviewable drift.

## Format

4–6 sessions (1–2 hours each) + hands-on labs + a capstone project. Mix of
lectures, live demos, pair programming, and individual exercises. Every
session ends with working time.

| # | Session | Length | Focus |
|---|---------|--------|-------|
| 1 | [Basics & Motivation](sessions/01-basics-and-motivation.md) | 1h | Why Allium exists; install; first distill |
| 2 | [Core Skills & Workflow](sessions/02-core-skills-and-workflow.md) | 1.5h | The Allium loop: elicit, distill, propagate, weed, tend |
| 3 | [Language Deep Dive](sessions/03-language-deep-dive.md) | 1.5–2h | v3 syntax, semantics, analysis, anti-patterns |
| 4 | [Internals & Architecture](sessions/04-internals-and-architecture.md) | 2h | Skills, CLI parser, analysis passes, extensibility |
| 5 | [Integration, Best Practices & Capstone](sessions/05-integration-and-capstone.md) | 1.5–2h | CI, obligations, scaling, capstone kickoff |
| 6 | [Contributing & Advanced Topics](sessions/06-contributing-and-advanced.md) (optional) | 1.5h | OSS workflow, custom skills, large specs |

Supporting material:

- [Facilitator guide](facilitator-guide.md) — teaching tips, pairing plan,
  assessment rubric.
- [Cheat sheets](cheatsheets/) — syntax, CLI, and the Allium loop on one page
  each.
- [Exercises](exercises/) — starter specs and worked solutions validated with
  `allium check` / `allium analyse`.
- [Internal examples](examples/) — the examples the sessions depend on.
- [Slides](slides/session-03-slides.md) — Session 3 expanded into a slide deck
  with a running library example.
- [Allium Tutor](tutorial/) — an isolated Pi-powered guided lesson runner
  for slow, hands-on practice.

## Internal example set

These are the canonical examples for this repo:

| Example | Purpose |
|---------|---------|
| [`examples/library-lending.allium`](examples/library-lending.allium) | Small state machine, surfaces, temporal observation. |
| [`examples/access-grant-lifecycle.allium`](examples/access-grant-lifecycle.allium) | Authority decisions, grant expiry/revocation, deny-by-default. |
| [`examples/pinned-installer.allium`](examples/pinned-installer.allium) + [`examples/code/pinned-installer.sh`](examples/code/pinned-installer.sh) | Distill-from-code demo with platform and checksum behavior. |
| [`examples/support-ticket-routing.allium`](examples/support-ticket-routing.allium) | Product-map style example with safe customer projection vs operations inspection. |
| [`examples/ci/github-actions-allium.yml`](examples/ci/github-actions-allium.yml) | Copyable CI gate for examples and exercise solutions. |

Validate the bundle:

```bash
allium check examples
for spec in examples/*.allium; do allium analyse "$spec"; done
```

## Optional remote examples

Patina and patina-mct provide optional production-scale case studies for
learners who are comfortable with the internal examples:

- `patina/layer/allium/mother/*.allium` — lifecycle specs and obligation-plan
  drift checks.
- `patina-mct/layer/allium/mct-product-map.allium` — a large product-map spec
  with decision-log entries, open questions, contracts, and safe projections.

## Prerequisites

Before Session 1, each attendee should have:

1. A working Rust toolchain (`rustup` + `cargo`).
2. This repository cloned.
3. Claude Code or another agent harness with the Allium skills available.

## Installing the toolchain

Two halves: the **skills** (agent-side) and the **CLI** (analysis-side).

```bash
# Skills (pick the flow that matches your harness)
npx skills add juxt/allium

# CLI (pick one)
brew tap juxt/allium && brew install allium   # macOS
cargo install --locked allium-cli
```

Verify:

```bash
allium --version           # course material validated against 3.5.0
allium check examples
```

Start the terminal Alliumlings practice:

```bash
./learn-allium
```

This opens exercises in the current terminal. Pi help is optional: choose “Ask Pi” for an inline answer when Pi and the Allium skill are available; otherwise the tutor shows a copyable fallback prompt. Use menu option `7` or `./learn-allium --auto-watch` for save-triggered rechecks, or run the optional two-pane mode:

```bash
./learn-allium tmux
```

Choose a faster model and reasoning level for Pi help:

```bash
pi --list-models
./learn-allium --model openai-codex/gpt-5.4-mini --thinking low
```

The tutor inherits your normal Pi model when `--model` is omitted and defaults tutor requests to `--thinking low`. The same flags work with `./learn-allium tmux`, `lesson-01`, and `pi`.

For setup checks only:

```bash
./learn-allium doctor
```

Terminal lesson modes:

```bash
./learn-allium lesson-01   # guided single-lesson tutor
./learn-allium pi          # embedded-in-Pi tutor
./learn-allium tmux        # two-pane Alliumlings + Pi helper
```

## Assessment

Completion = a reviewed spec + a small contribution. See the
[facilitator guide](facilitator-guide.md#assessment) for the rubric.
