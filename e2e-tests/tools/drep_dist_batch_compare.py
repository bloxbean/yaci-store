#!/usr/bin/env python3

import ast
import argparse
import csv
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Dict, List, Optional, Sequence, Tuple


SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parents[1]
EXCLUSION_SOURCE_PATH = REPO_ROOT / "aggregates/governance-aggr/src/main/java/com/bloxbean/cardano/yaci/store/governanceaggr/service/HardcodedDRepDelegationExclusionProvider.java"
DEFAULT_ENV_FILE_PATH = SCRIPT_DIR / "drep_dist_batch_compare.env"
ZERO_HASH = "00000000000000000000000000000000000000000000000000000000"

PUBLIC_PROTOCOL_MAGICS = {
    "mainnet": 764824073,
    "preprod": 1,
    "preview": 2,
}

POST_BOOTSTRAP_VALID_DELEGATION_CONDITION = """
  AND EXISTS (
      SELECT 1
      FROM ss_drep_status ds
      WHERE ds.drep_hash = rd.drep_hash
        AND ds.cred_type = rd.drep_type
        AND ds.rn = 1
        AND (ds.type = 'REG_DREP_CERT' OR ds.type = 'UPDATE_DREP_CERT')
        AND (
            rd.slot > ds.registration_slot
            OR (rd.slot = ds.registration_slot AND rd.tx_index > ds.registration_tx_index)
            OR (rd.slot = ds.registration_slot AND rd.tx_index <= ds.registration_tx_index AND rd.epoch <= {{MAX_BOOTSTRAP_PHASE_EPOCH}})
            OR (
                rd.slot < ds.registration_slot
                AND (
                    ds.unregistration_slot IS NULL
                    OR rd.slot > ds.unregistration_slot
                    OR (rd.slot = ds.unregistration_slot AND rd.tx_index > ds.unregistration_tx_index)
                    OR (
                        rd.slot = ds.unregistration_slot
                        AND rd.tx_index = ds.unregistration_tx_index
                        AND rd.cert_index > ds.unregistration_cert_index
                    )
                )
                AND rd.epoch <= {{MAX_BOOTSTRAP_PHASE_EPOCH}}
                AND ds.registration_epoch <= {{MAX_BOOTSTRAP_PHASE_EPOCH}}
            )
            OR (
                rd.slot = ds.registration_slot
                AND rd.tx_index = ds.registration_tx_index
                AND rd.epoch > {{MAX_BOOTSTRAP_PHASE_EPOCH}}
                AND rd.cert_index > ds.registration_cert_index
            )
        )
  )
""".strip("\n")

BOOTSTRAP_VALID_DELEGATION_CONDITION = """
  AND EXISTS (
      SELECT 1
      FROM ss_drep_status ds
      WHERE ds.drep_hash = rd.drep_hash
        AND ds.cred_type = rd.drep_type
        AND ds.rn = 1
        AND (ds.type = 'REG_DREP_CERT' OR ds.type = 'UPDATE_DREP_CERT')
        AND (
            ds.unregistration_slot IS NULL
            OR rd.slot > ds.unregistration_slot
            OR (rd.slot = ds.unregistration_slot AND rd.tx_index > ds.unregistration_tx_index)
            OR (
                rd.slot = ds.unregistration_slot
                AND rd.tx_index = ds.unregistration_tx_index
                AND rd.cert_index > ds.unregistration_cert_index
            )
        )
  )
""".strip("\n")

IDENTIFIER_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
ENV_VAR_NAME_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
EXCLUSION_BLOCK_RE = re.compile(
    r"private List<DRepDelegationExclusion> (?P<network>\w+)Exclusions\(\) \{\s+return List\.of\((?P<body>.*?)\);\s+\}",
    re.DOTALL,
)
EXCLUSION_ENTRY_RE = re.compile(
    r'address\("(?P<address>[^"]*)"\).*?'
    r'drepHash\("(?P<drep_hash>[^"]*)"\).*?'
    r'drepType\(DrepType\.(?P<drep_type>[A-Z_]+)\).*?'
    r'slot\((?P<slot>\d+)L\).*?'
    r'txIndex\((?P<tx_index>\d+)\).*?'
    r'certIndex\((?P<cert_index>\d+)\)',
    re.DOTALL,
)
STEP_TIMING_RE = re.compile(r"^STEP_TIMING\|(?P<name>[^|]+)\|(?P<duration_ms>-?\d+(?:\.\d+)?)$")
META_RE = re.compile(r"^META\|(?P<name>[^|]+)\|(?P<value>.*)$")

PRINT_LOCK = threading.Lock()


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    database: str
    user: str
    password: str
    schema: str


@dataclass(frozen=True)
class QuerySettings:
    lock_timeout: str
    statement_timeout: str
    work_mem: str
    maintenance_work_mem: str
    temp_buffers: str
    parallel_workers_per_gather: int
    disable_jit: bool
    effective_cache_size: str
    random_page_cost: str
    effective_io_concurrency: int
    parallel_setup_cost: str
    parallel_tuple_cost: str


@dataclass(frozen=True)
class DRepDelegationExclusion:
    address: str
    drep_hash: str
    drep_type: str
    slot: Optional[int]
    tx_index: Optional[int]
    cert_index: Optional[int]


@dataclass
class EpochExecutionContext:
    epoch: int
    is_bootstrap_phase: bool
    pv9_max_epoch: int
    max_bootstrap_phase_epoch: Optional[int]
    exclusions: List[DRepDelegationExclusion]


@dataclass
class EpochCompareResult:
    epoch: int
    status: str
    mismatch_count: int
    dbsync_row_count: int
    store_row_count: int
    duration_ms: int
    report_file: str
    error: Optional[str]


@dataclass
class EpochPrepareResult:
    epoch: int
    status: str
    duration_ms: int
    report_file: str
    error: Optional[str]


def sql_identifier(identifier: str) -> str:
    if not IDENTIFIER_RE.match(identifier):
        raise ValueError("Invalid SQL identifier: %s" % identifier)
    return '"%s"' % identifier


def sql_literal(value: Optional[object]) -> str:
    if value is None:
        return "NULL"
    return "'%s'" % str(value).replace("'", "''")


def normalize_hash(hash_value: Optional[str]) -> Optional[str]:
    if hash_value is None:
        return None
    hash_value = hash_value.strip()
    if hash_value.startswith("0x") or hash_value.startswith("0X"):
        hash_value = hash_value[2:]
    return hash_value.lower()


def require_value(name: str, explicit: Optional[str], env_name: str) -> str:
    if explicit:
        return explicit
    env_value = os.getenv(env_name)
    if env_value:
        return env_value
    raise ValueError("Missing %s. Pass --%s or set %s." % (name, name.replace("_", "-"), env_name))


def parse_env_bool(raw_value: str) -> bool:
    normalized = raw_value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    raise ValueError("Invalid boolean value in env file: %s" % raw_value)


def load_env_file(path_text: str) -> Dict[str, str]:
    path = Path(path_text).expanduser().resolve()
    if not path.is_file():
        raise ValueError("Env file not found: %s" % path)

    values: Dict[str, str] = {}
    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        stripped = raw_line.strip()
        if not stripped or stripped.startswith("#"):
            continue

        if stripped.startswith("export "):
            stripped = stripped[7:].lstrip()

        if "=" not in stripped:
            raise ValueError("Invalid env file entry at %s:%d" % (path, line_number))

        key, raw_value = stripped.split("=", 1)
        key = key.strip()
        if not ENV_VAR_NAME_RE.match(key):
            raise ValueError("Invalid env var name at %s:%d: %s" % (path, line_number, key))

        value = raw_value.strip()
        if value[:1] in {"'", '"'}:
            try:
                parsed_value = ast.literal_eval(value)
            except (SyntaxError, ValueError) as exc:
                raise ValueError("Invalid quoted value at %s:%d" % (path, line_number)) from exc
            if not isinstance(parsed_value, str):
                parsed_value = str(parsed_value)
        else:
            parsed_value = value.split(" #", 1)[0].rstrip()

        values[key] = parsed_value

    return values


def resolve_env_file_path(argv: Sequence[str]) -> Optional[str]:
    for index, token in enumerate(argv):
        if token == "--env-file":
            if index + 1 >= len(argv):
                raise ValueError("--env-file requires a value")
            return str(Path(argv[index + 1]).expanduser().resolve())
        if token.startswith("--env-file="):
            return str(Path(token.split("=", 1)[1]).expanduser().resolve())

    env_override = os.getenv("DREP_DIST_BATCH_COMPARE_ENV_FILE")
    if env_override:
        return str(Path(env_override).expanduser().resolve())

    if DEFAULT_ENV_FILE_PATH.is_file():
        return str(DEFAULT_ENV_FILE_PATH.resolve())

    return None


def resolve_value(
    explicit: Optional[object],
    env_values: Dict[str, str],
    env_name: str,
    default: Optional[object] = None,
    caster=None,
):
    if explicit is not None:
        return explicit

    raw_value = env_values.get(env_name)
    if raw_value is None:
        raw_value = os.getenv(env_name)

    if raw_value is None:
        return default

    if caster is None:
        return raw_value

    return caster(raw_value)


def apply_env_defaults(args: argparse.Namespace, env_values: Dict[str, str]) -> None:
    args.store_host = resolve_value(args.store_host, env_values, "STORE_HOST")
    args.store_port = resolve_value(args.store_port, env_values, "STORE_PORT", 5432, int)
    args.store_db = resolve_value(args.store_db, env_values, "STORE_DB")
    args.store_user = resolve_value(args.store_user, env_values, "STORE_USER")
    args.store_password = resolve_value(args.store_password, env_values, "STORE_PASSWORD")
    args.store_schema = resolve_value(args.store_schema, env_values, "STORE_SCHEMA", "yaci_store")

    if hasattr(args, "dbsync_host"):
        args.dbsync_host = resolve_value(args.dbsync_host, env_values, "DBSYNC_HOST")
        args.dbsync_port = resolve_value(args.dbsync_port, env_values, "DBSYNC_PORT", 5432, int)
        args.dbsync_db = resolve_value(args.dbsync_db, env_values, "DBSYNC_DB")
        args.dbsync_user = resolve_value(args.dbsync_user, env_values, "DBSYNC_USER")
        args.dbsync_password = resolve_value(args.dbsync_password, env_values, "DBSYNC_PASSWORD")
        args.dbsync_schema = resolve_value(args.dbsync_schema, env_values, "DBSYNC_SCHEMA", "public")

    args.workers = resolve_value(args.workers, env_values, "WORKERS", 1, int)
    args.protocol_magic = resolve_value(
        args.protocol_magic,
        env_values,
        "PROTOCOL_MAGIC",
        PUBLIC_PROTOCOL_MAGICS["mainnet"],
        int,
    )
    args.max_bootstrap_phase_epoch = resolve_value(
        args.max_bootstrap_phase_epoch,
        env_values,
        "MAX_BOOTSTRAP_PHASE_EPOCH",
        None,
        int,
    )
    args.debug_schema = resolve_value(args.debug_schema, env_values, "DREP_DEBUG_SCHEMA", "drep_debug")
    args.lock_timeout = resolve_value(args.lock_timeout, env_values, "LOCK_TIMEOUT", "5s")
    args.statement_timeout = resolve_value(args.statement_timeout, env_values, "STATEMENT_TIMEOUT", "0")
    args.work_mem = resolve_value(args.work_mem, env_values, "WORK_MEM", "1GB")
    args.maintenance_work_mem = resolve_value(
        args.maintenance_work_mem,
        env_values,
        "MAINTENANCE_WORK_MEM",
        "2GB",
    )
    args.temp_buffers = resolve_value(args.temp_buffers, env_values, "TEMP_BUFFERS", "512MB")
    args.parallel_workers_per_gather = resolve_value(
        args.parallel_workers_per_gather,
        env_values,
        "PARALLEL_WORKERS_PER_GATHER",
        4,
        int,
    )
    args.effective_cache_size = resolve_value(
        args.effective_cache_size,
        env_values,
        "EFFECTIVE_CACHE_SIZE",
        "48GB",
    )
    args.random_page_cost = resolve_value(args.random_page_cost, env_values, "RANDOM_PAGE_COST", "1.1")
    args.effective_io_concurrency = resolve_value(
        args.effective_io_concurrency,
        env_values,
        "EFFECTIVE_IO_CONCURRENCY",
        64,
        int,
    )
    args.parallel_setup_cost = resolve_value(args.parallel_setup_cost, env_values, "PARALLEL_SETUP_COST", "0")
    args.parallel_tuple_cost = resolve_value(args.parallel_tuple_cost, env_values, "PARALLEL_TUPLE_COST", "0.01")
    args.keep_jit = resolve_value(args.keep_jit, env_values, "KEEP_JIT", False, parse_env_bool)
    args.save_store_sql = resolve_value(args.save_store_sql, env_values, "SAVE_STORE_SQL", False, parse_env_bool)
    args.report_dir = resolve_value(args.report_dir, env_values, "REPORT_DIR")


def parse_epoch_spec(spec: str) -> List[int]:
    epochs = set()
    for token in spec.split(","):
        token = token.strip()
        if not token:
            continue
        if "-" in token:
            start_text, end_text = token.split("-", 1)
            start_epoch = int(start_text)
            end_epoch = int(end_text)
            if end_epoch < start_epoch:
                raise ValueError("Invalid epoch range: %s" % token)
            epochs.update(range(start_epoch, end_epoch + 1))
        else:
            epochs.add(int(token))
    return sorted(epochs)


def inject_default_command(argv: Sequence[str]) -> List[str]:
    if argv and argv[0] in {"prepare-range", "compare-range"}:
        return list(argv)
    return ["compare-range", *argv]


def run_psql(conn: DbConfig, sql: str, extra_args: Optional[Sequence[str]] = None) -> str:
    command = [
        "psql",
        "-X",
        "-v",
        "ON_ERROR_STOP=1",
        "-q",
        "-h",
        conn.host,
        "-p",
        str(conn.port),
        "-U",
        conn.user,
        "-d",
        conn.database,
    ]
    if extra_args:
        command.extend(extra_args)

    env = os.environ.copy()
    env["PGPASSWORD"] = conn.password

    completed = subprocess.run(
        command,
        input=sql,
        text=True,
        capture_output=True,
        env=env,
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            "psql failed against %s/%s: %s" % (
                conn.host,
                conn.database,
                completed.stderr.strip() or completed.stdout.strip() or "unknown error",
            )
        )
    return completed.stdout


def query_single_row(conn: DbConfig, sql: str) -> List[str]:
    output = run_psql(conn, sql, ["-At", "-F", "|"])
    lines = [line for line in output.splitlines() if line.strip()]
    if not lines:
        return []
    return lines[-1].split("|")


def parse_csv_rows(output: str) -> List[Dict[str, str]]:
    output = output.strip()
    if not output:
        return []
    return list(csv.DictReader(io.StringIO(output)))


def parse_step_timings(output: str) -> List[Dict[str, object]]:
    timings = []
    for line in output.splitlines():
        match = STEP_TIMING_RE.match(line.strip())
        if not match:
            continue
        timings.append(
            {
                "name": match.group("name"),
                "duration_ms": float(match.group("duration_ms")),
            }
        )
    return timings


def parse_meta_lines(output: str) -> Dict[str, str]:
    metadata = {}
    for line in output.splitlines():
        match = META_RE.match(line.strip())
        if not match:
            continue
        metadata[match.group("name")] = match.group("value")
    return metadata


def meta_bool(metadata: Dict[str, str], key: str) -> bool:
    return metadata.get(key, "").strip().lower() in {"1", "true", "yes", "on"}


def meta_int(metadata: Dict[str, str], key: str) -> int:
    value = metadata.get(key, "0").strip()
    return int(value) if value else 0


def timed_call(name: str, timings: List[Dict[str, object]], fn):
    started = time.perf_counter()
    result = fn()
    timings.append(
        {
            "name": name,
            "duration_ms": round((time.perf_counter() - started) * 1000, 3),
        }
    )
    return result


def load_hardcoded_exclusions(protocol_magic: int) -> List[DRepDelegationExclusion]:
    if protocol_magic != PUBLIC_PROTOCOL_MAGICS["mainnet"]:
        return []

    source = EXCLUSION_SOURCE_PATH.read_text(encoding="utf-8")
    for match in EXCLUSION_BLOCK_RE.finditer(source):
        if match.group("network") != "mainnet":
            continue
        exclusions = []
        for entry in EXCLUSION_ENTRY_RE.finditer(match.group("body")):
            exclusions.append(
                DRepDelegationExclusion(
                    address=entry.group("address"),
                    drep_hash=entry.group("drep_hash"),
                    drep_type=entry.group("drep_type"),
                    slot=int(entry.group("slot")),
                    tx_index=int(entry.group("tx_index")),
                    cert_index=int(entry.group("cert_index")),
                )
            )
        if exclusions:
            return exclusions
    raise RuntimeError("Unable to parse mainnet exclusions from %s" % EXCLUSION_SOURCE_PATH)


def build_hardcoded_exclusion_condition(exclusions: Sequence[DRepDelegationExclusion], enabled: bool) -> str:
    if not enabled or not exclusions:
        return ""

    rows = []
    for exclusion in exclusions:
        rows.append(
            "(%s, %s, %s, %s, %s, %s)" % (
                sql_literal(exclusion.address),
                sql_literal(exclusion.drep_hash),
                sql_literal(exclusion.drep_type),
                "NULL" if exclusion.slot is None else str(exclusion.slot),
                "NULL" if exclusion.tx_index is None else str(exclusion.tx_index),
                "NULL" if exclusion.cert_index is None else str(exclusion.cert_index),
            )
        )

    return (
        "\n  AND NOT EXISTS (\n"
        "      SELECT 1\n"
        "      FROM (VALUES\n"
        "          %s\n"
        "      ) AS excl(address, drep_hash, drep_type, slot, tx_index, cert_index)\n"
        "      WHERE excl.address = rd.address\n"
        "        AND excl.drep_hash = rd.drep_hash\n"
        "        AND excl.drep_type = rd.drep_type\n"
        "        AND (excl.slot IS NULL OR excl.slot = rd.slot)\n"
        "        AND (excl.tx_index IS NULL OR excl.tx_index = rd.tx_index)\n"
        "        AND (excl.cert_index IS NULL OR excl.cert_index = rd.cert_index)\n"
        "  )"
    ) % ",\n          ".join(rows)


def build_valid_delegation_condition(epoch_context: EpochExecutionContext) -> str:
    if epoch_context.is_bootstrap_phase:
        return BOOTSTRAP_VALID_DELEGATION_CONDITION
    return POST_BOOTSTRAP_VALID_DELEGATION_CONDITION.replace(
        "{{MAX_BOOTSTRAP_PHASE_EPOCH}}",
        str(epoch_context.max_bootstrap_phase_epoch),
    )


def render_dbsync_query(schema: str, epoch: int) -> str:
    return """
SET search_path TO %s, public;
COPY (
    SELECT
        encode(dh.raw, 'hex') AS drep_hash,
        COALESCE(dh.view, '') AS drep_view,
        d.amount
    FROM drep_distr d
    JOIN drep_hash dh ON dh.id = d.hash_id
    WHERE d.epoch_no = %d
    ORDER BY dh.view, encode(dh.raw, 'hex')
) TO STDOUT WITH CSV HEADER;
""" % (sql_identifier(schema), epoch)


def resolve_epoch_context(
    store: DbConfig,
    snapshot_epoch: int,
    protocol_magic: int,
    override_max_bootstrap_phase_epoch: Optional[int],
    exclusions: List[DRepDelegationExclusion],
) -> EpochExecutionContext:
    epoch = snapshot_epoch - 1
    if override_max_bootstrap_phase_epoch is not None:
        is_bootstrap_phase = epoch <= override_max_bootstrap_phase_epoch
        return EpochExecutionContext(
            epoch=epoch,
            is_bootstrap_phase=is_bootstrap_phase,
            pv9_max_epoch=min(epoch, override_max_bootstrap_phase_epoch),
            max_bootstrap_phase_epoch=override_max_bootstrap_phase_epoch,
            exclusions=exclusions,
        )

    if protocol_magic not in PUBLIC_PROTOCOL_MAGICS.values():
        raise ValueError(
            "Non-public protocol magic requires --max-bootstrap-phase-epoch so the tool can reproduce PV9/PV10 logic."
        )

    metadata_sql = """
SET search_path TO %s, public;
SELECT
    COALESCE((SELECT (params ->> 'protocol_major_ver')::int FROM epoch_param WHERE epoch = %d), 0) AS protocol_major_ver,
    COALESCE((
        SELECT MIN(epoch)
        FROM gov_action_proposal_status
        WHERE type = 'HARD_FORK_INITIATION_ACTION'
          AND status = 'RATIFIED'
          AND epoch < %d
    ), -1) AS max_bootstrap_phase_epoch;
""" % (sql_identifier(store.schema), epoch, epoch)
    row = query_single_row(store, metadata_sql)
    if len(row) != 2:
        raise RuntimeError("Failed to resolve bootstrap metadata for epoch %d" % snapshot_epoch)

    protocol_major_ver = int(row[0])
    max_bootstrap_phase_epoch = int(row[1])
    is_bootstrap_phase = protocol_major_ver < 10 or max_bootstrap_phase_epoch < 0

    return EpochExecutionContext(
        epoch=epoch,
        is_bootstrap_phase=is_bootstrap_phase,
        pv9_max_epoch=epoch if is_bootstrap_phase else max_bootstrap_phase_epoch,
        max_bootstrap_phase_epoch=None if is_bootstrap_phase else max_bootstrap_phase_epoch,
        exclusions=exclusions,
    )


def detect_epoch_range(store: DbConfig, dbsync: DbConfig) -> Tuple[int, int]:
    dbsync_row = query_single_row(
        dbsync,
        "SET search_path TO %s, public; SELECT COALESCE(MIN(epoch_no), -1), COALESCE(MAX(epoch_no), -1) FROM drep_distr;" % sql_identifier(dbsync.schema),
    )
    if len(dbsync_row) != 2 or int(dbsync_row[0]) < 0:
        raise RuntimeError("Unable to detect epoch range from cardano-db-sync drep_distr.")

    store_row = query_single_row(
        store,
        "SET search_path TO %s, public; SELECT COALESCE(MAX(epoch), -1) FROM epoch;" % sql_identifier(store.schema),
    )
    if len(store_row) != 1 or int(store_row[0]) < 0:
        raise RuntimeError("Unable to detect synced epoch range from yaci-store.")

    start_epoch = int(dbsync_row[0])
    end_epoch = min(int(dbsync_row[1]), int(store_row[0]))
    if end_epoch < start_epoch:
        raise RuntimeError("Detected invalid epoch range: %d..%d" % (start_epoch, end_epoch))
    return start_epoch, end_epoch


def compare_rows(
    epoch: int,
    dbsync_rows: Sequence[Dict[str, str]],
    store_rows: Sequence[Dict[str, str]],
) -> Tuple[int, List[Dict[str, object]]]:
    dbsync_values = {}
    store_values = {}
    drep_ids = {}
    dbsync_abstain = 0
    store_abstain = 0
    dbsync_no_confidence = 0
    store_no_confidence = 0

    for row in dbsync_rows:
        normalized_hash = normalize_hash(row["drep_hash"])
        amount = int(row["amount"])
        view = row["drep_view"].lower()
        if "abstain" in view:
            dbsync_abstain += amount
        elif "no_confidence" in view:
            dbsync_no_confidence += amount
        else:
            dbsync_values[normalized_hash] = amount

    for row in store_rows:
        normalized_hash = normalize_hash(row["drep_hash"])
        amount = int(row["amount"])
        drep_type = row["drep_type"]
        if drep_type == "ABSTAIN":
            store_abstain = amount
        elif drep_type == "NO_CONFIDENCE":
            store_no_confidence = amount
        else:
            store_values[normalized_hash] = amount
            if row.get("drep_id"):
                drep_ids[normalized_hash] = row["drep_id"]

    mismatches = []

    for hash_value, dbsync_amount in sorted(dbsync_values.items()):
        store_amount = store_values.get(hash_value)
        if store_amount is None:
            mismatches.append(
                {
                    "epoch": epoch,
                    "type": "missing_in_store",
                    "drep_hash": hash_value,
                    "drep_id": drep_ids.get(hash_value),
                    "dbsync_amount": dbsync_amount,
                    "store_amount": None,
                }
            )
        elif store_amount != dbsync_amount:
            mismatches.append(
                {
                    "epoch": epoch,
                    "type": "amount_mismatch",
                    "drep_hash": hash_value,
                    "drep_id": drep_ids.get(hash_value),
                    "dbsync_amount": dbsync_amount,
                    "store_amount": store_amount,
                }
            )

    for hash_value, store_amount in sorted(store_values.items()):
        if hash_value not in dbsync_values:
            mismatches.append(
                {
                    "epoch": epoch,
                    "type": "missing_in_dbsync",
                    "drep_hash": hash_value,
                    "drep_id": drep_ids.get(hash_value),
                    "dbsync_amount": None,
                    "store_amount": store_amount,
                }
            )

    if dbsync_abstain != store_abstain:
        mismatches.append(
            {
                "epoch": epoch,
                "type": "abstain_mismatch",
                "drep_hash": "virtual:abstain",
                "drep_id": None,
                "dbsync_amount": dbsync_abstain,
                "store_amount": store_abstain,
            }
        )

    if dbsync_no_confidence != store_no_confidence:
        mismatches.append(
            {
                "epoch": epoch,
                "type": "no_confidence_mismatch",
                "drep_hash": "virtual:no_confidence",
                "drep_id": None,
                "dbsync_amount": dbsync_no_confidence,
                "store_amount": store_no_confidence,
            }
        )

    return len(mismatches), mismatches


def write_json(path: Path, payload: object) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, sort_keys=False), encoding="utf-8")


def print_line(message: str) -> None:
    with PRINT_LOCK:
        print(message, flush=True)


def timed_sql(name: str, statement: str) -> str:
    statement = statement.strip().rstrip(";")
    return (
        "SELECT clock_timestamp() AS __step_started_at \\gset\n"
        + statement
        + ";\n"
        + "SELECT ROUND((EXTRACT(EPOCH FROM clock_timestamp() - :'__step_started_at'::timestamptz) * 1000)::numeric, 3) AS __step_duration_ms \\gset\n"
        + "\\echo STEP_TIMING|%s|:__step_duration_ms\n" % name
    )


def timed_psql_block(name: str, block: str) -> str:
    block = block.strip()
    return (
        "SELECT clock_timestamp() AS __step_started_at \\gset\n"
        + block
        + "\n"
        + "SELECT ROUND((EXTRACT(EPOCH FROM clock_timestamp() - :'__step_started_at'::timestamptz) * 1000)::numeric, 3) AS __step_duration_ms \\gset\n"
        + "\\echo STEP_TIMING|%s|:__step_duration_ms\n" % name
    )


def render_session_settings(store_schema: str, settings: QuerySettings) -> str:
    return "\n".join(
        [
            "BEGIN;",
            "SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;",
            "SET LOCAL search_path TO %s, public;" % sql_identifier(store_schema),
            "SET LOCAL client_min_messages = warning;",
            "SET LOCAL lock_timeout = %s;" % sql_literal(settings.lock_timeout),
            "SET LOCAL statement_timeout = %s;" % sql_literal(settings.statement_timeout),
            "SET LOCAL synchronous_commit = off;",
            "SET LOCAL work_mem = %s;" % sql_literal(settings.work_mem),
            "SET LOCAL maintenance_work_mem = %s;" % sql_literal(settings.maintenance_work_mem),
            "SET LOCAL temp_buffers = %s;" % sql_literal(settings.temp_buffers),
            "SET LOCAL max_parallel_workers_per_gather = %d;" % settings.parallel_workers_per_gather,
            "SET LOCAL parallel_setup_cost = %s;" % settings.parallel_setup_cost,
            "SET LOCAL parallel_tuple_cost = %s;" % settings.parallel_tuple_cost,
            "SET LOCAL enable_partitionwise_aggregate = on;",
            "SET LOCAL enable_partitionwise_join = on;",
            "SET LOCAL effective_cache_size = %s;" % sql_literal(settings.effective_cache_size),
            "SET LOCAL random_page_cost = %s;" % settings.random_page_cost,
            "SET LOCAL effective_io_concurrency = %d;" % settings.effective_io_concurrency,
            "SET LOCAL jit = %s;" % ("off" if settings.disable_jit else "on"),
        ]
    ) + "\n"


def render_current_drep_delegations_sql(epoch: int) -> str:
    return """
CREATE TEMP TABLE ss_current_drep_address ON COMMIT DROP AS
WITH ranked AS (
    SELECT
        address,
        drep_id,
        drep_hash,
        drep_type,
        epoch,
        slot,
        tx_index,
        cert_index,
        ROW_NUMBER() OVER (
            PARTITION BY address
            ORDER BY slot DESC, tx_index DESC, cert_index DESC
        ) AS rn
    FROM delegation_vote
    WHERE epoch <= %d
)
SELECT
    address,
    drep_id,
    drep_hash,
    drep_type,
    epoch,
    slot,
    tx_index,
    cert_index
FROM ranked
WHERE rn = 1
""" % epoch


def render_drep_status_sql(epoch: int) -> str:
    return """
CREATE TEMP TABLE ss_drep_status ON COMMIT DROP AS
WITH last_reg AS (
    SELECT
        dr.drep_id,
        dr.drep_hash,
        dr.cred_type,
        dr.epoch AS registration_epoch,
        dr.slot AS registration_slot,
        dr.tx_index AS registration_tx_index,
        dr.cert_index AS registration_cert_index
    FROM (
        SELECT
            drep_id,
            drep_hash,
            cred_type,
            epoch,
            slot,
            tx_index,
            cert_index,
            ROW_NUMBER() OVER (
                PARTITION BY drep_hash
                ORDER BY slot DESC, tx_index DESC, cert_index DESC
            ) AS rn
        FROM drep_registration
        WHERE epoch <= %d
          AND type = 'REG_DREP_CERT'
    ) dr
    WHERE dr.rn = 1
),
last_unreg AS (
    SELECT
        du.drep_id,
        du.drep_hash,
        du.epoch AS unregistration_epoch,
        du.slot AS unregistration_slot,
        du.tx_index AS unregistration_tx_index,
        du.cert_index AS unregistration_cert_index
    FROM (
        SELECT
            drep_id,
            drep_hash,
            epoch,
            slot,
            tx_index,
            cert_index,
            ROW_NUMBER() OVER (
                PARTITION BY drep_hash
                ORDER BY slot DESC, tx_index DESC, cert_index DESC
            ) AS rn
        FROM drep_registration
        WHERE epoch <= %d
          AND type = 'UNREG_DREP_CERT'
    ) du
    WHERE du.rn = 1
)
SELECT
    d.drep_id,
    d.drep_hash,
    d.tx_index,
    d.cert_index,
    d.type,
    d.slot,
    lr.cred_type,
    lr.registration_epoch,
    lr.registration_slot,
    lr.registration_tx_index,
    lr.registration_cert_index,
    lu.unregistration_slot,
    lu.unregistration_tx_index,
    lu.unregistration_cert_index,
    ROW_NUMBER() OVER (
        PARTITION BY d.drep_hash
        ORDER BY d.slot DESC, d.tx_index DESC, d.cert_index DESC
    ) AS rn
FROM drep_registration d
LEFT JOIN last_reg lr ON d.drep_hash = lr.drep_hash
LEFT JOIN last_unreg lu ON d.drep_hash = lu.drep_hash
WHERE d.epoch <= %d
""" % (epoch, epoch, epoch)


def render_active_proposal_deposits_sql(epoch: int) -> str:
    # Replay-safe version of the "active proposal deposits" logic.
    # New proposals at the snapshot boundary must still count even if status rows
    # already exist when we rerun/debug after the original live snapshot window.
    return """
CREATE TEMP TABLE ss_gov_active_proposal_deposits ON COMMIT DROP AS
SELECT
    g.return_address,
    SUM(g.deposit) AS deposit
FROM gov_action_proposal g
WHERE g.epoch = %d
   OR EXISTS (
       SELECT 1
       FROM gov_action_proposal_status s
       WHERE s.gov_action_tx_hash = g.tx_hash
         AND s.gov_action_index = g.idx
         AND s.status = 'ACTIVE'
         AND s.epoch = %d
   )
GROUP BY g.return_address
""" % (epoch, epoch)


def render_dropped_proposal_deposits_sql(epoch: int) -> str:
    # Rebuild the same "scheduled to drop" proposal set as the Java service by
    # walking proposal purpose-chains from expired/ratified roots.
    return """
CREATE TEMP TABLE ss_gov_scheduled_to_drop_proposal_deposits ON COMMIT DROP AS
WITH RECURSIVE
props AS (
    SELECT
        g.tx_hash,
        g.idx,
        g.type,
        CASE
            WHEN g.type IN ('NO_CONFIDENCE', 'UPDATE_COMMITTEE') THEN 'COMMITTEE'
            ELSE g.type
        END AS purpose_key,
        g.details -> 'govActionId' ->> 'transactionId' AS prev_tx_hash,
        (g.details -> 'govActionId' ->> 'gov_action_index')::int AS prev_idx
    FROM gov_action_proposal g
    WHERE g.type NOT IN ('INFO_ACTION', 'TREASURY_WITHDRAWALS_ACTION')
),
expired_set AS (
    SELECT s.gov_action_tx_hash AS tx_hash, s.gov_action_index AS idx
    FROM gov_action_proposal_status s
    WHERE s.status = 'EXPIRED' AND s.epoch = %d
),
ratified_set AS (
    SELECT s.gov_action_tx_hash AS tx_hash, s.gov_action_index AS idx
    FROM gov_action_proposal_status s
    WHERE s.status = 'RATIFIED' AND s.epoch = %d
),
active_set AS (
    SELECT tx_hash, idx
    FROM gov_action_proposal
    WHERE epoch = %d
    UNION
    SELECT gov_action_tx_hash AS tx_hash, gov_action_index AS idx
    FROM gov_action_proposal_status
    WHERE status = 'ACTIVE' AND epoch = %d
),
candidates AS (
    SELECT p.tx_hash, p.idx, p.purpose_key, p.prev_tx_hash, p.prev_idx
    FROM props p
    JOIN active_set a ON a.tx_hash = p.tx_hash AND a.idx = p.idx
    WHERE NOT EXISTS (
              SELECT 1 FROM expired_set e
              WHERE e.tx_hash = p.tx_hash AND e.idx = p.idx
          )
      AND NOT EXISTS (
              SELECT 1 FROM ratified_set r
              WHERE r.tx_hash = p.tx_hash AND r.idx = p.idx
          )
),
expired_roots AS (
    SELECT p.tx_hash, p.idx, p.purpose_key
    FROM props p
    JOIN expired_set e ON e.tx_hash = p.tx_hash AND e.idx = p.idx
),
ratified_roots AS (
    SELECT p.tx_hash, p.idx, p.purpose_key, p.prev_tx_hash, p.prev_idx
    FROM props p
    JOIN ratified_set r ON r.tx_hash = p.tx_hash AND r.idx = p.idx
),
ratified_siblings AS (
    SELECT c.tx_hash, c.idx, c.purpose_key
    FROM candidates c
    JOIN ratified_roots rr
      ON c.purpose_key = rr.purpose_key
     AND (c.tx_hash, c.idx) <> (rr.tx_hash, rr.idx)
     AND (
         (c.prev_tx_hash IS NULL AND rr.prev_tx_hash IS NULL)
         OR (c.prev_tx_hash = rr.prev_tx_hash AND c.prev_idx = rr.prev_idx)
     )
),
tree(tx_hash, idx, purpose_key) AS (
    SELECT tx_hash, idx, purpose_key FROM expired_roots
    UNION
    SELECT tx_hash, idx, purpose_key FROM ratified_siblings
    UNION
    SELECT c.tx_hash, c.idx, c.purpose_key
    FROM tree t
    JOIN candidates c
      ON c.prev_tx_hash = t.tx_hash
     AND c.prev_idx = t.idx
     AND c.purpose_key = t.purpose_key
),
dropped AS (
    SELECT DISTINCT t.tx_hash, t.idx
    FROM tree t
    JOIN candidates c ON c.tx_hash = t.tx_hash AND c.idx = t.idx
)
SELECT
    g.return_address,
    SUM(g.deposit) AS deposit
FROM gov_action_proposal g
LEFT JOIN gov_action_proposal_status s
    ON g.tx_hash = s.gov_action_tx_hash
   AND g.idx = s.gov_action_index
WHERE ((s.status = 'ACTIVE' AND g.epoch < %d AND s.epoch = %d)
    OR (s.status IS NULL AND g.epoch = %d))
  AND (g.tx_hash, g.idx) IN (SELECT tx_hash, idx FROM dropped)
GROUP BY g.return_address
""" % (epoch, epoch, epoch, epoch, epoch, epoch, epoch)


def render_pv9_cleared_insert_sql(debug_schema: str, snapshot_epoch: int, epoch: int, pv9_max_epoch: int) -> str:
    debug = sql_identifier(debug_schema)
    # Cache the expensive PV9 stale reverse-clearing calculation once per epoch
    # so compare runs can reuse it without touching source tables.
    return f"""
INSERT INTO {debug}.pv9_cleared_address_epoch (snapshot_epoch, pv9_max_epoch, address)
SELECT DISTINCT {snapshot_epoch}, {pv9_max_epoch}, stale_del.address
FROM delegation_vote stale_del
INNER JOIN drep_registration unreg
    ON unreg.drep_hash = stale_del.drep_hash
   AND unreg.cred_type = stale_del.drep_type
   AND unreg.type = 'UNREG_DREP_CERT'
   AND unreg.epoch <= {epoch}
   AND unreg.epoch <= {pv9_max_epoch}
   AND (
       unreg.slot > stale_del.slot
       OR (unreg.slot = stale_del.slot AND unreg.tx_index > stale_del.tx_index)
       OR (
           unreg.slot = stale_del.slot
           AND unreg.tx_index = stale_del.tx_index
           AND unreg.cert_index > stale_del.cert_index
       )
   )
WHERE stale_del.drep_type NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
  AND stale_del.epoch <= {pv9_max_epoch}
  AND stale_del.epoch <= {epoch}
  AND EXISTS (
      SELECT 1
      FROM delegation_vote redel
      WHERE redel.address = stale_del.address
        AND redel.drep_type NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
        AND redel.drep_hash <> stale_del.drep_hash
        AND redel.epoch <= {epoch}
        AND (
            redel.slot > stale_del.slot
            OR (redel.slot = stale_del.slot AND redel.tx_index > stale_del.tx_index)
            OR (
                redel.slot = stale_del.slot
                AND redel.tx_index = stale_del.tx_index
                AND redel.cert_index > stale_del.cert_index
            )
        )
        AND (
            redel.slot < unreg.slot
            OR (redel.slot = unreg.slot AND redel.tx_index < unreg.tx_index)
            OR (
                redel.slot = unreg.slot
                AND redel.tx_index = unreg.tx_index
                AND redel.cert_index < unreg.cert_index
            )
        )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM drep_registration earlier_unreg
      WHERE earlier_unreg.drep_hash = stale_del.drep_hash
        AND earlier_unreg.cred_type = stale_del.drep_type
        AND earlier_unreg.type = 'UNREG_DREP_CERT'
        AND (
            earlier_unreg.slot > stale_del.slot
            OR (earlier_unreg.slot = stale_del.slot AND earlier_unreg.tx_index > stale_del.tx_index)
            OR (
                earlier_unreg.slot = stale_del.slot
                AND earlier_unreg.tx_index = stale_del.tx_index
                AND earlier_unreg.cert_index > stale_del.cert_index
            )
        )
        AND (
            earlier_unreg.slot < unreg.slot
            OR (earlier_unreg.slot = unreg.slot AND earlier_unreg.tx_index < unreg.tx_index)
            OR (
                earlier_unreg.slot = unreg.slot
                AND earlier_unreg.tx_index = unreg.tx_index
                AND earlier_unreg.cert_index < unreg.cert_index
            )
        )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM stake_registration stake_dereg
      WHERE stake_dereg.address = stale_del.address
        AND stake_dereg.type = 'STAKE_DEREGISTRATION'
        AND stake_dereg.epoch <= {epoch}
        AND (
            stake_dereg.slot > stale_del.slot
            OR (stake_dereg.slot = stale_del.slot AND stake_dereg.tx_index > stale_del.tx_index)
            OR (
                stake_dereg.slot = stale_del.slot
                AND stake_dereg.tx_index = stale_del.tx_index
                AND stake_dereg.cert_index > stale_del.cert_index
            )
        )
        AND (
            stake_dereg.slot < unreg.slot
            OR (stake_dereg.slot = unreg.slot AND stake_dereg.tx_index < unreg.tx_index)
            OR (
                stake_dereg.slot = unreg.slot
                AND stake_dereg.tx_index = unreg.tx_index
                AND stake_dereg.cert_index < unreg.cert_index
            )
        )
        AND EXISTS (
            SELECT 1
            FROM delegation_vote current_before_dereg
            WHERE current_before_dereg.address = stale_del.address
              AND current_before_dereg.drep_hash = stale_del.drep_hash
              AND current_before_dereg.drep_type = stale_del.drep_type
              AND current_before_dereg.epoch <= {epoch}
              AND (
                  current_before_dereg.slot > stale_del.slot
                  OR (
                      current_before_dereg.slot = stale_del.slot
                      AND current_before_dereg.tx_index > stale_del.tx_index
                  )
                  OR (
                      current_before_dereg.slot = stale_del.slot
                      AND current_before_dereg.tx_index = stale_del.tx_index
                      AND current_before_dereg.cert_index >= stale_del.cert_index
                  )
              )
              AND (
                  current_before_dereg.slot < stake_dereg.slot
                  OR (
                      current_before_dereg.slot = stake_dereg.slot
                      AND current_before_dereg.tx_index < stake_dereg.tx_index
                  )
                  OR (
                      current_before_dereg.slot = stake_dereg.slot
                      AND current_before_dereg.tx_index = stake_dereg.tx_index
                      AND current_before_dereg.cert_index < stake_dereg.cert_index
                  )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM delegation_vote later_del
                  WHERE later_del.address = stale_del.address
                    AND later_del.epoch <= {epoch}
                    AND (
                        later_del.slot > current_before_dereg.slot
                        OR (
                            later_del.slot = current_before_dereg.slot
                            AND later_del.tx_index > current_before_dereg.tx_index
                        )
                        OR (
                            later_del.slot = current_before_dereg.slot
                            AND later_del.tx_index = current_before_dereg.tx_index
                            AND later_del.cert_index > current_before_dereg.cert_index
                        )
                    )
                    AND (
                        later_del.slot < stake_dereg.slot
                        OR (
                            later_del.slot = stake_dereg.slot
                            AND later_del.tx_index < stake_dereg.tx_index
                        )
                        OR (
                            later_del.slot = stake_dereg.slot
                            AND later_del.tx_index = stake_dereg.tx_index
                            AND later_del.cert_index < stake_dereg.cert_index
                        )
                    )
              )
        )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM delegation_vote virt_del
      WHERE virt_del.address = stale_del.address
        AND virt_del.drep_type IN ('ABSTAIN', 'NO_CONFIDENCE')
        AND virt_del.epoch <= {epoch}
        AND (
            virt_del.slot > stale_del.slot
            OR (virt_del.slot = stale_del.slot AND virt_del.tx_index > stale_del.tx_index)
            OR (
                virt_del.slot = stale_del.slot
                AND virt_del.tx_index = stale_del.tx_index
                AND virt_del.cert_index > stale_del.cert_index
            )
        )
        AND (
            virt_del.slot < unreg.slot
            OR (virt_del.slot = unreg.slot AND virt_del.tx_index < unreg.tx_index)
            OR (
                virt_del.slot = unreg.slot
                AND virt_del.tx_index = unreg.tx_index
                AND virt_del.cert_index < unreg.cert_index
            )
        )
        AND EXISTS (
            SELECT 1
            FROM delegation_vote prev_del
            WHERE prev_del.address = stale_del.address
              AND prev_del.drep_hash = stale_del.drep_hash
              AND prev_del.drep_type = stale_del.drep_type
              AND prev_del.epoch <= {epoch}
              AND (
                  prev_del.slot < virt_del.slot
                  OR (prev_del.slot = virt_del.slot AND prev_del.tx_index < virt_del.tx_index)
                  OR (
                      prev_del.slot = virt_del.slot
                      AND prev_del.tx_index = virt_del.tx_index
                      AND prev_del.cert_index < virt_del.cert_index
                  )
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM delegation_vote between_del
                  WHERE between_del.address = stale_del.address
                    AND (
                        between_del.slot > prev_del.slot
                        OR (
                            between_del.slot = prev_del.slot
                            AND between_del.tx_index > prev_del.tx_index
                        )
                        OR (
                            between_del.slot = prev_del.slot
                            AND between_del.tx_index = prev_del.tx_index
                            AND between_del.cert_index > prev_del.cert_index
                        )
                    )
                    AND (
                        between_del.slot < virt_del.slot
                        OR (
                            between_del.slot = virt_del.slot
                            AND between_del.tx_index < virt_del.tx_index
                        )
                        OR (
                            between_del.slot = virt_del.slot
                            AND between_del.tx_index = virt_del.tx_index
                            AND between_del.cert_index < virt_del.cert_index
                        )
                    )
              )
        )
        AND NOT EXISTS (
            SELECT 1
            FROM delegation_vote readd_del
            WHERE readd_del.address = stale_del.address
              AND readd_del.drep_hash = stale_del.drep_hash
              AND readd_del.drep_type = stale_del.drep_type
              AND readd_del.epoch <= {epoch}
              AND (
                  readd_del.slot > virt_del.slot
                  OR (readd_del.slot = virt_del.slot AND readd_del.tx_index > virt_del.tx_index)
                  OR (
                      readd_del.slot = virt_del.slot
                      AND readd_del.tx_index = virt_del.tx_index
                      AND readd_del.cert_index > virt_del.cert_index
                  )
              )
              AND (
                  readd_del.slot < unreg.slot
                  OR (readd_del.slot = unreg.slot AND readd_del.tx_index < unreg.tx_index)
                  OR (
                      readd_del.slot = unreg.slot
                      AND readd_del.tx_index = unreg.tx_index
                      AND readd_del.cert_index < unreg.cert_index
                  )
              )
        )
  )
  AND NOT EXISTS (
      SELECT 1
      FROM delegation_vote after_del
      WHERE after_del.address = stale_del.address
        AND after_del.epoch <= {epoch}
        AND (
            after_del.slot > unreg.slot
            OR (after_del.slot = unreg.slot AND after_del.tx_index > unreg.tx_index)
            OR (
                after_del.slot = unreg.slot
                AND after_del.tx_index = unreg.tx_index
                AND after_del.cert_index > unreg.cert_index
            )
        )
  )
ON CONFLICT DO NOTHING
"""


def render_prepare_sql(
    store_schema: str,
    debug_schema: str,
    snapshot_epoch: int,
    epoch_context: EpochExecutionContext,
    settings: QuerySettings,
) -> str:
    debug = sql_identifier(debug_schema)
    epoch = epoch_context.epoch

    parts = [render_session_settings(store_schema, settings)]
    parts.append(timed_sql("ss_current_drep_address_create", render_current_drep_delegations_sql(epoch)))
    parts.append(timed_sql("idx_ss_current_drep_address_address", "CREATE INDEX idx_ss_current_drep_address_address ON ss_current_drep_address(address)"))
    parts.append(timed_sql("analyze_ss_current_drep_address", "ANALYZE ss_current_drep_address"))
    parts.append("SELECT COUNT(*) AS __meta_value FROM ss_current_drep_address \\gset\n\\echo META|current_drep_address_count|:__meta_value\n")

    parts.append(
        # Cache the subset of current DRep addresses that cannot be sourced from
        # epoch_stake and therefore must be recomputed from raw tables.
        """
SELECT CASE
         WHEN EXISTS (
             SELECT 1
             FROM %s.cache_state
             WHERE cache_name = 'missing_address_epoch'
               AND snapshot_epoch = %d
               AND cache_key = 0
         )
         THEN 'true'
         ELSE 'false'
       END AS __missing_cache_hit \\gset
\\echo META|missing_address_cache_hit|:__missing_cache_hit
\\if :__missing_cache_hit
\\else
DELETE FROM %s.missing_address_epoch WHERE snapshot_epoch = %d;
%s
SELECT COUNT(*)::bigint AS __missing_row_count
FROM %s.missing_address_epoch
WHERE snapshot_epoch = %d \\gset
INSERT INTO %s.cache_state(cache_name, snapshot_epoch, cache_key, row_count, updated_at)
VALUES ('missing_address_epoch', %d, 0, :__missing_row_count, NOW())
ON CONFLICT (cache_name, snapshot_epoch, cache_key)
DO UPDATE SET row_count = EXCLUDED.row_count, updated_at = EXCLUDED.updated_at;
\\endif
SELECT COALESCE((
    SELECT row_count
    FROM %s.cache_state
    WHERE cache_name = 'missing_address_epoch'
      AND snapshot_epoch = %d
      AND cache_key = 0
), 0) AS __meta_value \\gset
\\echo META|missing_address_count|:__meta_value
"""
        % (
            debug,
            snapshot_epoch,
            debug,
            snapshot_epoch,
            timed_sql(
                "cache_missing_address_epoch_build",
                """
INSERT INTO %s.missing_address_epoch (snapshot_epoch, address)
SELECT %d, c.address
FROM ss_current_drep_address c
LEFT JOIN epoch_stake es
  ON es.epoch = %d
 AND es.address = c.address
WHERE es.address IS NULL
""" % (debug, snapshot_epoch, epoch),
            ),
            debug,
            snapshot_epoch,
            debug,
            snapshot_epoch,
            debug,
            snapshot_epoch,
        )
    )

    parts.append(
        # PV9 clearing is logically part of DRepDistService but expensive enough
        # to materialize once and reuse across repeated compare runs.
        """
SELECT CASE
         WHEN EXISTS (
             SELECT 1
             FROM %s.cache_state
             WHERE cache_name = 'pv9_cleared_address_epoch'
               AND snapshot_epoch = %d
               AND cache_key = %d
         )
         THEN 'true'
         ELSE 'false'
       END AS __pv9_cache_hit \\gset
\\echo META|pv9_cleared_cache_hit|:__pv9_cache_hit
\\if :__pv9_cache_hit
\\else
DELETE FROM %s.pv9_cleared_address_epoch WHERE snapshot_epoch = %d AND pv9_max_epoch = %d;
%s
SELECT COUNT(*)::bigint AS __pv9_row_count
FROM %s.pv9_cleared_address_epoch
WHERE snapshot_epoch = %d
  AND pv9_max_epoch = %d \\gset
INSERT INTO %s.cache_state(cache_name, snapshot_epoch, cache_key, row_count, updated_at)
VALUES ('pv9_cleared_address_epoch', %d, %d, :__pv9_row_count, NOW())
ON CONFLICT (cache_name, snapshot_epoch, cache_key)
DO UPDATE SET row_count = EXCLUDED.row_count, updated_at = EXCLUDED.updated_at;
\\endif
SELECT COALESCE((
    SELECT row_count
    FROM %s.cache_state
    WHERE cache_name = 'pv9_cleared_address_epoch'
      AND snapshot_epoch = %d
      AND cache_key = %d
), 0) AS __meta_value \\gset
\\echo META|pv9_cleared_address_count|:__meta_value
"""
        % (
            debug,
            snapshot_epoch,
            epoch_context.pv9_max_epoch,
            debug,
            snapshot_epoch,
            epoch_context.pv9_max_epoch,
            timed_sql(
                "cache_pv9_cleared_address_epoch_build",
                render_pv9_cleared_insert_sql(
                    debug_schema=debug_schema,
                    snapshot_epoch=snapshot_epoch,
                    epoch=epoch,
                    pv9_max_epoch=epoch_context.pv9_max_epoch,
                ),
            ),
            debug,
            snapshot_epoch,
            epoch_context.pv9_max_epoch,
            debug,
            snapshot_epoch,
            epoch_context.pv9_max_epoch,
            debug,
            snapshot_epoch,
            epoch_context.pv9_max_epoch,
        )
    )
    parts.append("COMMIT;\n")
    return "".join(parts)


def render_compare_sql(
    store_schema: str,
    debug_schema: str,
    snapshot_epoch: int,
    epoch_context: EpochExecutionContext,
    settings: QuerySettings,
    store_output_path: str,
    run_id: str,
) -> str:
    debug = sql_identifier(debug_schema)
    epoch = epoch_context.epoch
    valid_condition = build_valid_delegation_condition(epoch_context)
    hardcoded_condition = build_hardcoded_exclusion_condition(
        epoch_context.exclusions,
        not epoch_context.is_bootstrap_phase,
    )

    parts = [render_session_settings(store_schema, settings)]
    parts.append(timed_sql("ss_current_drep_address_create", render_current_drep_delegations_sql(epoch)))
    parts.append(timed_sql("idx_ss_current_drep_address_address", "CREATE INDEX idx_ss_current_drep_address_address ON ss_current_drep_address(address)"))
    parts.append(timed_sql("analyze_ss_current_drep_address", "ANALYZE ss_current_drep_address"))

    parts.append(
        timed_sql(
            "ss_missing_addresses_create",
            """
CREATE TEMP TABLE ss_missing_addresses ON COMMIT DROP AS
SELECT address
FROM %s.missing_address_epoch
WHERE snapshot_epoch = %d
"""
            % (debug, snapshot_epoch),
        )
    )
    parts.append(timed_sql("idx_ss_missing_addresses_address", "CREATE INDEX idx_ss_missing_addresses_address ON ss_missing_addresses(address)"))
    parts.append(timed_sql("analyze_ss_missing_addresses", "ANALYZE ss_missing_addresses"))

    parts.append(
        timed_sql(
            "ss_pv9_cleared_addresses_create",
            """
CREATE TEMP TABLE ss_pv9_cleared_addresses ON COMMIT DROP AS
SELECT address
FROM %s.pv9_cleared_address_epoch
WHERE snapshot_epoch = %d
  AND pv9_max_epoch = %d
"""
            % (debug, snapshot_epoch, epoch_context.pv9_max_epoch),
        )
    )
    parts.append(timed_sql("idx_ss_pv9_cleared_addresses_address", "CREATE INDEX idx_ss_pv9_cleared_addresses_address ON ss_pv9_cleared_addresses(address)"))

    parts.append(
        timed_sql(
            "ss_last_withdrawal_create",
            """
CREATE TEMP TABLE ss_last_withdrawal ON COMMIT DROP AS
SELECT w.address, MAX(w.slot) AS max_slot
FROM withdrawal w
JOIN ss_current_drep_address c ON c.address = w.address
WHERE w.epoch <= %d
GROUP BY w.address
"""
            % epoch,
        )
    )
    parts.append(timed_sql("idx_ss_last_withdrawal_address", "CREATE INDEX idx_ss_last_withdrawal_address ON ss_last_withdrawal(address)"))

    parts.append(
        timed_sql(
            "ss_epoch_stake_base_create",
            # Reuse epoch_stake as the primary financial base whenever an address
            # already exists in the previous epoch snapshot.
            """
CREATE TEMP TABLE ss_epoch_stake_base ON COMMIT DROP AS
SELECT
    c.address,
    c.drep_id,
    c.drep_hash,
    c.drep_type,
    c.epoch,
    c.slot,
    c.tx_index,
    c.cert_index,
    es.amount AS epoch_stake_amount
FROM ss_current_drep_address c
JOIN epoch_stake es
  ON es.epoch = %d
 AND es.address = c.address
"""
            % epoch,
        )
    )
    parts.append(timed_sql("idx_ss_epoch_stake_base_address", "CREATE INDEX idx_ss_epoch_stake_base_address ON ss_epoch_stake_base(address)"))
    parts.append(timed_sql("analyze_ss_epoch_stake_base", "ANALYZE ss_epoch_stake_base"))

    parts.append(
        timed_sql(
            "ss_epoch_stake_delta_reward_rest_create",
            """
CREATE TEMP TABLE ss_epoch_stake_delta_reward_rest ON COMMIT DROP AS
SELECT
    r.address,
    SUM(r.amount) AS withdrawable_reward_rest_delta
FROM reward_rest r
JOIN ss_epoch_stake_base esb ON esb.address = r.address
LEFT JOIN ss_last_withdrawal lw ON lw.address = r.address
WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
  AND r.spendable_epoch = %d
GROUP BY r.address
"""
            % snapshot_epoch,
        )
    )
    parts.append(timed_sql("idx_ss_epoch_stake_delta_reward_rest_address", "CREATE INDEX idx_ss_epoch_stake_delta_reward_rest_address ON ss_epoch_stake_delta_reward_rest(address)"))

    parts.append(
        timed_sql(
            "ss_epoch_stake_delta_pool_refund_rewards_create",
            """
CREATE TEMP TABLE ss_epoch_stake_delta_pool_refund_rewards ON COMMIT DROP AS
SELECT
    r.address,
    SUM(r.amount) AS pool_refund_withdrawable_reward_delta
FROM reward r
JOIN ss_epoch_stake_base esb ON esb.address = r.address
LEFT JOIN ss_last_withdrawal lw ON lw.address = r.address
WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
  AND r.spendable_epoch = %d
  AND r.type = 'refund'
GROUP BY r.address
"""
            % snapshot_epoch,
        )
    )
    parts.append(timed_sql("idx_ss_epoch_stake_delta_pool_refund_rewards_address", "CREATE INDEX idx_ss_epoch_stake_delta_pool_refund_rewards_address ON ss_epoch_stake_delta_pool_refund_rewards(address)"))

    parts.append(
        timed_sql(
            "ss_base_from_epoch_stake_create",
            """
CREATE TEMP TABLE ss_base_from_epoch_stake ON COMMIT DROP AS
SELECT
    esb.address,
    esb.drep_id,
    esb.drep_hash,
    esb.drep_type,
    esb.epoch,
    esb.slot,
    esb.tx_index,
    esb.cert_index,
    (
        COALESCE(esb.epoch_stake_amount, 0)
        + COALESCE(rr.withdrawable_reward_rest_delta, 0)
        + COALESCE(pr.pool_refund_withdrawable_reward_delta, 0)
    ) AS base_amount
FROM ss_epoch_stake_base esb
LEFT JOIN ss_epoch_stake_delta_reward_rest rr ON rr.address = esb.address
LEFT JOIN ss_epoch_stake_delta_pool_refund_rewards pr ON pr.address = esb.address
""",
        )
    )
    parts.append("SELECT COUNT(*) AS __meta_value FROM ss_base_from_epoch_stake \\gset\n\\echo META|base_from_epoch_stake_count|:__meta_value\n")

    parts.append(
        timed_sql(
            "ss_missing_max_slot_balances_create",
            """
CREATE TEMP TABLE ss_missing_max_slot_balances ON COMMIT DROP AS
SELECT s.address, MAX(s.slot) AS max_slot
FROM stake_address_balance s
JOIN ss_missing_addresses m ON m.address = s.address
WHERE s.epoch <= %d
GROUP BY s.address
"""
            % epoch,
        )
    )
    parts.append(timed_sql("idx_ss_missing_max_slot_balances_address", "CREATE INDEX idx_ss_missing_max_slot_balances_address ON ss_missing_max_slot_balances(address)"))

    parts.append(
        timed_sql(
            "ss_missing_pool_rewards_create",
            """
CREATE TEMP TABLE ss_missing_pool_rewards ON COMMIT DROP AS
SELECT
    r.address,
    SUM(r.amount) AS withdrawable_reward
FROM reward r
JOIN ss_missing_addresses m ON m.address = r.address
LEFT JOIN ss_last_withdrawal lw ON lw.address = r.address
WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
  AND r.earned_epoch <= %d
  AND r.spendable_epoch <= %d
  AND r.type IN ('member', 'leader')
GROUP BY r.address
"""
            % (epoch, snapshot_epoch),
        )
    )
    parts.append(timed_sql("idx_ss_missing_pool_rewards_address", "CREATE INDEX idx_ss_missing_pool_rewards_address ON ss_missing_pool_rewards(address)"))

    parts.append(
        timed_sql(
            "ss_missing_pool_refund_rewards_create",
            """
CREATE TEMP TABLE ss_missing_pool_refund_rewards ON COMMIT DROP AS
SELECT
    r.address,
    SUM(r.amount) AS pool_refund_withdrawable_reward
FROM reward r
JOIN ss_missing_addresses m ON m.address = r.address
LEFT JOIN ss_last_withdrawal lw ON lw.address = r.address
WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
  AND r.spendable_epoch <= %d
  AND r.type = 'refund'
GROUP BY r.address
"""
            % snapshot_epoch,
        )
    )
    parts.append(timed_sql("idx_ss_missing_pool_refund_rewards_address", "CREATE INDEX idx_ss_missing_pool_refund_rewards_address ON ss_missing_pool_refund_rewards(address)"))

    parts.append(
        timed_sql(
            "ss_missing_insta_spendable_rewards_create",
            """
CREATE TEMP TABLE ss_missing_insta_spendable_rewards ON COMMIT DROP AS
SELECT
    r.address,
    SUM(r.amount) AS insta_withdrawable_reward
FROM instant_reward r
JOIN ss_missing_addresses m ON m.address = r.address
LEFT JOIN ss_last_withdrawal lw ON lw.address = r.address
WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
  AND r.spendable_epoch <= %d
GROUP BY r.address
"""
            % snapshot_epoch,
        )
    )
    parts.append(timed_sql("idx_ss_missing_insta_spendable_rewards_address", "CREATE INDEX idx_ss_missing_insta_spendable_rewards_address ON ss_missing_insta_spendable_rewards(address)"))

    parts.append(
        timed_sql(
            "ss_missing_reward_rest_create",
            """
CREATE TEMP TABLE ss_missing_reward_rest ON COMMIT DROP AS
SELECT
    r.address,
    SUM(r.amount) AS withdrawable_reward_rest
FROM reward_rest r
JOIN ss_missing_addresses m ON m.address = r.address
LEFT JOIN ss_last_withdrawal lw ON lw.address = r.address
WHERE (lw.max_slot IS NULL OR r.slot > lw.max_slot)
  AND r.spendable_epoch <= %d
GROUP BY r.address
"""
            % snapshot_epoch,
        )
    )
    parts.append(timed_sql("idx_ss_missing_reward_rest_address", "CREATE INDEX idx_ss_missing_reward_rest_address ON ss_missing_reward_rest(address)"))

    parts.append(
        timed_sql(
            "ss_base_from_missing_raw_create",
            # Only addresses absent from epoch_stake fall back to the full raw
            # reconstruction path, which keeps the hybrid flow exact but cheaper.
            """
CREATE TEMP TABLE ss_base_from_missing_raw ON COMMIT DROP AS
SELECT
    c.address,
    c.drep_id,
    c.drep_hash,
    c.drep_type,
    c.epoch,
    c.slot,
    c.tx_index,
    c.cert_index,
    (
        COALESCE(sab.quantity, 0)
        + COALESCE(r.withdrawable_reward, 0)
        + COALESCE(pr.pool_refund_withdrawable_reward, 0)
        + COALESCE(ir.insta_withdrawable_reward, 0)
        + COALESCE(rr.withdrawable_reward_rest, 0)
    ) AS base_amount
FROM ss_current_drep_address c
JOIN ss_missing_addresses m ON m.address = c.address
LEFT JOIN ss_missing_max_slot_balances msb ON msb.address = c.address
LEFT JOIN stake_address_balance sab
  ON sab.address = msb.address
 AND sab.slot = msb.max_slot
LEFT JOIN ss_missing_pool_rewards r ON r.address = c.address
LEFT JOIN ss_missing_pool_refund_rewards pr ON pr.address = c.address
LEFT JOIN ss_missing_insta_spendable_rewards ir ON ir.address = c.address
LEFT JOIN ss_missing_reward_rest rr ON rr.address = c.address
""",
        )
    )
    parts.append("SELECT COUNT(*) AS __meta_value FROM ss_base_from_missing_raw \\gset\n\\echo META|base_from_missing_raw_count|:__meta_value\n")

    parts.append(timed_sql("ss_drep_status_create", render_drep_status_sql(epoch)))
    parts.append(timed_sql("idx_ss_drep_status_drep_hash", "CREATE INDEX idx_ss_drep_status_drep_hash ON ss_drep_status(drep_hash)"))
    parts.append(timed_sql("idx_ss_drep_status_type", "CREATE INDEX idx_ss_drep_status_type ON ss_drep_status(type)"))

    parts.append(timed_sql("ss_gov_active_proposal_deposits_create", render_active_proposal_deposits_sql(epoch)))
    parts.append(timed_sql("idx_ss_gov_active_proposal_deposits_ret_address", "CREATE INDEX idx_ss_gov_active_proposal_deposits_ret_address ON ss_gov_active_proposal_deposits(return_address)"))

    parts.append(timed_sql("ss_gov_scheduled_to_drop_proposal_deposits_create", render_dropped_proposal_deposits_sql(epoch)))
    parts.append(timed_sql("idx_ss_gov_scheduled_to_drop_proposal_deposits_ret_address", "CREATE INDEX idx_ss_gov_scheduled_to_drop_proposal_deposits_ret_address ON ss_gov_scheduled_to_drop_proposal_deposits(return_address)"))

    parts.append(
        timed_sql(
            "ss_effective_address_amount_create",
            # Governance deposits are applied after rebuilding the financial base,
            # matching the order used by DRepDistService.
            """
CREATE TEMP TABLE ss_effective_address_amount ON COMMIT DROP AS
SELECT
    base.address,
    base.drep_id,
    base.drep_hash,
    base.drep_type,
    base.epoch,
    base.slot,
    base.tx_index,
    base.cert_index,
    (
        base.base_amount
        + COALESCE(apd.deposit, 0)
        - COALESCE(dpd.deposit, 0)
    ) AS effective_amount
FROM (
    SELECT * FROM ss_base_from_epoch_stake
    UNION ALL
    SELECT * FROM ss_base_from_missing_raw
) base
LEFT JOIN ss_gov_active_proposal_deposits apd ON apd.return_address = base.address
LEFT JOIN ss_gov_scheduled_to_drop_proposal_deposits dpd ON dpd.return_address = base.address
""",
        )
    )
    parts.append(timed_sql("idx_ss_effective_address_amount_address", "CREATE INDEX idx_ss_effective_address_amount_address ON ss_effective_address_amount(address)"))
    parts.append(timed_sql("analyze_ss_effective_address_amount", "ANALYZE ss_effective_address_amount"))

    parts.append(
        timed_sql(
            "tmp_drep_dist_create",
            """
CREATE TEMP TABLE tmp_drep_dist (
    drep_hash varchar(255),
    drep_type varchar(50),
    drep_id varchar(255),
    amount numeric(38)
) ON COMMIT DROP
""",
        )
    )

    parts.append(
        timed_sql(
            "tmp_drep_dist_insert_regular",
            """
INSERT INTO tmp_drep_dist
SELECT
    rd.drep_hash,
    rd.drep_type,
    rd.drep_id,
    SUM(rd.effective_amount) AS amount
FROM ss_effective_address_amount rd
LEFT JOIN stake_registration sd
  ON sd.address = rd.address
 AND sd.type = 'STAKE_DEREGISTRATION'
 AND sd.epoch <= %d
 AND (
     sd.slot > rd.slot
     OR (sd.slot = rd.slot AND sd.tx_index > rd.tx_index)
     OR (sd.slot = rd.slot AND sd.tx_index = rd.tx_index AND sd.cert_index > rd.cert_index)
 )
WHERE rd.drep_type NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
  AND sd.address IS NULL
%s
%s
  AND rd.address NOT IN (SELECT address FROM ss_pv9_cleared_addresses)
GROUP BY
    rd.drep_hash,
    rd.drep_type,
    rd.drep_id
"""
            % (epoch, valid_condition, hardcoded_condition),
        )
    )

    parts.append(
        timed_sql(
            "tmp_drep_dist_insert_virtual",
            """
INSERT INTO tmp_drep_dist
SELECT
    %s,
    rd.drep_type,
    NULL,
    SUM(rd.effective_amount) AS amount
FROM ss_effective_address_amount rd
LEFT JOIN stake_registration sd
  ON sd.address = rd.address
 AND sd.type = 'STAKE_DEREGISTRATION'
 AND sd.epoch <= %d
 AND (
     sd.slot > rd.slot
     OR (sd.slot = rd.slot AND sd.tx_index > rd.tx_index)
     OR (sd.slot = rd.slot AND sd.tx_index = rd.tx_index AND sd.cert_index > rd.cert_index)
 )
WHERE rd.drep_type IN ('ABSTAIN', 'NO_CONFIDENCE')
  AND sd.address IS NULL
  AND rd.address NOT IN (SELECT address FROM ss_pv9_cleared_addresses)
GROUP BY rd.drep_type
"""
            % (sql_literal(ZERO_HASH), epoch),
        )
    )

    parts.append(
        timed_sql(
            "shadow_drep_dist_replace",
            """
DELETE FROM %s.shadow_drep_dist
WHERE snapshot_epoch = %d;

INSERT INTO %s.shadow_drep_dist (
    snapshot_epoch,
    drep_hash,
    drep_type,
    drep_id,
    amount,
    run_id,
    created_at
)
SELECT
    %d,
    drep_hash,
    drep_type,
    NULLIF(drep_id, ''),
    amount,
    %s,
    NOW()
FROM tmp_drep_dist
"""
            % (
                debug,
                snapshot_epoch,
                debug,
                snapshot_epoch,
                sql_literal(run_id),
            ),
        )
    )

    parts.append(
        timed_psql_block(
            "tmp_drep_dist_copy",
            """
\\o %s
COPY (
    SELECT drep_hash, drep_type, COALESCE(drep_id, '') AS drep_id, amount
    FROM tmp_drep_dist
    ORDER BY drep_type, drep_id, drep_hash
) TO STDOUT WITH CSV HEADER;
\\o
"""
            % store_output_path,
        )
    )
    parts.append("COMMIT;\n")
    return "".join(parts)


def extract_prepare_cache_details(metadata: Dict[str, str]) -> Tuple[Dict[str, bool], Dict[str, int]]:
    cache_hits = {
        "missing_address_epoch": meta_bool(metadata, "missing_address_cache_hit"),
        "pv9_cleared_address_epoch": meta_bool(metadata, "pv9_cleared_cache_hit"),
    }
    cache_counts = {
        "current_drep_address_count": meta_int(metadata, "current_drep_address_count"),
        "missing_address_count": meta_int(metadata, "missing_address_count"),
        "pv9_cleared_address_count": meta_int(metadata, "pv9_cleared_address_count"),
    }
    return cache_hits, cache_counts


def extract_compare_counts(metadata: Dict[str, str]) -> Dict[str, int]:
    return {
        "from_epoch_stake": meta_int(metadata, "base_from_epoch_stake_count"),
        "from_missing_raw": meta_int(metadata, "base_from_missing_raw_count"),
    }


def ensure_debug_schema(store: DbConfig, debug_schema: str) -> None:
    debug = sql_identifier(debug_schema)
    sql = f"""
CREATE SCHEMA IF NOT EXISTS {debug};

CREATE UNLOGGED TABLE IF NOT EXISTS {debug}.cache_state (
    cache_name varchar(128) NOT NULL,
    snapshot_epoch integer NOT NULL,
    cache_key integer NOT NULL DEFAULT 0,
    row_count bigint NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (cache_name, snapshot_epoch, cache_key)
);

CREATE UNLOGGED TABLE IF NOT EXISTS {debug}.missing_address_epoch (
    snapshot_epoch integer NOT NULL,
    address varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (snapshot_epoch, address)
);

CREATE INDEX IF NOT EXISTS idx_missing_address_epoch_epoch
    ON {debug}.missing_address_epoch (snapshot_epoch);

CREATE UNLOGGED TABLE IF NOT EXISTS {debug}.pv9_cleared_address_epoch (
    snapshot_epoch integer NOT NULL,
    pv9_max_epoch integer NOT NULL,
    address varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (snapshot_epoch, pv9_max_epoch, address)
);

CREATE INDEX IF NOT EXISTS idx_pv9_cleared_address_epoch_epoch
    ON {debug}.pv9_cleared_address_epoch (snapshot_epoch, pv9_max_epoch);

CREATE UNLOGGED TABLE IF NOT EXISTS {debug}.run_timing (
    run_id varchar(255) NOT NULL,
    snapshot_epoch integer NOT NULL,
    phase varchar(32) NOT NULL,
    step_name varchar(255) NOT NULL,
    duration_ms numeric(18, 3) NOT NULL,
    row_count bigint,
    created_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE UNLOGGED TABLE IF NOT EXISTS {debug}.shadow_drep_dist (
    snapshot_epoch integer NOT NULL,
    drep_hash varchar(255) NOT NULL,
    drep_type varchar(50) NOT NULL,
    drep_id varchar(255),
    amount numeric(38) NOT NULL,
    run_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (snapshot_epoch, drep_hash, drep_type)
);

CREATE INDEX IF NOT EXISTS idx_shadow_drep_dist_epoch
    ON {debug}.shadow_drep_dist (snapshot_epoch);
"""
    run_psql(store, sql)


def persist_run_timings(
    store: DbConfig,
    debug_schema: str,
    run_id: str,
    snapshot_epoch: int,
    phase: str,
    timings: Sequence[Dict[str, object]],
) -> None:
    if not timings:
        return

    debug = sql_identifier(debug_schema)
    values = []
    for timing in timings:
        duration_ms = float(timing["duration_ms"])
        row_count = timing.get("row_count")
        values.append(
            "(%s, %d, %s, %s, %s, %s, NOW())" % (
                sql_literal(run_id),
                snapshot_epoch,
                sql_literal(phase),
                sql_literal(str(timing["name"])),
                duration_ms,
                "NULL" if row_count is None else str(int(row_count)),
            )
        )

    sql = """
INSERT INTO %s.run_timing(run_id, snapshot_epoch, phase, step_name, duration_ms, row_count, created_at)
VALUES
%s
""" % (debug, ",\n".join(values))
    run_psql(store, sql)


def maybe_write_sql(report_dir: Path, filename: str, sql: str, enabled: bool) -> None:
    if not enabled:
        return
    sql_path = report_dir / "sql" / filename
    sql_path.parent.mkdir(parents=True, exist_ok=True)
    sql_path.write_text(sql, encoding="utf-8")


def run_prepare_phase(
    epoch: int,
    store: DbConfig,
    protocol_magic: int,
    exclusions: List[DRepDelegationExclusion],
    debug_schema: str,
    settings: QuerySettings,
    override_max_bootstrap_phase_epoch: Optional[int],
    report_dir: Path,
    save_store_sql: bool,
) -> Tuple[EpochExecutionContext, List[Dict[str, object]], Dict[str, str], Dict[str, bool], Dict[str, int]]:
    timings: List[Dict[str, object]] = []
    epoch_context = timed_call(
        "resolve_epoch_context",
        timings,
        lambda: resolve_epoch_context(
            store=store,
            snapshot_epoch=epoch,
            protocol_magic=protocol_magic,
            override_max_bootstrap_phase_epoch=override_max_bootstrap_phase_epoch,
            exclusions=exclusions,
        ),
    )
    prepare_sql = timed_call(
        "render_prepare_sql",
        timings,
        lambda: render_prepare_sql(
            store_schema=store.schema,
            debug_schema=debug_schema,
            snapshot_epoch=epoch,
            epoch_context=epoch_context,
            settings=settings,
        ),
    )
    maybe_write_sql(report_dir, "prepare_epoch_%d.sql" % epoch, prepare_sql, save_store_sql)
    prepare_output = timed_call("prepare_phase_total", timings, lambda: run_psql(store, prepare_sql))
    timings.extend(parse_step_timings(prepare_output))
    metadata = parse_meta_lines(prepare_output)
    cache_hits, cache_counts = extract_prepare_cache_details(metadata)
    return epoch_context, timings, metadata, cache_hits, cache_counts


def compare_single_epoch(
    epoch: int,
    store: DbConfig,
    dbsync: DbConfig,
    protocol_magic: int,
    exclusions: List[DRepDelegationExclusion],
    debug_schema: str,
    settings: QuerySettings,
    report_dir: Path,
    override_max_bootstrap_phase_epoch: Optional[int],
    save_store_sql: bool,
    run_id: str,
) -> EpochCompareResult:
    started = time.time()
    error_file = report_dir / "epochs" / ("epoch_%d.error.json" % epoch)

    epoch_context, prepare_timings, _, cache_hits, cache_counts = run_prepare_phase(
        epoch=epoch,
        store=store,
        protocol_magic=protocol_magic,
        exclusions=exclusions,
        debug_schema=debug_schema,
        settings=settings,
        override_max_bootstrap_phase_epoch=override_max_bootstrap_phase_epoch,
        report_dir=report_dir,
        save_store_sql=save_store_sql,
    )
    persist_run_timings(store, debug_schema, run_id, epoch, "prepare", prepare_timings)

    with tempfile.NamedTemporaryFile(prefix="drep_dist_store_%d_" % epoch, suffix=".csv", delete=False) as temp_output:
        store_output_path = temp_output.name

    compare_timings: List[Dict[str, object]] = []
    store_sql = timed_call(
        "render_compare_sql",
        compare_timings,
        lambda: render_compare_sql(
            store_schema=store.schema,
            debug_schema=debug_schema,
            snapshot_epoch=epoch,
            epoch_context=epoch_context,
            settings=settings,
            store_output_path=store_output_path,
            run_id=run_id,
        ),
    )
    maybe_write_sql(report_dir, "compare_epoch_%d.sql" % epoch, store_sql, save_store_sql)

    try:
        store_psql_output = timed_call(
            "store_shadow_snapshot_total",
            compare_timings,
            lambda: run_psql(store, store_sql),
        )
        compare_timings.extend(parse_step_timings(store_psql_output))
        store_meta = parse_meta_lines(store_psql_output)
        base_source_counts = extract_compare_counts(store_meta)
        store_rows = timed_call(
            "read_store_shadow_csv",
            compare_timings,
            lambda: parse_csv_rows(Path(store_output_path).read_text(encoding="utf-8")),
        )
    finally:
        try:
            os.unlink(store_output_path)
        except FileNotFoundError:
            pass

    persist_run_timings(store, debug_schema, run_id, epoch, "compare", compare_timings)

    dbsync_timings: List[Dict[str, object]] = []
    dbsync_rows = timed_call(
        "dbsync_copy",
        dbsync_timings,
        lambda: parse_csv_rows(run_psql(dbsync, render_dbsync_query(dbsync.schema, epoch))),
    )
    mismatch_count, mismatches = timed_call(
        "compare_rows",
        dbsync_timings,
        lambda: compare_rows(epoch, dbsync_rows, store_rows),
    )

    report_file = report_dir / "epochs" / ("epoch_%d.json" % epoch)
    report_payload = {
        "epoch": epoch,
        "status": "mismatch" if mismatch_count else "match",
        "mismatch_count": mismatch_count,
        "dbsync_row_count": len(dbsync_rows),
        "store_row_count": len(store_rows),
        "bootstrap_phase": epoch_context.is_bootstrap_phase,
        "pv9_max_epoch": epoch_context.pv9_max_epoch,
        "max_bootstrap_phase_epoch": epoch_context.max_bootstrap_phase_epoch,
        "cache_hits": cache_hits,
        "cache_counts": cache_counts,
        "base_source_counts": {
            **base_source_counts,
            "missing_address_count": cache_counts["missing_address_count"],
        },
        "shadow_storage": {
            "schema": debug_schema,
            "table": "shadow_drep_dist",
            "snapshot_epoch": epoch,
            "run_id": run_id,
        },
        "timings": prepare_timings + compare_timings + dbsync_timings,
        "mismatches": mismatches,
    }
    write_json(report_file, report_payload)
    if error_file.exists():
        error_file.unlink()

    return EpochCompareResult(
        epoch=epoch,
        status="mismatch" if mismatch_count else "match",
        mismatch_count=mismatch_count,
        dbsync_row_count=len(dbsync_rows),
        store_row_count=len(store_rows),
        duration_ms=int((time.time() - started) * 1000),
        report_file=str(report_file),
        error=None,
    )


def prepare_single_epoch(
    epoch: int,
    store: DbConfig,
    protocol_magic: int,
    exclusions: List[DRepDelegationExclusion],
    debug_schema: str,
    settings: QuerySettings,
    report_dir: Path,
    override_max_bootstrap_phase_epoch: Optional[int],
    save_store_sql: bool,
    run_id: str,
) -> EpochPrepareResult:
    started = time.time()
    error_file = report_dir / "epochs" / ("epoch_%d.error.json" % epoch)
    epoch_context, timings, _, cache_hits, cache_counts = run_prepare_phase(
        epoch=epoch,
        store=store,
        protocol_magic=protocol_magic,
        exclusions=exclusions,
        debug_schema=debug_schema,
        settings=settings,
        override_max_bootstrap_phase_epoch=override_max_bootstrap_phase_epoch,
        report_dir=report_dir,
        save_store_sql=save_store_sql,
    )
    persist_run_timings(store, debug_schema, run_id, epoch, "prepare", timings)

    report_file = report_dir / "epochs" / ("epoch_%d.json" % epoch)
    report_payload = {
        "epoch": epoch,
        "status": "prepared",
        "bootstrap_phase": epoch_context.is_bootstrap_phase,
        "pv9_max_epoch": epoch_context.pv9_max_epoch,
        "max_bootstrap_phase_epoch": epoch_context.max_bootstrap_phase_epoch,
        "cache_hits": cache_hits,
        "cache_counts": cache_counts,
        "timings": timings,
    }
    write_json(report_file, report_payload)
    if error_file.exists():
        error_file.unlink()

    return EpochPrepareResult(
        epoch=epoch,
        status="prepared",
        duration_ms=int((time.time() - started) * 1000),
        report_file=str(report_file),
        error=None,
    )


def compare_epoch_with_error_handling(
    epoch: int,
    store: DbConfig,
    dbsync: DbConfig,
    protocol_magic: int,
    exclusions: List[DRepDelegationExclusion],
    debug_schema: str,
    settings: QuerySettings,
    report_dir: Path,
    override_max_bootstrap_phase_epoch: Optional[int],
    save_store_sql: bool,
    run_id: str,
) -> EpochCompareResult:
    try:
        result = compare_single_epoch(
            epoch=epoch,
            store=store,
            dbsync=dbsync,
            protocol_magic=protocol_magic,
            exclusions=exclusions,
            debug_schema=debug_schema,
            settings=settings,
            report_dir=report_dir,
            override_max_bootstrap_phase_epoch=override_max_bootstrap_phase_epoch,
            save_store_sql=save_store_sql,
            run_id=run_id,
        )
        print_line(
            "epoch=%d status=%s mismatches=%d duration_ms=%d" % (
                result.epoch,
                result.status,
                result.mismatch_count,
                result.duration_ms,
            )
        )
        return result
    except Exception as exc:
        report_file = report_dir / "epochs" / ("epoch_%d.error.json" % epoch)
        write_json(
            report_file,
            {
                "epoch": epoch,
                "status": "error",
                "error": str(exc),
            },
        )
        print_line("epoch=%d status=error error=%s" % (epoch, exc))
        return EpochCompareResult(
            epoch=epoch,
            status="error",
            mismatch_count=0,
            dbsync_row_count=0,
            store_row_count=0,
            duration_ms=0,
            report_file=str(report_file),
            error=str(exc),
        )


def prepare_epoch_with_error_handling(
    epoch: int,
    store: DbConfig,
    protocol_magic: int,
    exclusions: List[DRepDelegationExclusion],
    debug_schema: str,
    settings: QuerySettings,
    report_dir: Path,
    override_max_bootstrap_phase_epoch: Optional[int],
    save_store_sql: bool,
    run_id: str,
) -> EpochPrepareResult:
    try:
        result = prepare_single_epoch(
            epoch=epoch,
            store=store,
            protocol_magic=protocol_magic,
            exclusions=exclusions,
            debug_schema=debug_schema,
            settings=settings,
            report_dir=report_dir,
            override_max_bootstrap_phase_epoch=override_max_bootstrap_phase_epoch,
            save_store_sql=save_store_sql,
            run_id=run_id,
        )
        print_line("epoch=%d status=%s duration_ms=%d" % (result.epoch, result.status, result.duration_ms))
        return result
    except Exception as exc:
        report_file = report_dir / "epochs" / ("epoch_%d.error.json" % epoch)
        write_json(
            report_file,
            {
                "epoch": epoch,
                "status": "error",
                "error": str(exc),
            },
        )
        print_line("epoch=%d status=error error=%s" % (epoch, exc))
        return EpochPrepareResult(
            epoch=epoch,
            status="error",
            duration_ms=0,
            report_file=str(report_file),
            error=str(exc),
        )


def write_compare_summary(report_dir: Path, results: Sequence[EpochCompareResult]) -> None:
    summary_csv = report_dir / "summary.csv"
    summary_json = report_dir / "summary.json"
    summary_csv.parent.mkdir(parents=True, exist_ok=True)

    with summary_csv.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(
            [
                "epoch",
                "status",
                "mismatch_count",
                "dbsync_row_count",
                "store_row_count",
                "duration_ms",
                "report_file",
                "error",
            ]
        )
        for result in sorted(results, key=lambda item: item.epoch):
            writer.writerow(
                [
                    result.epoch,
                    result.status,
                    result.mismatch_count,
                    result.dbsync_row_count,
                    result.store_row_count,
                    result.duration_ms,
                    result.report_file,
                    result.error or "",
                ]
            )

    payload = {
        "epochs": [asdict(result) for result in sorted(results, key=lambda item: item.epoch)],
        "total_epochs": len(results),
        "epochs_with_mismatches": sum(1 for result in results if result.status == "mismatch"),
        "epochs_with_errors": sum(1 for result in results if result.status == "error"),
        "total_mismatches": sum(result.mismatch_count for result in results),
    }
    write_json(summary_json, payload)


def write_prepare_summary(report_dir: Path, results: Sequence[EpochPrepareResult]) -> None:
    summary_csv = report_dir / "summary.csv"
    summary_json = report_dir / "summary.json"
    summary_csv.parent.mkdir(parents=True, exist_ok=True)

    with summary_csv.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["epoch", "status", "duration_ms", "report_file", "error"])
        for result in sorted(results, key=lambda item: item.epoch):
            writer.writerow(
                [
                    result.epoch,
                    result.status,
                    result.duration_ms,
                    result.report_file,
                    result.error or "",
                ]
            )

    payload = {
        "epochs": [asdict(result) for result in sorted(results, key=lambda item: item.epoch)],
        "total_epochs": len(results),
        "epochs_with_errors": sum(1 for result in results if result.status == "error"),
    }
    write_json(summary_json, payload)


def add_epoch_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--epochs", help="Comma-separated epoch list/ranges, for example 624-630,635,640-642")
    parser.add_argument("--epoch-start", type=int, help="Start epoch when running a contiguous range")
    parser.add_argument("--epoch-end", type=int, help="End epoch when running a contiguous range")


def add_config_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--env-file", help="Path to a .env-style file with STORE_*, DBSYNC_* and runtime settings")


def add_store_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--store-host")
    parser.add_argument("--store-port", type=int)
    parser.add_argument("--store-db")
    parser.add_argument("--store-user")
    parser.add_argument("--store-password")
    parser.add_argument("--store-schema")


def add_dbsync_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--dbsync-host")
    parser.add_argument("--dbsync-port", type=int)
    parser.add_argument("--dbsync-db")
    parser.add_argument("--dbsync-user")
    parser.add_argument("--dbsync-password")
    parser.add_argument("--dbsync-schema")


def add_runtime_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--workers", type=int, help="Number of epochs to process in parallel")
    parser.add_argument("--protocol-magic", type=int, help="Protocol magic used to load hardcoded exclusions and bootstrap logic")
    parser.add_argument("--max-bootstrap-phase-epoch", type=int, help="Override bootstrap/PV9 boundary for non-public or custom networks")
    parser.add_argument("--debug-schema")
    parser.add_argument("--lock-timeout")
    parser.add_argument("--statement-timeout")
    parser.add_argument("--work-mem")
    parser.add_argument("--maintenance-work-mem")
    parser.add_argument("--temp-buffers")
    parser.add_argument("--parallel-workers-per-gather", type=int)
    parser.add_argument("--effective-cache-size")
    parser.add_argument("--random-page-cost")
    parser.add_argument("--effective-io-concurrency", type=int)
    parser.add_argument("--parallel-setup-cost")
    parser.add_argument("--parallel-tuple-cost")
    parser.add_argument("--keep-jit", action="store_true", default=None, help="Keep PostgreSQL JIT enabled")
    parser.add_argument("--save-store-sql", action="store_true", default=None, help="Persist rendered store SQL for each epoch under the report directory")
    parser.add_argument("--report-dir", help="Directory for summaries and per-epoch reports")


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    normalized_argv = inject_default_command(argv)
    env_file = resolve_env_file_path(normalized_argv)
    parser = argparse.ArgumentParser(
        description="Prepare and compare shadow drep_dist snapshots without touching official yaci-store source tables.",
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    prepare_parser = subparsers.add_parser("prepare-range", help="Prepare persistent debug caches in drep_debug")
    add_config_args(prepare_parser)
    add_epoch_args(prepare_parser)
    add_store_args(prepare_parser)
    add_runtime_args(prepare_parser)

    compare_parser = subparsers.add_parser("compare-range", help="Compare shadow drep_dist snapshots against cardano-db-sync")
    add_config_args(compare_parser)
    add_epoch_args(compare_parser)
    add_store_args(compare_parser)
    add_dbsync_args(compare_parser)
    add_runtime_args(compare_parser)

    args = parser.parse_args(normalized_argv)
    args.env_file = args.env_file or env_file
    apply_env_defaults(args, load_env_file(args.env_file) if args.env_file else {})
    return args


def resolve_db_config(args: argparse.Namespace, prefix: str, default_schema: str) -> DbConfig:
    if prefix == "store":
        return DbConfig(
            host=require_value("store_host", args.store_host, "STORE_HOST"),
            port=args.store_port,
            database=require_value("store_db", args.store_db, "STORE_DB"),
            user=require_value("store_user", args.store_user, "STORE_USER"),
            password=require_value("store_password", args.store_password, "STORE_PASSWORD"),
            schema=args.store_schema or default_schema,
        )

    return DbConfig(
        host=require_value("dbsync_host", args.dbsync_host, "DBSYNC_HOST"),
        port=args.dbsync_port,
        database=require_value("dbsync_db", args.dbsync_db, "DBSYNC_DB"),
        user=require_value("dbsync_user", args.dbsync_user, "DBSYNC_USER"),
        password=require_value("dbsync_password", args.dbsync_password, "DBSYNC_PASSWORD"),
        schema=args.dbsync_schema or default_schema,
    )


def resolve_epochs(args: argparse.Namespace, store: DbConfig, dbsync: Optional[DbConfig]) -> List[int]:
    if args.epochs:
        return parse_epoch_spec(args.epochs)

    if args.epoch_start is not None and args.epoch_end is not None:
        if args.epoch_end < args.epoch_start:
            raise ValueError("--epoch-end must be greater than or equal to --epoch-start")
        return list(range(args.epoch_start, args.epoch_end + 1))

    if args.command == "prepare-range":
        raise ValueError("prepare-range requires --epochs or --epoch-start/--epoch-end")

    if dbsync is None:
        raise ValueError("dbsync configuration is required to auto-detect compare-range epochs")

    detected_start, detected_end = detect_epoch_range(store, dbsync)
    return list(range(detected_start, detected_end + 1))


def make_report_dir(custom_report_dir: Optional[str], command: str) -> Path:
    if custom_report_dir:
        path = Path(custom_report_dir).expanduser().resolve()
    else:
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        path = (SCRIPT_DIR.parent / "reports" / "drep-dist-batch" / ("%s_%s" % (command, timestamp))).resolve()
    path.mkdir(parents=True, exist_ok=True)
    return path


def build_query_settings(args: argparse.Namespace) -> QuerySettings:
    return QuerySettings(
        lock_timeout=args.lock_timeout,
        statement_timeout=args.statement_timeout,
        work_mem=args.work_mem,
        maintenance_work_mem=args.maintenance_work_mem,
        temp_buffers=args.temp_buffers,
        parallel_workers_per_gather=args.parallel_workers_per_gather,
        disable_jit=not args.keep_jit,
        effective_cache_size=args.effective_cache_size,
        random_page_cost=args.random_page_cost,
        effective_io_concurrency=args.effective_io_concurrency,
        parallel_setup_cost=args.parallel_setup_cost,
        parallel_tuple_cost=args.parallel_tuple_cost,
    )


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    store = resolve_db_config(args, "store", "yaci_store")
    dbsync = resolve_db_config(args, "dbsync", "public") if args.command == "compare-range" else None
    epochs = resolve_epochs(args, store, dbsync)
    report_dir = make_report_dir(args.report_dir, args.command)
    exclusions = load_hardcoded_exclusions(args.protocol_magic)
    settings = build_query_settings(args)
    run_id = "drep-batch-%s-%d" % (time.strftime("%Y%m%d_%H%M%S"), os.getpid())

    ensure_debug_schema(store, args.debug_schema)

    run_config = {
        "command": args.command,
        "run_id": run_id,
        "env_file": args.env_file,
        "store": {
            "host": store.host,
            "port": store.port,
            "database": store.database,
            "user": store.user,
            "schema": store.schema,
        },
        "dbsync": None if dbsync is None else {
            "host": dbsync.host,
            "port": dbsync.port,
            "database": dbsync.database,
            "user": dbsync.user,
            "schema": dbsync.schema,
        },
        "epochs": epochs,
        "protocol_magic": args.protocol_magic,
        "max_bootstrap_phase_epoch": args.max_bootstrap_phase_epoch,
        "workers": args.workers,
        "debug_schema": args.debug_schema,
        "query_settings": asdict(settings),
        "save_store_sql": args.save_store_sql,
        "report_dir": str(report_dir),
    }
    write_json(report_dir / "run_config.json", run_config)

    print_line(
        "report_dir=%s command=%s epochs=%d first_epoch=%d last_epoch=%d workers=%d env_file=%s"
        % (report_dir, args.command, len(epochs), epochs[0], epochs[-1], args.workers, args.env_file or "-")
    )

    if args.command == "prepare-range":
        results: List[EpochPrepareResult] = []
        with ThreadPoolExecutor(max_workers=args.workers) as executor:
            future_map = {
                executor.submit(
                    prepare_epoch_with_error_handling,
                    epoch,
                    store,
                    args.protocol_magic,
                    exclusions,
                    args.debug_schema,
                    settings,
                    report_dir,
                    args.max_bootstrap_phase_epoch,
                    args.save_store_sql,
                    run_id,
                ): epoch
                for epoch in epochs
            }
            for future in as_completed(future_map):
                results.append(future.result())

        write_prepare_summary(report_dir, results)
        error_epochs = sum(1 for result in results if result.status == "error")
        print_line(
            "done total_epochs=%d error_epochs=%d summary=%s" % (
                len(results),
                error_epochs,
                report_dir / "summary.json",
            )
        )
        return 0 if error_epochs == 0 else 1

    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        future_map = {
            executor.submit(
                compare_epoch_with_error_handling,
                epoch,
                store,
                dbsync,
                args.protocol_magic,
                exclusions,
                args.debug_schema,
                settings,
                report_dir,
                args.max_bootstrap_phase_epoch,
                args.save_store_sql,
                run_id,
            ): epoch
            for epoch in epochs
        }
        for future in as_completed(future_map):
            results.append(future.result())

    write_compare_summary(report_dir, results)

    mismatch_epochs = sum(1 for result in results if result.status == "mismatch")
    error_epochs = sum(1 for result in results if result.status == "error")
    total_mismatches = sum(result.mismatch_count for result in results)
    print_line(
        "done total_epochs=%d mismatch_epochs=%d error_epochs=%d total_mismatches=%d summary=%s" % (
            len(results),
            mismatch_epochs,
            error_epochs,
            total_mismatches,
            report_dir / "summary.json",
        )
    )
    return 0 if mismatch_epochs == 0 and error_epochs == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
