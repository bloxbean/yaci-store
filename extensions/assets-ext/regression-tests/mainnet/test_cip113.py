"""
CIP-113 programmable tokens on mainnet. See shared/cip113_suite.py.

Most registered tokens have no CIP-26 or CIP-68 metadata and are served with the CIP-113 error that they have no companion;
the 333 token of e102a5c7... has CIP-68 metadata and is served with it. Five CIP-26 tokens of unregistered policies must
stay NATIVE.
"""

from shared.cip113_suite import load_fixture, make_test_class

from .conftest import _cip26_token_ids

TestV2Cip113 = make_test_class(load_fixture(__file__), native_subjects=_cip26_token_ids()[:5])
