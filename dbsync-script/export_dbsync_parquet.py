#!/usr/bin/env python3
"""Export Cardano db-sync tables to Parquet format with resolved foreign keys.

Simple version: psycopg2 + pyarrow only (no DuckDB).
Best for running on the same server or LAN as the db-sync PostgreSQL database.

Strategy:
  1. PostgreSQL does JOINs (fast locally, ~2s per epoch with indexes)
  2. COPY TO STDOUT streams results via PostgreSQL's native protocol
  3. pyarrow converts CSV to Parquet with ZSTD compression
"""

import argparse
import datetime
import io
import os
import sys
import time

import psycopg2
import pyarrow as pa
import pyarrow.csv as pa_csv
import pyarrow.parquet as pq

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
    "START_EPOCH": "504",
}
# =============================================================================

# Tables exported epoch-by-epoch for progress tracking
EPOCH_PARTITIONED = {"epoch_stake", "reward"}

# Tables whose output filename is suffixed with the start epoch
EPOCH_SUFFIXED = {"epoch_stake", "reward", "drep_distr", "reward_rest"}

TABLE_CONFIGS = {
    "epoch_stake": {
        "sql": """
            SELECT es.epoch_no, sa.view AS stake_address, ph.view AS pool, es.amount
            FROM epoch_stake es
            JOIN stake_address sa ON es.addr_id = sa.id
            JOIN pool_hash ph ON es.pool_id = ph.id
            WHERE es.epoch_no = {epoch}
        """,
        "epoch_query": "SELECT DISTINCT epoch_no FROM epoch_stake WHERE epoch_no >= {start_epoch} ORDER BY epoch_no",
        "filename": "epoch_stake_from{start_epoch}.parquet",
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
        "epoch_query": "SELECT DISTINCT earned_epoch AS epoch_no FROM reward WHERE earned_epoch >= {start_epoch} ORDER BY earned_epoch",
        "filename": "reward_from{start_epoch}.parquet",
    },
    "drep_distr": {
        "sql_all": """
            SELECT dd.epoch_no, dh.view AS drep_id, dh.has_script,
                   dd.amount, dd.active_until
            FROM drep_distr dd
            JOIN drep_hash dh ON dd.hash_id = dh.id
            WHERE dd.epoch_no >= {start_epoch}
        """,
        "filename": "drep_distr_from{start_epoch}.parquet",
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
        "filename": "reward_rest_from{start_epoch}.parquet",
    },
}


def resolve_filename(table_name, config, start_epoch):
    return config["filename"].format(start_epoch=start_epoch)


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


def pg_copy_to_buffer(pg_conn, sql):
    """COPY query results to an in-memory BytesIO buffer via PostgreSQL's native protocol."""
    buf = io.BytesIO()
    copy_sql = f"COPY ({sql}) TO STDOUT WITH (FORMAT CSV, HEADER, NULL '')"
    with pg_conn.cursor() as cur:
        cur.copy_expert(copy_sql, buf)
    buf.seek(0)
    return buf


def buffer_to_table(buf):
    """Convert a CSV buffer to a pyarrow Table."""
    read_opts = pa_csv.ReadOptions()
    parse_opts = pa_csv.ParseOptions()
    convert_opts = pa_csv.ConvertOptions(strings_can_be_null=True)
    return pa_csv.read_csv(buf, read_options=read_opts, parse_options=parse_opts,
                           convert_options=convert_opts)


def write_parquet(table, filepath):
    """Write a pyarrow Table to Parquet with ZSTD compression, atomically."""
    tmp_path = filepath + ".tmp"
    pq.write_table(table, tmp_path, compression="zstd", compression_level=3)
    os.replace(tmp_path, filepath)


def export_epoch_partitioned(pg_conn, table_name, config, output_dir, start_epoch):
    """Export a large table epoch-by-epoch, then merge into one parquet file."""
    filename = resolve_filename(table_name, config, start_epoch)
    filepath = os.path.join(output_dir, filename)
    epoch_dir = os.path.join(output_dir, f".{table_name}_epochs")
    os.makedirs(epoch_dir, exist_ok=True)

    # Get list of epochs
    with pg_conn.cursor() as cur:
        cur.execute(config["epoch_query"].format(start_epoch=start_epoch))
        epochs = [row[0] for row in cur.fetchall()]

    if not epochs:
        log(f"  No epochs found for {table_name} >= {start_epoch}")
        return 0, filename

    log(f"  Found {len(epochs)} epochs to export: {epochs[0]} - {epochs[-1]}")

    total_rows = 0
    table_start = time.time()
    epoch_files = []

    for i, epoch in enumerate(epochs):
        epoch_start = time.time()
        epoch_file = os.path.join(epoch_dir, f"epoch_{epoch}.parquet")

        # Step 1: PostgreSQL JOIN + COPY TO STDOUT -> memory buffer
        sql = config["sql"].format(epoch=epoch)
        t1 = time.time()
        buf = pg_copy_to_buffer(pg_conn, sql)
        csv_size = buf.getbuffer().nbytes
        copy_time = time.time() - t1

        # Step 2: CSV buffer -> pyarrow Table -> Parquet
        t2 = time.time()
        table = buffer_to_table(buf)
        write_parquet(table, epoch_file)
        parquet_time = time.time() - t2

        row_count = table.num_rows
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
            f"  Epoch {epoch}: {row_count:,} rows, "
            f"PG COPY {format_size(csv_size)} in {copy_time:.1f}s, "
            f"Parquet {format_size(file_size)} in {parquet_time:.1f}s | "
            f"Total: {total_rows:,} rows, {format_duration(total_elapsed)} elapsed, "
            f"ETA: {format_duration(eta_seconds)} ({epochs_done}/{len(epochs)})"
        )

    # Merge all epoch parquet files into one
    log(f"  Merging {len(epoch_files)} epoch files into {filename}...")
    merge_start = time.time()

    writer = pq.ParquetWriter(filepath + ".tmp", pq.read_schema(epoch_files[0]),
                              compression="zstd", compression_level=3)
    for ef in epoch_files:
        t = pq.read_table(ef)
        writer.write_table(t)
    writer.close()
    os.replace(filepath + ".tmp", filepath)

    merge_elapsed = time.time() - merge_start
    log(f"  Merge complete in {format_duration(merge_elapsed)}")

    # Cleanup epoch files
    for f in epoch_files:
        os.remove(f)
    os.rmdir(epoch_dir)

    return total_rows, filename


def export_simple(pg_conn, table_name, config, output_dir, start_epoch):
    """Export a small table in one shot."""
    filename = resolve_filename(table_name, config, start_epoch)
    filepath = os.path.join(output_dir, filename)

    sql = config["sql_all"].format(start_epoch=start_epoch)

    t1 = time.time()
    buf = pg_copy_to_buffer(pg_conn, sql)
    csv_size = buf.getbuffer().nbytes
    copy_time = time.time() - t1
    log(f"  PG COPY: {format_size(csv_size)} in {copy_time:.1f}s")

    t2 = time.time()
    table = buffer_to_table(buf)
    write_parquet(table, filepath)
    parquet_time = time.time() - t2
    log(f"  Parquet: {table.num_rows:,} rows in {parquet_time:.1f}s")

    return table.num_rows, filename


def export_table(pg_conn, table_name, config, output_dir, start_epoch):
    """Export a single table and print summary."""
    log(f"Exporting {table_name}...")
    start = time.time()

    if table_name in EPOCH_PARTITIONED:
        row_count, filename = export_epoch_partitioned(pg_conn, table_name, config, output_dir, start_epoch)
    else:
        row_count, filename = export_simple(pg_conn, table_name, config, output_dir, start_epoch)

    elapsed = time.time() - start
    filepath = os.path.join(output_dir, filename)
    file_size = os.path.getsize(filepath) if os.path.exists(filepath) else 0

    log(
        f"DONE {table_name}: {row_count:,} rows, "
        f"{format_size(file_size)}, {format_duration(elapsed)} -> {filename}"
    )
    print(flush=True)


def main():
    parser = argparse.ArgumentParser(
        description="Export Cardano db-sync tables to Parquet (psycopg2 + pyarrow)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Configuration priority: CLI args > .env file / env vars > DEFAULTS in script.

Examples:
  python3 export_dbsync_parquet.py --env-file .env
  python3 export_dbsync_parquet.py --env-file .env --tables drep_hash
  python3 export_dbsync_parquet.py --pg-host localhost --pg-user admin \\
      --pg-password secret --pg-database dbsync --output-dir /data/parquet
        """,
    )

    parser.add_argument(
        "--tables", nargs="+",
        choices=list(TABLE_CONFIGS.keys()),
        default=list(TABLE_CONFIGS.keys()),
        help="Tables to export (default: all)",
    )
    parser.add_argument(
        "--output-dir", default=None,
        help="Output directory for parquet files",
    )
    parser.add_argument(
        "--env-file", default=None,
        help="Path to .env file to load DB connection variables from",
    )
    parser.add_argument(
        "--start-epoch", type=int, default=None,
        help="Starting epoch for filtered exports (default: from env START_EPOCH or DEFAULTS)",
    )

    db_group = parser.add_argument_group("database connection (override env vars / defaults)")
    db_group.add_argument("--pg-host", default=None, help="PostgreSQL host")
    db_group.add_argument("--pg-port", default=None, help="PostgreSQL port")
    db_group.add_argument("--pg-user", default=None, help="PostgreSQL user")
    db_group.add_argument("--pg-password", default=None, help="PostgreSQL password")
    db_group.add_argument("--pg-database", default=None, help="PostgreSQL database name")

    args = parser.parse_args()

    if args.env_file:
        load_env_file(args.env_file)

    db_config = resolve_config(args)

    output_dir = args.output_dir or os.environ.get("OUTPUT_DIR", DEFAULTS["OUTPUT_DIR"])
    os.makedirs(output_dir, exist_ok=True)

    if args.start_epoch is not None:
        start_epoch = args.start_epoch
    else:
        start_epoch = int(os.environ.get("START_EPOCH", DEFAULTS["START_EPOCH"]))

    log(f"Output directory: {os.path.abspath(output_dir)}")
    log(f"Start epoch: {start_epoch}")
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

    log(f"Starting export of {len(args.tables)} table(s): {', '.join(args.tables)}")
    print(flush=True)

    overall_start = time.time()
    try:
        for table_name in args.tables:
            export_table(pg_conn, table_name, TABLE_CONFIGS[table_name], output_dir, start_epoch)
    finally:
        pg_conn.close()

    overall_elapsed = time.time() - overall_start
    log(f"All exports complete in {format_duration(overall_elapsed)}")


if __name__ == "__main__":
    main()
