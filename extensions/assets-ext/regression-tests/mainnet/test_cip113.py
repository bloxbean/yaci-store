"""
CIP-113 programmable tokens on mainnet. See shared/cip113_suite.py.

None of the tokens in the fixture has CIP-26 or CIP-68 metadata, so the API serves all of them without metadata, with the
CIP-113 error that they have no companion. Five CIP-26 tokens of unregistered policies must stay NATIVE.
"""

from shared.cip113_suite import load_fixture, make_test_class

from .conftest import _cip26_token_ids

TestV2Cip113 = make_test_class(load_fixture(__file__), native_subjects=_cip26_token_ids()[:5])
