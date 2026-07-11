# Agent Instructions

## Product Guardrails

Before changing the learner experience, course model, or application platform:

1. Read `docs/decisions/0001-story-driven-tutoring.md`.
2. Read `docs/decisions/0002-kmp-application-platform.md`.
3. Read and preserve the behavioural contract in `layer/allium/lesson-lifecycle.allium`.
4. For the first vertical slice, read `docs/storyboards/temporal-overdue.md`.

Product application work follows the accepted Kotlin Multiplatform direction unless a superseding ADR is accepted. The Bash Alliumlings loop supports terminal exercises and deterministic CLI checks.

Documentation describes the present system and intended path. Do not leave migration narration, correction history, or phrases such as “now”, “previously”, and “we changed” in maintained guidance. Git carries change history; decision records retain only rationale necessary for current architectural constraints.

Keep concerns on the correct decision surface:

- Product voice, pacing, and learning principles belong in the PDR.
- Technology and source-set choices belong in ADRs.
- Observable learner behaviour belongs in `layer/allium/`.
- Do not leak KMP, Compose, widgets, or platform APIs into Allium specifications.

After changing `.allium` files, run `allium check layer/allium`. At behavioural checkpoints, also run `allium analyse layer/allium`.

The Kotlin Multiplatform application lives in `app/`. Run shared desktop tests with `cd app && ./gradlew :shared:desktopTest`. Verify the shared iOS framework with `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`.

## Working Principles (Binding)

Favor strong invariants over defensive fallbacks. Make bad states impossible where practical. Do not add complexity to paper over unclear design. Prefer simple data models, explicit contracts, and shared logic over local patches, duplicated code, or speculative abstractions — extend the ONE sink concept from slice 2 rather than inventing a parallel mechanism, but do not build a speculative generic observation framework either. Write Rust code that Jon Gjengset would agree with. Always read code before writing code. Git update with scalpel as you work, not with shotgun after. Kernel decides, adapters perform. Fail closed: if an authority-critical fact cannot be made durable, the effect it licenses does not proceed; discarded write errors are the fail-open version of this bug. Failing test first. New code lands in the decomposed modules — do not regrow `main.rs`. Stop at a task boundary if context runs low.

Add project-specific notes outside the Patina block.

<!-- PATINA:START -->
## Patina

This is the canonical Patina instruction surface for this project. Vendor shim files such as `GEMINI.md` or `CLAUDE.md` should point here instead of duplicating the main Patina payload.

## Where to Start

- Read `layer/` before non-trivial changes.
- Use `patina --help` to discover command surfaces.
- Use `patina context`, `patina scry`, and `patina assay` when MCP is not available.

## Environment

- **Platform**: macos (aarch64)
- **Directory**: /Users/nicabar/Projects/Patina/learn-allium
- **Tools**: cargo, git, docker

## Runtime Truth

Use the section matching the current runtime. Do not assume MCP exists unless that runtime section says it is configured.

### Claude Code

Patina MCP is not configured for this runtime. Do not assume `context`, `scry`, `assay`, `session.*`, or `spec.*` MCP tools exist here.

Use CLI/native fallbacks instead:
- Discovery: `patina context`, `patina scry`, `patina assay`
- Specs: `patina spec show <id>`, `patina spec check <id> --json`, `patina spec next`
- Sessions: `.claude/bin/session-new.sh "<title>"`
- Sessions: `.claude/bin/session-update.sh`
- Sessions: `.claude/bin/session-note.sh "<note>"`
- Sessions: `.claude/bin/session-end.sh`


### OpenCode

Patina MCP is not configured for this runtime. Do not assume `context`, `scry`, `assay`, `session.*`, or `spec.*` MCP tools exist here.

Use CLI/native fallbacks instead:
- Discovery: `patina context`, `patina scry`, `patina assay`
- Specs: `patina spec show <id>`, `patina spec check <id> --json`, `patina spec next`
- Sessions: `.opencode/bin/session-new.sh "<title>"`
- Sessions: `.opencode/bin/session-update.sh`
- Sessions: `.opencode/bin/session-note.sh "<note>"`
- Sessions: `.opencode/bin/session-end.sh`


### Gemini CLI

Patina MCP is not configured for this runtime. Do not assume `context`, `scry`, `assay`, `session.*`, or `spec.*` MCP tools exist here.

Use CLI/native fallbacks instead:
- Discovery: `patina context`, `patina scry`, `patina assay`
- Specs: `patina spec show <id>`, `patina spec check <id> --json`, `patina spec next`
- Sessions: `.gemini/bin/session-new.sh "<title>"`
- Sessions: `.gemini/bin/session-update.sh`
- Sessions: `.gemini/bin/session-note.sh "<note>"`
- Sessions: `.gemini/bin/session-end.sh`


Setup snapshots unmanaged conflicting files under `.patina/local/backups/` before takeover. Reruns refresh only the Patina-managed block unless `patina ai setup --force` is used.

*Generated by Patina*
<!-- PATINA:END -->
