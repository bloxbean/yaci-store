#!/usr/bin/env python3
"""Export Cardano db-sync tables to Parquet format with resolved foreign keys.

Strategy:
  1. PostgreSQL does the JOINs (fast, ~2s per epoch with indexes)
  2. COPY TO STDOUT streams results via PostgreSQL's native protocol (fastest path)
  3. DuckDB converts CSV to Parquet with ZSTD compression

This avoids postgres_scanner's slow wire protocol for large data transfers.
"""

import argparse
import datetime
import os
import sys
import time

import duckdb
import psycopg2

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

# Tables exported epoch-by-epoch for progress tracking
EPOCH_PARTITIONED = {"epoch_stake", "reward"}

TABLE_CONFIGS = {
    "epoch_stake": {
        "sql": """
            SELECT es.epoch_no, sa.view AS stake_address, ph.view AS pool, es.amount
            FROM epoch_stake es
            JOIN stake_address sa ON es.addr_id = sa.id
            JOIN pool_hash ph ON es.pool_id = ph.id
            WHERE es.epoch_no = {epoch}
        """,
        "sql_all": """
            SELECT es.epoch_no, sa.view AS stake_address, ph.view AS pool, es.amount
            FROM epoch_stake es
            JOIN stake_address sa ON es.addr_id = sa.id
            JOIN pool_hash ph ON es.pool_id = ph.id
            WHERE es.epoch_no >= {start_epoch}
            ORDER BY es.epoch_no
        """,
        "epoch_query": "SELECT DISTINCT epoch_no FROM epoch_stake WHERE epoch_no >= {start_epoch} ORDER BY epoch_no",
        "filename": "epoch_stake_from504.parquet",
    },
    "reward": {
        "sql": """
            SELECT sa.view AS stake_address, r.type::text AS type, r.amount,
                   r.earned_epoch, r.spendable_epoch, ph.view AS pool
            FROM reward r
            JOIN stake_address sa ON r.addr_id = sa.id
            JOIN pool_hash ph ON r.pool_id = ph.id
            WHERE r.earned_epoch = {epoch}
        """,
        "sql_all": """
            SELECT sa.view AS stake_address, r.type::text AS type, r.amount,
                   r.earned_epoch, r.spendable_epoch, ph.view AS pool
            FROM reward r
            JOIN stake_address sa ON r.addr_id = sa.id
            JOIN pool_hash ph ON r.pool_id = ph.id
            WHERE r.earned_epoch >= {start_epoch}
            ORDER BY r.earned_epoch
        """,
        "epoch_query": "SELECT DISTINCT earned_epoch AS epoch_no FROM reward WHERE earned_epoch >= {start_epoch} ORDER BY earned_epoch",
        "filename": "reward_from504.parquet",
    },
    "drep_distr": {
        "sql_all": """
            SELECT dd.epoch_no, dh.view AS drep_id, dh.has_script,
                   dd.amount, dd.active_until
            FROM drep_distr dd
            JOIN drep_hash dh ON dd.hash_id = dh.id
            WHERE dd.epoch_no >= {start_epoch}
        """,
        "filename": "drep_distr_from504.parquet",
    },
    "drep_hash": {
        "sql_all": "SELECT encode(raw, 'hex') AS raw, view, has_script FROM drep_hash",
        "filename": "drep_hash.parquet",
    },
    "drep_registration": {
        "sql_all": """
            SELECT dh.view AS drep_id, dh.has_script, dr.deposit,
                   dr.cert_index, dr.tx_id, dr.voting_anchor_id
            FROM drep_registration dr
            JOIN drep_hash dh ON dr.drep_hash_id = dh.id
        """,
        "filename": "drep_registration.parquet",
    },
    "reward_rest": {
        "sql_all": """
            SELECT sa.view AS stake_address, rr.type::text AS type, rr.amount,
                   rr.earned_epoch, rr.spendable_epoch
            FROM reward_rest rr
            JOIN stake_address sa ON rr.addr_id = sa.id
            WHERE rr.earned_epoch >= {start_epoch}
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


def pg_copy_to_csv(pg_conn, sql, csv_path):
    """Use PostgreSQL COPY TO STDOUT to stream query results to a local CSV file.

    This is the fastest way to extract data from PostgreSQL — uses the native
    COPY protocol with minimal overhead, much faster than row-by-row fetch
    or postgres_scanner.
    """
    copy_sql = f"COPY ({sql}) TO STDOUT WITH (FORMAT CSV, HEADER, NULL '')"
    with open(csv_path, "wb") as f:
        with pg_conn.cursor() as cur:
            cur.copy_expert(copy_sql, f)


def csv_to_parquet(duck_conn, csv_path, parquet_path, codec="ZSTD", compression_level=3):
    """Convert a CSV file to Parquet using DuckDB."""
    tmp_path = parquet_path + ".tmp"
    duck_conn.execute(
        f"COPY (SELECT * FROM read_csv('{csv_path}', header=true, auto_detect=true, null_padding=true)) "
        f"TO '{tmp_path}' (FORMAT PARQUET, CODEC '{codec}', COMPRESSION_LEVEL {compression_level})"
    )
    os.replace(tmp_path, parquet_path)


def get_row_count(duck_conn, filepath):
    """Get row count from a parquet file via DuckDB."""
    result = duck_conn.execute(f"SELECT COUNT(*) FROM read_parquet('{filepath}')").fetchone()
    return result[0]


def export_epoch_partitioned(pg_conn, duck_conn, table_name, config, output_dir):
    """Export a large table epoch-by-epoch, then combine into one parquet file."""
    filepath = os.path.join(output_dir, config["filename"])
    epoch_dir = os.path.join(output_dir, f".{table_name}_epochs")
    os.makedirs(epoch_dir, exist_ok=True)

    # Get list of epochs
    with pg_conn.cursor() as cur:
        cur.execute(config["epoch_query"].format(start_epoch=START_EPOCH))
        epochs = [row[0] for row in cur.fetchall()]

    if not epochs:
        log(f"  No epochs found for {table_name} >= {START_EPOCH}")
        return 0

    log(f"  Found {len(epochs)} epochs to export: {epochs[0]} - {epochs[-1]}")

    total_rows = 0
    table_start = time.time()
    epoch_files = []

    for i, epoch in enumerate(epochs):
        epoch_start = time.time()
        epoch_csv = os.path.join(epoch_dir, f"epoch_{epoch}.csv")
        epoch_parquet = os.path.join(epoch_dir, f"epoch_{epoch}.parquet")

        # Step 1: PostgreSQL JOIN + COPY TO STDOUT -> CSV
        sql = config["sql"].format(epoch=epoch)
        t1 = time.time()
        pg_copy_to_csv(pg_conn, sql, epoch_csv)
        csv_time = time.time() - t1
        csv_size = os.path.getsize(epoch_csv)

        # Step 2: DuckDB CSV -> Parquet
        t2 = time.time()
        csv_to_parquet(duck_conn, epoch_csv, epoch_parquet)
        parquet_time = time.time() - t2

        # Cleanup CSV immediately
        os.remove(epoch_csv)

        row_count = get_row_count(duck_conn, epoch_parquet)
        file_size = os.path.getsize(epoch_parquet)
        epoch_elapsed = time.time() - epoch_start
        total_rows += row_count
        total_elapsed = time.time() - table_start

        epoch_files.append(epoch_parquet)

        # Calculate ETA
        epochs_done = i + 1
        epochs_remaining = len(epochs) - epochs_done
        avg_per_epoch = total_elapsed / epochs_done
        eta_seconds = avg_per_epoch * epochs_remaining

        log(
            f"  Epoch {epoch}: {row_count:,} rows, "
            f"CSV {format_size(csv_size)} in {csv_time:.1f}s, "
            f"Parquet {format_size(file_size)} in {parquet_time:.1f}s | "
            f"Total: {total_rows:,} rows, {format_duration(total_elapsed)} elapsed, "
            f"ETA: {format_duration(eta_seconds)} ({epochs_done}/{len(epochs)})"
        )

    # Merge all epoch parquet files into one
    log(f"  Merging {len(epoch_files)} epoch files into {config['filename']}...")
    merge_start = time.time()
    glob_pattern = os.path.join(epoch_dir, "epoch_*.parquet")
    duck_conn.execute(
        f"COPY (SELECT * FROM read_parquet('{glob_pattern}')) "
        f"TO '{filepath}.tmp' (FORMAT PARQUET, CODEC 'ZSTD', COMPRESSION_LEVEL 3)"
    )
    os.replace(filepath + ".tmp", filepath)
    merge_elapsed = time.time() - merge_start
    log(f"  Merge complete in {format_duration(merge_elapsed)}")

    # Cleanup epoch files
    for f in epoch_files:
        os.remove(f)
    os.rmdir(epoch_dir)

    return total_rows


def export_simple(pg_conn, duck_conn, table_name, config, output_dir):
    """Export a small table in one shot."""
    filepath = os.path.join(output_dir, config["filename"])
    csv_path = filepath + ".csv"

    sql = config["sql_all"].format(start_epoch=START_EPOCH)

    t1 = time.time()
    pg_copy_to_csv(pg_conn, sql, csv_path)
    csv_time = time.time() - t1
    csv_size = os.path.getsize(csv_path)
    log(f"  CSV exported: {format_size(csv_size)} in {csv_time:.1f}s")

    t2 = time.time()
    csv_to_parquet(duck_conn, csv_path, filepath)
    parquet_time = time.time() - t2
    log(f"  Parquet converted in {parquet_time:.1f}s")

    os.remove(csv_path)
    return get_row_count(duck_conn, filepath)


def export_table(pg_conn, duck_conn, table_name, config, output_dir):
    """Export a single table and print summary."""
    log(f"Exporting {table_name}...")
    start = time.time()

    if table_name in EPOCH_PARTITIONED:
        row_count = export_epoch_partitioned(pg_conn, duck_conn, table_name, config, output_dir)
    else:
        row_count = export_simple(pg_conn, duck_conn, table_name, config, output_dir)

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
        description="Export Cardano db-sync tables to Parquet format",
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

    # Connect to PostgreSQL directly (for COPY TO STDOUT)
    log(f"Connecting to PostgreSQL: {db_config['host']}:{db_config['port']}/{db_config['database']}")
    pg_conn = psycopg2.connect(
        host=db_config["host"],
        port=db_config["port"],
        user=db_config["user"],
        password=db_config["password"],
        database=db_config["database"],
    )
    pg_conn.autocommit = True
    log("PostgreSQL connected.")

    # DuckDB for CSV -> Parquet conversion (local only, no postgres_scanner needed)
    duck_conn = duckdb.connect()

    log(f"Starting export of {len(args.tables)} table(s): {', '.join(args.tables)}")
    print(flush=True)

    overall_start = time.time()
    try:
        for table_name in args.tables:
            export_table(pg_conn, duck_conn, table_name, TABLE_CONFIGS[table_name], output_dir)
    finally:
        pg_conn.close()
        duck_conn.close()

    overall_elapsed = time.time() - overall_start
    log(f"All exports complete in {format_duration(overall_elapsed)}")


if __name__ == "__main__":
    main()
