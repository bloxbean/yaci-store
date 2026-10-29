"""
GET /api/v1/tokens/subject/{subject}

Tests the subject endpoint with CIP priority, property filtering,
and CIP details display.
"""

import allure
import pytest
import requests

from .conftest import _cip26_token_ids, _cip68_token_ids, _load_json, API_BASE_URL

CIP26_SUBJECTS = _cip26_token_ids()
CIP26_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip26_tokens.json")}
CIP68_SUBJECTS = _cip68_token_ids()
CIP68_BY_SUBJECT = {t["subject"]: t for t in _load_json("cip68_tokens.json")}


@allure.epic("assets-ext API")
@allure.feature("GET /api/v1/tokens/subject/{subject}")
@pytest.mark.v2
@pytest.mark.cip26
class TestV2GetSubjectCip26:

    @allure.story("Return metadata for known CIP-26 subject")
    @pytest.mark.parametrize("subject", CIP26_SUBJECTS, ids=lambda s: s[:16])
    def test_get_subject_returns_200(self, subject):
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"query_priority": "CIP_26"},
        )
        assert resp.status_code == 200, f"Expected 200 for {subject[:16]}..., got {resp.status_code}"

        data = resp.json()
        assert data["subject"]["subject"] == subject

        metadata = data["subject"]["metadata"]
        expected = CIP26_BY_SUBJECT[subject]

        self._assert_metadata_field(metadata, expected, "name")
        self._assert_metadata_field(metadata, expected, "ticker")
        self._assert_metadata_field(metadata, expected, "description")
        self._assert_metadata_field(metadata, expected, "url")
        self._assert_metadata_decimals(metadata, expected)

    @allure.story("Return 404 for unknown subject")
    def test_get_unknown_subject_returns_404(self):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{'00' * 56}")
        assert resp.status_code == 404

    @allure.story("Default query priority is CIP_68, CIP_26")
    def test_default_query_priority(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject}")
        data = resp.json()
        assert data["queryPriority"] == ["CIP_68", "CIP_26"]

    @allure.story("CIP-26 only priority returns CIP_26 source")
    def test_cip26_only_priority(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"query_priority": "CIP_26"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["queryPriority"] == ["CIP_26"]

        metadata = data["subject"]["metadata"]
        if metadata.get("name"):
            assert metadata["name"]["source"] == "CIP_26"

    @allure.story("show_cips_details=true returns standards with correct values")
    @pytest.mark.parametrize("subject", CIP26_SUBJECTS, ids=lambda s: s[:16])
    def test_show_cips_details(self, subject):
        expected = CIP26_BY_SUBJECT[subject]
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"show_cips_details": "true"},
        )
        assert resp.status_code == 200
        data = resp.json()
        standards = data["subject"]["standards"]
        assert standards is not None, "standards should be present when show_cips_details=true"
        cip26 = standards.get("cip26")
        assert cip26 is not None, "cip26 standard should be present for CIP-26 token"

        if expected["name"] and cip26.get("name"):
            assert cip26["name"]["value"] == expected["name"]
        if expected["description"] and cip26.get("description"):
            assert cip26["description"]["value"] == expected["description"]
        if expected["ticker"] and cip26.get("ticker"):
            assert cip26["ticker"]["value"] == expected["ticker"]
        if expected["url"] and cip26.get("url"):
            assert cip26["url"]["value"] == expected["url"]
        if expected["decimals"] is not None and cip26.get("decimals"):
            assert cip26["decimals"]["value"] == expected["decimals"]
        if expected["policy"] and cip26.get("policy"):
            assert cip26["policy"] == expected["policy"]

    @allure.story("show_cips_details=false does not return standards")
    def test_hide_cips_details(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"show_cips_details": "false"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["subject"]["standards"] is None

    @allure.story("Property filter returns only requested properties")
    def test_property_filter(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"property": ["name", "description", "ticker"]},
        )
        assert resp.status_code == 200
        data = resp.json()
        metadata = data["subject"]["metadata"]
        expected = CIP26_BY_SUBJECT[subject]

        if expected["name"]:
            assert metadata["name"] is not None
        assert metadata.get("url") is None
        assert metadata.get("decimals") is None
        assert metadata.get("logo") is None

    @allure.story("A property filter does not need name and description")
    def test_property_filter_without_description(self):
        subject = CIP26_SUBJECTS[0]
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"property": ["name", "ticker"]},
        )
        assert resp.status_code == 200
        metadata = resp.json()["subject"]["metadata"]
        assert metadata.get("description") is None
        assert metadata["name"]["value"] == CIP26_BY_SUBJECT[subject]["name"]

    @allure.story("CIP-26 standards detail matches the fixture")
    def test_cips_details_match_fixture(self):
        subject = CIP26_SUBJECTS[0]
        expected = CIP26_BY_SUBJECT[subject]
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"show_cips_details": "true"},
        )
        data = resp.json()
        cip26 = data["subject"]["standards"]["cip26"]

        if expected["name"]:
            assert cip26["name"]["value"] == expected["name"]
        if expected["ticker"]:
            assert cip26["ticker"]["value"] == expected["ticker"]
        if expected["policy"]:
            assert cip26["policy"] == expected["policy"]

    @allure.story("Metadata source is CIP_26 for CIP-26 only tokens")
    @pytest.mark.parametrize("subject", CIP26_SUBJECTS[:30], ids=lambda s: s[:16])
    def test_metadata_source_cip26(self, subject):
        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"query_priority": "CIP_26"},
        )
        assert resp.status_code == 200
        metadata = resp.json()["subject"]["metadata"]
        for field in ["name", "description", "ticker", "url", "decimals", "logo"]:
            val = metadata.get(field)
            if val is not None:
                assert val["source"] == "CIP_26", f"{field} source should be CIP_26"

    def _assert_metadata_field(self, metadata, expected, field):
        expected_val = expected.get(field)
        if expected_val is not None:
            actual = metadata.get(field)
            assert actual is not None, f"Expected {field} in the response"
            assert actual["value"] == expected_val, (
                f"{field}: expected '{expected_val}', got '{actual['value']}'"
            )

    def _assert_metadata_decimals(self, metadata, expected):
        expected_val = expected.get("decimals")
        if expected_val is not None:
            actual = metadata.get("decimals")
            assert actual is not None, "Expected decimals in the response"
            assert actual["value"] == expected_val


@allure.epic("assets-ext API")
@allure.feature("GET /api/v1/tokens/subject/{subject}")
@pytest.mark.v2
@pytest.mark.cip68
class TestV2GetSubjectCip68:

    @allure.story("Return metadata for known CIP-68 subject")
    @pytest.mark.parametrize("subject", CIP68_SUBJECTS if CIP68_SUBJECTS else ["skip"],
                             ids=lambda s: s[:16] if len(s) > 16 else s)
    def test_get_subject_returns_200(self, subject):
        if subject == "skip":
            pytest.skip("No CIP-68 tokens available yet")

        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject}")
        assert resp.status_code == 200

        data = resp.json()
        assert data["subject"]["subject"] == subject

        metadata = data["subject"]["metadata"]
        expected = CIP68_BY_SUBJECT[subject]

        if expected["name"]:
            assert metadata["name"]["value"] == expected["name"]
        if expected["description"]:
            assert metadata["description"]["value"] == expected["description"]
        if expected["ticker"]:
            assert metadata["ticker"]["value"] == expected["ticker"]
        if expected["decimals"] is not None:
            assert metadata["decimals"]["value"] == expected["decimals"]

    @allure.story("CIP-68 only priority returns CIP_68 source")
    @pytest.mark.parametrize("subject", CIP68_SUBJECTS[:20] if CIP68_SUBJECTS else ["skip"],
                             ids=lambda s: s[:16] if len(s) > 16 else s)
    def test_cip68_only_priority(self, subject):
        if subject == "skip":
            pytest.skip("No CIP-68 tokens available yet")

        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"query_priority": "CIP_68"},
        )
        assert resp.status_code == 200
        metadata = resp.json()["subject"]["metadata"]
        for field in ["name", "description", "ticker", "url", "decimals", "logo"]:
            val = metadata.get(field)
            if val is not None:
                assert val["source"] == "CIP_68"

    @allure.story("CIP-68 show_cips_details includes cip68 standard")
    @pytest.mark.parametrize("subject", CIP68_SUBJECTS[:20] if CIP68_SUBJECTS else ["skip"],
                             ids=lambda s: s[:16] if len(s) > 16 else s)
    def test_show_cips_details_cip68(self, subject):
        if subject == "skip":
            pytest.skip("No CIP-68 tokens available yet")

        resp = requests.get(
            f"{API_BASE_URL}/api/v1/tokens/subject/{subject}",
            params={"show_cips_details": "true", "query_priority": "CIP_68"},
        )
        assert resp.status_code == 200
        standards = resp.json()["subject"]["standards"]
        assert standards is not None
        assert standards.get("cip68") is not None

    @allure.story("CIP-68 version field is returned when present")
    @pytest.mark.parametrize("subject", CIP68_SUBJECTS[:20] if CIP68_SUBJECTS else ["skip"],
                             ids=lambda s: s[:16] if len(s) > 16 else s)
    def test_version_field(self, subject):
        if subject == "skip":
            pytest.skip("No CIP-68 tokens available yet")

        expected = CIP68_BY_SUBJECT[subject]
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject}")
        assert resp.status_code == 200
        metadata = resp.json()["subject"]["metadata"]

        if expected.get("version") is not None:
            assert metadata.get("version") is not None
            assert metadata["version"]["value"] == expected["version"]
