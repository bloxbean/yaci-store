-- ADR 0008 M0 core probes. Expects: lake attached as `lake`, DATA_INLINING_ROW_LIMIT 0 for the
-- session (as DuckDbConnectionHelper.disableDuckLakeDataInlining does), and :META set by the runner.
.mode list
.headers off
SELECT '## engine ' || version() || ' ducklake ' || (SELECT extension_version FROM duckdb_extensions() WHERE extension_name='ducklake');
SELECT '## catalog spec ' || value FROM __ducklake_metadata_lake.ducklake_metadata WHERE key = 'version';
CALL lake.set_option('data_inlining_row_limit', 0);

-- P1: atomic whole-partition replace
CREATE TABLE lake.t (id INTEGER, date DATE);
ALTER TABLE lake.t SET PARTITIONED BY (date);
INSERT INTO lake.t SELECT i, DATE '2024-01-01' FROM range(100) r(i);
INSERT INTO lake.t SELECT i, DATE '2024-01-02' FROM range(50) r(i);
SELECT '## P1 before: snapshots=' || (SELECT max(snapshot_id) FROM lake.snapshots());
BEGIN;
DELETE FROM lake.t WHERE date = DATE '2024-01-01';
INSERT INTO lake.t SELECT i, DATE '2024-01-01' FROM range(100) r(i);
COMMIT;
SELECT '## P1 replace snapshot: ' || snapshot_id || ' changes=' || changes::VARCHAR FROM lake.snapshots() ORDER BY snapshot_id DESC LIMIT 1;
SELECT '## P1 files: ' || string_agg(regexp_extract(data_file, 'date=[0-9-]+') || ' delete_file=' || coalesce(delete_file, 'NULL'), '; ' ORDER BY data_file) FROM ducklake_list_files('lake', 't');
SELECT '## P1 counts: ' || string_agg(date || '=' || c, ', ' ORDER BY date) FROM (SELECT date, count(*) c FROM lake.t GROUP BY date);
-- P1b: partial delete
DELETE FROM lake.t WHERE date = DATE '2024-01-02' AND id < 10;
SELECT '## P1b partial delete files: ' || string_agg(regexp_extract(data_file, 'date=[0-9-]+') || ' delete_file=' || coalesce(regexp_extract(delete_file, '[^/]+$'), 'NULL'), '; ' ORDER BY data_file) FROM ducklake_list_files('lake', 't');

-- P3: positional insert vs BY NAME with ADD COLUMN
CREATE TABLE lake.a (x INTEGER, y INTEGER, z INTEGER);
INSERT INTO lake.a SELECT 1, 2, 3, 4;
ALTER TABLE lake.a ADD COLUMN extra VARCHAR;
INSERT INTO lake.a BY NAME SELECT 7 AS z, 5 AS x;
SELECT '## P3 by name: ' || string_agg(x || '|' || coalesce(y::VARCHAR,'NULL') || '|' || z || '|' || coalesce(extra,'NULL'), ', ') FROM lake.a;

-- p8: inlined delete under a non-zero inlining limit
CREATE TABLE lake.i (id INTEGER, date DATE);
ALTER TABLE lake.i SET PARTITIONED BY (date);
INSERT INTO lake.i SELECT i, DATE '2024-02-01' FROM range(5) r(i);
INSERT INTO lake.i SELECT i, DATE '2024-02-02' FROM range(50) r(i);
CALL lake.set_option('data_inlining_row_limit', 10);
BEGIN;
DELETE FROM lake.i WHERE date = DATE '2024-02-01';
INSERT INTO lake.i SELECT i, DATE '2024-02-01' FROM range(5) r(i);
COMMIT;
CALL lake.set_option('data_inlining_row_limit', 0);
SELECT '## p8 files: ' || string_agg(regexp_extract(data_file, 'date=[0-9-]+') || ' delete_file=' || coalesce(delete_file, 'NULL'), '; ' ORDER BY data_file) FROM ducklake_list_files('lake', 'i');
SELECT '## p8 inlined data tables: ' || coalesce(string_agg(table_name, ','), 'none') FROM __ducklake_metadata_lake.ducklake_inlined_data_tables;
SELECT '## p8 catalog count=' || count(*) FROM lake.i;
SET VARIABLE p8files = (SELECT list(data_file) FROM ducklake_list_files('lake', 'i'));
SELECT '## p8 file-list reader count=' || count(*) FROM read_parquet(getvariable('p8files'));

-- p12: a file holding several partition values (written before partitioning)
CREATE TABLE lake.m (id INTEGER, date DATE);
INSERT INTO lake.m SELECT i, CASE WHEN i < 5 THEN DATE '2024-03-01' ELSE DATE '2024-03-02' END FROM range(10) r(i);
ALTER TABLE lake.m SET PARTITIONED BY (date);
DELETE FROM lake.m WHERE date = DATE '2024-03-01';
SELECT '## p12 mixed-file delete: ' || string_agg('delete_file=' || coalesce(regexp_extract(delete_file, '[^/]+$'), 'NULL'), '; ') FROM ducklake_list_files('lake', 'm');

-- p14: commit marker in the same snapshot as the data
CREATE TABLE lake.e (epoch INTEGER, v INTEGER);
ALTER TABLE lake.e SET PARTITIONED BY (epoch);
INSERT INTO lake.e VALUES (450, 1), (450, 2);
BEGIN;
DELETE FROM lake.e WHERE epoch = 450;
INSERT INTO lake.e VALUES (450, 1), (450, 2);
CALL lake.set_commit_message('yaci-store', 'replace e/450', extra_info => '{"attempt_id":"A1"}');
COMMIT;
SELECT '## p14 marker: ' || snapshot_id || ' author=' || author || ' msg=' || commit_message || ' extra=' || commit_extra_info || ' changes=' || changes::VARCHAR FROM lake.snapshots() ORDER BY snapshot_id DESC LIMIT 1;

-- p15: no-change transaction records no snapshot
SELECT '## p15 before=' || max(snapshot_id) FROM lake.snapshots();
BEGIN;
DELETE FROM lake.e WHERE epoch = 999;
INSERT INTO lake.e SELECT * FROM (VALUES (999, 1)) v(epoch, v) WHERE false;
CALL lake.set_commit_message('yaci-store', 'replace e/999', extra_info => '{"attempt_id":"A2"}');
COMMIT;
SELECT '## p15 after=' || max(snapshot_id) FROM lake.snapshots();

-- p16: epoch key guard aborts the transaction
BEGIN;
DELETE FROM lake.e WHERE epoch = 450;
INSERT INTO lake.e SELECT q.epoch, q.v FROM (VALUES (NULL::INTEGER, 9)) q(epoch, v)
  WHERE CASE WHEN q.epoch IS DISTINCT FROM 450 THEN error('epoch key mismatch: ' || coalesce(q.epoch::VARCHAR, 'NULL')) ELSE true END;
COMMIT;
ROLLBACK;
SELECT '## p16 rows kept for 450: ' || count(*) FROM lake.e WHERE epoch = 450;

-- empty partition: no file
SELECT '## empty-partition files for 999: ' || count(*) FROM ducklake_list_files('lake', 'e') WHERE data_file LIKE '%epoch=999%';

-- p13 / p9: cleanup bounded by older_than, then cleanup_all
CALL ducklake_expire_snapshots('lake', older_than => now() + INTERVAL 1 MINUTE);
SELECT '## p13 cleanup older_than 1h dry-run files: ' || count(*) FROM ducklake_cleanup_old_files('lake', older_than => now() - INTERVAL 1 HOUR, dry_run => true);
SELECT '## p9 cleanup_all dry-run files: ' || count(*) FROM ducklake_cleanup_old_files('lake', cleanup_all => true, dry_run => true);
