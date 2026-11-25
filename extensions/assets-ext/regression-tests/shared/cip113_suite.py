"""
CIP-113 programmable tokens, against a committed fixture of the network's CIP-113 registry.

The fixture (<network>/fixtures/cip113_tokens.json, made from the chain) holds, for every policy registered in the
official CIP-113 registry, the extension the API must return and the tokens minted under that policy. Every token of a
registered policy is served, type PROGRAMMABLE, with extensions.cip113 equal to the registry node. CIP-113 adds an
extension to a token, it does not describe one, so:

- with CIP-26 or CIP-68 metadata: the metadata, with its own validation errors;
- without any (no companion): no metadata, and a CIP-113 validation error on companion_metadata. The error is MANDATORY:
  such a token is left out by validation=MANDATORY_ONLY.

Each network's test module builds its test class with make_test_class.
"""

import json
import os

import allure
import pytest
import requests

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
CIP113_FIELDS = ("minting_logic_script", "transfer_logic_script", "third_party_transfer_logic_script",
                 "unfracking_logic_script", "global_state_policy_id")


def load_fixture(test_module_file):
    path = os.path.join(os.path.dirname(os.path.abspath(test_module_file)), "fixtures", "cip113_tokens.json")
    with open(path) as f:
        return json.load(f)


def _cases(fixture):
    return [(s, p) for p in fixture["policies"] for s in p["subjects"]]


def _has_metadata(subject):
    return subject["koios_cip26"] or bool(subject["koios_cip68_labels"])


def _assert_programmable(entry, policy):
    assert entry["type"] == "PROGRAMMABLE", f"{entry['subject'][:24]}...: type {entry.get('type')}"
    cip113 = (entry.get("extensions") or {}).get("cip113")
    assert cip113 is not None, f"{entry['subject'][:24]}...: no extensions.cip113"
    for field in CIP113_FIELDS:
        assert cip113.get(field) == policy["cip113"][field], (
            f"{entry['subject'][:24]}...: {field} is {cip113.get(field)}, the registry node "
            f"{policy['registry_utxo']} has {policy['cip113'][field]}")


def make_test_class(fixture, with_mandatory_errors=None, native_subjects=()):
    """
    The test class for one network. Every registered token with on-chain CIP-26 or CIP-68 metadata is served, with its
    validation errors; one without any is served with the error that it has no companion. with_mandatory_errors maps a
    subject whose metadata misses something its CIP requires to the reason: it is served, but not with
    validation=MANDATORY_ONLY. native_subjects are subjects of unregistered policies the API serves, so they must be NATIVE.
    """
    with_mandatory_errors = with_mandatory_errors or {}
    cases = _cases(fixture)
    expected_served = [(s, p) for s, p in cases if _has_metadata(s)]
    without_companion = [(s, p) for s, p in cases if not _has_metadata(s)]
    with_errors = [(s, p) for s, p in expected_served if s["subject"] in with_mandatory_errors]
    policies_by_id = {p["policy_id"]: p for p in fixture["policies"]}
    case_id = lambda case: case[0]["subject"][:16] + "-" + case[0]["subject"][56:][-16:]

    @allure.epic("assets-ext API")
    @allure.feature(f"CIP-113 programmable tokens ({fixture['network']})")
    @pytest.mark.v2
    @pytest.mark.cip113
    class TestV2Cip113:

        @allure.story("A programmable token with metadata is PROGRAMMABLE, with the registry node's extension")
        @pytest.mark.parametrize("case", expected_served, ids=case_id)
        def test_served_as_programmable(self, case):
            subject, policy = case
            resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject['subject']}")
            assert resp.status_code == 200, f"{subject['subject']}: expected 200, got {resp.status_code}"
            _assert_programmable(resp.json()["subject"], policy)

        @allure.story("A programmable token without CIP-26 or CIP-68 metadata (no companion) is served with the error")
        @pytest.mark.parametrize("case", without_companion, ids=case_id)
        def test_served_without_companion_metadata_with_the_error(self, case):
            subject, policy = case
            resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject['subject']}")
            assert resp.status_code == 200, (f"{subject['subject']}: expected 200 (no CIP-26 or CIP-68 metadata on chain, "
                                             f"served with an error), got {resp.status_code}")
            entry = resp.json()["subject"]
            _assert_programmable(entry, policy)
            assert entry["metadata"]["valid"] is False
            assert [e for e in entry["validation_errors"]
                    if (e["cip"], e["field_name"], e["field_category"], e["code"])
                    == ("CIP-113", "companion_metadata", "MANDATORY", "missing_value")], (
                f"{subject['subject']}: no MANDATORY CIP-113 companion_metadata error in {entry['validation_errors']}")
            mandatory_only = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject['subject']}",
                                          params={"validation": "MANDATORY_ONLY"})
            assert mandatory_only.status_code == 404

        @allure.story("A programmable token whose metadata misses something its CIP requires is served with the error, "
                      "but not with validation=MANDATORY_ONLY")
        @pytest.mark.parametrize("case", with_errors, ids=case_id)
        def test_served_with_mandatory_errors(self, case):
            subject, policy = case
            reason = with_mandatory_errors[subject["subject"]]
            resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject['subject']}")
            assert resp.status_code == 200, f"{subject['subject']} ({reason}): got {resp.status_code}"
            entry = resp.json()["subject"]
            _assert_programmable(entry, policy)
            assert [e for e in entry["validation_errors"] if e["field_category"] == "MANDATORY"], reason
            strict = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject['subject']}", params={"validation": "MANDATORY_ONLY"})
            assert strict.status_code == 404

        @allure.story("The extension is also returned with show_cips_details")
        @pytest.mark.parametrize("case", expected_served, ids=case_id)
        def test_served_as_programmable_with_cips_details(self, case):
            subject, policy = case
            resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject['subject']}",
                                params={"show_cips_details": "true"})
            assert resp.status_code == 200
            entry = resp.json()["subject"]
            _assert_programmable(entry, policy)
            assert entry.get("standards"), "show_cips_details=true returned no standards"

        @allure.story("The batch query returns every programmable token, as the single queries do")
        @pytest.mark.batch
        def test_batch_query(self):
            if not cases:
                pytest.skip("no programmable token is registered on this network")
            resp = requests.post(f"{API_BASE_URL}/api/v1/tokens/subject/query",
                                 json={"subjects": [s["subject"] for s, _ in cases]})
            assert resp.status_code == 200
            returned = {entry["subject"]: entry for entry in resp.json()["subjects"]}
            assert set(returned) == {s["subject"] for s, _ in cases}
            for subject, entry in returned.items():
                _assert_programmable(entry, policies_by_id[subject[:56]])

        @allure.story("A token of a policy that is not registered is NATIVE, without extensions")
        @pytest.mark.parametrize("subject", list(native_subjects), ids=lambda s: s[:16])
        def test_unregistered_token_is_native(self, subject):
            assert subject[:56] not in policies_by_id
            resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject}")
            assert resp.status_code == 200
            entry = resp.json()["subject"]
            assert entry["type"] == "NATIVE"
            assert not entry.get("extensions")

    return TestV2Cip113
