"""
CIP-26: entries deleted from the upstream registry must not be served (#1102).

Guards the invariant that a CIP-26 token whose mapping file was removed from the upstream cardano-token-registry is no
longer served.

The fixture `cip26_removed_tokens.json` was mined from the upstream repository's git history (copied from
cf-token-metadata-registry, where it guards the same invariant): every entry was once a canonical mapping
(filename == inner subject, valid name + description, so the sync WOULD have indexed it) whose file was later deleted
and is still absent at HEAD. It includes subject 2d8cee2c...6672414441, "ADAFR Pool Reward ADA", removed upstream in
cardano-token-registry PR #8014.

This fixture is a frozen baseline like the others — do not regenerate during a test run. If one of these tokens is ever
legitimately re-added upstream, remove that single entry deliberately.

Only the CIP-26 view is asserted (`/tokens/cip26/{subject}`, and the subject query with `query_priority=CIP_26`): the
merged view may legitimately serve some of these subjects from on-chain CIP-68 data (e.g. subjects carrying the 0014df10
fungible-token prefix), so a 404 there would be flaky by design.
"""

import allure
import pytest
import requests

from .conftest import _load_json, API_BASE_URL

REMOVED_TOKENS = _load_json("cip26_removed_tokens.json")
REMOVED_SUBJECTS = [t["subject"] for t in REMOVED_TOKENS]


@allure.epic("assets-ext API")
@allure.feature("Removed registry entries")
@pytest.mark.cip26
@pytest.mark.deletion
class TestCip26RemovedSubjects:

    @allure.story("Subject deleted from the upstream registry returns 404")
    @pytest.mark.parametrize("subject", REMOVED_SUBJECTS, ids=lambda s: s[:16])
    def test_removed_subject_returns_404(self, subject):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/cip26/{subject}")
        assert resp.status_code == 404, (
            f"Subject {subject[:24]}... was deleted from the upstream registry "
            f"but the API still serves it (HTTP {resp.status_code}). "
            f"Deletions must be propagated to the local DB (#1102)."
        )

    @allure.story("A CIP-26 subject query does not return subjects deleted upstream")
    def test_query_excludes_removed_subjects(self):
        resp = requests.post(
            f"{API_BASE_URL}/api/v1/tokens/subject/query",
            params={"query_priority": "CIP_26"},
            json={"subjects": REMOVED_SUBJECTS},
        )
        assert resp.status_code == 200
        returned = {s["subject"] for s in resp.json()["subjects"]}
        leaked = returned & set(REMOVED_SUBJECTS)
        assert not leaked, (
            f"{len(leaked)} deleted subject(s) still returned by the subject query, "
            f"e.g. {sorted(leaked)[0][:24]}..."
        )
