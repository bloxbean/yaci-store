"""
POST /api/v1/tokens/subject/query

Tests batch query endpoint with CIP priority, property filtering, and CIP details.
The batch returns every indexed token with its validation_errors; ?validation= filters (see
test_validation.py). The tests below still pick tokens with a name and a description.
"""

import allure
import pytest
import requests

from .conftest import _cip26_token_ids, _cip68_token_ids, _load_json, API_BASE_URL

CIP26_SUBJECTS = _cip26_token_ids()
CIP26_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip26_tokens.json")}
CIP68_SUBJECTS = _cip68_token_ids()
CIP68_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip68_tokens.json")}

# Tokens with both name and description, so the merged name/description assertions have values to compare
VALID_CIP26_SUBJECTS = [
    s for s in CIP26_SUBJECTS
    if CIP26_BY_SUBJECT[s].get("name") and CIP26_BY_SUBJECT[s].get("description")
]


@allure.epic("assets-ext API")
@allure.feature("POST /api/v1/tokens/subject/query")
@pytest.mark.v2
@pytest.mark.batch
class TestV2BatchQuery:

    @allure.story("Batch query returns all valid CIP-26 subjects")
    def test_batch_all_cip26_subjects(self):
        batch_size = 50
        for i in range(0, len(VALID_CIP26_SUBJECTS), batch_size):
            batch = VALID_CIP26_SUBJECTS[i:i + batch_size]
            resp = requests.post(
                f"{API_BASE_URL}/api/v1/tokens/subject/query",
                json={"subjects": batch},
            )
            assert resp.status_code == 200
            data = resp.json()
            returned_subjects = {s["subject"] for s in data["subjects"]}

            for subj in batch:
                assert subj in returned_subjects, (
                    f"Valid subject {subj[:16]}... missing from the batch response"
                )

    @allure.story("Batch query includes queryPriority in response")
    def test_batch_includes_query_priority(self):
        batch = VALID_CIP26_SUBJECTS[:5]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "queryPriority" in data
        assert data["queryPriority"] == ["CIP_68", "CIP_26"]

    @allure.story("Batch query with CIP_26 only priority")
    def test_batch_cip26_only_priority(self):
        batch = VALID_CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch},
            params={"query_priority": "CIP_26"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["queryPriority"] == ["CIP_26"]

        for entry in data["subjects"]:
            metadata = entry["metadata"]
            for field in ["name", "description", "ticker", "url", "decimals", "logo"]:
                val = metadata.get(field)
                if val is not None:
                    assert val["source"] == "CIP_26"

    @allure.story("Batch query values match DB fixture")
    def test_batch_values_match_fixture(self):
        batch = VALID_CIP26_SUBJECTS[:50]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch},
        )
        assert resp.status_code == 200
        data = resp.json()

        for entry in data["subjects"]:
            subject = entry["subject"]
            expected = CIP26_BY_SUBJECT[subject]
            metadata = entry["metadata"]

            if expected["name"]:
                assert metadata["name"]["value"] == expected["name"]
            if expected["description"]:
                assert metadata["description"]["value"] == expected["description"]
            if expected["ticker"] and metadata.get("ticker"):
                assert metadata["ticker"]["value"] == expected["ticker"]
            if expected["decimals"] is not None and metadata.get("decimals"):
                assert metadata["decimals"]["value"] == expected["decimals"]

    @allure.story("Batch query with show_cips_details=true validates values")
    def test_batch_show_cips_details(self):
        batch = VALID_CIP26_SUBJECTS[:50]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch},
            params={"show_cips_details": "true"},
        )
        assert resp.status_code == 200
        data = resp.json()

        for entry in data["subjects"]:
            subject = entry["subject"]
            expected = CIP26_BY_SUBJECT[subject]
            assert entry.get("standards") is not None, (
                f"standards should be present for {subject[:16]}..."
            )
            cip26 = entry["standards"].get("cip26")
            assert cip26 is not None

            if expected["name"] and cip26.get("name"):
                assert cip26["name"]["value"] == expected["name"]
            if expected["description"] and cip26.get("description"):
                assert cip26["description"]["value"] == expected["description"]
            if expected["ticker"] and cip26.get("ticker"):
                assert cip26["ticker"]["value"] == expected["ticker"]
            if expected["decimals"] is not None and cip26.get("decimals"):
                assert cip26["decimals"]["value"] == expected["decimals"]
            if expected["policy"] and cip26.get("policy"):
                assert cip26["policy"] == expected["policy"]

    @allure.story("Batch query with show_cips_details=false hides standards")
    def test_batch_hide_cips_details(self):
        batch = VALID_CIP26_SUBJECTS[:10]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch},
            params={"show_cips_details": "false"},
        )
        assert resp.status_code == 200
        data = resp.json()

        for entry in data["subjects"]:
            assert entry.get("standards") is None

    @allure.story("Batch query with mixed known and unknown subjects")
    def test_batch_mixed_known_unknown(self):
        known = VALID_CIP26_SUBJECTS[:5]
        unknown = ["00" * 56, "ff" * 56]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": known + unknown},
        )
        assert resp.status_code == 200
        data = resp.json()
        returned_subjects = {s["subject"] for s in data["subjects"]}

        for subj in known:
            assert subj in returned_subjects
        for subj in unknown:
            assert subj not in returned_subjects

    @allure.story("Large batch query (all valid CIP-26 tokens, 100 at a time)")
    def test_large_batch(self):
        # a batch holds at most 100 subjects (BatchRequest); the registry this suite was copied from has no limit
        for i in range(0, len(VALID_CIP26_SUBJECTS), 100):
            batch = VALID_CIP26_SUBJECTS[i:i + 100]
            resp = requests.post(
                f"{API_BASE_URL}/api/v1/tokens/subject/query",
                json={"subjects": batch},
            )
            assert resp.status_code == 200
            assert len(resp.json()["subjects"]) == len(batch)

    @allure.story("A batch of more than 100 subjects is rejected")
    def test_batch_over_limit_is_rejected(self):
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": VALID_CIP26_SUBJECTS[:101]},
        )
        assert resp.status_code == 400

    @allure.story("Batch with CIP-68 subjects")
    def test_batch_cip68_subjects(self):
        if not CIP68_SUBJECTS:
            pytest.skip("No CIP-68 tokens available yet")

        # Filter to CIP-68 tokens with name and description
        valid_cip68 = [
            s for s in CIP68_SUBJECTS
            if CIP68_BY_SUBJECT[s].get("name") and CIP68_BY_SUBJECT[s].get("description")
        ]
        if not valid_cip68:
            pytest.skip("No valid CIP-68 tokens available")

        batch = valid_cip68[:50]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch},
        )
        assert resp.status_code == 200
        data = resp.json()
        returned_subjects = {s["subject"] for s in data["subjects"]}

        for subj in batch:
            assert subj in returned_subjects

    @allure.story("Batch with mixed CIP-26 and CIP-68 subjects")
    def test_batch_mixed_cip26_cip68(self):
        if not CIP68_SUBJECTS:
            pytest.skip("No CIP-68 tokens available yet")

        cip26_batch = VALID_CIP26_SUBJECTS[:10]
        valid_cip68 = [
            s for s in CIP68_SUBJECTS
            if CIP68_BY_SUBJECT[s].get("name") and CIP68_BY_SUBJECT[s].get("description")
        ][:10]
        if not valid_cip68:
            pytest.skip("No valid CIP-68 tokens available")

        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": cip26_batch + valid_cip68},
            params={"show_cips_details": "true"},
        )
        assert resp.status_code == 200
        data = resp.json()

        for entry in data["subjects"]:
            standards = entry.get("standards")
            assert standards is not None
            # Should have at least one CIP standard
            assert standards.get("cip26") is not None or standards.get("cip68") is not None

    @allure.story("Batch query with properties filter (name+description keeps results valid)")
    def test_batch_with_valid_properties_filter(self):
        """When filtering to name+description, results should still pass the validity check."""
        batch = VALID_CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch, "properties": ["name", "description"]},
        )
        assert resp.status_code == 200
        data = resp.json()
        # Should return results since name+description are both present
        assert len(data["subjects"]) > 0

    @allure.story("A batch property filter does not need name and description")
    def test_batch_with_properties_filter_without_description(self):
        batch = VALID_CIP26_SUBJECTS[:20]
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            json={"subjects": batch, "properties": ["name", "ticker"]},
        )
        assert resp.status_code == 200
        returned = resp.json()["subjects"]
        assert len(returned) == len(batch)
        assert all(s["metadata"].get("description") is None for s in returned)
