# Session 1: Basics & Motivation (1 hour)

**Objective**: Understand why Allium exists and get to first hands-on success.

## Agenda

| Time | Segment |
|------|---------|
| 0:00–0:15 | The problems Allium solves |
| 0:15–0:25 | The Allium philosophy |
| 0:25–0:35 | Install (skills + CLI) |
| 0:35–0:55 | Live demo: distill a local script |
| 0:55–1:00 | Homework briefing |

## 1. The problems it solves (15 min)

Talking points — ground each one in something the team has felt:

- **Prompt drift within/across sessions.** Every new agent session
  re-explains the same intent, slightly differently. Allium fights this at
  the *behavior* level: the spec is the durable, re-loadable statement of
  what the system must do.
- **Code as "single source of truth" mixes intent with accidents.** Show
  `examples/code/pinned-installer.sh` — which parts are required behavior
  and which are incidental implementation? You can read the code, but the
  spec in `examples/pinned-installer.allium` names the important promises:
  unsupported platforms refuse, checksum mismatch never installs, and PATH
  notice behavior is explicit.
- **Ambiguities hidden in prose/markdown.** A design doc can say "access
  grants expire" and never define whether revoked grants can authorize, who
  attributes revocation, or what refusal looks like. The Allium version is
  forced to decide: see `examples/access-grant-lifecycle.allium`.

## 2. The Allium philosophy (10 min)

- **Behavioral specification: what, not how.** Entities, states, rules,
  invariants — never SQL schemas, HTTP handlers, or UI layouts.
- A **durable, LLM-readable, contradiction-detecting contract** between
  humans, agents, and code:
  - *Durable* — lives in git, survives sessions.
  - *LLM-readable* — agents load the spec instead of re-eliciting intent.
  - *Contradiction-detecting* — the CLI mechanically flags structural
    problems (dead entities, rules listening for events nothing provides),
    and the weed skill hunts spec/code contradictions; Session 3 covers
    exactly where the mechanical net ends and review takes over.
- Allium is **not a runtime**. Code and tests implement and verify the
  contract; the spec never executes.
- Optional remote note: production case studies are listed in
  `examples/README.md`, but this course's required examples are all local
  under `examples/` and `exercises/`.

## 3. Quick install (10 min)

Everyone installs during the session — don't defer this.

```bash
# Skills — pick the flow that matches your harness
npx skills add juxt/allium
# or your harness's plugin/skill installation flow

# CLI — pick one
brew tap juxt/allium && brew install allium
cargo install --locked allium-cli
```

Smoke test against the local bundle:

```bash
allium --version
allium check examples
allium analyse examples/library-lending.allium
```

Both should return empty `diagnostics`/`findings` JSON. If someone's install
fails, pair them up — do not let this block the demo.

## 4. Live demo: `/allium` entry point → distill (20 min)

Run in an agent session on **this repo**, projected for the room:

1. Invoke the `/allium` entry point and show how it routes to the right
   skill for the situation.
2. **Distill** (backward, from code): use `examples/code/pinned-installer.sh`,
   a small shell script with genuinely interesting behavior: platform
   gating, version pinning, checksum verification, install result, and PATH
   notice.
3. Watch the skill produce entities/rules. Discuss as a group:
   - What did it capture that the script only implies?
     (e.g. *"install is refused on checksum mismatch"* is an explicit rule.)
   - What did it correctly leave out? (curl flags, temp dir names, exact URL.)
4. Compare with the provided reference spec in
   `examples/pinned-installer.allium`.
5. Run `allium check` + `allium analyse` on the result. Green output = first
   success.

If time allows, show the contrast: **elicit** (forward, from intent) a
one-sentence feature idea into a skeleton spec.

## Homework

1. Install skills + CLI on your own machine (if not done in session).
2. Distill **one small existing function or module you own** into a spec.
   Keep it under ~60 lines of Allium. Suggested targets:
   `examples/code/pinned-installer.sh`, one CLI subcommand, or one small
   state-carrying struct.
3. Run `allium check` and `allium analyse` until both are clean.
4. Bring the spec to Session 2 — we'll trade and read each other's.

## Resources

- Main Allium README and https://juxt.github.io/allium/
- Installation guide (skills + CLI)
- [Internal examples](../examples/)
- [Exercise 01: first distill](../exercises/01-first-distill.md)
