# Allium Tutor

This directory contains the shareable guided tutor layer for the course.

The default app is resilient and single-terminal. Pi can still help as a coach, but every lesson run happens in an isolated disposable workspace under `.tutorial/runs/`.

## Learner quickstart

From the repo root:

```bash
./learn-allium
```

That one command runs the setup check, creates an isolated lesson workspace, and opens **Alliumlings** in the current terminal.

Stay in the app for the training loop. When you choose "Ask Pi", the app prints a copyable prompt. Choose `7` to auto-rerun checks when the current exercise file is saved from another terminal. If you want the older two-pane helper, run `./learn-allium tmux`.

Setup check only:

```bash
./learn-allium doctor
```

Legacy/fallback modes:

```bash
./learn-allium tmux        # two-pane Alliumlings + Pi helper
./learn-allium lesson-01   # guided single-lesson tutor
./learn-allium pi          # embedded-in-Pi tutor
```

## Alliumlings controls

Controls are intentionally simple:

```text
Enter  check my work
1      edit
2      hint
3      ask Pi
4      next exercise
5      progress
6      reset
7      watch for saves / auto-rerun
q      quit
```

## Smoke test without launching Pi or tmux

```bash
./learn-allium --smoke
./learn-allium lesson-01 --smoke
```

These create disposable run directories, check clean solutions, and verify that starters begin unsolved.
## Classroom materials

- [Classroom script](classroom-script.md) — copy/paste flow for the first guided run.
- [Facilitator notes](facilitator-notes.md) — pacing, common sticking points, and artifact collection.

## Exporting a learner run

Submit the edited exercise files:

```text
.tutorial/runs/alliumlings-<timestamp>/workspace/exercises/*.allium
```

If using a Pi helper mode, learners can also export their agent session from Pi:

```text
/export alliumlings.html
```

## Isolation model

Each run creates a disposable directory under `.tutorial/runs/`, for example:

```text
.tutorial/runs/alliumlings-<timestamp>/
  workspace/       # learner and agent work here
  skills/          # copied Allium skill only
  sessions/        # Pi session logs for this lesson
  home/            # lesson HOME for safe bash commands
  tmp/             # lesson TMPDIR
```

In default mode, no tmux or Pi process is launched. The standalone Alliumlings app (`tutorial/bin/alliumlings.sh`) runs in the current terminal, and "Ask Pi" prints a copyable prompt.

In optional Pi helper modes, Pi is launched with:

- no project/global context files;
- no discovered skills, only the copied Allium skill;
- no discovered extensions, only the isolation guard;
- a per-run session directory;
- tool access limited to `read`, `edit`, `write`, and `bash`;
- a guard extension that blocks file access outside the lesson workspace and restricts bash to deterministic lesson commands.

The older Pi-embedded tutor extension still exists as fallback mode via `./learn-allium pi`.

## Strict mode

Default Alliumlings does not launch Pi, so `--strict` is not needed.

For stricter Pi isolation in helper modes:

```bash
./learn-allium tmux --strict
./learn-allium pi --strict
```

Strict mode sets `PI_CODING_AGENT_DIR` inside the run directory, so Pi settings/auth are isolated too. Learners may need API keys or a fresh login.
