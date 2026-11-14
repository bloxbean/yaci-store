#!/bin/bash
# ADR 0008 M0: run core.sql across the catalog matrix, plus the catalog-compatibility cases.
# Needs: DUCKDB2 (2.0.0 CLI), DUCKDB15 (1.5.5 CLI), DUCKDB14 (1.4.1 CLI), WORK (scratch dir),
# and a PostgreSQL reachable through PGCONN with empty databases cat11 and cat10.
#   PGCONN="host=localhost port=55433 user=postgres password=..." ./matrix.sh
set -u
HERE=$(cd "$(dirname "$0")" && pwd)
: "${DUCKDB2:?}" "${DUCKDB15:?}" "${DUCKDB14:?}" "${WORK:?}" "${PGCONN:?}"
E2=$WORK/ext2; E15=$WORK/ext15; E14=$WORK/ext14; mkdir -p "$E2" "$E15" "$E14"
fresh() { rm -rf "$WORK/$1"; mkdir -p "$WORK/$1/data"; echo "$WORK/$1"; }
seed() { # cli extdir attach-target datapath
  echo "SET extension_directory='$2'; INSTALL ducklake; INSTALL postgres; LOAD ducklake; LOAD postgres;
        ATTACH 'ducklake:$3' AS lake (DATA_PATH '$4'); CREATE TABLE lake.seed AS SELECT range AS id FROM range(3);
        SELECT 'seed spec ' || value FROM __ducklake_metadata_lake.ducklake_metadata WHERE key='version';" \
    | "$1" -list :memory: 2>&1 | grep -E 'spec|Error'; }
attach2() { # target datapath extra-options -> spec line
  echo "SET extension_directories=['$E2']; LOAD ducklake; LOAD postgres;
        ATTACH 'ducklake:$1' AS lake (DATA_PATH '$2'$3);
        SELECT 'spec after 2.0 attach: ' || value FROM __ducklake_metadata_lake.ducklake_metadata WHERE key='version';" \
    | "$DUCKDB2" -no-agent -list :memory: 2>&1 | grep -E 'spec|Error'; }

echo "### A: 2.0, new file catalog (1.1)"; W=$(fresh A); "$HERE/run.sh" "$DUCKDB2" "$E2" "ATTACH 'ducklake:$W/cat.ducklake' AS lake (DATA_PATH '$W/data/');" "$W/out.txt"
echo "### B: 2.0, new file catalog, DUCKLAKE_VERSION 1.0"; W=$(fresh B); "$HERE/run.sh" "$DUCKDB2" "$E2" "ATTACH 'ducklake:$W/cat.ducklake' AS lake (DATA_PATH '$W/data/', DUCKLAKE_VERSION '1.0');" "$W/out.txt"
echo "### B15: 1.5.5 baseline, new file catalog (1.0)"; W=$(fresh B15); "$HERE/run.sh" "$DUCKDB15" "$E15" "ATTACH 'ducklake:$W/cat.ducklake' AS lake (DATA_PATH '$W/data/');" "$W/out.txt"
echo "### C: 1.0 file catalog from 1.5.5, then 2.0 without migration"; W=$(fresh C); seed "$DUCKDB15" "$E15" "$W/cat.ducklake" "$W/data/"
"$HERE/run.sh" "$DUCKDB2" "$E2" "ATTACH 'ducklake:$W/cat.ducklake' AS lake (DATA_PATH '$W/data/');" "$W/out.txt"
echo "--- C: 1.5.5 re-opens it"; echo "SET extension_directory='$E15'; LOAD ducklake; ATTACH 'ducklake:$W/cat.ducklake' AS lake (DATA_PATH '$W/data/', READ_ONLY); SELECT count(*) FROM lake.seed;" | "$DUCKDB15" -list :memory: 2>&1 | tail -1
echo "### C2: 1.0 file catalog from 1.5.5, 2.0 with AUTOMATIC_MIGRATION true"; W=$(fresh C2); seed "$DUCKDB15" "$E15" "$W/cat.ducklake" "$W/data/"; attach2 "$W/cat.ducklake" "$W/data/" ", AUTOMATIC_MIGRATION true"
for m in none auto; do
  echo "### F-$m: 0.3 file catalog from 1.4.1, 2.0 migration=$m"; W=$(fresh F-$m); seed "$DUCKDB14" "$E14" "$W/cat.ducklake" "$W/data/"
  o=""; [ $m = auto ] && o=", AUTOMATIC_MIGRATION true"; attach2 "$W/cat.ducklake" "$W/data/" "$o"
done
echo "### D: 2.0, new PostgreSQL catalog"; W=$(fresh D); "$HERE/run.sh" "$DUCKDB2" "$E2" "ATTACH 'ducklake:postgres:dbname=cat11 $PGCONN' AS lake (DATA_PATH '$W/data/');" "$W/out.txt"
echo "### E: 1.0 PostgreSQL catalog from 1.5.5, then 2.0 without migration"; W=$(fresh E); seed "$DUCKDB15" "$E15" "postgres:dbname=cat10 $PGCONN" "$W/data/"
"$HERE/run.sh" "$DUCKDB2" "$E2" "ATTACH 'ducklake:postgres:dbname=cat10 $PGCONN' AS lake (DATA_PATH '$W/data/');" "$W/out.txt"
echo "--- E: 1.5.5 re-opens it"; echo "SET extension_directory='$E15'; LOAD ducklake; LOAD postgres; ATTACH 'ducklake:postgres:dbname=cat10 $PGCONN' AS lake (DATA_PATH '$W/data/', READ_ONLY); SELECT 'seed=' || (SELECT count(*) FROM lake.seed) || ' t=' || (SELECT count(*) FROM lake.t);" | "$DUCKDB15" -list :memory: 2>&1 | tail -1
