# ADR 0002: Kotlin Multiplatform Application Platform

- **Type:** Architecture Decision Record
- **Status:** Accepted
- **Date:** 2026-07-10

## Context

The product is a reusable platform for story-driven technical training. Phones and tablets are primary delivery platforms. Desktop extends the same lessons with keyboard input, larger workspaces, local files, repositories, and command-line integrations.

Kotlin is a deliberate product-platform and engineering investment. The platform must remain understandable through release, maintenance, and dependency upgrades across mobile, desktop, and web delivery.

## Decision

Use **Kotlin Multiplatform and Compose Multiplatform** as the primary application platform.

The target architecture is:

```text
commonMain
├── lesson state machine
├── course and content model
├── learner progress and mastery
├── deterministic grading interfaces
├── presentation models
├── design system
└── shared Compose presentation where it remains appropriate

androidMain
└── Android lifecycle and capabilities

iosMain
└── Apple lifecycle and capabilities through Kotlin/Native

jvmMain
└── macOS, Windows, and Linux desktop capabilities

wasmJsMain or a separate web client
└── browser delivery
```

### Mobile first, desktop extended

The shared product defines capability tiers rather than forcing identical interfaces:

- **Compact:** phone story, prediction, evidence, short repairs, review, and progress.
- **Comfortable:** tablet and browser layouts with richer evidence and editing.
- **Workbench:** desktop editing, keyboard workflows, local Allium checks, repositories, and advanced coaching.

Desktop adds capability. It does not redefine the lesson model.

### Web is required but renderer-pluggable

Web delivery is a product requirement, not a commitment to one rendering technology.

Evaluate Compose Multiplatform for Wasm first when appropriate. Preserve the option to use Kotlin/JS, a TypeScript/PWA client, or another web renderer over the same course packages and serializable presentation/action contract.

Compose Wasm maturity alone must not determine whether KMP succeeds for the product.

### Explicit ownership and platform boundaries

- Keep lesson progression deterministic and independent of UI lifecycle.
- Prefer explicit application graphs and dependency ownership over global `object` service locators.
- Put platform capabilities behind narrow interfaces.
- Do not claim code sharing by line count; evaluate whether behaviour and maintenance are genuinely shared.
- Platform-specific presentation is allowed when required for input, accessibility, lifecycle, or platform quality.
- Apple delivery still requires Apple platform knowledge, Xcode, signing, lifecycle, and interoperability work.

### Release and maintenance are part of the platform test

The first vertical slice must be exercised as release software, including:

- physical iOS hardware as well as a simulator;
- signed macOS packaging;
- termination and state restoration;
- keyboard, touch, software keyboard, and accessibility behaviour;
- Kotlin/Swift error and type boundaries;
- dependency upgrades after the first working version; and
- diagnosis of failures across common, Native, JVM, Wasm, and platform layers.

## Platform boundaries

- The terminal interface supports developer-focused exercises, local repositories, and deterministic command-line checks.
- SwiftUI is an approved Apple presentation fallback over shared Kotlin logic when native presentation provides a clearer maintenance boundary.
- A dedicated TypeScript/PWA client is an approved web renderer over the shared lesson contract.
- Browser-runtime desktop shells do not satisfy the mobile delivery requirement.

## Success signals

- Lesson behaviour and content remain shared across targets.
- Platform source sets stay focused on genuine platform capabilities.
- iOS and macOS experiences feel intentional rather than mechanically identical.
- Input, accessibility, release packaging, and state restoration are trustworthy.
- Cross-runtime failures can be located and repaired without sustained archaeology.
- The web experience can consume the lesson contract even if it uses a separate renderer.

## Reconsider when

- routine Apple behaviour repeatedly requires fragile escape hatches;
- editing, input, or accessibility remains unreliable;
- platform branching dominates shared product work;
- build and debugging cycles overwhelm the reuse gained; or
- maintaining common code requires assumptions that do not hold across Native, JVM, and Wasm.

Possible fallback outcomes are KMP shared logic with native presentation, SwiftUI for Apple products, or a terminal-native product for developer-only courses. A KMP limitation does not automatically select any one fallback.

## Architecture constraints

- Product application work belongs in the Kotlin Multiplatform application.
- Platform capability is developed through complete lesson vertical slices.
- Shared code avoids accidental JVM assumptions.
- Web portability depends on a clean lesson/presentation contract rather than guaranteed shared widgets.
- Allium specifications remain implementation-independent; this ADR records the KMP choice.
