"""
CIP-113 programmable tokens on preprod. See shared/cip113_suite.py. Not in the default testpaths (mainnet): run it against a
preprod yaci-store with `uv run pytest preprod`.

Most of the tokens registered on preprod are tests without metadata; they have no companion and are served with the CIP-113
error that says so. A token with on-chain metadata is served with its validation errors; WITH_MANDATORY_ERRORS lists those
whose metadata misses something its CIP requires, with the reason, so they are hidden with validation=MANDATORY_ONLY.
"""

from shared.cip113_suite import load_fixture, make_test_class

WITH_MANDATORY_ERRORS = {
    # BRLT: the 333 datum has name, ticker and decimals but no description, which CIP-68 requires for label 333.
    # The same token under policy b75439f5... has a description and is served.
    "baddd482c787be3b2f11dd2cf916755bd7a30f0c41070011ec7190ee0014df1042524c54":
        "CIP-68 333 datum has no description",
}

TestV2Cip113 = make_test_class(load_fixture(__file__), with_mandatory_errors=WITH_MANDATORY_ERRORS)
