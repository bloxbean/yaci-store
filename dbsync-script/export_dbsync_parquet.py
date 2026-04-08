#!/usr/bin/env python3
"""Export Cardano db-sync tables to Parquet format with resolved foreign keys.

Uses DuckDB + postgres_scanner for high-performance columnar streaming.
Lookup tables (stake_address, pool_hash, drep_hash) are loaded into DuckDB
memory once, then raw data is streamed from PostgreSQL WITHOUT JOINs.
JOINs happen inside DuckDB's vectorized engine — much faster than PostgreSQL.
"""

import argparse
import datetime
import os
import sys
import time

import duckdb

# =============================================================================
# DEFAULT CONFIGURATION
# Edit these values to hardcode your settings directly in the script.
# These are used as fallbacks when no CLI argument or env variable is provided.
# Priority: CLI args > .env file / env vars > defaults below
# =============================================================================
DEFAULTS = {
    "PGHOST": "localhost",
    "PGPORT": "5432",
    "PGUSER": "",
    "PGPASSWORD": "",
    "PGDATABASE": "dbsync",
    "OUTPUT_DIR": ".",
}
# =============================================================================

# Starting epoch for filtered exports
START_EPOCH = 504

# Lookup tables to cache in DuckDB memory before exporting.
# These are loaded once and reused for all JOINs.
LOOKUP_TABLES = {
    "stake_address": "SELECT id, view FROM stake_address",
    "pool_hash": "SELECT id, view FROM pool_hash",
    "drep_hash": "SELECT id, encode(raw, 'hex') AS raw, view, has_script FROM drep_hash",
}

# Which lookup tables each export table needs
TABLE_LOOKUPS = {
    "epoch_stake": ["stake_address", "pool_hash"],
    "reward": ["stake_address", "pool_hash"],
    "reward_rest": ["stake_address"],
    "drep_distr": ["drep_hash"],
    "drep_hash": [],
    "drep_registration": ["drep_hash"],
}

# Tables exported epoch-by-epoch for progress tracking
EPOCH_PARTITIONED = {"epoch_stake", "reward"}

TABLE_CONFIGS = {
    "epoch_stake": {
        # Raw query: NO JOINs, just IDs — fast PostgreSQL index scan
        "raw_sql": "SELECT epoch_no, addr_id, pool_id, amount FROM epoch_stake WHERE epoch_no = {epoch}",
        "raw_sql_all": "SELECT epoch_no, addr_id, pool_id, amount FROM epoch_stake WHERE epoch_no >= {start_epoch}",
        "epoch_query": "SELECT DISTINCT epoch_no FROM epoch_stake WHERE epoch_no >= {start_epoch} ORDER BY epoch_no",
        # DuckDB-side JOIN with cached lookup tables
        "join_sql": """
            SELECT es.epoch_no, sa.view AS stake_address, ph.view AS pool, es.amount
            FROM raw_data es
            JOIN stake_address sa ON es.addr_id = sa.id
            JOIN pool_hash ph ON es.pool_id = ph.id
        """,
        "filename": "epoch_stake_from504.parquet",
    },
    "reward": {
        "raw_sql": "SELECT addr_id, type::text AS type, amount, earned_epoch, spendable_epoch, pool_id FROM reward WHERE earned_epoch = {epoch}",
        "raw_sql_all": "SELECT addr_id, type::text AS type, amount, earned_epoch, spendable_epoch, pool_id FROM reward WHERE earned_epoch >= {start_epoch}",
        "epoch_query": "SELECT DISTINCT earned_epoch AS epoch_no FROM reward WHERE earned_epoch >= {start_epoch} ORDER BY earned_epoch",
        "join_sql": """
            SELECT sa.view AS stake_address, r.type, r.amount,
                   r.earned_epoch, r.spendable_epoch, ph.view AS pool
            FROM raw_data r
            JOIN stake_address sa ON r.addr_id = sa.id
            JOIN pool_hash ph ON r.pool_id = ph.id
        """,
        "filename": "reward_from504.parquet",
    },
    "drep_distr": {
        "raw_sql_all": "SELECT hash_id, epoch_no, amount, active_until FROM drep_distr WHERE epoch_no >= {start_epoch}",
        "join_sql": """
            SELECT dd.epoch_no, dh.view AS drep_id, dh.has_script, dd.amount, dd.active_until
            FROM raw_data dd
            JOIN drep_hash dh ON dd.hash_id = dh.id
        """,
        "filename": "drep_distr_from504.parquet",
    },
    "drep_hash": {
        "raw_sql_all": "SELECT encode(raw, 'hex') AS raw, view, has_script FROM drep_hash",
        "join_sql": "SELECT * FROM raw_data",
        "filename": "drep_hash.parquet",
    },
    "drep_registration": {
        "raw_sql_all": "SELECT drep_hash_id, deposit, cert_index, tx_id, voting_anchor_id FROM drep_registration",
        "join_sql": """
            SELECT dh.view AS drep_id, dh.has_script, dr.deposit,
                   dr.cert_index, dr.tx_id, dr.voting_anchor_id
            FROM raw_data dr
            JOIN drep_hash dh ON dr.drep_hash_id = dh.id
        """,
        "filename": "drep_registration.parquet",
    },
    "reward_rest": {
        "raw_sql_all": "SELECT addr_id, type::text AS type, amount, earned_epoch, spendable_epoch FROM reward_rest WHERE earned_epoch >= {start_epoch}",
        "join_sql": """
            SELECT sa.view AS stake_address, rr.type, rr.amount,
                   rr.earned_epoch, rr.spendable_epoch
            FROM raw_data rr
            JOIN stake_address sa ON rr.addr_id = sa.id
        """,
        "filename": "reward_rest_from504.parquet",
    },
}


def log(msg):
    ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{ts}] {msg}", flush=True)


def format_size(size_bytes):
    if size_bytes >= 1_073_741_824:
        return f"{size_bytes / 1_073_741_824:.2f} GB"
    elif size_bytes >= 1_048_576:
        return f"{size_bytes / 1_048_576:.1f} MB"
    return f"{size_bytes / 1024:.1f} KB"


def format_duration(seconds):
    if seconds >= 3600:
        return f"{seconds / 3600:.1f}h"
    elif seconds >= 60:
        return f"{seconds / 60:.1f}m"
    return f"{seconds:.1f}s"


def load_env_file(env_file):
    """Load variables from a .env file into os.environ (does not override existing)."""
    if not os.path.isfile(env_file):
        log(f"Warning: env file '{env_file}' not found, skipping.")
        return
    with open(env_file) as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip("\"'")
            os.environ.setdefault(key, value)


def resolve_config(args):
    """Resolve configuration with priority: CLI args > env vars > DEFAULTS."""
    def pick(cli_val, env_key):
        if cli_val is not None:
            return cli_val
        return os.environ.get(env_key, DEFAULTS.get(env_key, ""))

    cfg = {
        "host": pick(args.pg_host, "PGHOST"),
        "port": pick(args.pg_port, "PGPORT"),
        "user": pick(args.pg_user, "PGUSER"),
        "password": pick(args.pg_password, "PGPASSWORD"),
        "database": pick(args.pg_database, "PGDATABASE"),
    }

    missing = [k for k in ("user", "password", "database") if not cfg[k]]
    if missing:
        log(f"Error: missing required DB config: {', '.join(missing)}")
        log("Provide via CLI args, env vars, .env file, or edit DEFAULTS in the script.")
        sys.exit(1)

    return cfg


def setup_duckdb(db_config):
    """Create DuckDB connection with postgres_scanner attached."""
    conn = duckdb.connect()
    conn.execute("INSTALL postgres_scanner")
    conn.execute("LOAD postgres_scanner")

    attach_str = (
        f"dbname={db_config['database']} "
        f"user={db_config['user']} "
        f"password={db_config['password']} "
        f"host={db_config['host']} "
        f"port={db_config['port']}"
    )
    conn.execute(f"ATTACH '{attach_str}' AS pg_db (TYPE POSTGRES, READ_ONLY)")
    log(f"DuckDB connected to PostgreSQL: {db_config['host']}:{db_config['port']}/{db_config['database']}")
    return conn


def load_lookup_tables(duck_conn, tables_needed):
    """Load lookup tables from PostgreSQL into DuckDB memory (once)."""
    for table_name in tables_needed:
        if table_name not in LOOKUP_TABLES:
            continue
        # Check if already loaded
        existing = duck_conn.execute(
            f"SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '{table_name}'"
        ).fetchone()[0]
        if existing:
            continue

        sql = LOOKUP_TABLES[table_name]
        start = time.time()
        duck_conn.execute(
            f"CREATE TABLE {table_name} AS "
            f"SELECT * FROM postgres_query('pg_db', $$ {sql} $$)"
        )
        row_count = duck_conn.execute(f"SELECT COUNT(*) FROM {table_name}").fetchone()[0]
        elapsed = time.time() - start
        log(f"  Cached {table_name}: {row_count:,} rows in {elapsed:.1f}s")


def copy_to_parquet(duck_conn, sql, filepath, codec="ZSTD", compression_level=3):
    """Execute COPY (sql) TO parquet via DuckDB."""
    tmp_path = filepath + ".tmp"
    copy_cmd = (
        f"COPY ({sql}) TO '{tmp_path}' "
        f"(FORMAT PARQUET, CODEC '{codec}', COMPRESSION_LEVEL {compression_level})"
    )
    duck_conn.execute(copy_cmd)
    os.replace(tmp_path, filepath)


def get_row_count(duck_conn, filepath):
    """Get row count from a parquet file via DuckDB."""
    result = duck_conn.execute(f"SELECT COUNT(*) FROM read_parquet('{filepath}')").fetchone()
    return result[0]


def export_epoch_partitioned(duck_conn, table_name, config, output_dir):
    """Export a large table epoch-by-epoch, then combine into one parquet file."""
    filepath = os.path.join(output_dir, config["filename"])
    epoch_dir = os.path.join(output_dir, f".{table_name}_epochs")
    os.makedirs(epoch_dir, exist_ok=True)

    # Get list of epochs to export
    epoch_sql = f"SELECT * FROM postgres_query('pg_db', $$ {config['epoch_query'].format(start_epoch=START_EPOCH)} $$)"
    epochs = [row[0] for row in duck_conn.execute(epoch_sql).fetchall()]

    if not epochs:
        log(f"  No epochs found for {table_name} >= {START_EPOCH}")
        duck_conn.execute("CREATE OR REPLACE TEMP TABLE raw_data AS SELECT 1 WHERE false")
        copy_to_parquet(duck_conn, config["join_sql"], filepath)
        return 0

    log(f"  Found {len(epochs)} epochs to export: {epochs[0]} - {epochs[-1]}")

    total_rows = 0
    table_start = time.time()
    epoch_files = []

    for i, epoch in enumerate(epochs):
        epoch_start = time.time()
        epoch_file = os.path.join(epoch_dir, f"epoch_{epoch}.parquet")

        # Stream raw data (no JOINs) from PostgreSQL into DuckDB temp table
        raw_sql = config["raw_sql"].format(epoch=epoch)
        duck_conn.execute(
            f"CREATE OR REPLACE TEMP TABLE raw_data AS "
            f"SELECT * FROM postgres_query('pg_db', $$ {raw_sql} $$)"
        )

        # JOIN with cached lookup tables in DuckDB and write to parquet
        copy_to_parquet(duck_conn, config["join_sql"], epoch_file)

        row_count = get_row_count(duck_conn, epoch_file)
        file_size = os.path.getsize(epoch_file)
        epoch_elapsed = time.time() - epoch_start
        total_rows += row_count
        total_elapsed = time.time() - table_start

        epoch_files.append(epoch_file)

        # Calculate ETA
        epochs_done = i + 1
        epochs_remaining = len(epochs) - epochs_done
        avg_per_epoch = total_elapsed / epochs_done
        eta_seconds = avg_per_epoch * epochs_remaining

        log(
            f"  Epoch {epoch}: {row_count:,} rows, {format_size(file_size)}, "
            f"{epoch_elapsed:.1f}s | "
            f"Total: {total_rows:,} rows, {format_duration(total_elapsed)} elapsed, "
            f"ETA: {format_duration(eta_seconds)} ({epochs_done}/{len(epochs)} epochs)"
        )

    # Merge all epoch files into one parquet file
    log(f"  Merging {len(epoch_files)} epoch files into {config['filename']}...")
    merge_start = time.time()
    glob_pattern = os.path.join(epoch_dir, "epoch_*.parquet")
    copy_to_parquet(duck_conn, f"SELECT * FROM read_parquet('{glob_pattern}')", filepath)
    merge_elapsed = time.time() - merge_start
    log(f"  Merge complete in {format_duration(merge_elapsed)}")

    # Cleanup epoch files
    for f in epoch_files:
        os.remove(f)
    os.rmdir(epoch_dir)

    return total_rows


def export_simple(duck_conn, table_name, config, output_dir):
    """Export a small table in one shot via DuckDB COPY."""
    filepath = os.path.join(output_dir, config["filename"])

    raw_sql = config["raw_sql_all"].format(start_epoch=START_EPOCH)
    duck_conn.execute(
        f"CREATE OR REPLACE TEMP TABLE raw_data AS "
        f"SELECT * FROM postgres_query('pg_db', $$ {raw_sql} $$)"
    )

    copy_to_parquet(duck_conn, config["join_sql"], filepath)
    return get_row_count(duck_conn, filepath)


def export_table(duck_conn, table_name, config, output_dir):
    """Export a single table and print summary."""
    log(f"Exporting {table_name}...")

    # Load required lookup tables into DuckDB memory
    needed_lookups = TABLE_LOOKUPS.get(table_name, [])
    if needed_lookups:
        load_lookup_tables(duck_conn, needed_lookups)

    start = time.time()

    if table_name in EPOCH_PARTITIONED:
        row_count = export_epoch_partitioned(duck_conn, table_name, config, output_dir)
    else:
        row_count = export_simple(duck_conn, table_name, config, output_dir)

    elapsed = time.time() - start
    filepath = os.path.join(output_dir, config["filename"])
    file_size = os.path.getsize(filepath)

    log(
        f"DONE {table_name}: {row_count:,} rows, "
        f"{format_size(file_size)}, {format_duration(elapsed)} -> {config['filename']}"
    )
    print(flush=True)


def main():
    parser = argparse.ArgumentParser(
        description="Export Cardano db-sync tables to Parquet format (DuckDB-accelerated)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Configuration priority: CLI args > .env file / env vars > DEFAULTS in script.

Examples:
  # Use .env file
  python export_dbsync_parquet.py --env-file .env

  # Pass everything via CLI
  python export_dbsync_parquet.py --pg-host db.example.com --pg-user admin \\
      --pg-password secret --pg-database dbsync --output-dir /data/parquet

  # Mix: env file for DB, CLI for output and table selection
  python export_dbsync_parquet.py --env-file .env --output-dir /data --tables drep_hash
        """,
    )

    # Table selection
    parser.add_argument(
        "--tables", nargs="+",
        choices=list(TABLE_CONFIGS.keys()),
        default=list(TABLE_CONFIGS.keys()),
        help="Tables to export (default: all)",
    )

    # Output directory
    parser.add_argument(
        "--output-dir", default=None,
        help=f"Output directory for parquet files (default: DEFAULTS['{DEFAULTS['OUTPUT_DIR']}'])",
    )

    # Env file
    parser.add_argument(
        "--env-file", default=None,
        help="Path to .env file to load DB connection variables from",
    )

    # DB connection overrides
    db_group = parser.add_argument_group("database connection (override env vars / defaults)")
    db_group.add_argument("--pg-host", default=None, help="PostgreSQL host")
    db_group.add_argument("--pg-port", default=None, help="PostgreSQL port")
    db_group.add_argument("--pg-user", default=None, help="PostgreSQL user")
    db_group.add_argument("--pg-password", default=None, help="PostgreSQL password")
    db_group.add_argument("--pg-database", default=None, help="PostgreSQL database name")

    args = parser.parse_args()

    # Load .env file if provided (before resolving config)
    if args.env_file:
        load_env_file(args.env_file)

    # Resolve DB config
    db_config = resolve_config(args)

    # Resolve output directory: CLI > env > DEFAULTS
    output_dir = args.output_dir or os.environ.get("OUTPUT_DIR", DEFAULTS["OUTPUT_DIR"])
    os.makedirs(output_dir, exist_ok=True)

    log(f"Output directory: {os.path.abspath(output_dir)}")

    # Setup DuckDB with postgres_scanner
    duck_conn = setup_duckdb(db_config)

    log(f"Starting export of {len(args.tables)} table(s): {', '.join(args.tables)}")
    print(flush=True)

    overall_start = time.time()
    try:
        for table_name in args.tables:
            export_table(duck_conn, table_name, TABLE_CONFIGS[table_name], output_dir)
    finally:
        duck_conn.close()

    overall_elapsed = time.time() - overall_start
    log(f"All exports complete in {format_duration(overall_elapsed)}")


if __name__ == "__main__":
    main()
