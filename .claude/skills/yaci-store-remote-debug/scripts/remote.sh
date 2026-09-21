#!/usr/bin/env bash
#
# remote.sh — run a command on a remote yaci-store host, handling the sudo hop.
#
# Usage:
#   remote.sh --host HOST [--as USER] [--probe] [--raw] -- COMMAND...
#   remote.sh --host HOST [--as USER] --probe
#
# The command is base64-encoded before transport, so it survives both the ssh
# shell and the remote shell without quoting gymnastics. Write the command
# exactly as you would type it locally — quotes, $, globs, pipes all work.
#
# Exit codes: 0 ok, 2 usage error, 3 ssh/auth failure.

set -euo pipefail

HOST=""; AS=""; PROBE=0; RAW=0
SSH_OPTS=(-o BatchMode=yes -o ConnectTimeout=15)

die() { echo "remote.sh: $*" >&2; exit 2; }

while [ $# -gt 0 ]; do
  case "$1" in
    --host)  HOST="${2:-}"; shift 2 ;;
    --as)    AS="${2:-}";   shift 2 ;;
    --probe) PROBE=1;       shift ;;
    --raw)   RAW=1;         shift ;;
    --)      shift; break ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) die "unknown option: $1" ;;
  esac
done

[ -n "$HOST" ] || die "--host is required"
CMD="$*"
[ $PROBE -eq 1 ] || [ -n "$CMD" ] || die "no command given (did you forget '--'?)"

# A login shell under sudo's use_pty leaks terminal escapes (classically
# ESC%G, the UTF-8 charset selector) onto the FIRST line of output. Left in,
# that silently corrupts every parsed value. Strip ESC-sequences unless --raw.
sanitize() {
  if [ $RAW -eq 1 ]; then cat
  else sed -e $'s/\033\[[0-9;?]*[A-Za-z]//g' -e $'s/\033[%()][A-Za-z0-9]//g' -e $'s/\033[=>]//g'
  fi
}

b64() { printf '%s' "$1" | base64 | tr -d '\n'; }

run_direct() { ssh "${SSH_OPTS[@]}" "$HOST" "base64 -d <<<'$(b64 "$1")' | bash -s"                        | sanitize; }
run_root()   { ssh "${SSH_OPTS[@]}" "$HOST" "base64 -d <<<'$(b64 "$1")' | sudo -n bash -s"                | sanitize; }
run_sudo_u() { ssh "${SSH_OPTS[@]}" "$HOST" "base64 -d <<<'$(b64 "$1")' | sudo -n -u '$AS' bash -s"       | sanitize; }
# Login su ('-') keeps the target user's PATH/env, which a bare-JVM deployment
# may depend on. Needed when sudoers grants '(root) NOPASSWD: ALL' — root is
# allowed but other runas targets are not, so 'sudo -u USER' fails.
run_su()     { ssh "${SSH_OPTS[@]}" "$HOST" "base64 -d <<<'$(b64 "$1")' | sudo -n su -s /bin/bash - '$AS'" | sanitize; }

CACHE="${TMPDIR:-/tmp}/.yaci-remote-mode.$(printf '%s' "$HOST/$AS" | tr -c 'A-Za-z0-9' _)"

# Compare on alphanumerics only — defends against any escape that slips through.
norm() { tr -cd 'A-Za-z0-9'; }

detect_mode() {
  if [ -n "$AS" ]; then
    local want; want=$(printf '%s' "$AS" | norm)
    [ "$(run_sudo_u 'whoami' 2>/dev/null | norm || true)" = "$want" ] && { echo sudo_u; return; }
    [ "$(run_su    'whoami' 2>/dev/null | norm || true)" = "$want" ] && { echo su;     return; }
    {
      echo "remote.sh: cannot become '$AS' on $HOST without a password."
      echo "  Run: ssh $HOST 'sudo -n -l'"
      echo "  A '(root) NOPASSWD: ALL' rule permits root but NOT other runas"
      echo "  targets; the su fallback then needs that root grant to work."
    } >&2
    exit 3
  fi
  run_direct 'true' >/dev/null 2>&1 && { echo direct; return; }
  echo "remote.sh: cannot reach $HOST non-interactively (key not loaded?)" >&2
  exit 3
}

if [ -s "$CACHE" ] && [ $PROBE -eq 0 ]; then MODE="$(cat "$CACHE")"
else MODE="$(detect_mode)"; printf '%s' "$MODE" > "$CACHE"; fi

if [ $PROBE -eq 1 ]; then
  echo "host=$HOST as=${AS:-<login user>} mode=$MODE"
  ssh "${SSH_OPTS[@]}" "$HOST" 'echo "login=$(whoami) docker_group=$(id -nG | grep -qw docker && echo yes || echo no)"; echo "utc=$(date -u +%FT%TZ)  local=$(date +%FT%T%z)"' | sanitize
  [ -n "$CMD" ] || exit 0
fi

case "$MODE" in
  direct) run_direct "$CMD" ;;
  root)   run_root   "$CMD" ;;
  sudo_u) run_sudo_u "$CMD" ;;
  su)     run_su     "$CMD" ;;
esac
