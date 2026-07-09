# Allium Tutor

This directory contains the shareable guided tutor layer for the course.

The tutor uses Pi as the live agent, but every lesson run happens in an isolated disposable workspace under `.tutorial/runs/`.

## Learner quickstart

From the repo root:

```bash
./learn-allium
```

That one command runs the setup check, creates an isolated lesson workspace, and opens **Alliumlings** in a two-pane tmux session:

- left pane: tiny failing Allium exercises;
- right pane: Pi as the agent helper.

Stay in the left pane for the training loop. When you choose "Ask Pi", the tutor sends the prompt to the Pi pane for you. Press `q` in the tutor menu to quit and close the tmux session cleanly.

Setup check only:

```bash
./learn-allium doctor
```

Legacy/fallback modes:

```bash
./learn-allium lesson-01   # guided single-lesson tutor
./learn-allium pi          # embedded-in-Pi tutor
```

## Alliumlings controls

Controls in the left pane are intentionally simple:

```text
Enter  check my work
1      edit
2      hint
3      ask Pi
4      next exercise
5      progress
6      reset
q      quit
```

## Smoke test without launching Pi

```bash
./learn-allium --smoke
./learn-allium lesson-01 --smoke
```

These create disposable run directories, check clean solutions, and verify that starters begin unsolved.
## Classroom materials

- [Classroom script](classroom-script.md) — copy/paste flow for the first guided run.
- [Facilitator notes](facilitator-notes.md) — pacing, common sticking points, and artifact collection.

## Exporting a learner run

Inside Pi, learners can export their session:

```text
/export lesson-01.html
```

Or submit the edited lesson file:

```text
.tutorial/runs/lesson-01-<timestamp>/workspace/library-starter.allium
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

In default mode, tmux launches two panes:

- the standalone Alliumlings app (`tutorial/bin/alliumlings.sh`);
- Pi with only the copied Allium skill and the isolation guard.

Pi is launched with:

- no project/global context files;
- no discovered skills, only the copied Allium skill;
- no discovered extensions, only the isolation guard;
- a per-run session directory;
- tool access limited to `read`, `edit`, `write`, and `bash`;
- a guard extension that blocks file access outside the lesson workspace and restricts bash to deterministic lesson commands.

The older Pi-embedded tutor extension still exists as fallback mode via `./learn-allium pi`.

## Strict mode

Default mode reuses the learner's normal Pi auth/model setup while isolating lesson files and sessions.

For stricter isolation:

```bash
./learn-allium --strict
```

Strict mode sets `PI_CODING_AGENT_DIR` inside the run directory, so Pi settings/auth are isolated too. Learners may need API keys or a fresh login.
