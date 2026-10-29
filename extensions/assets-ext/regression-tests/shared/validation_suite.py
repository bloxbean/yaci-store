"""
The validation contract (validation errors are reported, not enforced), on a sample of a network's tokens.

Every token is served with a `validation_errors` array (empty when it follows its CIP), `metadata.valid` is true exactly
when it is empty, `?validation=MANDATORY_ONLY|STRICT` returns a token exactly when its errors allow it, and under
`?property=` the errors are about the requested properties only. What each token's errors are is not asserted here:
only that the response is consistent with them, so the suite needs subjects, not expected values.

Each network's test module builds its test class with make_test_class.
"""

import os

import allure
import pytest
import requests

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")


def get(subject, **params):
    return requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{subject}", params=params)


def has_mandatory(errors):
    return any(e["field_category"] == "MANDATORY" for e in errors)


def make_test_class(sample):
    """The contract tests, parametrized over `sample`, a list of subjects the API serves."""

    @allure.epic("assets-ext API")
    @allure.feature("Validation errors")
    @pytest.mark.v2
    @pytest.mark.validation
    class TestV2Validation:

        @allure.story("Every token has a validation_errors array, and metadata.valid says whether it is empty")
        @pytest.mark.parametrize("subject", sample, ids=lambda s: s[:16])
        def test_errors_and_valid_agree(self, subject):
            resp = get(subject)
            assert resp.status_code == 200
            body = resp.json()["subject"]
            errors = body["validation_errors"]
            assert isinstance(errors, list)
            assert body["metadata"]["valid"] is (len(errors) == 0)
            for error in errors:
                assert error["cip"] in ("CIP-26", "CIP-68")
                assert error["field_category"] in ("MANDATORY", "OPTIONAL")
                assert error["code"] and error["error"] and error["field_name"]

        @allure.story("?validation= returns a token exactly when its errors allow it")
        @pytest.mark.parametrize("subject", sample, ids=lambda s: s[:16])
        def test_validation_scope(self, subject):
            errors = get(subject).json()["subject"]["validation_errors"]

            assert get(subject, validation="ALL").status_code == 200
            assert get(subject, validation="MANDATORY_ONLY").status_code == (404 if has_mandatory(errors) else 200)
            assert get(subject, validation="STRICT").status_code == (404 if errors else 200)

        @allure.story("Under ?property= the errors are about the requested properties only")
        @pytest.mark.parametrize("subject", sample[::5], ids=lambda s: s[:16])
        def test_errors_scoped_to_properties(self, subject):
            resp = get(subject, property="ticker")
            assert resp.status_code == 200
            for error in resp.json()["subject"]["validation_errors"]:
                assert error["field_name"] in ("ticker", "721"), error

        @allure.story("A STRICT batch returns exactly the tokens with no error")
        def test_strict_batch(self):
            all_tokens = requests.post(f"{API_BASE_URL}/api/v1/tokens/subject/query", json={"subjects": sample}).json()["subjects"]
            strict = requests.post(f"{API_BASE_URL}/api/v1/tokens/subject/query", params={"validation": "STRICT"},
                                   json={"subjects": sample}).json()["subjects"]

            assert {s["subject"] for s in strict} == {s["subject"] for s in all_tokens if not s["validation_errors"]}

    return TestV2Validation
