#!/usr/bin/env python3
"""Fast DRep active_until verifier for Yaci Store PostgreSQL snapshots."""

import argparse
import os
import re
import shlex
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from typing import Dict, List, Optional, Set, Tuple

COMPARE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
sys.path.insert(0, COMPARE_DIR)

from common import (  # noqa: E402
    Logger,
    MismatchCsvWriter,
    MismatchRecorder,
    add_common_args,
    connect,
    exit_code,
    finish_result,
    new_result,
    normalize_hash,
    redact_url,
    render_summary,
    resolve_config,
    run_report_dir,
    summary_payload,
    write_json,
    write_report_files,
)


CONWAY_ERA_VALUE = 7
TARGET_TABLE = "tmp_drep_active_until_verify_targets"
DEFAULT_RESULT_TABLE = "drep_active_until_verify_result"
MISMATCH_FIELDS = [
    "epoch",
    "issue",
    "drep_hash",
    "drep_type",
    "recomputed_active_until",
    "yaci_store_active_until",
    "dbsync_active_until",
    "detail",
]
RESULT_FIELDS = [
    "epoch",
    "drep_hash",
    "drep_type",
    "recomputed_active_until",
    "yaci_store_active_until",
    "dbsync_active_until",
    "status",
    "detail",
]


@dataclass(frozen=True)
class RegistrationInfo:
    slot: int
    epoch: int
    drep_activity: int
    protocol_major_version: int
    tx_index: int
    cert_index: int


@dataclass(frozen=True)
class InteractionInfo:
    epoch: int
    drep_activity: int
    slot: int
    tx_index: int
    event_index: int


@dataclass(frozen=True)
class ProposalInfo:
    slot: int
    epoch: int
    gov_action_lifetime: int
    tx_index: int
    index: int


@dataclass(frozen=True)
class ExpiryEvent:
    event_type: str
    epoch: int
    slot: int
    tx_index: int
    event_index: int
    drep_activity: int
    protocol_major_version: int = 0

    def priority(self) -> int:
        if self.event_type == "PROPOSAL":
            return 0
        if self.event_type == "REGISTRATION":
            return 1
        return 2


def int_or_none(value) -> Optional[int]:
    if value is None:
        return None
    return int(value)


def validate_table_name(table_name: str) -> str:
    if not re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", table_name or ""):
        raise ValueError("Result table name must be an unqualified SQL identifier")
    return table_name


def is_after_or_same_position(
    epoch: int,
    slot: int,
    tx_index: int,
    event_index: int,
    other_epoch: int,
    other_slot: int,
    other_tx_index: int,
    other_event_index: int,
) -> bool:
    if epoch != other_epoch:
        return epoch > other_epoch
    if slot != other_slot:
        return slot > other_slot
    if tx_index != other_tx_index:
        return tx_index > other_tx_index
    return event_index >= other_event_index


def calculate_active_until(
    registration: RegistrationInfo,
    interactions: List[InteractionInfo],
    proposals: List[ProposalInfo],
    non_dormant_proposal_epochs: Set[int],
    era_first_epoch: int,
    evaluated_epoch: int,
) -> int:
    active_until = 0
    dormant_counter = 0
    registered = False

    events: List[ExpiryEvent] = [
        ExpiryEvent(
            "REGISTRATION",
            registration.epoch,
            registration.slot,
            registration.tx_index,
            registration.cert_index,
            registration.drep_activity,
            registration.protocol_major_version,
        )
    ]

    for interaction in interactions:
        if interaction.epoch > evaluated_epoch:
            continue
        if not is_after_or_same_position(
            interaction.epoch,
            interaction.slot,
            interaction.tx_index,
            interaction.event_index,
            registration.epoch,
            registration.slot,
            registration.tx_index,
            registration.cert_index,
        ):
            continue
        events.append(
            ExpiryEvent(
                "INTERACTION",
                interaction.epoch,
                interaction.slot,
                interaction.tx_index,
                interaction.event_index,
                interaction.drep_activity,
            )
        )

    for proposal in proposals:
        if era_first_epoch <= proposal.epoch <= evaluated_epoch:
            events.append(
                ExpiryEvent(
                    "PROPOSAL",
                    proposal.epoch,
                    proposal.slot,
                    proposal.tx_index,
                    proposal.index,
                    0,
                )
            )

    events.sort(key=lambda e: (e.epoch, e.slot, e.tx_index, e.priority(), e.event_index))

    event_index = 0
    for epoch in range(era_first_epoch, evaluated_epoch + 1):
        if epoch not in non_dormant_proposal_epochs:
            dormant_counter += 1

        while event_index < len(events) and events[event_index].epoch == epoch:
            event = events[event_index]
            if event.event_type == "PROPOSAL":
                if registered and active_until + dormant_counter >= epoch:
                    active_until += dormant_counter
                dormant_counter = 0
            elif event.event_type == "REGISTRATION":
                registered = True
                if event.protocol_major_version == 9:
                    active_until = epoch + event.drep_activity
                else:
                    active_until = epoch + event.drep_activity - dormant_counter
            else:
                if registered:
                    active_until = epoch + event.drep_activity - dormant_counter
            event_index += 1

    return active_until


def detect_era_first_epoch(conn, override: Optional[int], logger: Logger) -> int:
    if override is not None:
        return override

    queries = [
        """
        SELECT b.epoch
        FROM era e
        JOIN block b ON b.slot >= e.start_slot
        WHERE e.era >= %s
        ORDER BY e.era ASC, b.slot ASC
        LIMIT 1
        """,
        """
        SELECT min(epoch)
        FROM epoch_param
        WHERE (params->>'protocol_major_ver')::int >= 9
        """,
    ]

    with conn.cursor() as cur:
        for idx, query in enumerate(queries):
            try:
                if idx == 0:
                    cur.execute(query, (CONWAY_ERA_VALUE,))
                else:
                    cur.execute(query)
                row = cur.fetchone()
                if row and row[0] is not None:
                    return int(row[0])
            except Exception as exc:
                conn.rollback()
                logger.log(f"  Warning: failed to auto-detect Conway first epoch: {type(exc).__name__}: {exc}")

    logger.log("  Warning: could not auto-detect Conway first epoch; using 0")
    return 0


def create_target_table(conn, epoch: int, drep_hashes: List[str]) -> Dict[Tuple[str, str], Optional[int]]:
    hash_filter = ""
    params: List[object] = [epoch]
    if drep_hashes:
        hash_filter = "AND lower(drep_hash) = ANY(%s)"
        params.append([normalize_hash(h) for h in drep_hashes])

    with conn.cursor() as cur:
        cur.execute(f"DROP TABLE IF EXISTS {TARGET_TABLE}")
        cur.execute(
            f"""
            CREATE TEMP TABLE {TARGET_TABLE} ON COMMIT DROP AS
            SELECT DISTINCT
                   lower(drep_hash) AS drep_hash,
                   drep_type,
                   active_until AS yaci_active_until
            FROM drep_dist
            WHERE epoch = %s
              AND drep_type NOT IN ('ABSTAIN', 'NO_CONFIDENCE')
              {hash_filter}
            """,
            params,
        )
        cur.execute(f"CREATE INDEX ON {TARGET_TABLE} (drep_hash, drep_type)")
        cur.execute(f"SELECT drep_hash, drep_type, yaci_active_until FROM {TARGET_TABLE}")
        rows = cur.fetchall()

    return {(normalize_hash(row[0]), row[1]): int_or_none(row[2]) for row in rows}


def fetch_registrations(conn, evaluated_epoch: int) -> Dict[Tuple[str, str], RegistrationInfo]:
    with conn.cursor() as cur:
        cur.execute(
            f"""
            WITH ranked AS (
                SELECT lower(dr.drep_hash) AS drep_hash,
                       dr.cred_type AS drep_type,
                       dr.slot,
                       dr.epoch,
                       dr.tx_index,
                       dr.cert_index,
                       row_number() OVER (
                           PARTITION BY lower(dr.drep_hash), dr.cred_type
                           ORDER BY dr.slot DESC, dr.tx_index DESC, dr.cert_index DESC
                       ) AS rn
                FROM drep_registration dr
                JOIN {TARGET_TABLE} t
                  ON t.drep_hash = lower(dr.drep_hash)
                 AND t.drep_type = dr.cred_type
                WHERE dr.type = 'REG_DREP_CERT'
                  AND dr.epoch <= %s
            )
            SELECT r.drep_hash,
                   r.drep_type,
                   r.slot,
                   r.epoch,
                   r.tx_index,
                   r.cert_index,
                   (ep.params->>'drep_activity')::int AS drep_activity,
                   (ep.params->>'protocol_major_ver')::int AS protocol_major_ver
            FROM ranked r
            JOIN epoch_param ep ON ep.epoch = r.epoch
            WHERE r.rn = 1
            """,
            (evaluated_epoch,),
        )
        rows = cur.fetchall()

    return {
        (normalize_hash(row[0]), row[1]): RegistrationInfo(
            slot=int(row[2]),
            epoch=int(row[3]),
            tx_index=int(row[4]),
            cert_index=int(row[5]),
            drep_activity=int(row[6]),
            protocol_major_version=int(row[7]),
        )
        for row in rows
    }


def fetch_interactions(conn, evaluated_epoch: int) -> Dict[Tuple[str, str], List[InteractionInfo]]:
    with conn.cursor() as cur:
        cur.execute(
            f"""
            WITH interactions AS (
                SELECT lower(dr.drep_hash) AS drep_hash,
                       dr.cred_type AS drep_type,
                       dr.epoch,
                       dr.slot,
                       dr.tx_index,
                       dr.cert_index AS event_index
                FROM drep_registration dr
                JOIN {TARGET_TABLE} t
                  ON t.drep_hash = lower(dr.drep_hash)
                 AND t.drep_type = dr.cred_type
                WHERE dr.type = 'UPDATE_DREP_CERT'
                  AND dr.epoch <= %s

                UNION ALL

                SELECT lower(vp.voter_hash) AS drep_hash,
                       CASE vp.voter_type
                           WHEN 'DREP_KEY_HASH' THEN 'ADDR_KEYHASH'
                           WHEN 'DREP_SCRIPT_HASH' THEN 'SCRIPTHASH'
                       END AS drep_type,
                       vp.epoch,
                       vp.slot,
                       vp.tx_index,
                       vp.idx AS event_index
                FROM voting_procedure vp
                JOIN {TARGET_TABLE} t
                  ON t.drep_hash = lower(vp.voter_hash)
                 AND t.drep_type = CASE vp.voter_type
                       WHEN 'DREP_KEY_HASH' THEN 'ADDR_KEYHASH'
                       WHEN 'DREP_SCRIPT_HASH' THEN 'SCRIPTHASH'
                   END
                WHERE vp.epoch <= %s
                  AND vp.voter_type IN ('DREP_KEY_HASH', 'DREP_SCRIPT_HASH')
            )
            SELECT i.drep_hash,
                   i.drep_type,
                   i.epoch,
                   i.slot,
                   i.tx_index,
                   i.event_index,
                   (ep.params->>'drep_activity')::int AS drep_activity
            FROM interactions i
            JOIN epoch_param ep ON ep.epoch = i.epoch
            ORDER BY i.drep_hash, i.drep_type, i.epoch, i.slot, i.tx_index, i.event_index
            """,
            (evaluated_epoch, evaluated_epoch),
        )
        rows = cur.fetchall()

    interactions: Dict[Tuple[str, str], List[InteractionInfo]] = {}
    for row in rows:
        key = (normalize_hash(row[0]), row[1])
        interactions.setdefault(key, []).append(
            InteractionInfo(
                epoch=int(row[2]),
                slot=int(row[3]),
                tx_index=int(row[4]),
                event_index=int(row[5]),
                drep_activity=int(row[6]),
            )
        )
    return interactions


def fetch_proposals(conn, evaluated_epoch: int) -> List[ProposalInfo]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT gap.slot,
                   gap.epoch,
                   gap.tx_index,
                   gap.idx,
                   (ep.params->>'gov_action_lifetime')::int AS gov_action_lifetime
            FROM gov_action_proposal gap
            JOIN epoch_param ep ON ep.epoch = gap.epoch
            WHERE gap.epoch <= %s
            """,
            (evaluated_epoch,),
        )
        rows = cur.fetchall()

    return [
        ProposalInfo(
            slot=int(row[0]),
            epoch=int(row[1]),
            tx_index=int(row[2]),
            index=int(row[3]),
            gov_action_lifetime=int(row[4]),
        )
        for row in rows
    ]


def fetch_non_dormant_proposal_epochs(conn, era_first_epoch: int, evaluated_epoch: int) -> Set[int]:
    if era_first_epoch > evaluated_epoch:
        return set()

    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT DISTINCT gps.epoch
            FROM gov_action_proposal_status gps
            JOIN gov_action_proposal gap
              ON gps.gov_action_tx_hash = gap.tx_hash
             AND gps.gov_action_index = gap.idx
            JOIN epoch_param ep ON ep.epoch = gap.epoch
            WHERE gps.status IN ('ACTIVE', 'RATIFIED')
              AND gps.epoch >= %s
              AND gps.epoch <= %s
              AND gps.epoch <= gap.epoch + (ep.params->>'gov_action_lifetime')::int
            """,
            (era_first_epoch, evaluated_epoch),
        )
        return {int(row[0]) for row in cur.fetchall()}


def fetch_dbsync_active_until(dbsync_url: str, epoch: int) -> Dict[str, int]:
    conn = connect(dbsync_url)
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT dh.raw, d.active_until
                FROM drep_distr d
                JOIN drep_hash dh ON dh.id = d.hash_id
                WHERE d.epoch_no = %s
                  AND d.active_until IS NOT NULL
                """,
                (epoch,),
            )
            active_until_by_hash = {}
            for raw_hash, active_until in cur.fetchall():
                drep_hash = normalize_hash(raw_hash)
                if drep_hash:
                    active_until_by_hash[drep_hash] = int(active_until)
            return active_until_by_hash
    finally:
        conn.close()


def write_result_rows(result_dir: str, epoch: int, rows: List[Dict[str, object]]) -> str:
    import csv

    os.makedirs(result_dir, exist_ok=True)
    path = os.path.join(result_dir, f"drep_active_until_fast_epoch_{epoch}.csv")
    with open(path, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=RESULT_FIELDS, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key) for key in RESULT_FIELDS})
    return path


def reset_persistent_result_table(store_url: str, store_schema: str, table_name: str) -> None:
    from psycopg2 import sql

    table_name = validate_table_name(table_name)
    conn = connect(store_url, store_schema)
    try:
        with conn:
            with conn.cursor() as cur:
                table = sql.Identifier(table_name)
                cur.execute(sql.SQL("DROP TABLE IF EXISTS {}").format(table))
                cur.execute(
                    sql.SQL(
                        """
                        CREATE TABLE {} (
                            run_id varchar(32) not null,
                            epoch int not null,
                            drep_hash varchar(56),
                            drep_type varchar(40),
                            recomputed_active_until int,
                            dbsync_active_until int,
                            status varchar(160) not null,
                            detail text,
                            created_at timestamp not null default now()
                        )
                        """
                    ).format(table)
                )
                cur.execute(sql.SQL("CREATE INDEX {} ON {} (epoch, status)").format(
                    sql.Identifier(f"idx_{table_name}_epoch_status"),
                    table,
                ))
                cur.execute(sql.SQL("CREATE INDEX {} ON {} (drep_hash)").format(
                    sql.Identifier(f"idx_{table_name}_drep_hash"),
                    table,
                ))
    finally:
        conn.close()


def insert_persistent_result_rows(
    store_url: str,
    store_schema: str,
    table_name: str,
    run_id: str,
    rows: List[Dict[str, object]],
) -> None:
    if not rows:
        return

    from psycopg2 import sql

    table_name = validate_table_name(table_name)
    conn = connect(store_url, store_schema)
    try:
        with conn:
            with conn.cursor() as cur:
                insert_sql = sql.SQL(
                    """
                    INSERT INTO {} (
                        run_id,
                        epoch,
                        drep_hash,
                        drep_type,
                        recomputed_active_until,
                        dbsync_active_until,
                        status,
                        detail
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    """
                ).format(sql.Identifier(table_name))
                cur.executemany(
                    insert_sql,
                    [
                        (
                            run_id,
                            row.get("epoch"),
                            row.get("drep_hash"),
                            row.get("drep_type"),
                            row.get("recomputed_active_until"),
                            row.get("dbsync_active_until"),
                            row.get("status"),
                            row.get("detail"),
                        )
                        for row in rows
                    ],
                )
    finally:
        conn.close()


def compare_epoch(
    epoch: int,
    args,
    logger: Logger,
    mismatch_dir: str,
    result_dir: str,
    era_first_epoch_override: Optional[int],
    run_id: str,
) -> Tuple[int, Optional[str], Optional[str], Dict[str, int]]:
    evaluated_epoch = epoch - 1
    stats = {
        "targets": 0,
        "registrations": 0,
        "interactions": 0,
        "proposals": 0,
        "non_dormant_epochs": 0,
        "dbsync_rows": 0,
        "era_first_epoch": None,
    }

    conn = connect(args.store_url, args.store_schema)
    try:
        with conn:
            era_first_epoch = detect_era_first_epoch(conn, era_first_epoch_override, logger)
            stats["era_first_epoch"] = era_first_epoch
            targets = create_target_table(conn, epoch, args.drep_hash or [])
            stats["targets"] = len(targets)
            if not targets:
                logger.log("  No target DRep rows found in drep_dist")
                return 0, None, None, stats

            registrations = fetch_registrations(conn, evaluated_epoch)
            interactions = fetch_interactions(conn, evaluated_epoch)
            proposals = fetch_proposals(conn, evaluated_epoch)
            non_dormant_epochs = fetch_non_dormant_proposal_epochs(conn, era_first_epoch, evaluated_epoch)

            stats["registrations"] = len(registrations)
            stats["interactions"] = sum(len(v) for v in interactions.values())
            stats["proposals"] = len(proposals)
            stats["non_dormant_epochs"] = len(non_dormant_epochs)
    finally:
        conn.close()

    dbsync_map = {}
    if not args.skip_dbsync:
        dbsync_map = fetch_dbsync_active_until(args.dbsync_url, epoch)
        stats["dbsync_rows"] = len(dbsync_map)

    writer = MismatchCsvWriter(mismatch_dir, f"drep_active_until_fast_epoch_{epoch}", MISMATCH_FIELDS, args.max_mismatches)
    recorder = MismatchRecorder(logger, writer, args.max_mismatches)

    expected_by_hash: Dict[str, int] = {}
    target_hashes = {key[0] for key in targets}
    result_rows: List[Dict[str, object]] = []
    for key, yaci_active_until in targets.items():
        drep_hash, drep_type = key
        registration = registrations.get(key)
        if registration is None:
            result_rows.append(
                {
                    "epoch": epoch,
                    "drep_hash": drep_hash,
                    "drep_type": drep_type,
                    "recomputed_active_until": None,
                    "yaci_store_active_until": yaci_active_until,
                    "dbsync_active_until": dbsync_map.get(drep_hash),
                    "status": "MISSING_REGISTRATION",
                    "detail": "No REG_DREP_CERT found up to the evaluated epoch",
                }
            )
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "MISSING_REGISTRATION",
                    "drep_hash": drep_hash,
                    "drep_type": drep_type,
                    "recomputed_active_until": None,
                    "yaci_store_active_until": yaci_active_until,
                    "dbsync_active_until": dbsync_map.get(drep_hash),
                    "detail": "No REG_DREP_CERT found up to the evaluated epoch",
                },
                [f"  Missing registration for {drep_hash} ({drep_type})"],
            )
            continue

        expected = calculate_active_until(
            registration,
            interactions.get(key, []),
            proposals,
            non_dormant_epochs,
            era_first_epoch,
            evaluated_epoch,
        )
        expected_by_hash[drep_hash] = expected

        status = "OK"
        detail = ""
        dbsync_active_until = dbsync_map.get(drep_hash)
        if dbsync_map:
            if dbsync_active_until is None:
                status = "MISSING_DBSYNC"
                detail = "DB Sync has no active_until row for this DRep hash"
            elif dbsync_active_until != expected:
                status = "DBSYNC_ACTIVE_UNTIL_MISMATCH"
                detail = f"recomputed={expected}, dbsync={dbsync_active_until}"

        result_rows.append(
            {
                "epoch": epoch,
                "drep_hash": drep_hash,
                "drep_type": drep_type,
                "recomputed_active_until": expected,
                "yaci_store_active_until": yaci_active_until,
                "dbsync_active_until": dbsync_active_until,
                "status": status,
                "detail": detail,
            }
        )

        if yaci_active_until != expected:
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "YACI_ACTIVE_UNTIL_MISMATCH",
                    "drep_hash": drep_hash,
                    "drep_type": drep_type,
                    "recomputed_active_until": expected,
                    "yaci_store_active_until": yaci_active_until,
                    "dbsync_active_until": dbsync_map.get(drep_hash),
                    "detail": f"recomputed={expected}, yaci_store={yaci_active_until}",
                },
                [
                    f"  Yaci mismatch: {drep_hash} ({drep_type})",
                    f"    Recomputed : active_until = {expected}",
                    f"    Yaci Store : active_until = {yaci_active_until}",
                ],
            )

        if dbsync_map and dbsync_active_until != expected:
            recorder.record(
                {
                    "epoch": epoch,
                    "issue": "DBSYNC_ACTIVE_UNTIL_MISMATCH",
                    "drep_hash": drep_hash,
                    "drep_type": drep_type,
                    "recomputed_active_until": expected,
                    "yaci_store_active_until": yaci_active_until,
                    "dbsync_active_until": dbsync_active_until,
                    "detail": f"recomputed={expected}, dbsync={dbsync_active_until}",
                },
                [
                    f"  DB Sync mismatch: {drep_hash} ({drep_type})",
                    f"    Recomputed : active_until = {expected}",
                    f"    DB Sync    : active_until = {dbsync_active_until}",
                ],
            )

    if dbsync_map:
        for drep_hash, dbsync_active_until in dbsync_map.items():
            if drep_hash not in target_hashes:
                result_rows.append(
                    {
                        "epoch": epoch,
                        "drep_hash": drep_hash,
                        "drep_type": None,
                        "recomputed_active_until": None,
                        "yaci_store_active_until": None,
                        "dbsync_active_until": dbsync_active_until,
                        "status": "ONLY_IN_DBSYNC",
                        "detail": "DB Sync has active_until for a hash absent from the Yaci target drep_dist",
                    }
                )
                recorder.record(
                    {
                        "epoch": epoch,
                        "issue": "ONLY_IN_DBSYNC",
                        "drep_hash": drep_hash,
                        "drep_type": None,
                        "recomputed_active_until": None,
                        "yaci_store_active_until": None,
                        "dbsync_active_until": dbsync_active_until,
                        "detail": "DB Sync has active_until for a hash absent from Yaci target drep_dist",
                    },
                    [f"  Hash {drep_hash} present in DB Sync but not in Yaci target drep_dist"],
                )

    result_file = write_result_rows(result_dir, epoch, result_rows)
    insert_persistent_result_rows(
        args.store_url,
        args.store_schema,
        args.result_table,
        run_id,
        result_rows,
    )
    return (*recorder.finish(), result_file, stats)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Fast recompute verifier for Yaci Store drep_dist.active_until.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --epoch 624
  %(prog)s --epoch 624 --skip-dbsync
  %(prog)s --start-epoch 620 --end-epoch 624 --max-mismatches 50
  %(prog)s --epoch 624 --drep-hash <hex-hash> --skip-dbsync
        """,
    )

    epoch_group = parser.add_mutually_exclusive_group(required=True)
    epoch_group.add_argument("--epoch", type=int, help="Single snapshot epoch to verify")
    epoch_group.add_argument("--start-epoch", type=int, help="Start snapshot epoch, inclusive")
    parser.add_argument("--end-epoch", type=int, help="End snapshot epoch, inclusive")
    parser.add_argument("--era-first-epoch", type=int, help="Override detected Conway first epoch")
    parser.add_argument("--drep-hash", action="append", help="Limit verification to one DRep hash; repeatable")
    parser.add_argument(
        "--result-table",
        default=DEFAULT_RESULT_TABLE,
        help=f"Persistent result table to drop and recreate in the store schema (default: {DEFAULT_RESULT_TABLE})",
    )
    parser.add_argument("--skip-dbsync", action="store_true", default=None, help="Only compare recomputed values with Yaci Store")
    parser.add_argument("--max-mismatches", type=int, default=None, help="Limit mismatch samples per epoch (0 = unlimited)")

    add_common_args(parser)
    args = resolve_config(parser.parse_args())
    args.skip_dbsync = bool(args.skip_dbsync)
    args.result_table = validate_table_name(args.result_table)

    if args.epoch is not None:
        start_epoch = args.epoch
        end_epoch = args.epoch
    else:
        start_epoch = args.start_epoch
        end_epoch = args.end_epoch
        if end_epoch is None:
            parser.error("--end-epoch is required when using --start-epoch")
        if end_epoch < start_epoch:
            parser.error("--end-epoch must be >= --start-epoch")

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    command = shlex.join([sys.executable] + sys.argv)
    report_dir = run_report_dir(args, "verify_drep_active_until_fast", run_id)
    mismatch_dir = os.path.join(report_dir, "mismatches")
    result_dir = os.path.join(report_dir, "results")
    log_file = os.path.join(args.logs_dir, f"drep_active_until_fast_{run_id}.log")
    os.makedirs(mismatch_dir, exist_ok=True)
    os.makedirs(result_dir, exist_ok=True)
    logger = Logger(log_file, quiet=args.quiet)

    logger.log("===== Starting fast drep active_until verification =====")
    logger.log(f"Log file: {os.path.abspath(log_file)}")
    logger.log(f"Report directory: {os.path.abspath(report_dir)}")
    logger.log(f"Epoch range: {start_epoch} -> {end_epoch}")
    logger.log(f"Yaci Store URL: {redact_url(args.store_url)} (schema: {args.store_schema})")
    if args.skip_dbsync:
        logger.log("DB Sync comparison: disabled")
    else:
        logger.log(f"DB Sync URL: {redact_url(args.dbsync_url)}")
    if args.era_first_epoch is not None:
        logger.log(f"Conway first epoch override: {args.era_first_epoch}")
    if args.drep_hash:
        logger.log(f"DRep hash filter: {', '.join(args.drep_hash)}")
    logger.log(f"Persistent result table: {args.store_schema}.{args.result_table} (drop/create on each run)")
    logger.log()

    reset_persistent_result_table(args.store_url, args.store_schema, args.result_table)

    result = new_result("drep_active_until_fast")
    result["epochs_compared"] = end_epoch - start_epoch + 1
    result["log_file"] = os.path.abspath(log_file)
    result_started = time.time()
    epoch_stats = {}

    for epoch in range(start_epoch, end_epoch + 1):
        logger.log(f"############ Epoch {epoch} - recompute active_until ############")
        try:
            count, mismatch_file, result_file, stats = compare_epoch(
                epoch,
                args,
                logger,
                mismatch_dir,
                result_dir,
                args.era_first_epoch,
                run_id,
            )
            epoch_stats[str(epoch)] = stats
            logger.log(
                "  Loaded: "
                f"targets={stats['targets']}, registrations={stats['registrations']}, "
                f"interactions={stats['interactions']}, proposals={stats['proposals']}, "
                f"non_dormant_epochs={stats['non_dormant_epochs']}, dbsync_rows={stats['dbsync_rows']}"
            )
            if result_file:
                logger.log(f"  Result CSV: {result_file}")
            if count == 0:
                logger.log("  OK - recomputed active_until matches the checked data")
            else:
                result["epochs_with_mismatch"] += 1
                result["total_mismatches"] += count
                if mismatch_file:
                    result["mismatch_files"].append(mismatch_file)
                logger.log(f"  MISMATCH: {count} mismatch(es)")
                if mismatch_file:
                    logger.log(f"  Sample: {mismatch_file}")
        except Exception as exc:
            result["errors"] += 1
            logger.error(f"Verification error for epoch {epoch}", exc)
        logger.log()

    result = finish_result(result, result_started)
    finished_at = datetime.now()
    epoch_scope = f"epochs {start_epoch} -> {end_epoch}" if start_epoch != end_epoch else f"epoch {start_epoch}"
    summary_text = render_summary(
        [result],
        "verify_drep_active_until_fast",
        started_at,
        finished_at,
        command,
        epoch_scope,
        report_dir,
        os.path.abspath(log_file),
    )
    payload = summary_payload(
        [result],
        started_at,
        finished_at,
        command,
        {"start_epoch": start_epoch, "end_epoch": end_epoch},
        report_dir,
        os.path.abspath(log_file),
        {
            "store_url": redact_url(args.store_url),
            "store_schema": args.store_schema,
            "dbsync_url": None if args.skip_dbsync else redact_url(args.dbsync_url),
            "skip_dbsync": args.skip_dbsync,
            "era_first_epoch": args.era_first_epoch,
            "drep_hash": args.drep_hash or [],
            "result_table": f"{args.store_schema}.{args.result_table}",
            "max_mismatches": args.max_mismatches,
        },
    )
    payload["result"] = result
    payload["epoch_stats"] = epoch_stats

    logger.log(summary_text)
    summary_log, summary_json = write_report_files(report_dir, summary_text, payload)
    logger.log(f"Summary log written to: {summary_log}")
    logger.log(f"Summary JSON written to: {summary_json}")
    if args.result_json:
        write_json(args.result_json, payload)
        logger.log(f"Result JSON written to: {args.result_json}")

    return exit_code([result])


if __name__ == "__main__":
    raise SystemExit(main())
