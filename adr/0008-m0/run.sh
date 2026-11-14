#!/bin/bash
# usage: run.sh <duckdb-cli> <ext-dir> <attach-sql> <out-file>
D=$1; EXT=$2; ATTACH=$3; OUT=$4
if "$D" -no-agent :memory: -c "SELECT 1" >/dev/null 2>&1; then FLAGS=-no-agent; SETEXT="SET extension_directories=['$EXT'];"; else FLAGS=; SETEXT="SET extension_directory='$EXT';"; fi
{ echo "$SETEXT INSTALL ducklake; INSTALL postgres; LOAD ducklake; LOAD postgres;"
  echo "$ATTACH"
  cat "$(dirname "$0")/core.sql"; } | "$D" $FLAGS :memory: > "$OUT" 2>&1
grep -E '^## |Error' "$OUT"
