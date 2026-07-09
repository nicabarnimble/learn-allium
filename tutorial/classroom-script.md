# Classroom Script — First Alliumlings Run

Copy/paste friendly flow for a first class session.

## 1. Clone and enter the repo

```bash
git clone <your-course-repo-url> learn-allium
cd learn-allium
```

## 2. Install prerequisites

Learners need Pi, Allium CLI, the Allium skill, and tmux for the clean two-pane lesson UI.

```bash
npm install -g --ignore-scripts @earendil-works/pi-coding-agent
brew tap juxt/allium && brew install allium
brew install tmux
npx skills add juxt/allium
```

If not on macOS/Homebrew, install the CLI with Cargo:

```bash
cargo install --locked allium-cli
```

## 3. Setup check

```bash
./learn-allium doctor
```

Expected: Pi version, Allium `3.5.0` or compatible, Allium skill path, tmux version, terminal note, and local examples check cleanly.

## 4. Launch Alliumlings

```bash
./learn-allium
```

This runs the setup check, creates an isolated run, and opens tmux:

- left pane: Alliumlings exercises;
- right pane: Pi as the agent helper.

Learners stay in the left pane:

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

Useful controls:

```text
q                        quit from the tutor menu and close tmux
Ctrl-b then Left/Right    switch panes
Ctrl-b then d             detach without closing
```

## 5. If someone gets stuck

Use the simple progression:

```text
2      show hint
3      ask Pi
6      reset current exercise
```

Or start a fresh isolated run:

```bash
./learn-allium
```

## 6. Collect artifacts

Ask learners to export from the Pi pane if you want the agent conversation:

```text
/export alliumlings.html
```

Or collect the edited specs:

```text
.tutorial/runs/alliumlings-<timestamp>/workspace/exercises/*.allium
```

## 7. Instructor smoke test before class

```bash
./learn-allium --smoke
./learn-allium lesson-01 --smoke   # legacy guided lesson, optional
```

Legacy/fallback modes:

```bash
./learn-allium lesson-01   # guided single-lesson tutor
./learn-allium pi          # embedded-in-Pi tutor
```
