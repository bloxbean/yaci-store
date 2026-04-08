#!/usr/bin/env python3
"""Export Cardano db-sync tables to Parquet format with resolved foreign keys."""

import argparse
import os
import sys
import time
from decimal import Decimal

import psycopg2
import pyarrow as pa
import pyarrow.parquet as pq

BATCH_SIZE = 500_000

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

TABLE_CONFIGS = {
    "epoch_stake": {
        "sql": """
            SELECT es.epoch_no, sa.view AS stake_address, ph.view AS pool, es.amount
            FROM epoch_stake es
            JOIN stake_address sa ON es.addr_id = sa.id
            JOIN pool_hash ph ON es.pool_id = ph.id
            WHERE es.epoch_no >= 504
            ORDER BY es.epoch_no
        """,
        "schema": pa.schema([
            ("epoch_no", pa.int32()),
            ("stake_address", pa.string()),
            ("pool", pa.string()),
            ("amount", pa.int64()),
        ]),
        "is_huge": True,
        "filename": "epoch_stake_from504.parquet",
    },
    "reward": {
        "sql": """
            SELECT sa.view AS stake_address, r.type::text, r.amount,
                   r.earned_epoch, r.spendable_epoch, ph.view AS pool
            FROM reward r
            JOIN stake_address sa ON r.addr_id = sa.id
            JOIN pool_hash ph ON r.pool_id = ph.id
            WHERE r.earned_epoch >= 504
            ORDER BY r.earned_epoch
        """,
        "schema": pa.schema([
            ("stake_address", pa.string()),
            ("type", pa.string()),
            ("amount", pa.int64()),
            ("earned_epoch", pa.int64()),
            ("spendable_epoch", pa.int64()),
            ("pool", pa.string()),
        ]),
        "is_huge": True,
        "filename": "reward_from504.parquet",
    },
    "drep_distr": {
        "sql": """
            SELECT dd.epoch_no, dh.view AS drep_id, dh.has_script,
                   dd.amount, dd.active_until
            FROM drep_distr dd
            JOIN drep_hash dh ON dd.hash_id = dh.id
            WHERE dd.epoch_no >= 504
        """,
        "schema": pa.schema([
            ("epoch_no", pa.int32()),
            ("drep_id", pa.string()),
            ("has_script", pa.bool_()),
            ("amount", pa.int64()),
            ("active_until", pa.int32()),
        ]),
        "is_huge": False,
        "filename": "drep_distr_from504.parquet",
    },
    "drep_hash": {
        "sql": "SELECT encode(raw, 'hex') AS raw, view, has_script FROM drep_hash",
        "schema": pa.schema([
            ("raw", pa.string()),
            ("view", pa.string()),
            ("has_script", pa.bool_()),
        ]),
        "is_huge": False,
        "filename": "drep_hash.parquet",
    },
    "drep_registration": {
        "sql": """
            SELECT dh.view AS drep_id, dh.has_script, dr.deposit,
                   dr.cert_index, dr.tx_id, dr.voting_anchor_id
            FROM drep_registration dr
            JOIN drep_hash dh ON dr.drep_hash_id = dh.id
        """,
        "schema": pa.schema([
            ("drep_id", pa.string()),
            ("has_script", pa.bool_()),
            ("deposit", pa.int64()),
            ("cert_index", pa.int32()),
            ("tx_id", pa.int64()),
            ("voting_anchor_id", pa.int64()),
        ]),
        "is_huge": False,
        "filename": "drep_registration.parquet",
    },
    "reward_rest": {
        "sql": """
            SELECT sa.view AS stake_address, rr.type::text, rr.amount,
                   rr.earned_epoch, rr.spendable_epoch
            FROM reward_rest rr
            JOIN stake_address sa ON rr.addr_id = sa.id
            WHERE rr.earned_epoch >= 504
        """,
        "schema": pa.schema([
            ("stake_address", pa.string()),
            ("type", pa.string()),
            ("amount", pa.int64()),
            ("earned_epoch", pa.int64()),
            ("spendable_epoch", pa.int64()),
        ]),
        "is_huge": False,
        "filename": "reward_rest_from504.parquet",
    },
}


def load_env_file(env_file):
    """Load variables from a .env file into os.environ (does not override existing)."""
    if not os.path.isfile(env_file):
        print(f"Warning: env file '{env_file}' not found, skipping.")
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
        print(f"Error: missing required DB config: {', '.join(missing)}")
        print("Provide via CLI args, env vars, .env file, or edit DEFAULTS in the script.")
        sys.exit(1)

    return cfg


def get_connection(db_config):
    return psycopg2.connect(
        host=db_config["host"],
        port=db_config["port"],
        user=db_config["user"],
        password=db_config["password"],
        database=db_config["database"],
    )


def rows_to_record_batch(rows, schema):
    """Convert fetched rows to a pyarrow RecordBatch, handling Decimal -> int."""
    columns = {}
    for i, field in enumerate(schema):
        values = [row[i] for row in rows]
        if field.type in (pa.int64(), pa.int32()):
            values = [int(v) if v is not None else None for v in values]
        columns[field.name] = values
    return pa.RecordBatch.from_pydict(columns, schema=schema)


def export_huge_table(conn, table_name, config, output_dir):
    """Export a large table using a server-side cursor and incremental ParquetWriter."""
    filepath = os.path.join(output_dir, config["filename"])
    schema = config["schema"]
    total_rows = 0

    print(f"  Exporting {table_name} (streaming)...")

    cur = conn.cursor(name=f"cur_{table_name}")
    cur.itersize = BATCH_SIZE
    cur.execute(config["sql"])

    writer = pq.ParquetWriter(filepath, schema, compression="snappy")
    try:
        while True:
            rows = cur.fetchmany(BATCH_SIZE)
            if not rows:
                break
            batch = rows_to_record_batch(rows, schema)
            writer.write_batch(batch)
            total_rows += len(rows)
            print(f"    ... {total_rows:,} rows written")
    finally:
        writer.close()
        cur.close()
        conn.rollback()

    return total_rows


def export_small_table(conn, table_name, config, output_dir):
    """Export a small table with a single fetchall."""
    filepath = os.path.join(output_dir, config["filename"])
    schema = config["schema"]

    print(f"  Exporting {table_name}...")

    cur = conn.cursor()
    cur.execute(config["sql"])
    rows = cur.fetchall()
    cur.close()
    conn.rollback()

    if not rows:
        # Write an empty parquet file with the correct schema
        table = pa.table({f.name: pa.array([], type=f.type) for f in schema}, schema=schema)
    else:
        batch = rows_to_record_batch(rows, schema)
        table = pa.Table.from_batches([batch], schema=schema)

    pq.write_table(table, filepath, compression="snappy")
    return len(rows)


def export_table(conn, table_name, config, output_dir):
    """Export a single table and print summary."""
    start = time.time()

    if config["is_huge"]:
        row_count = export_huge_table(conn, table_name, config, output_dir)
    else:
        row_count = export_small_table(conn, table_name, config, output_dir)

    elapsed = time.time() - start
    filepath = os.path.join(output_dir, config["filename"])
    file_size = os.path.getsize(filepath)

    if file_size >= 1_073_741_824:
        size_str = f"{file_size / 1_073_741_824:.2f} GB"
    elif file_size >= 1_048_576:
        size_str = f"{file_size / 1_048_576:.1f} MB"
    else:
        size_str = f"{file_size / 1024:.1f} KB"

    print(f"  Done: {row_count:,} rows, {size_str}, {elapsed:.1f}s -> {config['filename']}")
    print()


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

    print("Connecting to database...")
    print(f"  Host: {db_config['host']}:{db_config['port']}, DB: {db_config['database']}, User: {db_config['user']}")
    conn = get_connection(db_config)
    print(f"Connected. Exporting {len(args.tables)} table(s) to {os.path.abspath(output_dir)}")
    print()

    try:
        for table_name in args.tables:
            export_table(conn, table_name, TABLE_CONFIGS[table_name], output_dir)
    finally:
        conn.close()

    print("All exports complete.")


if __name__ == "__main__":
    main()
