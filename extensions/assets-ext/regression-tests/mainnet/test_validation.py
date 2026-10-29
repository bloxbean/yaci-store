"""
The validation contract (validation errors are reported, not enforced) on samples of the mainnet CIP-26 and CIP-68 baselines. See
shared/validation_suite.py; what each token's errors are is asserted in test_cip68_validation_errors.py.
"""

from shared.validation_suite import make_test_class

from .conftest import _cip26_sample, _cip68_sample

TestV2Validation = make_test_class(_cip26_sample(25) + _cip68_sample(25))
