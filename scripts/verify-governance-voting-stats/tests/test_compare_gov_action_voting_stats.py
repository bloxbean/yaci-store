#!/usr/bin/env python3

import io
import json
import os
import sys
import tempfile
import unittest
from contextlib import redirect_stderr
from datetime import datetime, timezone
from decimal import Decimal
from types import SimpleNamespace


TEST_DIR = os.path.dirname(os.path.abspath(__file__))
COMPARE_DIR = os.path.dirname(TEST_DIR)
FIXTURE_DIR = os.path.join(TEST_DIR, "fixtures", "adastat")
sys.path.insert(0, COMPARE_DIR)

from adastat_voting_stats import (  # noqa: E402
    FetchedResponse,
    ReferenceHttpError,
    ReferenceUnavailableError,
    UnsupportedActionIndex,
    build_expected_stats,
    parse_adastat_response,
    resolve_verifier_settings,
)
from compare_gov_action_voting_stats import (  # noqa: E402
    build_parser,
    compare_one_proposal,
    determine_exit_code,
    redact_command,
    run_verification,
    validate_selection,
)


def load_payload(name="preview_new_committee_1369.json"):
    with open(os.path.join(FIXTURE_DIR, name), "r", encoding="utf-8") as fixture:
        return json.load(fixture)


def payload_text(payload):
    return json.dumps(payload, separators=(",", ":"))


def parameter_change_payload():
    payload = load_payload()
    payload["data"]["type"] = "parameterchange"
    payload["data"]["cc_total"] = 7
    payload["data"]["cc_yes"] = 4
    payload["data"]["cc_no"] = 1
    payload["data"]["cc_abstain"] = 1
    return payload


def expected_stats(payload):
    normalized = build_expected_stats(parse_adastat_response(payload))
    return dict(normalized.values)


def store_row(payload, stats=None, epoch=None, status="RATIFIED"):
    reference = parse_adastat_response(payload)
    if stats is None:
        stats = expected_stats(payload)
    if epoch is None:
        epoch = (
            reference.ratified_epoch if status == "RATIFIED" else reference.expired_epoch
        )
    store_type = {
        "NEW_COMMITTEE": "UPDATE_COMMITTEE",
        "PARAMETER_CHANGE": "PARAMETER_CHANGE_ACTION",
        "TREASURY_WITHDRAWALS": "TREASURY_WITHDRAWALS_ACTION",
    }.get(reference.action_type, reference.action_type)
    return (
        reference.tx_hash,
        reference.index,
        store_type,
        status,
        json.dumps(stats, default=str),
        epoch,
    )


class FakeCursor:
    def __init__(self, rows_by_epoch, error_epochs):
        self.rows_by_epoch = rows_by_epoch
        self.error_epochs = error_epochs
        self.rows = []

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def execute(self, _query, params):
        if len(params) == 2:
            proposal_filter = None
            start_epoch, end_epoch = params
        else:
            proposal_filter = (params[0], params[1])
            start_epoch, end_epoch = params[2], params[3]
        if any(start_epoch <= epoch <= end_epoch for epoch in self.error_epochs):
            raise RuntimeError("Store unavailable")

        latest_by_proposal = {}
        for epoch_rows in self.rows_by_epoch.values():
            for row in epoch_rows:
                key = (row[0], row[1])
                if proposal_filter is not None and key != proposal_filter:
                    continue
                current = latest_by_proposal.get(key)
                if current is None or row[5] > current[5]:
                    latest_by_proposal[key] = row
        self.rows = sorted(
            (
                row
                for row in latest_by_proposal.values()
                if start_epoch <= row[5] <= end_epoch
            ),
            key=lambda row: (row[5], row[0], row[1]),
        )

    def fetchall(self):
        return self.rows


class FakeConnection:
    def __init__(self, rows_by_epoch=None, error_epochs=()):
        self.rows_by_epoch = rows_by_epoch or {}
        self.error_epochs = set(error_epochs)

    def cursor(self):
        return FakeCursor(self.rows_by_epoch, self.error_epochs)


class FakeClient:
    def __init__(self, responses=None):
        self.network = "preview"
        self.responses = responses or {}
        self.requests = 0
        self.retry_count = 0
        self.cache_hits = 0

    def fetch(self, tx_hash, index):
        self.requests += 1
        outcome = self.responses[(tx_hash, index)]
        if isinstance(outcome, BaseException):
            raise outcome
        return FetchedResponse(
            raw_text=payload_text(outcome),
            url=f"https://preview.adastat.net/api/rest/v2/gov_actions/{tx_hash}00.json?currency=usd",
            fetched_at=datetime(2026, 7, 27, tzinfo=timezone.utc).isoformat(),
            attempts=1,
        )


class FakeLogger:
    def __init__(self):
        self.lines = []
        self.errors = []

    def log(self, message=""):
        self.lines.append(message)

    def error(self, message, exc=None):
        self.errors.append((message, exc))


def run_case(test_case, rows_by_epoch, responses, epochs, error_epochs=()):
    temp_dir = tempfile.TemporaryDirectory()
    test_case.addCleanup(temp_dir.cleanup)
    logger = FakeLogger()
    result = run_verification(
        FakeConnection(rows_by_epoch, error_epochs),
        FakeClient(responses),
        epochs,
        None,
        temp_dir.name,
        logger,
    )
    return result, logger, temp_dir.name


class ProposalComparisonTest(unittest.TestCase):
    def test_full_match_visits_all_23_fields(self):
        payload = parameter_change_payload()
        row = store_row(payload)
        reference = parse_adastat_response(payload)
        connection = FakeConnection({reference.ratified_epoch: [row]})
        from adastat_voting_stats import load_yaci_proposals

        proposal = load_yaci_proposals(connection, reference.ratified_epoch)[0]
        comparison = compare_one_proposal(
            proposal,
            FakeClient({(reference.tx_hash, reference.index): payload}),
        )
        self.assertEqual("MATCH", comparison.result)
        self.assertEqual(23, comparison.compared_fields)
        self.assertFalse(comparison.mismatches)

    def test_partial_match_reports_unavailable_body(self):
        payload = load_payload()
        reference = parse_adastat_response(payload)
        result, _, report_dir = run_case(
            self,
            {reference.ratified_epoch: [store_row(payload)]},
            {(reference.tx_hash, reference.index): payload},
            [reference.ratified_epoch],
        )
        self.assertEqual(1, result["partial_proposals"])
        self.assertEqual(18, result["compared_fields"])
        self.assertEqual(0, determine_exit_code(result, False))
        self.assertTrue(os.path.exists(result["coverage_file"]))
        self.assertTrue(os.path.isdir(os.path.join(report_dir, "references")))

    def test_distinguishes_value_missing_null_and_type_mismatches(self):
        payload = parameter_change_payload()
        stats = expected_stats(payload)
        stats["spo_total_yes_stake"] += 1
        stats.pop("drep_total_yes_stake")
        stats["cc_yes"] = None
        stats["cc_no"] = "not-an-integer"
        reference = parse_adastat_response(payload)
        row = store_row(payload, stats)
        from adastat_voting_stats import load_yaci_proposals

        proposal = load_yaci_proposals(
            FakeConnection({reference.ratified_epoch: [row]}),
            reference.ratified_epoch,
        )[0]
        comparison = compare_one_proposal(
            proposal,
            FakeClient({(reference.tx_hash, reference.index): payload}),
        )
        issues = {mismatch.issue for mismatch in comparison.mismatches}
        self.assertEqual(
            {
                "VALUE_MISMATCH",
                "MISSING_YACI_FIELD",
                "NULL_YACI_FIELD",
                "TYPE_MISMATCH",
            },
            issues,
        )

    def test_null_reference_body_produces_partial_coverage(self):
        payload = load_payload()
        for key in (
            "pool_total_stake",
            "pool_yes_stake",
            "pool_no_stake",
            "pool_abstain_stake",
            "pool_always_abstain_stake",
            "pool_always_no_confidence_stake",
        ):
            payload["data"][key] = None
        reference = parse_adastat_response(payload)
        result, _, _ = run_case(
            self,
            {reference.ratified_epoch: [store_row(payload)]},
            {(reference.tx_hash, reference.index): payload},
            [reference.ratified_epoch],
        )
        self.assertEqual(10, result["compared_fields"])
        self.assertEqual(1, result["partial_proposals"])
        self.assertEqual(0, determine_exit_code(result, False))


class RunClassificationTest(unittest.TestCase):
    def test_active_only_is_inconclusive_without_http_request(self):
        payload = load_payload()
        reference = parse_adastat_response(payload)
        row = store_row(payload, epoch=1370, status="ACTIVE")
        result, _, _ = run_case(self, {1370: [row]}, {}, [1370])

        self.assertEqual(1, result["inconclusive_proposals"])
        self.assertEqual({"INCONCLUSIVE_LIVE": 1}, result["reason_counts"])
        self.assertEqual(0, result["http"]["requests"])
        self.assertEqual(2, determine_exit_code(result, False))
        self.assertEqual(reference.tx_hash, row[0])

    def test_terminal_outcome_epoch_mismatch_is_inconclusive(self):
        payload = load_payload()
        reference = parse_adastat_response(payload)
        wrong_epoch = reference.ratified_epoch + 1
        result, _, _ = run_case(
            self,
            {wrong_epoch: [store_row(payload, epoch=wrong_epoch)]},
            {(reference.tx_hash, reference.index): payload},
            [wrong_epoch],
        )
        self.assertEqual(
            {"INCONCLUSIVE_EPOCH_MISMATCH": 1},
            result["reason_counts"],
        )
        self.assertEqual(2, determine_exit_code(result, False))

    def test_http_failure_is_operational_error(self):
        payload = load_payload()
        reference = parse_adastat_response(payload)
        result, _, _ = run_case(
            self,
            {reference.ratified_epoch: [store_row(payload)]},
            {
                (reference.tx_hash, reference.index): ReferenceHttpError(
                    "AdaStat unavailable"
                )
            },
            [reference.ratified_epoch],
        )
        self.assertEqual(1, result["error_proposals"])
        self.assertEqual({"HTTP_ERROR": 1}, result["reason_counts"])
        self.assertEqual(2, determine_exit_code(result, False))

    def test_maps_unavailable_index_identity_and_schema_failures(self):
        payload = load_payload()
        reference = parse_adastat_response(payload)
        row = store_row(payload)
        from adastat_voting_stats import load_yaci_proposals

        proposal = load_yaci_proposals(
            FakeConnection({reference.ratified_epoch: [row]}),
            reference.ratified_epoch,
        )[0]
        cases = (
            (
                ReferenceUnavailableError("missing"),
                "INCONCLUSIVE",
                "ADASTAT_NOT_FOUND",
            ),
            (
                UnsupportedActionIndex("large index"),
                "INCONCLUSIVE",
                "UNSUPPORTED_ACTION_INDEX",
            ),
        )
        for outcome, expected_result, expected_reason in cases:
            with self.subTest(reason=expected_reason):
                comparison = compare_one_proposal(
                    proposal,
                    FakeClient({(proposal.tx_hash, proposal.index): outcome}),
                )
                self.assertEqual(expected_result, comparison.result)
                self.assertEqual(expected_reason, comparison.reason)

        identity_payload = load_payload()
        identity_payload["data"]["tx_hash"] = "ff" * 32
        comparison = compare_one_proposal(
            proposal,
            FakeClient({(proposal.tx_hash, proposal.index): identity_payload}),
        )
        self.assertEqual("ADASTAT_IDENTITY_MISMATCH", comparison.reason)

        schema_payload = load_payload()
        del schema_payload["data"]["drep_yes_stake"]
        comparison = compare_one_proposal(
            proposal,
            FakeClient({(proposal.tx_hash, proposal.index): schema_payload}),
        )
        self.assertEqual("ADASTAT_SCHEMA_ERROR", comparison.reason)

    def test_malformed_latest_row_is_store_error(self):
        payload = load_payload()
        reference = parse_adastat_response(payload)
        malformed = list(store_row(payload))
        malformed[4] = "{invalid"
        rows = {
            reference.ratified_epoch: [tuple(malformed)],
            reference.ratified_epoch + 1: [],
        }
        result, logger, _ = run_case(
            self,
            rows,
            {},
            [reference.ratified_epoch, reference.ratified_epoch + 1],
        )
        self.assertEqual(1, result["errors"])
        self.assertEqual({"STORE_ERROR": 1}, result["reason_counts"])
        self.assertEqual(1, len(logger.errors))
        self.assertEqual(2, determine_exit_code(result, False))

    def test_zero_rows_is_an_error_exit_because_no_field_was_compared(self):
        result, _, _ = run_case(self, {}, {}, [1, 2])
        self.assertEqual(0, result["selected_proposals"])
        self.assertEqual(0, result["errors"])
        self.assertEqual(2, determine_exit_code(result, False))

    def test_range_processes_only_latest_snapshot_per_proposal(self):
        payload = parameter_change_payload()
        reference = parse_adastat_response(payload)
        active_epoch = reference.ratified_epoch - 1
        rows = {
            active_epoch: [store_row(payload, epoch=active_epoch, status="ACTIVE")],
            reference.ratified_epoch: [store_row(payload)],
        }
        result, _, _ = run_case(
            self,
            rows,
            {(reference.tx_hash, reference.index): payload},
            [active_epoch, reference.ratified_epoch],
        )
        self.assertEqual(1, result["matched_proposals"])
        self.assertEqual(1, result["selected_proposals"])
        self.assertEqual(0, result["inconclusive_proposals"])
        self.assertEqual(0, determine_exit_code(result, False))
        self.assertEqual(0, determine_exit_code(result, True))

    def test_field_mismatch_returns_one_without_operational_error(self):
        payload = parameter_change_payload()
        stats = expected_stats(payload)
        stats["cc_yes"] += 1
        reference = parse_adastat_response(payload)
        result, _, _ = run_case(
            self,
            {reference.ratified_epoch: [store_row(payload, stats)]},
            {(reference.tx_hash, reference.index): payload},
            [reference.ratified_epoch],
        )
        self.assertEqual(1, result["total_mismatches"])
        self.assertEqual(1, determine_exit_code(result, False))
        self.assertEqual(1, len(result["mismatch_files"]))


class CliSafetyTest(unittest.TestCase):
    def test_network_is_required_from_cli_or_config(self):
        args = SimpleNamespace(config=None)
        with self.assertRaisesRegex(ValueError, "network is required"):
            resolve_verifier_settings(args)

    def test_command_redacts_password_and_url_userinfo(self):
        command = redact_command(
            [
                "python3",
                "script.py",
                "--store-password",
                "super-secret",
                "--store-url=postgresql://user:another-secret@localhost/db",
            ]
        )
        self.assertNotIn("super-secret", command)
        self.assertNotIn("another-secret", command)
        self.assertIn("****", command)

    def test_selection_rejects_invalid_range_and_proposal_before_io(self):
        parser = build_parser()
        args = parser.parse_args(
            ["--network", "preview", "--start-epoch", "10", "--end-epoch", "9"]
        )
        with redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                validate_selection(parser, args)

        args = parser.parse_args(
            ["--network", "preview", "--epoch", "10", "--proposal", "invalid"]
        )
        with redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit):
                validate_selection(parser, args)


if __name__ == "__main__":
    unittest.main()
