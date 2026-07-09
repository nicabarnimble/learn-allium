#!/usr/bin/env bash
set -euo pipefail

LESSON_ID="${ALLIUM_TUTOR_LESSON_ID:-01-first-rule}"
WORKSPACE="${ALLIUM_TUTOR_WORKSPACE:-$(pwd)}"
STARTER="${ALLIUM_TUTOR_STARTER:-$WORKSPACE/library-starter.allium}"
SOLUTION="${ALLIUM_TUTOR_SOLUTION:-$WORKSPACE/../library-solution.allium}"
FIXTURE="${ALLIUM_TUTOR_FIXTURE:-$STARTER}"
PI_PANE="${ALLIUM_TUTOR_PI_PANE:-}"
EDITOR_CMD="${VISUAL:-${EDITOR:-nano}}"

cd "$WORKSPACE"

# Soft, high-contrast ANSI palette. Respects NO_COLOR and non-TTY output.
RESET="" BOLD="" DIM="" CYAN="" BLUE="" GREEN="" YELLOW="" RED="" MAGENTA="" MUTED=""
if [[ -t 1 && -z "${NO_COLOR:-}" ]] && command -v tput >/dev/null 2>&1; then
  if [[ "$(tput colors 2>/dev/null || echo 0)" -ge 8 ]]; then
    RESET="$(tput sgr0)"
    BOLD="$(tput bold)"
    DIM="$(tput dim 2>/dev/null || true)"
    CYAN="$(tput setaf 6)"
    BLUE="$(tput setaf 4)"
    GREEN="$(tput setaf 2)"
    YELLOW="$(tput setaf 3)"
    RED="$(tput setaf 1)"
    MAGENTA="$(tput setaf 5)"
    MUTED="$DIM"
  fi
fi

paint() { printf '%b%s%b' "$1" "$2" "$RESET"; }

short_path() {
  local path="$1"
  local max="${2:-76}"
  if (( ${#path} <= max )); then
    printf '%s' "$path"
  else
    printf '…%s' "${path: -$((max - 1))}"
  fi
}

line() {
  local width="${COLUMNS:-80}"
  local rule
  rule="$(printf '%*s' "$width" '' | tr ' ' '─')"
  printf '%b%s%b\n' "$MUTED" "$rule" "$RESET"
}

pause() {
  echo
  read -r -n 1 -s -p "$(paint "$DIM" "Press any key to return to the menu...")" || true
}

header() {
  clear
  printf '%b%s%b\n' "$BOLD$CYAN" "Allium Tutor — Lesson 01" "$RESET"
  printf '%bMain pane:%b %bguided lesson%b     %bPi pane:%b %bagent helper%b\n' "$DIM" "$RESET" "$GREEN" "$RESET" "$DIM" "$RESET" "$MAGENTA" "$RESET"
  printf '%bWorkspace:%b %s\n' "$DIM" "$RESET" "$(short_path "$WORKSPACE" "${COLUMNS:-80}")"
  printf '%bFile:%b      %b%s%b\n' "$DIM" "$RESET" "$YELLOW" "$(basename "$STARTER")" "$RESET"
  line
}

card() {
  local title="$1"
  shift
  header
  printf '%b%s%b\n' "$BOLD$YELLOW" "$title" "$RESET"
  line
  echo
  printf '%s\n' "$@"
  pause
}

start_here() {
  card "Start here: what are we doing?" \
    "You are adding exactly ONE missing Allium rule." \
    "" \
    "Plain-English behavior:" \
    "  When a member borrows an available book, the book becomes borrowed," \
    "  records the borrower, receives a due date, and emits a BookBorrowed fact." \
    "" \
    "Ignore the busy Pi pane for now. This left pane is the lesson." \
    "Use the menu, one small step at a time." \
    "" \
    "Recommended path:" \
    "  1. Run the first check and see the expected failure." \
    "  2. Ask Pi to explain the TODO without editing." \
    "  3. Edit the missing rule yourself." \
    "  4. Check your work until green."
}

summarize_gate() {
  local check_output="$1"
  local check_status="$2"
  local analyse_output="$3"
  local analyse_status="$4"

  if [[ "$check_status" -eq 0 && "$analyse_status" -eq 0 ]]; then
    printf '%b✅ Clean.%b allium check and allium analyse both passed.\n' "$GREEN$BOLD" "$RESET"
    echo
    echo "You completed the core loop for Lesson 01."
    return 0
  fi

  if grep -q "allium.status.unreachableValue" <<<"$check_output"; then
    printf '%b⚠️ Expected first failure:%b borrowed is never assigned.\n' "$YELLOW$BOLD" "$RESET"
    echo
    echo "That means the spec has a borrowed state, but no rule currently moves"
    echo "the book into that state. The missing BorrowBook rule is exactly what"
    echo "will fix this."
    echo
    printf '%bNext:%b choose %bAsk Pi to explain the TODO%b or %bShow a hint%b.\n' "$CYAN" "$RESET" "$MAGENTA" "$RESET" "$MAGENTA" "$RESET"
    return 0
  fi

  printf '%b⚠️ The gate is not clean yet.%b\n' "$YELLOW$BOLD" "$RESET"
  echo
  echo "check exit:   $check_status"
  echo "analyse exit: $analyse_status"
  echo
  printf '%bRaw check output:%b\n' "$DIM" "$RESET"
  echo "$check_output"
  echo
  printf '%bRaw analyse output:%b\n' "$DIM" "$RESET"
  echo "$analyse_output"
}

run_gate() {
  header
  printf '%bRunning deterministic gate...%b\n' "$BOLD$BLUE" "$RESET"
  echo
  printf '  %ballium check%b %s\n' "$CYAN" "$RESET" "$(basename "$STARTER")"
  printf '  %ballium analyse%b %s\n' "$CYAN" "$RESET" "$(basename "$STARTER")"
  echo

  set +e
  local check_output analyse_output check_status analyse_status
  check_output="$(allium check "$STARTER" 2>&1)"
  check_status=$?
  analyse_output="$(allium analyse "$STARTER" 2>&1)"
  analyse_status=$?
  set -e

  summarize_gate "$check_output" "$check_status" "$analyse_output" "$analyse_status"
  pause
}

send_to_pi() {
  local prompt="$1"
  header

  if [[ -z "$PI_PANE" ]]; then
    printf '%bNo Pi pane is registered.%b Copy this prompt into Pi manually:\n' "$YELLOW" "$RESET"
    echo
    echo "$prompt"
    pause
    return 0
  fi

  if ! command -v tmux >/dev/null 2>&1 || ! tmux display-message -p -t "$PI_PANE" '#{pane_id}' >/dev/null 2>&1; then
    printf '%bCould not reach the Pi tmux pane.%b Copy this prompt into Pi manually:\n' "$YELLOW" "$RESET"
    echo
    echo "$prompt"
    pause
    return 0
  fi

  tmux send-keys -t "$PI_PANE" "$prompt" C-m
  printf '%bSent this prompt to the Pi pane:%b\n' "$GREEN$BOLD" "$RESET"
  echo
  printf '%b%s%b\n' "$MAGENTA" "$prompt" "$RESET"
  echo
  echo "Watch the Pi pane for the answer. When you are ready, come back here."
  echo
  printf '%btmux tip:%b Ctrl-b then Left/Right switches panes.\n' "$DIM" "$RESET"
  pause
}

ask_pi_explain() {
  send_to_pi "/skill:allium We are in Allium Tutor lesson 01. Explain library-starter.allium slowly, focusing only on the TODO for BorrowBook. Do not edit files yet. Teach one idea at a time."
}

ask_pi_edit() {
  send_to_pi "/skill:allium We are in Allium Tutor lesson 01. Make the smallest safe edit to library-starter.allium: add only the missing BorrowBook rule at the TODO. Do not read the solution file. Then run allium check library-starter.allium and allium analyse library-starter.allium."
}

edit_file() {
  header
  printf '%bOpening%b %s %bin:%b %s\n' "$BOLD$BLUE" "$RESET" "$(basename "$STARTER")" "$DIM" "$RESET" "$EDITOR_CMD"
  echo
  echo "Add only the missing BorrowBook rule where the TODO appears."
  echo "Save and quit your editor, then choose 'Check my work'."
  echo
  read -r -n 1 -s -p "Press any key to open the editor..." || true
  $EDITOR_CMD "$STARTER"
}

show_hint() {
  card "Hint: shape of the missing rule" \
    "Add this where the TODO appears:" \
    "" \
    "rule BorrowBook {" \
    "    when: MemberBorrowsBook(member)" \
    "    requires: book.status = on_shelf" \
    "    ensures: book.status = borrowed" \
    "    ensures: book.borrowed_by = member.name" \
    "    ensures: book.due_at = now + config.loan_period" \
    "    ensures: BookBorrowed(book: book, member: member)" \
    "}" \
    "" \
    "Read it as: event → case guard → state change → attribution → deadline → emitted fact."
}

show_answer() {
  header
  printf '%bAnswer comparison%b\n' "$BOLD$YELLOW" "$RESET"
  line
  echo
  if [[ -f "$SOLUTION" ]]; then
    awk '/^rule BorrowBook /,/^}/ { print }' "$SOLUTION" | sed "s/^/${CYAN}/; s/$/${RESET}/"
  else
    echo "Solution file not found: $SOLUTION"
  fi
  echo
  echo "Your answer can differ in comments/spacing, but the behavior should match."
  pause
}

reset_file() {
  header
  printf '%bReset the starter file?%b\n' "$BOLD$YELLOW" "$RESET"
  echo
  echo "This will replace: $STARTER"
  echo "with the original lesson fixture."
  echo
  read -r -p "Reset? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES)
      cp "$FIXTURE" "$STARTER"
      echo "Reset complete."
      ;;
    *)
      echo "Reset cancelled."
      ;;
  esac
  pause
}

open_file_path() {
  card "Where is my work?" \
    "Your editable lesson file is:" \
    "" \
    "  $STARTER" \
    "" \
    "This is a disposable copy under .tutorial/runs/." \
    "The course examples are not edited."
}

quit_tutor() {
  header
  printf '%bQuit Allium Tutor?%b\n' "$BOLD$YELLOW" "$RESET"
  echo
  echo "This will close the tutor pane and the Pi helper pane."
  echo "Your lesson files remain saved under:"
  printf '  %b%s%b\n' "$CYAN" "$(short_path "${ALLIUM_TUTOR_RUN_DIR:-$WORKSPACE}" "${COLUMNS:-80}")" "$RESET"
  echo
  read -r -p "Quit and close tmux session? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES)
      clear
      printf '%bClosing Allium Tutor...%b\n' "$GREEN" "$RESET"
      sleep 0.3
      if [[ -n "${TMUX:-}" ]] && command -v tmux >/dev/null 2>&1; then
        local session
        session="$(tmux display-message -p '#S' 2>/dev/null || true)"
        if [[ -n "$session" ]]; then
          tmux kill-session -t "$session"
        fi
      fi
      exit 0
      ;;
    *)
      return 0
      ;;
  esac
}

menu_item() {
  local key="$1"
  local text="$2"
  printf '  %b%2s%b) %s\n' "$CYAN$BOLD" "$key" "$RESET" "$text"
}

menu() {
  while true; do
    header
    printf '%bChoose one small step:%b\n\n' "$BOLD" "$RESET"
    menu_item "1" "Start here: what are we doing?"
    menu_item "2" "Run the first check and see the expected failure"
    menu_item "3" "Ask Pi to explain the TODO slowly, without editing"
    menu_item "4" "Edit the missing rule yourself"
    menu_item "5" "Check my work"
    menu_item "6" "Show a hint"
    menu_item "7" "Let Pi make the smallest safe edit"
    menu_item "8" "Compare with the answer"
    menu_item "9" "Reset the starter file"
    menu_item "f" "Show the file path for my work"
    menu_item "q" "Quit tutor and close tmux"
    echo
    printf '%bTip:%b Ctrl-b then Left/Right switches panes.\n\n' "$DIM" "$RESET"
    read -r -n 1 -p "$(paint "$BOLD$YELLOW" "Selection: ")" choice
    echo
    case "$choice" in
      1) start_here ;;
      2) run_gate ;;
      3) ask_pi_explain ;;
      4) edit_file ;;
      5) run_gate ;;
      6) show_hint ;;
      7) ask_pi_edit ;;
      8) show_answer ;;
      9) reset_file ;;
      f|F) open_file_path ;;
      q|Q) quit_tutor ;;
      *) card "Unknown selection" "Choose a number from the menu." ;;
    esac
  done
}

menu
