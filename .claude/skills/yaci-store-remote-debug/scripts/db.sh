#!/usr/bin/env bash
#
# db.sh — run SQL against a remote yaci-store postgres, with credentials,
#         container and schema all auto-discovered from the running host.
#
# Usage:
#   db.sh --host HOST [--as USER] [--container NAME] [--schema S] [--csv] -- "SQL"
#   db.sh --host HOST [--as USER] --info
#
# Nothing is hardcoded: the DB user and database come from the container's own
# environment, and the schema is found by locating the table that yaci-store
# always creates. Never assume a user/db/schema name — deployments differ.

set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REMOTE="$HERE/remote.sh"

HOST=""; AS=""; CONTAINER=""; SCHEMA=""; CSV=0; INFO=0
die() { echo "db.sh: $*" >&2; exit 2; }

while [ $# -gt 0 ]; do
  case "$1" in
    --host) HOST="${2:-}"; shift 2 ;;
    --as) AS="${2:-}"; shift 2 ;;
    --container) CONTAINER="${2:-}"; shift 2 ;;
    --schema) SCHEMA="${2:-}"; shift 2 ;;
    --csv) CSV=1; shift ;;
    --info) INFO=1; shift ;;
    --) shift; break ;;
    -h|--help) sed -n '2,13p' "$0"; exit 0 ;;
    *) die "unknown option: $1" ;;
  esac
done
[ -n "$HOST" ] || die "--host is required"

rem() { local a=(--host "$HOST"); [ -n "$AS" ] && a+=(--as "$AS"); "$REMOTE" "${a[@]}" -- "$1"; }

# --- discover the postgres container -------------------------------------
# A benchmark host often runs several instances side by side. Picking one
# silently is how you end up debugging the wrong database, so if there is
# more than one we list them and stop.
PGLIST="$(rem 'docker ps --format "{{.Names}}\t{{.Image}}" 2>/dev/null | awk -F"\t" "\$2 ~ /postgres/ {print \$1}"')"
if [ -z "$CONTAINER" ]; then
  N="$(printf '%s\n' "$PGLIST" | grep -c . || true)"
  if [ "$N" -eq 0 ]; then die "no running postgres container on $HOST; pass --container"
  elif [ "$N" -gt 1 ]; then
    { echo "db.sh: $HOST runs $N postgres containers — pass --container to choose:";
      printf '%s\n' "$PGLIST" | sed 's/^/  /'; } >&2
    exit 2
  fi
  CONTAINER="$(printf '%s\n' "$PGLIST" | head -1)"
fi

# --- credentials straight from the container's environment ----------------
ENVOUT="$(rem "docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' '$CONTAINER'")"
PGUSER="$(printf '%s\n' "$ENVOUT" | sed -n 's/^POSTGRES_USER=//p'   | head -1)"
PGDB="$(  printf '%s\n' "$ENVOUT" | sed -n 's/^POSTGRES_DB=//p'     | head -1)"
PGUSER="${PGUSER:-postgres}"; PGDB="${PGDB:-$PGUSER}"

# --- schema: find the one holding yaci-store's cursor table ---------------
# Far more reliable than guessing a name or parsing currentSchema= out of a
# JDBC URL that may not be readable from here.
if [ -z "$SCHEMA" ]; then
  Q="select table_schema from information_schema.tables where table_name in ('cursor_','cursor') order by 1 limit 1;"
  SCHEMA="$(rem "docker exec -i '$CONTAINER' psql -U '$PGUSER' -d '$PGDB' -tAc \"$Q\" 2>/dev/null" | tr -d '[:space:]')"
  SCHEMA="${SCHEMA:-public}"
fi

if [ $INFO -eq 1 ]; then
  echo "container=$CONTAINER user=$PGUSER db=$PGDB schema=$SCHEMA"
  exit 0
fi

SQL="$*"
[ -n "$SQL" ] || die "no SQL given (did you forget '--'?)"

# search_path first, so unqualified table names resolve. Without it psql
# reports 'relation "block" does not exist' on a perfectly healthy database.
FULL="set search_path=$SCHEMA; $SQL"
B64="$(printf '%s' "$FULL" | base64 | tr -d '\n')"
FLAGS="-v ON_ERROR_STOP=1"; [ $CSV -eq 1 ] && FLAGS="$FLAGS --csv"

rem "echo '$B64' | base64 -d | docker exec -i '$CONTAINER' psql -U '$PGUSER' -d '$PGDB' $FLAGS -f -"
