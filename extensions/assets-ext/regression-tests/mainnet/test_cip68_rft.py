"""
CIP-68 rich fungible tokens (RFTs, label 444, user-token prefix 001bc280).

The fixture `cip68_rft_tokens.json` comes from the chain, through Koios (copied from cf-token-metadata-registry, which
generated it), not from a database. It holds the RFTs whose current 444 datum has a name, a description and an image
URI, read from a live reference NFT with an inline datum. An RFT has no logo in CIP-68: its image is served as
`metadata.logo`.

13 entries have no description (added by hand on 2026-10-28): RFTs whose 444 token and reference NFT were minted in
different transactions (Hydra DOOM Stats, five of 9bdf437b..., three JEET BUSH, four Omen's handle). Each holds the
current 444 datum from Koios, from a live reference NFT with an inline datum.

This fixture is a frozen baseline like the others: do not regenerate it during a test run. A token whose datum changes
on chain is corrected deliberately.
"""

import allure
import pytest
import requests

from .conftest import _load_json, API_BASE_URL

RFT_FIXTURE = _load_json("cip68_rft_tokens.json")
RFT_TOKENS = RFT_FIXTURE["tokens"] if RFT_FIXTURE else []
RFT_SAMPLE = RFT_TOKENS[::max(1, len(RFT_TOKENS) // 25)][:25]
BATCH_SIZE = 50
NFT_PREFIX = "000de140"
REFERENCE_NFT_PREFIX = "000643b0"
RFT_PREFIX = "001bc280"


def token_id(token):
    return token["subject"][:16] + "…" + token["subject"][-8:]


def value(metadata, field):
    return (metadata.get(field) or {}).get("value")


@allure.epic("assets-ext API")
@allure.feature("CIP-68 rich fungible tokens (444)")
@pytest.mark.v2
@pytest.mark.cip68
@pytest.mark.rft
class TestV2Cip68Rft:

    @allure.story("An RFT is served from CIP-68 with its image as the logo")
    @pytest.mark.parametrize("token", RFT_TOKENS, ids=token_id)
    def test_get_subject_returns_200(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['subject']}", params={"query_priority": "CIP_68"})
        assert resp.status_code == 200, f"{token['name']}: got {resp.status_code}"

        subject = resp.json()["subject"]
        assert subject["subject"] == token["subject"]
        metadata = subject["metadata"]
        assert value(metadata, "name") == token["name"]
        assert value(metadata, "description") == token["description"]
        assert value(metadata, "ticker") == token["ticker"]
        assert value(metadata, "decimals") == token["decimals"]
        assert value(metadata, "logo") == token["image"]
        assert value(metadata, "version") == token["version"]
        # the fixture holds RFTs with everything CIP-68 requires: no MANDATORY error; an optional one (files, an extra
        # property) is possible, and metadata.valid says whether there is any
        errors = subject["validation_errors"]
        assert not [e for e in errors if e["field_category"] == "MANDATORY"], errors
        assert metadata["valid"] is (len(errors) == 0)

    @allure.story("Every served field of an RFT comes from CIP-68")
    @pytest.mark.parametrize("token", RFT_SAMPLE, ids=token_id)
    def test_fields_come_from_cip68(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['subject']}")
        assert resp.status_code == 200
        metadata = resp.json()["subject"]["metadata"]
        assert metadata["logo"]["source"] == "CIP_68"
        for field in ["name", "description", "ticker", "decimals", "logo", "url", "version"]:
            if metadata.get(field) is not None:
                assert metadata[field]["source"] == "CIP_68", field

    @allure.story("show_cips_details shows the image as the cip68 logo")
    @pytest.mark.parametrize("token", RFT_SAMPLE, ids=token_id)
    def test_show_cips_details(self, token):
        resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['subject']}",
                            params={"show_cips_details": "true", "query_priority": "CIP_68"})
        assert resp.status_code == 200
        standards = resp.json()["subject"]["standards"]
        assert standards["cip26"] is None
        assert standards["cip68"]["name"] == token["name"]
        assert standards["cip68"]["logo"] == token["image"]

    @allure.story("The batch query returns every RFT with its image as the logo")
    @pytest.mark.parametrize("start", range(0, len(RFT_TOKENS), BATCH_SIZE), ids=lambda s: f"tokens {s}+")
    def test_batch_query(self, start):
        batch = RFT_TOKENS[start:start + BATCH_SIZE]
        resp = requests.post(f"{API_BASE_URL}/api/v1/tokens/subject/query",
                             json={"subjects": [t["subject"] for t in batch]})
        assert resp.status_code == 200
        returned = {s["subject"]: s["metadata"] for s in resp.json()["subjects"]}
        assert set(returned) == {t["subject"] for t in batch}
        for token in batch:
            assert value(returned[token["subject"]], "logo") == token["image"], token["name"]

    @allure.story("The NFT and the reference NFT of an RFT's base name are not served")
    @pytest.mark.parametrize("token", RFT_SAMPLE[:10], ids=token_id)
    def test_nft_and_reference_nft_not_served(self, token):
        base_name = token["asset_name"][len(RFT_PREFIX):]
        for prefix in (NFT_PREFIX, REFERENCE_NFT_PREFIX):
            resp = requests.get(f"{API_BASE_URL}/api/v1/tokens/subject/{token['policy_id']}{prefix}{base_name}",
                                params={"query_priority": "CIP_68"})
            assert resp.status_code == 404, f"{prefix}: got {resp.status_code}"
