#!/usr/bin/env python3

import io
import json
import os
import socket
import sys
import tempfile
import unittest
from datetime import datetime, timezone
from email.message import Message
from types import SimpleNamespace
from urllib.error import HTTPError


TEST_DIR = os.path.dirname(os.path.abspath(__file__))
COMPARE_DIR = os.path.dirname(TEST_DIR)
FIXTURE_DIR = os.path.join(TEST_DIR, "fixtures", "adastat")
sys.path.insert(0, COMPARE_DIR)

from adastat_voting_stats import (  # noqa: E402
    ADASTAT_USER_AGENT,
    NETWORK_BASE_URLS,
    AdaStatClient,
    ReferenceHttpError,
    ReferenceUnavailableError,
    ResponseSchemaError,
    StoreDataError,
    build_expected_stats,
    check_eligibility,
    load_yaci_proposals,
    parse_adastat_response,
    resolve_verifier_settings,
    save_normalized_reference,
)


def fixture_text(name="preview_new_committee_1369.json"):
    with open(os.path.join(FIXTURE_DIR, name), "r", encoding="utf-8") as fixture:
        return fixture.read()


class FakeResponse:
    def __init__(self, body, headers=None):
        self.body = body.encode("utf-8") if isinstance(body, str) else body
        self.headers = headers or {}
        self.closed = False

    def read(self):
        return self.body

    def close(self):
        self.closed = True


class SequenceOpener:
    def __init__(self, outcomes):
        self.outcomes = list(outcomes)
        self.calls = []

    def __call__(self, request, timeout):
        self.calls.append((request, timeout))
        outcome = self.outcomes.pop(0)
        if isinstance(outcome, BaseException):
            raise outcome
        return outcome


class FakeClock:
    def __init__(self):
        self.value = 100.0
        self.sleeps = []

    def monotonic(self):
        return self.value

    def sleep(self, seconds):
        self.sleeps.append(seconds)
        self.value += seconds


def http_error(code, retry_after=None):
    headers = Message()
    if retry_after is not None:
        headers["Retry-After"] = str(retry_after)
    return HTTPError(
        "https://example.test/action",
        code,
        "test response",
        headers,
        io.BytesIO(b"{}"),
    )


class AdaStatHttpClientTest(unittest.TestCase):
    def setUp(self):
        self.tx_hash = "ab" * 32
        self.now = lambda: datetime(2026, 7, 27, tzinfo=timezone.utc)

    def client(self, outcomes, **overrides):
        clock = overrides.pop("clock", FakeClock())
        opener = SequenceOpener(outcomes)
        client = AdaStatClient(
            network=overrides.pop("network", "preview"),
            base_url=overrides.pop("base_url", "https://mirror.example/api/rest/v2"),
            timeout=overrides.pop("timeout", 20),
            retries=overrides.pop("retries", 3),
            min_interval=overrides.pop("min_interval", 0),
            opener=opener,
            sleep=clock.sleep,
            monotonic=clock.monotonic,
            now=self.now,
            **overrides,
        )
        return client, opener, clock

    def test_builds_each_network_url_and_required_headers(self):
        for network, base_url in NETWORK_BASE_URLS.items():
            with self.subTest(network=network):
                opener = SequenceOpener([FakeResponse(fixture_text())])
                client = AdaStatClient(
                    network,
                    opener=opener,
                    sleep=lambda _: None,
                    monotonic=lambda: 1.0,
                    now=self.now,
                )
                fetched = client.fetch(self.tx_hash, 10)
                request, timeout = opener.calls[0]
                self.assertEqual(
                    f"{base_url}/gov_actions/{self.tx_hash}0a.json?currency=usd",
                    fetched.url,
                )
                self.assertEqual(20, timeout)
                self.assertEqual("application/json", request.get_header("Accept"))
                self.assertEqual("identity", request.get_header("Accept-encoding"))
                self.assertEqual(ADASTAT_USER_AGENT, request.get_header("User-agent"))

    def test_caches_success_and_spaces_distinct_requests(self):
        clock = FakeClock()
        client, opener, _ = self.client(
            [FakeResponse(fixture_text()), FakeResponse(fixture_text())],
            clock=clock,
            min_interval=1.1,
        )
        first = client.fetch(self.tx_hash, 0)
        cached = client.fetch(self.tx_hash, 0)
        client.fetch(self.tx_hash, 1)

        self.assertIs(first, cached)
        self.assertEqual(2, len(opener.calls))
        self.assertEqual([1.1], clock.sleeps)
        self.assertEqual(1, client.cache_hits)
        self.assertEqual(2, client.requests)

    def test_retries_timeout_429_and_server_error(self):
        cases = (
            (socket.timeout("slow"), None, 1.0),
            (http_error(429, 7), "7", 7.0),
            (http_error(503), None, 1.0),
        )
        for failure, _, expected_sleep in cases:
            with self.subTest(failure=type(failure).__name__, code=getattr(failure, "code", None)):
                client, opener, clock = self.client(
                    [failure, FakeResponse(fixture_text())],
                    retries=1,
                )
                fetched = client.fetch(self.tx_hash, 0)
                self.assertEqual(2, fetched.attempts)
                self.assertEqual(2, len(opener.calls))
                self.assertEqual([expected_sleep], clock.sleeps)
                self.assertEqual(1, client.retry_count)

    def test_uses_bounded_exponential_backoff_until_exhausted(self):
        client, _, clock = self.client(
            [http_error(500), http_error(502), http_error(503)],
            retries=2,
        )
        with self.assertRaises(ReferenceHttpError):
            client.fetch(self.tx_hash, 0)
        self.assertEqual([1.0, 2.0], clock.sleeps)
        self.assertEqual(3, client.requests)

    def test_classifies_http_and_wrapper_unavailability(self):
        for outcome in (
            http_error(404),
            FakeResponse('{"code":404,"data":null}'),
        ):
            with self.subTest(outcome=type(outcome).__name__):
                client, opener, _ = self.client([outcome])
                with self.assertRaises(ReferenceUnavailableError):
                    client.fetch(self.tx_hash, 0)
                self.assertEqual(1, len(opener.calls))

    def test_retries_wrapper_server_code_and_rejects_invalid_encoding(self):
        client, _, _ = self.client(
            [
                FakeResponse('{"code":503}'),
                FakeResponse(fixture_text()),
            ],
            retries=1,
        )
        self.assertEqual(2, client.fetch(self.tx_hash, 0).attempts)

        client, _, _ = self.client(
            [FakeResponse(fixture_text(), {"Content-Encoding": "br"})]
        )
        with self.assertRaises(ReferenceHttpError):
            client.fetch(self.tx_hash, 0)

    def test_leaves_invalid_json_for_strict_parser(self):
        client, _, _ = self.client([FakeResponse("not json")])
        fetched = client.fetch(self.tx_hash, 0)
        with self.assertRaises(ResponseSchemaError):
            parse_adastat_response(fetched.raw_text)


class FakeCursor:
    def __init__(self, rows=(), error=None):
        self.rows = list(rows)
        self.error = error
        self.query = None
        self.params = None

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def execute(self, query, params):
        self.query = query
        self.params = params
        if self.error:
            raise self.error

    def fetchall(self):
        return self.rows


class FakeConnection:
    def __init__(self, cursor):
        self.fake_cursor = cursor

    def cursor(self):
        return self.fake_cursor


class YaciStoreLoaderTest(unittest.TestCase):
    def setUp(self):
        self.tx_hash = "06" * 32
        self.stats_text = '{"drep_approval_ratio":0.5060,"cc_yes":2}'
        self.row = (
            self.tx_hash,
            0,
            "UPDATE_COMMITTEE",
            "RATIFIED",
            self.stats_text,
            1369,
        )

    def test_loads_epoch_in_one_parameterized_query(self):
        cursor = FakeCursor([self.row])
        proposals = load_yaci_proposals(FakeConnection(cursor), 1369)

        self.assertEqual(1, len(proposals))
        self.assertEqual("NEW_COMMITTEE", proposals[0].action_type)
        self.assertEqual("0.5060", str(proposals[0].voting_stats["drep_approval_ratio"]))
        self.assertEqual((1369,), cursor.params)
        self.assertIn("WHERE epoch = %s", cursor.query)
        self.assertNotIn(self.tx_hash, cursor.query)

    def test_applies_proposal_filter_with_parameters(self):
        cursor = FakeCursor([self.row])
        load_yaci_proposals(
            FakeConnection(cursor),
            1369,
            (self.tx_hash.upper(), 0),
        )
        self.assertEqual((1369, self.tx_hash, 0), cursor.params)
        self.assertIn("gov_action_tx_hash = %s", cursor.query)
        self.assertIn("gov_action_index = %s", cursor.query)

    def test_preserves_empty_epoch_and_active_rows(self):
        self.assertEqual([], load_yaci_proposals(FakeConnection(FakeCursor()), 1369))
        active = list(self.row)
        active[3] = "ACTIVE"
        proposals = load_yaci_proposals(FakeConnection(FakeCursor([tuple(active)])), 1369)
        self.assertEqual("ACTIVE", proposals[0].status)

    def test_rejects_duplicate_and_malformed_rows(self):
        with self.assertRaises(StoreDataError):
            load_yaci_proposals(FakeConnection(FakeCursor([self.row, self.row])), 1369)

        malformed = list(self.row)
        malformed[4] = None
        with self.assertRaises(StoreDataError):
            load_yaci_proposals(FakeConnection(FakeCursor([tuple(malformed)])), 1369)

    def test_wraps_database_failure(self):
        cursor = FakeCursor(error=RuntimeError("database unavailable"))
        with self.assertRaisesRegex(StoreDataError, "failed to load Store rows"):
            load_yaci_proposals(FakeConnection(cursor), 1369)


class SettingsAndReferenceReportTest(unittest.TestCase):
    def test_resolves_config_then_cli_without_reference_credentials(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = os.path.join(temp_dir, "config.json")
            with open(config_path, "w", encoding="utf-8") as config:
                json.dump(
                    {
                        "network": "preview",
                        "store_url": "postgresql://localhost/yaci_store",
                        "store_user": "config-user",
                        "store_password": "config-password",
                        "reports_dir": "reports",
                        "adastat_delay": 2.0,
                    },
                    config,
                )
            args = SimpleNamespace(
                config=config_path,
                network="preprod",
                adastat_delay=0.25,
            )
            settings = resolve_verifier_settings(args)

            self.assertEqual("preprod", settings.network)
            self.assertEqual(NETWORK_BASE_URLS["preprod"], settings.adastat_base_url)
            self.assertEqual(0.25, settings.adastat_delay)
            self.assertEqual(os.path.join(temp_dir, "reports"), settings.reports_dir)
            self.assertIn("config-user:config-password@", settings.store_url)

    def test_saves_compact_normalized_reference_without_store_secret(self):
        raw = fixture_text()
        reference = parse_adastat_response(raw)
        normalized = build_expected_stats(reference)
        cursor = FakeCursor(
            [
                (
                    reference.tx_hash,
                    reference.index,
                    "UPDATE_COMMITTEE",
                    "RATIFIED",
                    "{}",
                    reference.ratified_epoch,
                )
            ]
        )
        proposal = load_yaci_proposals(
            FakeConnection(cursor),
            reference.ratified_epoch,
        )[0]
        self.assertTrue(check_eligibility(proposal, reference).eligible)

        opener = SequenceOpener([FakeResponse(raw)])
        client = AdaStatClient(
            "preview",
            base_url="https://mirror.example/api/rest/v2",
            min_interval=0,
            opener=opener,
            now=lambda: datetime(2026, 7, 27, tzinfo=timezone.utc),
        )
        fetched = client.fetch(reference.tx_hash, reference.index)
        with tempfile.TemporaryDirectory() as report_dir:
            path = save_normalized_reference(
                report_dir,
                "preview",
                proposal,
                fetched,
                normalized,
            )
            with open(path, "r", encoding="utf-8") as saved:
                saved_text = saved.read()
            self.assertNotIn("config-password", saved_text)
            payload = json.loads(saved_text)
            self.assertEqual(18, len(payload["expected_values"]))
            self.assertEqual("0.9414", payload["expected_values"]["drep_approval_ratio"])


if __name__ == "__main__":
    unittest.main()
