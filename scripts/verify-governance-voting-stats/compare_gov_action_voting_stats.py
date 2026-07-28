#!/usr/bin/env python3
"""Verify Yaci governance voting statistics against terminal AdaStat data."""

from __future__ import annotations

import argparse
import json
import os
import shlex
import sys
import time
from collections import Counter
from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from typing import Any, Dict, Mapping, Optional, Sequence, Tuple

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from adastat_voting_stats import (  # noqa: E402
    ALL_FIELDS,
    BODY_FIELDS,
    EXPECTED_BODIES,
    NETWORK_BASE_URLS,
    AdaStatClient,
    FetchedResponse,
    IdentityMismatchError,
    NormalizedReference,
    ReferenceContractError,
    ReferenceHttpError,
    ReferenceUnavailableError,
    ResponseSchemaError,
    UnsupportedActionIndex,
    YaciProposal,
    build_expected_stats,
    check_eligibility,
    encode_adastat_action_id,
    load_yaci_proposals,
    normalize_actual_value,
    parse_adastat_response,
    parse_legacy_proposal_id,
    resolve_verifier_settings,
    save_normalized_reference,
)
from verifier_common import (  # noqa: E402
    Logger,
    MismatchCsvWriter,
    MismatchRecorder,
    connect,
    finish_result,
    new_result,
    redact_url,
    render_summary,
    run_report_dir,
    summary_payload,
    write_json,
    write_report_files,
)


MISMATCH_FIELDS = (
    "network",
    "epoch",
    "action_id",
    "action_type",
    "yaci_status",
    "body",
    "field",
    "issue",
    "expected",
    "actual",
    "signed_delta",
    "adastat_outcome_epoch",
    "source_tip_epoch",
    "source_components",
)

COVERAGE_FIELDS = (
    "network",
    "epoch",
    "action_id",
    "action_type",
    "yaci_status",
    "proposal_result",
    "body",
    "compared_field_count",
    "unavailable_field_count",
    "expected_field_count",
    "reason",
    "expected_coverage",
)


@dataclass(frozen=True)
class FieldMismatch:
    body: str
    field: str
    issue: str
    expected: Any
    actual: Any
    signed_delta: Optional[Any]


@dataclass(frozen=True)
class ProposalComparison:
    proposal: YaciProposal
    result: str
    reason: Optional[str]
    compared_fields: int
    mismatches: Tuple[FieldMismatch, ...]
    normalized: Optional[NormalizedReference] = None
    fetched: Optional[FetchedResponse] = None


def field_body(field: str) -> str:
    for body, fields in BODY_FIELDS.items():
        if field in fields:
            return body
    raise ValueError(f"unknown voting stats field: {field}")


def compare_one_proposal(
    proposal: YaciProposal,
    client: AdaStatClient,
    report_dir: Optional[str] = None,
) -> ProposalComparison:
    """Verify one row without hiding inconclusive or partial coverage."""

    if proposal.status.upper() == "ACTIVE":
        return ProposalComparison(
            proposal=proposal,
            result="INCONCLUSIVE",
            reason="INCONCLUSIVE_LIVE",
            compared_fields=0,
            mismatches=(),
        )

    try:
        fetched = client.fetch(proposal.tx_hash, proposal.index)
    except UnsupportedActionIndex:
        return ProposalComparison(
            proposal, "INCONCLUSIVE", "UNSUPPORTED_ACTION_INDEX", 0, ()
        )
    except ReferenceUnavailableError:
        return ProposalComparison(proposal, "INCONCLUSIVE", "ADASTAT_NOT_FOUND", 0, ())
    except ReferenceHttpError:
        return ProposalComparison(proposal, "ERROR", "HTTP_ERROR", 0, ())

    try:
        reference = parse_adastat_response(fetched.raw_text)
        eligibility = check_eligibility(proposal, reference)
        if not eligibility.eligible:
            return ProposalComparison(
                proposal,
                "INCONCLUSIVE",
                eligibility.reason,
                0,
                (),
                fetched=fetched,
            )
        normalized = build_expected_stats(reference)
    except IdentityMismatchError:
        return ProposalComparison(
            proposal,
            "ERROR",
            "ADASTAT_IDENTITY_MISMATCH",
            0,
            (),
            fetched=fetched,
        )
    except (ResponseSchemaError, ReferenceContractError):
        return ProposalComparison(
            proposal,
            "ERROR",
            "ADASTAT_SCHEMA_ERROR",
            0,
            (),
            fetched=fetched,
        )

    if report_dir is not None:
        save_normalized_reference(
            report_dir,
            client.network,
            proposal,
            fetched,
            normalized,
        )

    mismatches = []
    for field in ALL_FIELDS:
        if field not in normalized.values:
            continue
        expected = normalized.values[field]
        if field not in proposal.voting_stats:
            mismatches.append(
                FieldMismatch(
                    field_body(field),
                    field,
                    "MISSING_YACI_FIELD",
                    expected,
                    None,
                    None,
                )
            )
            continue
        raw_actual = proposal.voting_stats[field]
        if raw_actual is None:
            mismatches.append(
                FieldMismatch(
                    field_body(field),
                    field,
                    "NULL_YACI_FIELD",
                    expected,
                    None,
                    None,
                )
            )
            continue
        try:
            actual = normalize_actual_value(field, raw_actual)
        except ReferenceContractError:
            mismatches.append(
                FieldMismatch(
                    field_body(field),
                    field,
                    "TYPE_MISMATCH",
                    expected,
                    raw_actual,
                    None,
                )
            )
            continue
        if actual != expected:
            mismatches.append(
                FieldMismatch(
                    field_body(field),
                    field,
                    "VALUE_MISMATCH",
                    expected,
                    actual,
                    actual - expected,
                )
            )

    if mismatches:
        result = "MISMATCH"
    elif normalized.compared_fields == len(ALL_FIELDS):
        result = "MATCH"
    else:
        result = "PARTIAL_MATCH"
    return ProposalComparison(
        proposal=proposal,
        result=result,
        reason=None,
        compared_fields=normalized.compared_fields,
        mismatches=tuple(mismatches),
        normalized=normalized,
        fetched=fetched,
    )


def run_verification(
    connection: Any,
    client: AdaStatClient,
    epochs: Sequence[int],
    proposal_filter: Optional[Tuple[str, int]],
    report_dir: str,
    logger: Any,
    max_mismatches: int = 0,
) -> Dict[str, Any]:
    """Verify one globally latest status row per proposal in the epoch scope."""

    epoch_values = list(epochs)
    if not epoch_values:
        raise ValueError("at least one epoch must be selected")
    start_epoch = epoch_values[0]
    end_epoch = epoch_values[-1]
    result = new_result("gov_action_voting_stats_adastat")
    result_started = time.time()
    result["epochs_compared"] = len(epoch_values)
    result.update(
        {
            "selection_mode": "LATEST_PER_PROPOSAL",
            "selected_proposals": 0,
            "eligible_proposals": 0,
            "compared_proposals": 0,
            "matched_proposals": 0,
            "partial_proposals": 0,
            "mismatched_proposals": 0,
            "inconclusive_proposals": 0,
            "error_proposals": 0,
            "compared_fields": 0,
            "selected_fields": 0,
            "matched_fields": 0,
            "unavailable_fields": 0,
            "body_counts": {
                body: {"compared_fields": 0, "unavailable_fields": 0}
                for body in BODY_FIELDS
            },
            "reason_counts": {},
            "proposals": [],
            "coverage_file": None,
        }
    )
    mismatch_dir = os.path.join(report_dir, "mismatches")
    coverage_writer = MismatchCsvWriter(
        report_dir,
        "coverage",
        COVERAGE_FIELDS,
        0,
    )
    coverage_writer.ensure_open()
    reasons: Counter[str] = Counter()

    logger.log(
        f"############ Latest proposal statuses with latest epoch "
        f"{start_epoch} -> {end_epoch} ############"
    )
    try:
        proposals = load_yaci_proposals(
            connection,
            start_epoch,
            end_epoch,
            proposal_filter,
        )
    except Exception as exc:
        result["errors"] += 1
        reasons["STORE_ERROR"] += 1
        logger.error(
            f"Yaci Store latest-status query/data error for epochs "
            f"{start_epoch}..{end_epoch}",
            exc,
        )
        proposals = []

    result["selected_proposals"] = len(proposals)
    result["selected_fields"] = len(proposals) * len(ALL_FIELDS)
    proposals_by_epoch: Dict[int, list[YaciProposal]] = {}
    for proposal in proposals:
        proposals_by_epoch.setdefault(proposal.epoch, []).append(proposal)

    if not proposals and not result["errors"]:
        logger.log("  No latest Store rows selected.")
        logger.log()

    for epoch in sorted(proposals_by_epoch):
        epoch_proposals = proposals_by_epoch[epoch]
        logger.log(f"  Latest row epoch {epoch}: {len(epoch_proposals)} proposal(s)")
        writer = MismatchCsvWriter(
            mismatch_dir,
            f"gov_action_voting_stats_epoch_{epoch}",
            MISMATCH_FIELDS,
            max_mismatches,
        )
        recorder = MismatchRecorder(logger, writer, max_mismatches)
        epoch_mismatched = False

        for proposal in epoch_proposals:
            comparison = compare_one_proposal(proposal, client, report_dir)
            _accumulate_comparison(result, comparison, reasons)
            _write_coverage_rows(coverage_writer, client.network, comparison)
            result["proposals"].append(_proposal_payload(comparison))

            logger.log(
                f"  {proposal.tx_hash}#{proposal.index}: {comparison.result} "
                f"({comparison.compared_fields}/{len(ALL_FIELDS)} fields)"
                + (f" [{comparison.reason}]" if comparison.reason else "")
            )

            for mismatch in comparison.mismatches:
                epoch_mismatched = True
                recorder.record(
                    _mismatch_row(client.network, comparison, mismatch),
                    [
                        f"    {mismatch.issue}: {mismatch.field}",
                        f"      expected={_display(mismatch.expected)}, "
                        f"actual={_display(mismatch.actual)}, "
                        f"delta={_display(mismatch.signed_delta)}",
                    ],
                )

        mismatch_count, mismatch_file = recorder.finish()
        if mismatch_count:
            result["total_mismatches"] += mismatch_count
        if mismatch_file:
            result["mismatch_files"].append(mismatch_file)
        if epoch_mismatched:
            result["epochs_with_mismatch"] += 1
        logger.log()

    coverage_writer.close()
    result["coverage_file"] = coverage_writer.path
    result["reason_counts"] = dict(sorted(reasons.items()))
    result["http"] = {
        "requests": client.requests,
        "retries": client.retry_count,
        "cache_hits": client.cache_hits,
    }
    result["matched_fields"] = result["compared_fields"] - result["total_mismatches"]
    finish_result(result, result_started)
    return result


def _accumulate_comparison(
    result: Dict[str, Any],
    comparison: ProposalComparison,
    reasons: Counter[str],
) -> None:
    if comparison.result in ("MATCH", "PARTIAL_MATCH", "MISMATCH"):
        result["eligible_proposals"] += 1
    if comparison.compared_fields:
        result["compared_proposals"] += 1
        result["compared_fields"] += comparison.compared_fields
    if comparison.result == "MATCH":
        result["matched_proposals"] += 1
    elif comparison.result == "PARTIAL_MATCH":
        result["partial_proposals"] += 1
    elif comparison.result == "MISMATCH":
        result["mismatched_proposals"] += 1
    elif comparison.result == "INCONCLUSIVE":
        result["inconclusive_proposals"] += 1
    elif comparison.result == "ERROR":
        result["error_proposals"] += 1
        result["errors"] += 1

    if comparison.reason:
        reasons[comparison.reason] += 1
    for mismatch in comparison.mismatches:
        reasons[mismatch.issue] += 1
    if comparison.normalized is None:
        result["unavailable_fields"] += len(ALL_FIELDS)
        for body, fields in BODY_FIELDS.items():
            result["body_counts"][body]["unavailable_fields"] += len(fields)
        return

    for body, fields in BODY_FIELDS.items():
        compared = sum(field in comparison.normalized.values for field in fields)
        unavailable = len(fields) - compared
        result["body_counts"][body]["compared_fields"] += compared
        result["body_counts"][body]["unavailable_fields"] += unavailable
        result["unavailable_fields"] += unavailable
        if unavailable:
            body_reason = comparison.normalized.diagnostics.get(body, {}).get(
                "reason",
                "BODY_UNAVAILABLE",
            )
            reasons[body_reason] += 1


def _write_coverage_rows(
    writer: MismatchCsvWriter,
    network: str,
    comparison: ProposalComparison,
) -> None:
    proposal = comparison.proposal
    expected_bodies = set(EXPECTED_BODIES[proposal.action_type])
    for body, fields in BODY_FIELDS.items():
        expected_count = len(fields) if body in expected_bodies else 0
        if comparison.normalized is None:
            compared_count = 0
            unavailable_count = len(fields)
            reason = comparison.reason
        else:
            compared_count = sum(field in comparison.normalized.values for field in fields)
            unavailable_count = len(fields) - compared_count
            body_diagnostics = comparison.normalized.diagnostics.get(body, {})
            reason = body_diagnostics.get("reason")
            if reason is None and compared_count == len(fields):
                reason = "COMPARED"
        writer.write(
            {
                "network": network,
                "epoch": proposal.epoch,
                "action_id": f"{proposal.tx_hash}#{proposal.index}",
                "action_type": proposal.action_type,
                "yaci_status": proposal.status,
                "proposal_result": comparison.result,
                "body": body,
                "compared_field_count": compared_count,
                "unavailable_field_count": unavailable_count,
                "expected_field_count": expected_count,
                "reason": reason,
                "expected_coverage": body in expected_bodies,
            }
        )


def _mismatch_row(
    network: str,
    comparison: ProposalComparison,
    mismatch: FieldMismatch,
) -> Mapping[str, Any]:
    proposal = comparison.proposal
    normalized = comparison.normalized
    reference = normalized.reference if normalized else None
    outcome_epoch = None
    source_tip = None
    diagnostics: Mapping[str, Any] = {}
    if reference is not None:
        outcome_epoch = (
            reference.ratified_epoch
            if proposal.status == "RATIFIED"
            else reference.expired_epoch
        )
        source_tip = reference.tip_epoch
        diagnostics = normalized.diagnostics.get(mismatch.body, {})
    return {
        "network": network,
        "epoch": proposal.epoch,
        "action_id": f"{proposal.tx_hash}#{proposal.index}",
        "action_type": proposal.action_type,
        "yaci_status": proposal.status,
        "body": mismatch.body,
        "field": mismatch.field,
        "issue": mismatch.issue,
        "expected": _display(mismatch.expected),
        "actual": _display(mismatch.actual),
        "signed_delta": _display(mismatch.signed_delta),
        "adastat_outcome_epoch": outcome_epoch,
        "source_tip_epoch": source_tip,
        "source_components": json.dumps(
            diagnostics,
            separators=(",", ":"),
            sort_keys=True,
            default=str,
        ),
    }


def _proposal_payload(comparison: ProposalComparison) -> Mapping[str, Any]:
    proposal = comparison.proposal
    return {
        "epoch": proposal.epoch,
        "action_id": f"{proposal.tx_hash}#{proposal.index}",
        "adastat_action_id": (
            encode_adastat_action_id(proposal.tx_hash, proposal.index)
            if 0 <= proposal.index <= 255
            else None
        ),
        "action_type": proposal.action_type,
        "yaci_status": proposal.status,
        "result": comparison.result,
        "reason": comparison.reason,
        "compared_fields": comparison.compared_fields,
        "selected_fields": len(ALL_FIELDS),
        "mismatch_count": len(comparison.mismatches),
    }


def _display(value: Any) -> Any:
    if value is None:
        return None
    if isinstance(value, Decimal):
        return format(value, "f")
    return value


def determine_exit_code(
    result: Mapping[str, Any],
    fail_on_inconclusive: bool,
) -> int:
    if (
        result["errors"]
        or result["compared_fields"] == 0
        or (fail_on_inconclusive and result["inconclusive_proposals"])
    ):
        return 2
    if result["total_mismatches"]:
        return 1
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Verify Yaci gov_action_proposal_status.voting_stats against "
            "terminal AdaStat governance-action data."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s --network preview --epoch 1369 --config config.json
  %(prog)s --network mainnet --start-epoch 640 --end-epoch 650 --config config.json
  %(prog)s --network preview --epoch 1369 --proposal TX_HASH#0 --config config.json
        """,
    )
    selection = parser.add_mutually_exclusive_group(required=True)
    selection.add_argument(
        "--epoch",
        type=int,
        help="Select proposals whose latest Store row is at this epoch",
    )
    selection.add_argument(
        "--start-epoch",
        type=int,
        help="First latest-row epoch to select (inclusive)",
    )
    parser.add_argument(
        "--end-epoch",
        type=int,
        help="Last latest-row epoch to select (inclusive)",
    )
    parser.add_argument("--proposal", help="Restrict selection to TX_HASH#INDEX")

    parser.add_argument("--config", metavar="FILE", help="JSON configuration file")
    parser.add_argument("--network", choices=tuple(NETWORK_BASE_URLS), default=None)
    parser.add_argument("--adastat-base-url", default=None, help="Explicit AdaStat mirror/test URL")
    parser.add_argument("--adastat-timeout", type=float, default=None)
    parser.add_argument("--adastat-retries", type=int, default=None)
    parser.add_argument(
        "--adastat-delay",
        type=float,
        default=None,
        help="Minimum seconds between AdaStat requests (default: 1.1)",
    )
    parser.add_argument("--store-url", default=None)
    parser.add_argument("--store-user", default=None)
    parser.add_argument("--store-password", default=None)
    parser.add_argument("--store-schema", default=None)
    parser.add_argument("--reports-dir", default=None)
    parser.add_argument("--logs-dir", default=None)
    parser.add_argument("--max-mismatches", type=int, default=None)
    parser.add_argument("--quiet", action="store_true", default=None)
    parser.add_argument("--fail-on-inconclusive", action="store_true", default=None)
    parser.add_argument("--report-dir", default=None, help=argparse.SUPPRESS)
    parser.add_argument("--result-json", default=None, help=argparse.SUPPRESS)
    return parser


def validate_selection(
    parser: argparse.ArgumentParser,
    args: argparse.Namespace,
) -> Tuple[Sequence[int], Optional[Tuple[str, int]]]:
    if args.epoch is not None:
        if args.end_epoch is not None:
            parser.error("--end-epoch cannot be used with --epoch")
        start_epoch = end_epoch = args.epoch
    else:
        start_epoch = args.start_epoch
        end_epoch = args.end_epoch
        if end_epoch is None:
            parser.error("--end-epoch is required with --start-epoch")
    if start_epoch is None or start_epoch < 0 or end_epoch < start_epoch:
        parser.error("epoch values must be non-negative and end epoch must be >= start epoch")

    proposal_filter = None
    if args.proposal:
        try:
            proposal_filter = parse_legacy_proposal_id(args.proposal)
        except ReferenceContractError as exc:
            parser.error(str(exc))
    return range(start_epoch, end_epoch + 1), proposal_filter


def redact_command(argv: Sequence[str]) -> str:
    safe = []
    hide_next = False
    redact_url_next = False
    for token in argv:
        if hide_next:
            safe.append("****")
            hide_next = False
            continue
        if redact_url_next:
            safe.append(redact_url(token))
            redact_url_next = False
            continue
        if token == "--store-password":
            safe.append(token)
            hide_next = True
        elif token.startswith("--store-password="):
            safe.append("--store-password=****")
        elif token == "--store-url":
            safe.append(token)
            redact_url_next = True
        elif token.startswith("--store-url="):
            safe.append("--store-url=" + str(redact_url(token.split("=", 1)[1])))
        else:
            safe.append(token)
    return shlex.join(safe)


def render_detail(result: Mapping[str, Any]) -> str:
    return "\n".join(
        (
            "",
            "Governance voting-stats coverage:",
            f"  Proposals: selected={result['selected_proposals']}, "
            f"eligible={result['eligible_proposals']}, "
            f"compared={result['compared_proposals']}, "
            f"matched={result['matched_proposals']}, "
            f"partial={result['partial_proposals']}, "
            f"mismatched={result['mismatched_proposals']}, "
            f"inconclusive={result['inconclusive_proposals']}, "
            f"error={result['error_proposals']}",
            f"  Fields: compared={result['compared_fields']}/"
            f"{result['selected_fields']}, matched={result['matched_fields']}, "
            f"unavailable={result['unavailable_fields']}",
            f"  Reasons: {json.dumps(result['reason_counts'], sort_keys=True)}",
            f"  HTTP: requests={result['http']['requests']}, "
            f"retries={result['http']['retries']}, "
            f"cache_hits={result['http']['cache_hits']}",
        )
    )


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    epochs, proposal_filter = validate_selection(parser, args)
    try:
        settings = resolve_verifier_settings(args)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        parser.error(str(exc))

    started_at = datetime.now()
    run_id = started_at.strftime("%Y%m%d_%H%M%S")
    report_dir = run_report_dir(settings, "compare_gov_action_voting_stats", run_id)
    log_file = os.path.join(
        settings.logs_dir,
        f"gov_action_voting_stats_compare_{run_id}.log",
    )
    logger = Logger(log_file, quiet=settings.quiet)
    command_argv = [sys.executable, sys.argv[0]] + (
        list(argv) if argv is not None else sys.argv[1:]
    )
    command = redact_command(command_argv)

    logger.log("===== Starting AdaStat governance voting-stats verification =====")
    logger.log(f"Network: {settings.network}")
    logger.log(f"AdaStat base URL: {settings.adastat_base_url}")
    logger.log(
        f"Yaci Store URL: {redact_url(settings.store_url)} "
        f"(schema: {settings.store_schema})"
    )
    logger.log(f"Report directory: {os.path.abspath(report_dir)}")
    logger.log()

    client = AdaStatClient(
        network=settings.network,
        base_url=settings.adastat_base_url,
        timeout=settings.adastat_timeout,
        retries=settings.adastat_retries,
        min_interval=settings.adastat_delay,
    )
    connection = None
    try:
        connection = connect(settings.store_url, settings.store_schema)
        result = run_verification(
            connection,
            client,
            epochs,
            proposal_filter,
            report_dir,
            logger,
            settings.max_mismatches,
        )
    except Exception as exc:
        logger.error("Unable to start Yaci Store verification", exc)
        result = new_result("gov_action_voting_stats_adastat")
        result.update(
            {
                "epochs_compared": 0,
                "errors": 1,
                "selected_proposals": 0,
                "eligible_proposals": 0,
                "compared_proposals": 0,
                "matched_proposals": 0,
                "partial_proposals": 0,
                "mismatched_proposals": 0,
                "inconclusive_proposals": 0,
                "error_proposals": 0,
                "compared_fields": 0,
                "selected_fields": 0,
                "matched_fields": 0,
                "unavailable_fields": 0,
                "body_counts": {},
                "reason_counts": {"STORE_ERROR": 1},
                "proposals": [],
                "coverage_file": None,
                "http": {"requests": 0, "retries": 0, "cache_hits": 0},
            }
        )
        result["status"] = "ERROR"
    finally:
        if connection is not None:
            connection.close()

    result["log_file"] = os.path.abspath(log_file)
    exit_status = determine_exit_code(result, settings.fail_on_inconclusive)
    if exit_status == 2:
        result["status"] = "ERROR"
    elif exit_status == 1:
        result["status"] = "MISMATCH"
    else:
        result["status"] = "OK"

    finished_at = datetime.now()
    start_epoch = epochs.start if isinstance(epochs, range) else epochs[0]
    end_epoch = epochs.stop - 1 if isinstance(epochs, range) else epochs[-1]
    epoch_label = (
        f"epoch {start_epoch}"
        if start_epoch == end_epoch
        else f"epochs {start_epoch} -> {end_epoch}"
    )
    summary_text = render_summary(
        [result],
        "compare_gov_action_voting_stats",
        started_at,
        finished_at,
        command,
        epoch_label,
        report_dir,
        os.path.abspath(log_file),
    ) + render_detail(result)
    payload = summary_payload(
        [result],
        started_at,
        finished_at,
        command,
        {"start_epoch": start_epoch, "end_epoch": end_epoch},
        report_dir,
        os.path.abspath(log_file),
        {
            "network": settings.network,
            "adastat_base_url": settings.adastat_base_url,
            "adastat_timeout": settings.adastat_timeout,
            "adastat_retries": settings.adastat_retries,
            "adastat_delay": settings.adastat_delay,
            "store_url": redact_url(settings.store_url),
            "store_schema": settings.store_schema,
            "max_mismatches": settings.max_mismatches,
            "fail_on_inconclusive": settings.fail_on_inconclusive,
            "proposal": args.proposal,
        },
    )
    payload["result"] = result
    payload["exit_code"] = exit_status

    logger.log(summary_text)
    summary_log, summary_json = write_report_files(report_dir, summary_text, payload)
    if settings.result_json:
        write_json(settings.result_json, payload)
    logger.log()
    logger.log(f"Summary log written to: {summary_log}")
    logger.log(f"Summary JSON written to: {summary_json}")
    return exit_status


if __name__ == "__main__":
    sys.exit(main())
