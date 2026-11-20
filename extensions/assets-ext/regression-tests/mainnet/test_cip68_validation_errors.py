"""
CIP-68 tokens that break CIP-68 are served with their validation errors, not dropped.

The fixture `cip68_validation_errors_tokens.json` holds tokens whose datum departs from CIP-68 in a known way, each with
the reason and the errors that must be reported. Builds that dropped invalid tokens answered 404 for them (or lost their logo). The
errors listed must be among the token's errors; a token may have others (optional ones), which are not asserted here.

This fixture is a frozen baseline like the others: do not regenerate it during a test run. Correct an entry
deliberately, with the reason, when its datum changes on chain.
"""

import allure
import pytest
import requests

from .conftest import _load_json, API_BASE_URL

TOKENS = _load_json("cip68_validation_errors_tokens.json")


def token_id(token):
    return token["name"].strip()[:24]


def errors_of(subject):
    return [(e["field_name"], e["code"], e["field_category"]) for e in subject["validation_errors"]]


@allure.epic("assets-ext API")
@allure.feature("CIP-68 tokens served with validation errors")
@pytest.mark.v2
@pytest.mark.cip68
@pytest.mark.validation
class TestV2Cip68ValidationErrors:

    @allure.story("A token that breaks CIP-68 is served from CIP-68 with its errors")
    @pytest.mark.parametrize("token", TOKENS, ids=token_id)
    def test_served_with_its_errors(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['subject']}", params={"query_priority": "CIP_68"})
        assert resp.status_code == 200, f"{token['name']}: {token['why']}; got {resp.status_code}"

        subject = resp.json()["subject"]
        errors = errors_of(subject)
        for expected in token["expected_errors"]:
            assert (expected["field_name"], expected["code"], expected["field_category"]) in errors, errors
        if "absent_sub_standard" in token:
            assert not [e for e in subject["validation_errors"] if e.get("sub_standard") == token["absent_sub_standard"]]
        if "expected_logo" in token:
            assert subject["metadata"]["logo"]["value"] == token["expected_logo"]
        assert subject["metadata"]["valid"] is (len(errors) == 0)

    @allure.story("MANDATORY_ONLY hides a token with a MANDATORY error and serves one without")
    @pytest.mark.parametrize("token", TOKENS, ids=token_id)
    def test_mandatory_only(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['subject']}",
                            params={"query_priority": "CIP_68", "validation": "MANDATORY_ONLY"})
        assert resp.status_code == (404 if token["has_mandatory_error"] else 200)

    @allure.story("STRICT hides a token with any error")
    @pytest.mark.parametrize("token", [t for t in TOKENS if t["expected_errors"]], ids=token_id)
    def test_strict(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['subject']}",
                            params={"query_priority": "CIP_68", "validation": "STRICT"})
        assert resp.status_code == 404
