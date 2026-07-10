# TODO — Story-Driven Allium Tutor

Build a reusable, mobile-first tutor that teaches Allium through authored incidents, investigation, focused repair, deterministic verification, and transfer.

Canonical guidance:

- `docs/decisions/0001-story-driven-tutoring.md`
- `docs/decisions/0002-kmp-application-platform.md`
- `layer/allium/lesson-lifecycle.allium`
- `docs/storyboards/temporal-overdue.md`

## Guiding principles

- Follow the lifecycle: incident → prediction → evidence → concept → workbench → verification → consequence → transfer.
- Teach one idea through one focused action and one deterministic result.
- Keep lessons local and self-contained; use `examples/` and `exercises/` as source material.
- Give each learner an isolated workspace while preserving canonical course examples.
- Pair every probabilistic agent step with deterministic evaluation.
- Use Pi as optional coaching around authored narrative and checks.
- Treat mobile as the baseline experience; tablet, browser, and desktop add capability.
- Deliver the web experience through a replaceable renderer and a stable lesson contract.

## Current slice — authored temporal-overdue lesson

- [ ] Refine the incident, prediction, evidence, concept, repair, feedback, consequence, and transfer as one causal narrative.
- [ ] Test the refined copy at phone width for reading rhythm, action cadence, and terminology timing.
- [ ] Check that repair and transfer prompts ask for exactly what deterministic evaluation measures.
- [ ] Add parser-backed Allium validation for packaged starter and learner fragments.
- [ ] Verify state restoration after process termination on iOS and desktop.
- [ ] Verify VoiceOver labels, focus order, text scaling, contrast, and reduced-motion behaviour in the simulator.

## Apple release checkpoint

- [ ] Connect a physical iOS device and run the signed lesson build.
- [ ] Verify multiline editing through direct touch on physical iOS hardware.
- [ ] Configure macOS signing and notarisation for release packaging.
- [ ] Resolve the Skiko ICU simulator deployment-target warning before release.

## Platform sequence

- [ ] Add Android after the Apple and desktop lifecycle trial is complete.
- [ ] Evaluate Compose Wasm against the lesson contract and interaction requirements.
- [ ] Use a Kotlin/JS or TypeScript/PWA renderer if it provides the stronger web product.
- [ ] Upgrade one meaningful dependency and record whether failures remain diagnosable across common, Native, JVM, and platform layers.

## Foundational lesson path

- [ ] First behaviour rule: English behaviour, entity state, one rule, deterministic check.
- [ ] Reachable triggers: expose and repair `allium.rule.unreachableTrigger` with a surface.
- [ ] Temporal behaviour: teach temporal guards and single-transition state changes.
- [ ] Distillation: derive a specification from `examples/code/pinned-installer.sh` and compare it with the authored model.
- [ ] Refactoring: separate language diagnostics from behavioural review findings.
- [ ] Obligations: run `allium plan`, connect obligation IDs to tagged tests, and verify transfer.

## Proficiency path

- [ ] Define Beginner, Practitioner, and Expert completion gates.
- [ ] Require foundational lesson completion for the Beginner gate.
- [ ] Require one code distillation, generated obligations, and tagged tests for the Practitioner gate.
- [ ] Require a reviewed capstone specification and implementation reconciliation for the Expert gate.
- [ ] Export a factual completion summary with exercises, artifacts, review state, and next step.
- [ ] Reserve certification language for completion that includes human review.

## Classroom readiness

- [ ] Validate setup and lesson execution from a clean clone.
- [ ] Decide whether learner artifacts are committed, exported as Pi sessions, or submitted as archives.
- [ ] Define strict-mode isolation for Pi authentication and settings.
- [ ] Define the smallest safe command surface available to the lesson coach.

## Quality gates

```bash
allium --version
allium check layer/allium
allium analyse layer/allium
allium check examples
for spec in examples/*.allium; do allium analyse "$spec"; done
while IFS= read -r spec; do
  allium check "$spec"
  allium analyse "$spec"
done < <(find exercises -path '*/solution/*.allium' | sort)

./learn-allium doctor
./learn-allium --smoke

cd app
./gradlew :shared:desktopTest
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :desktopApp:packageDmg
```
