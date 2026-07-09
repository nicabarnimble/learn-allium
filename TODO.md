# TODO — Shareable Allium Tutor

Goal: turn this repo into a class-ready Alliumlings training path: tiny failing Allium specs, deterministic gates, Pi help, progress, and a capstone so a learner can credibly say they passed the path and are Allium-proficient.

## Guiding principles

- Teach slowly: one idea, one tiny edit, one deterministic check.
- Keep lessons local and self-contained; use `examples/` and `exercises/` as the source of truth.
- Never edit course examples during a lesson run; copy fixtures into a disposable workspace.
- Every probabilistic agent step gets a deterministic gate: `allium check`, `allium analyse`, and eventually `allium plan`.
- Pi is the guided agent; the tutorial should make the Allium skill easy to invoke and inspect.

## Milestone 1 — Minimal guided Pi tutor

Definition of done: legacy guided Lesson 01 remains available, but the canonical default is Alliumlings.

- [x] Add friendly `./learn-allium` entrypoint and `scripts/allium-tutor` launcher.
  - [x] Default to a two-pane tmux experience: standalone tutor on the left, Pi helper on the right.
  - [x] Keep `./learn-allium pi` as fallback embedded-in-Pi mode.
  - [x] Creates `.tutorial/runs/<lesson-id>-<timestamp>/`.
  - [x] Creates `workspace/`, `skills/`, `sessions/`, `home/`, and `tmp/` under the run directory.
  - [x] Copies lesson fixtures into `workspace/`.
  - [x] Copies only the needed Allium skill(s) into `skills/`.
  - [x] Launches Pi with `--session-dir`, `--no-context-files`, `--no-skills`, explicit `--skill`, `--no-extensions`, explicit tutor extension, and lesson cwd.
- [x] Add `.tutorial/runs/` to `.gitignore`.
- [x] Add `tutorial/pi/isolation-guard.ts`.
  - [x] Block `read`, `write`, and `edit` outside the lesson workspace.
  - [x] Block writes to sensitive paths: `.env`, `.ssh`, `.aws`, `.git`, keys, tokens.
  - [x] Restrict `bash` to safe lesson commands: `pwd`, `ls`, `allium --version`, `allium check`, `allium analyse`, `allium plan`, and `git diff` inside workspace.
  - [x] Set lesson-safe environment variables for bash: `HOME`, `TMPDIR`, and a predictable `PATH`.
- [x] Add `tutorial/pi/allium-tutor.ts` Pi extension.
  - [x] Register `/allium-tutor` command.
  - [x] Show a guided lesson panel/status widget.
  - [x] Provide slow actions: explain, show starter, run check, show hint, compare solution, reset.
  - [x] Keep the learner in control; agent suggestions should not auto-overwrite files without explicit confirmation.
- [x] Add Lesson 01 fixtures.
  - [x] `tutorial/fixtures/01-first-rule/library-starter.allium`
  - [x] `tutorial/solutions/01-first-rule/library-solution.allium`
  - [x] `tutorial/lessons/01-first-rule.md`
- [x] Add smoke test instructions for the tutor launcher.

## Milestone 2 — Rustlings-style Alliumlings engine

Definition of done: `./learn-allium watch` launches a Rustlings-style loop with tiny failing `.allium` exercises, `run`, `edit`, `hint`, `next`, `reset`, `list`, and Pi help.

- [x] Add `tutorial/alliumlings/exercises.toml` manifest.
- [x] Add alliumlings starter/solution/hint directories.
- [x] Add Exercise 01: `01_first_rule`.
  - [x] Starts with missing `BorrowBook` rule.
  - [x] Solution passes `allium check` and `allium analyse`.
- [x] Add Exercise 02: `02_reachable_trigger`.
  - [x] Starts with `allium.rule.unreachableTrigger`.
  - [x] Solution adds `surface provides:` and passes gates.
- [x] Add `tutorial/bin/alliumlings.sh` runner.
  - [x] `Enter` run/check current exercise.
  - [x] `1` edit current exercise.
  - [x] `2` show hint.
  - [x] `3` send current-exercise prompt to Pi helper pane.
  - [x] `4` advance after pass.
  - [x] `5` list progress.
  - [x] `6` reset current exercise.
  - [x] `q` quit and close tmux.
- [x] Make `./learn-allium` default to Alliumlings.
- [x] Add `./learn-allium watch` / `./learn-allium alliumlings` aliases.
- [x] Add `./learn-allium watch --smoke` validation.
- [ ] Add true file-watch auto-rerun on save.
- [ ] Add more exercises: temporal behavior, distill-from-code, refactor, obligations.

## Milestone 3 — Beginner lesson sequence

Definition of done: a learner can complete a 60–90 minute guided intro without facilitator intervention.

- [ ] Lesson 01: first behavior rule.
  - [ ] English behavior → one Allium rule.
  - [ ] Entity state enum.
  - [ ] `allium check` success.
- [ ] Lesson 02: surfaces and reachable triggers.
  - [ ] Show `allium.rule.unreachableTrigger` intentionally.
  - [ ] Add a `surface provides:` block to fix it.
- [ ] Lesson 03: temporal behavior.
  - [ ] Use a tiny lending or access-grant expiry example.
  - [ ] Teach temporal guards and avoiding re-fire/races.
- [ ] Lesson 04: distill from code with Pi + Allium skill.
  - [ ] Copy `examples/code/pinned-installer.sh` into workspace.
  - [ ] Ask Pi to use the Allium/distill flow.
  - [ ] Compare with `examples/pinned-installer.allium`.
- [ ] Lesson 05: refactor a flawed spec.
  - [ ] Use a reduced version of `exercises/03-spec-refactor/starter/plugin-approval.allium`.
  - [ ] Separate CLI-caught defects from human/agent review defects.
- [ ] Lesson 06: obligations and tests.
  - [ ] Run `allium plan`.
  - [ ] Explain obligation IDs and test tagging.

## Milestone 3 — Classroom readiness

Definition of done: another instructor can clone the repo, run one setup command, and teach from it.

- [x] Add `tutorial/README.md` with instructor and learner quickstarts.
- [x] Add preflight command.
  - [x] Check `pi` is installed.
  - [x] Check `allium --version` is available and compatible.
  - [x] Check required skill source exists.
  - [x] Check terminal supports interactive Pi reasonably.
- [x] Add reset command for lesson runs.
- [x] Add export command or instructions.
  - [x] Export Pi lesson session to HTML or JSONL for homework review.
- [x] Add facilitator notes for where students usually get stuck.
- [x] Add copyable classroom script:
  - [x] install tools
  - [x] run preflight
  - [x] launch lesson 01
  - [x] collect artifacts
- [ ] Validate on a clean clone.

## Milestone 4 — Proficiency path / “I passed Alliumlings”

Definition of done: a learner who finishes the path has practiced syntax, surfaces, temporal rules, distillation, refactoring, obligations, weed/tend loops, and a reviewed capstone.

- [ ] Define levels: Beginner, Practitioner, Expert.
- [ ] Beginner gate: all foundational Alliumlings exercises pass.
- [ ] Practitioner gate: distill one code artifact, generate obligations, tag tests.
- [ ] Expert gate: capstone spec + implementation reconciliation + review ledger.
- [ ] Add progress summary: completed exercises, capstone artifacts, next step.
- [ ] Add final message and printable/exportable completion summary.
- [ ] Avoid “certification” language unless there is human review; use “Allium-proficient” for automated completion.

## Milestone 5 — Optional HTML companion

Definition of done: the same lesson content can be read as a static workbook, while live practice still happens in Pi.

- [ ] Generate or hand-build `tutorial/site/index.html` from `tutorial/lessons/*.md`.
- [ ] Include code snippets and expected CLI outputs.
- [ ] Link each HTML step to the matching tutor command.
- [ ] Add “open in tutor” commands for local runs.

## Quality gates before sharing

Run these before class:

```bash
allium --version
allium check examples
for spec in examples/*.allium; do allium analyse "$spec"; done
while IFS= read -r spec; do
  allium check "$spec"
  allium analyse "$spec"
done < <(find exercises -path '*/solution/*.allium' | sort)
```

Tutor-specific gates to add:

```bash
./learn-allium doctor
./learn-allium lesson-01 --smoke
./learn-allium watch --smoke
```

## Open decisions

- [x] Should the MVP tutor be a Pi extension UI, a plain terminal script, or both?
  - Decision: default to terminal/tmux app; keep Pi extension as fallback mode.
- [ ] Should strict mode use `PI_CODING_AGENT_DIR=$RUN/pi-agent`?
  - Normal mode can reuse user Pi auth; strict mode should isolate auth/settings too.
- [ ] How much bash should the lesson agent get?
  - Current preference: allow only deterministic Allium and basic inspection commands.
- [ ] Should lesson artifacts be committed by students, exported as Pi sessions, or submitted as zipped run directories?

## Next implementation slice

Build Milestone 1 in this order:

1. `.gitignore` update for `.tutorial/runs/`.
2. `scripts/allium-tutor` launcher with `--preflight` and `lesson-01`.
3. Lesson 01 starter/solution/markdown.
4. Isolation guard extension.
5. Minimal tutor extension command.
6. Smoke run and document the exact classroom command.
