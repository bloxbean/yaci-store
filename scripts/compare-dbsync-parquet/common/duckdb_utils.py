"""DuckDB helpers for reading Parquet/CSV and writing mismatch samples."""

import os
import re
import sys


duckdb = None


def load_runtime_dependency():
    global duckdb
    if duckdb is not None:
        return
    try:
        import duckdb as duckdb_module
    except ModuleNotFoundError as e:
        print(f"ERROR: missing Python dependency: {e.name}", file=sys.stderr)
        print("Run: pip3 install psycopg2-binary duckdb", file=sys.stderr)
        sys.exit(2)
    duckdb = duckdb_module


def sql_literal(value):
    return "'" + str(value).replace("'", "''") + "'"


def sql_identifier(value):
    if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", value):
        raise ValueError(f"invalid SQL identifier: {value}")
    return value


class DuckDbContext:
    def __init__(self, memory_limit=None):
        load_runtime_dependency()
        self.conn = duckdb.connect(":memory:")
        if memory_limit:
            self.conn.execute(f"SET memory_limit = {sql_literal(memory_limit)}")

    def close(self):
        self.conn.close()

    def parquet_source(self, parquet_path):
        return f"read_parquet({sql_literal(parquet_path)})"

    def create_view(self, view_name, select_sql):
        self.conn.execute(
            f"CREATE OR REPLACE TEMP VIEW {sql_identifier(view_name)} AS {select_sql}"
        )

    def create_csv_view(self, view_name, csv_path):
        self.create_view(
            view_name,
            (
                "SELECT * FROM read_csv_auto("
                f"{sql_literal(csv_path)}, header=true, nullstr='', sample_size=-1"
                ")"
            ),
        )

    def diff_count(self, diff_sql):
        row = self.conn.execute(f"SELECT count(*) FROM ({diff_sql}) diff").fetchone()
        return int(row[0])

    def write_diff_sample(self, diff_sql, csv_path, max_rows):
        os.makedirs(os.path.dirname(csv_path), exist_ok=True)
        limit_clause = f" LIMIT {int(max_rows)}" if max_rows else ""
        self.conn.execute(
            "COPY ("
            f"SELECT * FROM ({diff_sql}) diff{limit_clause}"
            f") TO {sql_literal(csv_path)} (HEADER, DELIMITER ',')"
        )

    def columns(self, parquet_path):
        rows = self.conn.execute(
            f"DESCRIBE SELECT * FROM {self.parquet_source(parquet_path)}"
        ).fetchall()
        return [row[0] for row in rows]
