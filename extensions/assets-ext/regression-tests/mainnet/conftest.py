import json
import os
import pytest

FIXTURES_DIR = os.path.join(os.path.dirname(__file__), "fixtures")
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")


def _load_json(filename):
    path = os.path.join(FIXTURES_DIR, filename)
    if not os.path.exists(path):
        return []
    with open(path) as f:
        return json.load(f)


@pytest.fixture(scope="session")
def api_base_url():
    return API_BASE_URL


@pytest.fixture(scope="session")
def cip26_tokens():
    tokens = _load_json("cip26_tokens.json")
    assert len(tokens) > 0, "No CIP-26 fixture tokens found in fixtures/cip26_tokens.json."
    return tokens


@pytest.fixture(scope="session")
def cip68_tokens():
    return _load_json("cip68_tokens.json")


@pytest.fixture(scope="session")
def cip26_subjects(cip26_tokens):
    return [t["subject"] for t in cip26_tokens]


@pytest.fixture(scope="session")
def cip68_subjects(cip68_tokens):
    return [t["subject"] for t in cip68_tokens]


@pytest.fixture(scope="session")
def cip26_tokens_by_subject(cip26_tokens):
    return {t["subject"]: t for t in cip26_tokens}


@pytest.fixture(scope="session")
def cip68_tokens_by_subject(cip68_tokens):
    return {t["subject"]: t for t in cip68_tokens}


def _cip26_token_ids():
    """Load CIP-26 subjects for parametrize (runs at collection time)."""
    tokens = _load_json("cip26_tokens.json")
    return [t["subject"] for t in tokens]


def _cip68_token_ids():
    """Load CIP-68 subjects for parametrize (runs at collection time)."""
    tokens = _load_json("cip68_tokens.json")
    return [t["subject"] for t in tokens]


def _cip26_sample(n=50):
    """Return a sample of CIP-26 subjects for lighter tests."""
    subjects = _cip26_token_ids()
    step = max(1, len(subjects) // n)
    return subjects[::step][:n]


def _cip68_sample(n=50):
    """Return a sample of CIP-68 subjects for lighter tests."""
    subjects = _cip68_token_ids()
    step = max(1, len(subjects) // n)
    return subjects[::step][:n]
