# Classroom Script — First Alliumlings Run

Copy/paste friendly flow for a first class session.

## 1. Clone and enter the repo

```bash
git clone <your-course-repo-url> learn-allium
cd learn-allium
```

## 2. Install prerequisites

Learners only need the Allium CLI for the default resilient app.

```bash
brew tap juxt/allium && brew install allium
```

If not on macOS/Homebrew, install the CLI with Cargo:

```bash
cargo install --locked allium-cli
```

Optional coach mode uses Pi and the Allium skill:

```bash
npm install -g --ignore-scripts @earendil-works/pi-coding-agent
npx skills add juxt/allium
```

Optional two-pane mode also needs tmux:

```bash
brew install tmux
```

## 3. Setup check

```bash
./learn-allium doctor
```

Expected: Allium `3.5.0` or compatible, optional Pi/tmux notes, terminal note, and local examples check cleanly.

## 4. Launch Alliumlings

```bash
./learn-allium
```

This runs the setup check, creates an isolated run, and opens Alliumlings in the current terminal. Learners can optionally choose `7` to auto-rerun checks when they save the current exercise from another terminal.

Learners use:

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

If a learner chooses `3`, the app asks Pi inline when available; otherwise it prints a prompt they can copy into Pi. For the persistent paired-pane experience:

```bash
./learn-allium tmux
```

## 5. If someone gets stuck

Use the simple progression:

```text
2      show hint
3      ask Pi prompt
6      reset current exercise
```

Or start a fresh isolated run:

```bash
./learn-allium
```

## 6. Collect artifacts

Collect the edited specs:

```text
.tutorial/runs/alliumlings-<timestamp>/workspace/exercises/*.allium
```

If learners used Pi separately, they can export that conversation from Pi:

```text
/export alliumlings.html
```

## 7. Instructor smoke test before class

```bash
./learn-allium --smoke
./learn-allium tmux --smoke        # optional Pi/tmux mode
./learn-allium lesson-01 --smoke   # legacy guided lesson, optional
```

Legacy/fallback modes:

```bash
./learn-allium tmux        # two-pane Alliumlings + Pi helper
./learn-allium lesson-01   # guided single-lesson tutor
./learn-allium pi          # embedded-in-Pi tutor
```
