"""PostgreSQL helpers for reading Yaci Store data."""

import sys


psycopg2 = None
pg_sql = None


def load_runtime_dependency():
    global psycopg2, pg_sql
    if psycopg2 is not None:
        return
    try:
        import psycopg2 as psycopg2_module
        from psycopg2 import sql as pg_sql_module
    except ModuleNotFoundError as e:
        print(f"ERROR: missing Python dependency: {e.name}", file=sys.stderr)
        print("Run: pip3 install psycopg2-binary duckdb", file=sys.stderr)
        sys.exit(2)

    psycopg2 = psycopg2_module
    pg_sql = pg_sql_module


def connect(url, schema=None):
    load_runtime_dependency()
    conn = psycopg2.connect(url)
    if schema:
        with conn.cursor() as cur:
            cur.execute(
                pg_sql.SQL("SET search_path TO {}").format(pg_sql.Identifier(schema))
            )
    return conn


def copy_query_to_csv(conn, query, params, csv_path):
    """Write a parametrized PostgreSQL query result to a CSV file with headers."""
    with conn.cursor() as cur:
        rendered_query = cur.mogrify(query, params or ())
        copy_sql = b"COPY (" + rendered_query + b") TO STDOUT WITH (FORMAT CSV, HEADER, NULL '')"
        with open(csv_path, "w", encoding="utf-8", newline="") as f:
            cur.copy_expert(copy_sql.decode("utf-8"), f)
