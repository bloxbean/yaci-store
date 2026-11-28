#!/bin/bash
# ADR 0008 M0: engine-level probes on 2.0 (P2, P4, p10) and opt-in deletion vectors.
# Needs DUCKDB2 and WORK.  ./files.sh
set -u
: "${DUCKDB2:?}" "${WORK:?}"
W=$WORK/X; rm -rf "$W"; mkdir -p "$W/data" "$W/legacy/t/date=2024-01-01" "$W/pq"
q() { "$DUCKDB2" -no-agent -list -noheader :memory: -c "SET extension_directories=['$WORK/ext2']; LOAD ducklake;" -c "$1" 2>&1 | grep -E '^## |Error' ; }
"$DUCKDB2" -no-agent :memory: -c "COPY (SELECT range AS id FROM range(10)) TO '$W/legacy/t/date=2024-01-01/data.parquet'" \
  -c "COPY (SELECT 1 AS a, 2 AS b) TO '$W/pq/old.parquet'" -c "COPY (SELECT 3 AS a, 'x' AS x, 4 AS b) TO '$W/pq/new.parquet'"
q "ATTACH 'ducklake:$W/cat.ducklake' AS lake (DATA_PATH '$W/data/');
   CREATE TABLE lake.t (id BIGINT, date DATE); ALTER TABLE lake.t SET PARTITIONED BY (date);
   CALL ducklake_add_data_files('lake', 't', '$W/legacy/t/date=2024-01-01/data.parquet', hive_partitioning => true);
   SELECT '## P2 ' || string_agg(date || ' | ' || c, ', ') FROM (SELECT date, count(*) c FROM lake.t GROUP BY date);"
q "SELECT '## P4 old-first columns: ' || string_agg(column_name, ',') FROM (DESCRIBE SELECT * FROM read_parquet(['$W/pq/old.parquet', '$W/pq/new.parquet']));"
# reads every column, so x must be found in old.parquet (count(*) alone is pushed down and succeeds)
"$DUCKDB2" -no-agent -list -noheader :memory: -c "SELECT string_agg(a || '|' || x || '|' || b, ', ') FROM read_parquet(['$W/pq/new.parquet', '$W/pq/old.parquet']);" 2>&1 \
  | grep -m1 -o 'schema mismatch in glob: column "x"[^,]*' | sed 's/^/## P4 new-first: /'
q "SELECT '## P4 union_by_name: ' || string_agg(a || '|' || b || '|' || coalesce(x, 'NULL'), ', ' ORDER BY a) FROM read_parquet(['$W/pq/old.parquet', '$W/pq/new.parquet'], union_by_name => true);"
q "SELECT '## p10 hive_types + union_by_name: ' || count(*) || ' rows, date ' || min(date) || ' ' || typeof(min(date)) FROM read_parquet('$W/legacy/*/*/*.parquet', hive_partitioning => true, hive_types => {'date': 'DATE'}, union_by_name => true);"
# hive_types with a bare type identifier (what ParquetReadConnectionProvider.hiveTypesClause emits today)
"$DUCKDB2" -no-agent -list -noheader :memory: -c "SET table_function_identifier_conversion='DISABLE_IMPLICIT_STRING'" \
  -c "SELECT count(*) FROM read_parquet('$W/legacy/*/*/*.parquet', hive_partitioning => true, hive_types => {'date': DATE});" 2>&1 \
  | grep -m1 -o 'Binder Error: Deprecated implicit conversion[^.]*' | sed 's/^/## p10b bare type, strict mode: /'
q "ATTACH 'ducklake:$W/dv.ducklake' AS lake (DATA_PATH '$W/data/', WRITE_DELETION_VECTORS true, DATA_INLINING_ROW_LIMIT 0);
   CREATE TABLE lake.dv (id INTEGER, date DATE); ALTER TABLE lake.dv SET PARTITIONED BY (date);
   INSERT INTO lake.dv SELECT i, DATE '2024-05-01' FROM range(20) r(i);
   INSERT INTO lake.dv SELECT i, DATE '2024-05-02' FROM range(20) r(i);
   DELETE FROM lake.dv WHERE date = DATE '2024-05-02' AND id < 5;
   BEGIN; DELETE FROM lake.dv WHERE date = DATE '2024-05-01'; INSERT INTO lake.dv SELECT i, DATE '2024-05-01' FROM range(20) r(i); COMMIT;
   SELECT '## DV opt-in files: ' || string_agg(regexp_extract(data_file, 'date=[0-9-]+') || ' delete_file=' || coalesce(regexp_extract(delete_file, '[^.]+$'), 'NULL'), '; ' ORDER BY data_file) FROM ducklake_list_files('lake', 'dv');"
