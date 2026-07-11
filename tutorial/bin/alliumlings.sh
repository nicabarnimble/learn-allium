#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${ALLIUM_TUTOR_WORKSPACE:-$(pwd)}"
WORKSPACE="$(cd "$WORKSPACE" && pwd -P)"
RUN_DIR="${ALLIUM_TUTOR_RUN_DIR:-$WORKSPACE}"
if [[ -d "$RUN_DIR" ]]; then
  RUN_DIR="$(cd "$RUN_DIR" && pwd -P)"
fi
PI_PANE="${ALLIUM_TUTOR_PI_PANE:-}"
TMUX_SESSION="${ALLIUM_TUTOR_TMUX_SESSION:-}"
REPO_ROOT="${ALLIUM_TUTOR_REPO_ROOT:-}"
PI_MODEL="${ALLIUM_TUTOR_PI_MODEL:-}"
PI_THINKING="${ALLIUM_TUTOR_PI_THINKING:-low}"
EDITOR_CMD="${VISUAL:-${EDITOR:-nano}}"
EX_DIR="$WORKSPACE/exercises"
META_DIR="$WORKSPACE/.alliumlings"
STATE_FILE="$META_DIR/state"
DONE_FILE="$META_DIR/done"

MANIFEST_FILE="$META_DIR/exercises.toml"
NAMES=()
TITLES=()
FILES=()
HINTS=()
SOLUTIONS=()

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

load_manifest() {
  NAMES=(); TITLES=(); FILES=(); HINTS=(); SOLUTIONS=()
  [[ -f "$MANIFEST_FILE" ]] || { echo "error: missing Alliumlings manifest: $MANIFEST_FILE" >&2; exit 1; }

  local name title file hint solution
  while IFS=$'\t' read -r name title file hint solution; do
    [[ -n "$name" ]] || continue
    if [[ -z "$title" || -z "$file" ]]; then
      echo "error: invalid exercise in manifest: name=$name title=$title file=$file" >&2
      exit 1
    fi
    NAMES+=("$name")
    TITLES+=("$title")
    FILES+=("$file")
    HINTS+=("${hint:-hints/$name.md}")
    SOLUTIONS+=("${solution:-solutions/$file}")
  done < <(awk '
    function trim(s) { gsub(/^[[:space:]]+|[[:space:]]+$/, "", s); return s }
    function val(line) {
      sub(/^[^=]*=[[:space:]]*/, "", line)
      sub(/[[:space:]]*#.*/, "", line)
      line = trim(line)
      if (line ~ /^".*"$/) { sub(/^"/, "", line); sub(/"$/, "", line) }
      return line
    }
    function emit() {
      if (name != "" || title != "" || file != "") {
        print name "\t" title "\t" file "\t" hint "\t" solution
      }
    }
    /^[[:space:]]*\[\[exercises\]\][[:space:]]*$/ { emit(); name=""; title=""; file=""; hint=""; solution=""; next }
    $1 == "name" { name = val($0); next }
    $1 == "title" { title = val($0); next }
    $1 == "file" { file = val($0); next }
    $1 == "hint" { hint = val($0); next }
    $1 == "solution" { solution = val($0); next }
    END { emit() }
  ' "$MANIFEST_FILE")

  [[ "${#NAMES[@]}" -gt 0 ]] || { echo "error: manifest has no exercises: $MANIFEST_FILE" >&2; exit 1; }
}

current_index() { cat "$STATE_FILE" 2>/dev/null || echo 0; }
set_current_index() { echo "$1" > "$STATE_FILE"; }
count() { printf '%s' "${#NAMES[@]}"; }
sanitize_current_index() {
  local i
  i="$(current_index)"
  if ! [[ "$i" =~ ^[0-9]+$ ]] || [[ "$i" -ge "$(count)" ]]; then
    set_current_index 0
  fi
}
current_name() { local i; i="$(current_index)"; printf '%s' "${NAMES[$i]}"; }
current_title() { local i; i="$(current_index)"; printf '%s' "${TITLES[$i]}"; }
current_file() { local i; i="$(current_index)"; printf '%s/%s' "$EX_DIR" "${FILES[$i]}"; }
current_hint() { local i; i="$(current_index)"; printf '%s/%s' "$META_DIR" "${HINTS[$i]}"; }
is_done() { grep -qx "$1" "$DONE_FILE" 2>/dev/null; }
mark_done() { is_done "$1" || echo "$1" >> "$DONE_FILE"; }
mark_not_done() { grep -vx "$1" "$DONE_FILE" > "$DONE_FILE.tmp" 2>/dev/null || true; mv "$DONE_FILE.tmp" "$DONE_FILE"; }

mkdir -p "$META_DIR"
[[ -f "$STATE_FILE" ]] || echo 0 > "$STATE_FILE"
[[ -f "$DONE_FILE" ]] || : > "$DONE_FILE"
load_manifest
sanitize_current_index

header() {
  clear 2>/dev/null || true
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
      echo "Press 4 for the next exercise."
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

file_mtime() {
  local file="$1"
  stat -f %m "$file" 2>/dev/null || stat -c %Y "$file" 2>/dev/null
}

watch_current() {
  local file last_mtime="" mtime key
  while true; do
    file="$(current_file)"
    mtime="$(file_mtime "$file" || echo missing)"
    if [[ "$mtime" != "$last_mtime" ]]; then
      last_mtime="$mtime"
      run_current
      echo
      printf '%bWatching for saves.%b Edit in another terminal, press Enter to re-run, or press q to return.\n' "$CYAN$BOLD" "$RESET"
    fi

    key=""
    if IFS= read -r -s -t 1 -n 1 key; then
      case "$key" in
        q|Q) return 0 ;;
        *) last_mtime="" ;;
      esac
    elif [[ ! -t 0 ]]; then
      sleep 1
    fi
  done
}

show_hint() {
  header
  local hint
  hint="$(current_hint)"
  printf '%bHint%b\n' "$BOLD$YELLOW" "$RESET"; line; echo
  [[ -f "$hint" ]] && cat "$hint" || echo "No hint found: $hint"
}

edit_current() {
  header
  echo "Opening $(current_file) in $EDITOR_CMD"
  echo "Save and quit, then press Enter to check."
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
  local answer=""
  read -r -p "Reset $name? [y/N] " answer || answer=""
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

wait_with_spinner() {
  local pid="$1"
  local label="$2"

  if [[ ! -t 1 ]]; then
    wait "$pid"
    return $?
  fi

  local frames=("⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏")
  local i=0
  while kill -0 "$pid" 2>/dev/null; do
    printf '\r%b%s%b %s' "$CYAN$BOLD" "${frames[$i]}" "$RESET" "$label"
    i=$(((i + 1) % ${#frames[@]}))
    sleep 0.12
  done

  local status=0
  wait "$pid" || status=$?
  if [[ "$status" -eq 0 ]]; then
    printf '\r\033[2K%b✓%b Pi answered.\n\n' "$GREEN$BOLD" "$RESET"
  else
    printf '\r\033[2K%b✗%b Pi stopped with exit %s.\n\n' "$RED$BOLD" "$RESET" "$status"
  fi
  return "$status"
}

ask_pi() {
  header
  local prompt="/skill:allium We are doing Alliumlings exercise $(current_name). Please explain what is wrong in exercises/${FILES[$(current_index)]} slowly. Do not edit unless I ask. Give one small next step."
  if [[ -n "$PI_PANE" ]] && command -v tmux >/dev/null 2>&1 && tmux display-message -p -t "$PI_PANE" '#{pane_id}' >/dev/null 2>&1; then
    # Literal input plus a delayed Enter is required by Pi's TUI. Sending the
    # prompt and C-m in one tmux call only fills the editor without submitting.
    tmux send-keys -t "$PI_PANE" -l -- "$prompt"
    sleep 0.2
    tmux send-keys -t "$PI_PANE" Enter
    printf '%bSent prompt to Pi pane.%b\n\n' "$GREEN$BOLD" "$RESET"
    printf '%b%s%b\n' "$MAGENTA" "$prompt" "$RESET"
  elif command -v pi >/dev/null 2>&1 && [[ -f "$RUN_DIR/skills/allium/SKILL.md" && -f "$REPO_ROOT/tutorial/pi/isolation-guard.ts" ]]; then
    printf '%bAsking Pi…%b\n\n' "$GREEN$BOLD" "$RESET"
    local status
    local -a pi_selection_args=(--thinking "$PI_THINKING")
    if [[ -n "$PI_MODEL" ]]; then
      pi_selection_args+=(--model "$PI_MODEL")
    fi
    printf '%bModel:%b %s  %bThinking:%b %s\n\n' "$DIM" "$RESET" "${PI_MODEL:-user default}" "$DIM" "$RESET" "$PI_THINKING"
    local output_file
    output_file="$(mktemp "$RUN_DIR/tmp/ask-pi.XXXXXX")"
    set +e
    pi "${pi_selection_args[@]}" \
      --session-dir "$RUN_DIR/sessions" \
      --no-context-files \
      --no-prompt-templates \
      --no-skills \
      --skill "$RUN_DIR/skills/allium/SKILL.md" \
      --no-extensions \
      -e "$REPO_ROOT/tutorial/pi/isolation-guard.ts" \
      --no-approve \
      --tools read,edit,write,bash \
      --name "Alliumlings: Inline Pi Help" \
      --append-system-prompt "Teach slowly. Do not edit unless explicitly asked. Keep the answer focused on the current exercise." \
      -p "$prompt" >"$output_file" 2>&1 &
    local pi_pid=$!
    wait_with_spinner "$pi_pid" "Pi is thinking…"
    status=$?
    set -e
    cat "$output_file"
    rm -f "$output_file"
    if [[ "$status" -ne 0 ]]; then
      echo
      printf '%bPi could not answer (exit %s). Copy this prompt into Pi:%b\n\n%s\n' "$YELLOW$BOLD" "$status" "$RESET" "$prompt"
    fi
  else
    printf '%bPi or the Allium skill is unavailable. Copy this into Pi:%b\n\n%s\n' "$YELLOW$BOLD" "$RESET" "$prompt"
  fi
}

quit_all() {
  header
  local answer=""
  read -r -p "Quit Alliumlings? [y/N] " answer || answer=""
  case "$answer" in
    y|Y|yes|YES)
      # Only close a tmux session explicitly created by the tutor launcher.
      # The single-terminal mode may itself run inside an unrelated tmux host
      # (for example Patina's Pi session), which must remain alive.
      if [[ -n "$TMUX_SESSION" ]] && command -v tmux >/dev/null 2>&1; then
        tmux kill-session -t "$TMUX_SESSION" 2>/dev/null || true
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
    printf '  %b7%b      Watch for saves / auto-rerun\n' "$CYAN$BOLD" "$RESET"
    printf '  %bq%b      Quit\n\n' "$CYAN$BOLD" "$RESET"
    local choice=""
    if ! read -r -p "$(paint "$BOLD$YELLOW" "Choice: ")" choice; then
      echo
      return 0
    fi
    case "${choice:-}" in
      ""|r|R|run|check) run_current; pause ;;
      1|e|E|edit) edit_current ;;
      2|h|H|hint) show_hint; pause ;;
      3|a|A|ask) ask_pi; pause ;;
      4|n|N|next) next_exercise ;;
      5|l|L|list|progress) list_exercises; pause ;;
      6|x|X|reset) reset_current; pause ;;
      7|w|W|watch|auto|auto-watch) watch_current ;;
      q|Q|quit|exit) quit_all ;;
      *) header; echo "Unknown choice. Press Enter to check, or choose 1–7."; pause ;;
    esac
  done
}

case "${1:-watch}" in
  watch|menu) menu ;;
  auto-watch|watch-file|watch-save) watch_current ;;
  run) run_current ;;
  hint) show_hint ;;
  edit) edit_current ;;
  next) next_exercise ;;
  reset) reset_current ;;
  list) list_exercises ;;
  ask) ask_pi ;;
  *) echo "Usage: alliumlings.sh [watch|auto-watch|run|hint|edit|next|reset|list|ask]" >&2; exit 1 ;;
esac
