#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${ALLIUM_TUTOR_WORKSPACE:-$(pwd)}"
RUN_DIR="${ALLIUM_TUTOR_RUN_DIR:-$WORKSPACE}"
PI_PANE="${ALLIUM_TUTOR_PI_PANE:-}"
EDITOR_CMD="${VISUAL:-${EDITOR:-nano}}"
EX_DIR="$WORKSPACE/exercises"
META_DIR="$WORKSPACE/.alliumlings"
STATE_FILE="$META_DIR/state"
DONE_FILE="$META_DIR/done"

NAMES=("01_first_rule" "02_reachable_trigger")
TITLES=("First rule" "Reachable trigger")
FILES=("01_first_rule.allium" "02_reachable_trigger.allium")

cd "$WORKSPACE"

RESET="" BOLD="" DIM="" CYAN="" GREEN="" YELLOW="" RED="" MAGENTA="" MUTED=""
if [[ -t 1 && -z "${NO_COLOR:-}" ]] && command -v tput >/dev/null 2>&1; then
  if [[ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]]; then
    RESET="$(tput sgr0)"; BOLD="$(tput bold)"; DIM="$(tput dim 2>/dev/null || true)"
    CYAN="$(tput setaf 6)"; GREEN="$(tput setaf 2)"; YELLOW="$(tput setaf 3)"; RED="$(tput setaf 1)"; MAGENTA="$(tput setaf 5)"; MUTED="$DIM"
  fi
fi
paint() { printf '%b%s%b' "$1" "$2" "$RESET"; }
line() { printf '%b%s%b\n' "$MUTED" "$(printf '%*s' "${COLUMNS:-80}" '' | tr ' ' '─')" "$RESET"; }
short_path() { local p="$1" max="${2:-76}"; (( ${#p} <= max )) && printf '%s' "$p" || printf '…%s' "${p: -$((max - 1))}"; }

mkdir -p "$META_DIR"
[[ -f "$STATE_FILE" ]] || echo 0 > "$STATE_FILE"
[[ -f "$DONE_FILE" ]] || : > "$DONE_FILE"

current_index() { cat "$STATE_FILE" 2>/dev/null || echo 0; }
set_current_index() { echo "$1" > "$STATE_FILE"; }
current_name() { local i; i="$(current_index)"; printf '%s' "${NAMES[$i]}"; }
current_title() { local i; i="$(current_index)"; printf '%s' "${TITLES[$i]}"; }
current_file() { local i; i="$(current_index)"; printf '%s/%s' "$EX_DIR" "${FILES[$i]}"; }
count() { printf '%s' "${#NAMES[@]}"; }
is_done() { grep -qx "$1" "$DONE_FILE" 2>/dev/null; }
mark_done() { is_done "$1" || echo "$1" >> "$DONE_FILE"; }
mark_not_done() { grep -vx "$1" "$DONE_FILE" > "$DONE_FILE.tmp" 2>/dev/null || true; mv "$DONE_FILE.tmp" "$DONE_FILE"; }

header() {
  clear
  local i total name title status
  i="$(current_index)"; total="$(count)"; name="$(current_name)"; title="$(current_title)"
  if is_done "$name"; then status="$(paint "$GREEN" "done")"; else status="$(paint "$YELLOW" "pending")"; fi
  printf '%b%s%b\n' "$BOLD$CYAN" "Alliumlings" "$RESET"
  printf '%bExercise:%b %d/%d  %b%s%b — %s  [%b]\n' "$DIM" "$RESET" "$((i+1))" "$total" "$MAGENTA" "$name" "$RESET" "$title" "$status"
  printf '%bFile:%b     %s\n' "$DIM" "$RESET" "$(short_path "$(current_file)" "${COLUMNS:-80}")"
  printf '%bRun:%b      %s\n' "$DIM" "$RESET" "$(short_path "$RUN_DIR" "${COLUMNS:-80}")"
  line
}

pause() { echo; read -r -n 1 -s -p "$(paint "$DIM" "Press any key...")" || true; }

gate_ok() {
  local out="$1"
  grep -q '"diagnostics": \[\]' <<<"$out" && grep -q '"findings": \[\]' <<<"$out"
}

show_findings() {
  local out="$1"
  grep -E '"(code|type|severity|message)"' <<<"$out" | sed 's/^[[:space:]]*//; s/[",]$//' || true
}

run_current() {
  header
  local file name check_out analyse_out check_status analyse_status ok=true
  file="$(current_file)"; name="$(current_name)"
  printf '%bRunning:%b allium check + analyse\n\n' "$BOLD$CYAN" "$RESET"

  set +e
  check_out="$(allium check "$file" 2>&1)"; check_status=$?
  analyse_out="$(allium analyse "$file" 2>&1)"; analyse_status=$?
  set -e

  if [[ "$check_status" -ne 0 ]] || ! gate_ok "$check_out"; then ok=false; fi
  if [[ "$analyse_status" -ne 0 ]] || ! gate_ok "$analyse_out"; then ok=false; fi

  if [[ "$ok" == true ]]; then
    mark_done "$name"
    printf '%b✅ Passed.%b check/analyse are clean.\n' "$GREEN$BOLD" "$RESET"
    echo
    if [[ "$(current_index)" -lt $(($(count)-1)) ]]; then
      echo "Press n for the next exercise."
    else
      printf '%bYou finished the current Alliumlings set.%b\n' "$GREEN$BOLD" "$RESET"
    fi
  else
    mark_not_done "$name"
    printf '%b❌ Not solved yet.%b This is expected while learning.\n\n' "$YELLOW$BOLD" "$RESET"
    printf '%bcheck:%b exit %s\n' "$DIM" "$RESET" "$check_status"
    show_findings "$check_out"
    echo
    printf '%banalyse:%b exit %s\n' "$DIM" "$RESET" "$analyse_status"
    show_findings "$analyse_out"
    echo
    echo "Next: press Enter to re-check, 1 to edit, 2 for a hint, or 3 to ask Pi."
  fi
}

show_hint() {
  header
  local hint="$META_DIR/hints/$(current_name).md"
  printf '%bHint%b\n' "$BOLD$YELLOW" "$RESET"; line; echo
  [[ -f "$hint" ]] && cat "$hint" || echo "No hint found: $hint"
}

edit_current() {
  header
  echo "Opening $(current_file) in $EDITOR_CMD"
  echo "Save and quit, then run r."
  pause
  $EDITOR_CMD "$(current_file)"
}

next_exercise() {
  local i name
  i="$(current_index)"; name="${NAMES[$i]}"
  if ! is_done "$name"; then
    header; printf '%bFinish this exercise before moving on.%b\n' "$YELLOW$BOLD" "$RESET"; echo "Press Enter to check, 1 to edit, 2 for a hint, or 3 to ask Pi."; pause; return
  fi
  if [[ "$i" -ge $(($(count)-1)) ]]; then
    header; printf '%bAll exercises in this set are complete.%b\n' "$GREEN$BOLD" "$RESET"; pause; return
  fi
  set_current_index "$((i+1))"
  run_current
}

reset_current() {
  header
  local name src dst
  name="$(current_name)"; src="$META_DIR/starters/$name.allium"; dst="$(current_file)"
  read -r -p "Reset $name? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES) cp "$src" "$dst"; mark_not_done "$name"; echo "Reset." ;;
    *) echo "Cancelled." ;;
  esac
}

list_exercises() {
  header
  printf '%bExercises%b\n' "$BOLD$YELLOW" "$RESET"; line; echo
  local i name marker
  for i in "${!NAMES[@]}"; do
    name="${NAMES[$i]}"
    if is_done "$name"; then marker="$(paint "$GREEN" "✓")"; else marker="$(paint "$YELLOW" "•")"; fi
    printf ' %s %d. %b%s%b — %s\n' "$marker" "$((i+1))" "$MAGENTA" "$name" "$RESET" "${TITLES[$i]}"
  done
}

ask_pi() {
  header
  local prompt="/skill:allium We are doing Alliumlings exercise $(current_name). Please explain what is wrong in exercises/${FILES[$(current_index)]} slowly. Do not edit unless I ask. Give one small next step."
  if [[ -n "$PI_PANE" ]] && command -v tmux >/dev/null 2>&1 && tmux display-message -p -t "$PI_PANE" '#{pane_id}' >/dev/null 2>&1; then
    tmux send-keys -t "$PI_PANE" "$prompt" C-m
    printf '%bSent prompt to Pi pane.%b\n\n' "$GREEN$BOLD" "$RESET"
    printf '%b%s%b\n' "$MAGENTA" "$prompt" "$RESET"
  else
    printf '%bCopy this into Pi:%b\n\n%s\n' "$YELLOW$BOLD" "$RESET" "$prompt"
  fi
}

quit_all() {
  header
  read -r -p "Quit Alliumlings and close tmux session? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES)
      if [[ -n "${TMUX:-}" ]] && command -v tmux >/dev/null 2>&1; then
        session="$(tmux display-message -p '#S' 2>/dev/null || true)"
        [[ -n "$session" ]] && tmux kill-session -t "$session"
      fi
      exit 0 ;;
  esac
}

menu() {
  while true; do
    header
    printf '%bWhat do you want to do next?%b\n\n' "$BOLD" "$RESET"
    printf '  %bEnter%b  Check my work / run Allium\n' "$GREEN$BOLD" "$RESET"
    printf '  %b1%b      Edit the exercise file\n' "$CYAN$BOLD" "$RESET"
    printf '  %b2%b      Show a hint\n' "$CYAN$BOLD" "$RESET"
    printf '  %b3%b      Ask Pi to explain this exercise\n' "$CYAN$BOLD" "$RESET"
    printf '  %b4%b      Move to the next exercise\n' "$CYAN$BOLD" "$RESET"
    printf '  %b5%b      Show progress\n' "$CYAN$BOLD" "$RESET"
    printf '  %b6%b      Reset this exercise\n' "$CYAN$BOLD" "$RESET"
    printf '  %bq%b      Quit and close tmux\n\n' "$CYAN$BOLD" "$RESET"
    read -r -n 1 -p "$(paint "$BOLD$YELLOW" "Choice: ")" choice; echo
    case "$choice" in
      ""|$'\n'|$'\r'|r|R) run_current; pause ;;
      1|e|E) edit_current ;;
      2|h|H) show_hint; pause ;;
      3|a|A) ask_pi; pause ;;
      4|n|N) next_exercise ;;
      5|l|L) list_exercises; pause ;;
      6|x|X) reset_current; pause ;;
      q|Q) quit_all ;;
      *) header; echo "Unknown choice. Press Enter to check, or choose 1–6."; pause ;;
    esac
  done
}

case "${1:-watch}" in
  watch|menu) menu ;;
  run) run_current ;;
  hint) show_hint ;;
  edit) edit_current ;;
  next) next_exercise ;;
  reset) reset_current ;;
  list) list_exercises ;;
  ask) ask_pi ;;
  *) echo "Usage: alliumlings.sh [watch|run|hint|edit|next|reset|list|ask]" >&2; exit 1 ;;
esac
