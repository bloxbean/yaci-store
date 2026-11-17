"""
CIP-26 entries whose registry file writes the subject in upper case hex (39 on mainnet), on
GET /api/v1/tokens/subject/{subject} and POST /api/v1/tokens/subject/query.

The entry is stored and looked up in lower case, so a client finds it whichever case it asks in. standards.cip26 must
carry the subject as the file writes it: every signature is made over that string, and none verifies against the subject
in lower case. Three entries from cip26_upper_case_subjects.json (cardano-token-registry 5503caec), with their real
signatures.
"""

import allure
import pytest
import requests

from shared.cip26_signatures import verify_entry

from .conftest import _load_json, API_BASE_URL

ENTRIES = _load_json("cip26_upper_case_subjects.json")
CIP26_DETAILS = {"query_priority": "CIP_26", "show_cips_details": "true"}


def _ids(entry):
    return entry["name"]


def _assert_served_as_written(cip26, entry):
    assert cip26 is not None, f"no standards.cip26 for {entry['name']}"
    assert cip26["subject"] == entry["subject"], (
        f"{entry['name']}: standards.cip26.subject is {cip26['subject']}, the registry file writes {entry['subject']}")
    checked, failed = verify_entry(cip26)
    assert not failed, f"{entry['name']}: signatures that do not verify against the subject served: {failed}"
    assert checked >= len(entry["signed_properties"]), (
        f"{entry['name']}: {checked} signatures served, the file signs {entry['signed_properties']}")
    _, failed_in_lower_case = verify_entry(cip26, subject=entry["subject"].lower())
    assert failed_in_lower_case, f"{entry['name']}: a signature verified against the lower-case subject too"


@allure.epic("assets-ext API")
@allure.feature("CIP-26 signatures of subjects written in upper case")
@pytest.mark.cip26
@pytest.mark.signatures
class TestCip26UpperCaseSubjects:

    @allure.story("Found in either case, served with the subject as written, every signature verifies")
    @pytest.mark.parametrize("entry", ENTRIES, ids=_ids)
    @pytest.mark.parametrize("case", ["lower", "as_written"])
    def test_get_subject(self, entry, case):
        asked = entry["subject"].lower() if case == "lower" else entry["subject"]
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{asked}", params=CIP26_DETAILS)
        assert resp.status_code == 200, f"{entry['name']} asked {case}: got {resp.status_code}"

        subject = resp.json()["subject"]
        assert subject["metadata"]["name"]["value"] == entry["name"]
        _assert_served_as_written(subject["standards"]["cip26"], entry)

    @allure.story("A batch asked in either case: every entry served with the subject as written")
    @pytest.mark.parametrize("case", ["lower", "as_written"])
    def test_batch(self, case):
        asked = [e["subject"].lower() if case == "lower" else e["subject"] for e in ENTRIES]
        resp = requests.post(f"{API_BASE_URL}/api/v1/tokens/subject/query", params=CIP26_DETAILS, json={"subjects": asked})
        assert resp.status_code == 200

        served = {s["standards"]["cip26"]["subject"]: s for s in resp.json()["subjects"] if (s.get("standards") or {}).get("cip26")}
        for entry in ENTRIES:
            assert entry["subject"] in served, f"{entry['name']} asked {case}: not in the batch with the subject as written"
            _assert_served_as_written(served[entry["subject"]]["standards"]["cip26"], entry)
