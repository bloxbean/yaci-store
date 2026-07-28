#!/usr/bin/env python3
"""AdaStat-backed verification support for governance voting statistics.

The strict normalization functions validate one AdaStat governance-action
response, check whether it represents the same terminal epoch as a Yaci Store
row, and derive the comparable subset of the 23 ``ProposalVotingStats`` fields.
The transport and Store loader keep retrieval separate from normalization.
"""

from __future__ import annotations

import json
import math
import os
import re
import socket
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal, ROUND_HALF_UP
from email.utils import parsedate_to_datetime
from typing import Any, Callable, Dict, Mapping, Optional, Sequence, Tuple, Union
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen

from verifier_common import (
    DEFAULT_STORE_SCHEMA,
    DEFAULT_STORE_URL,
    TOOL_DIR,
    apply_credentials,
    load_config,
    resolve_path,
)


SPO_FIELDS: Tuple[str, ...] = (
    "spo_total_yes_stake",
    "spo_total_no_stake",
    "spo_total_abstain_stake",
    "spo_yes_vote_stake",
    "spo_no_vote_stake",
    "spo_abstain_vote_stake",
    "spo_do_not_vote_stake",
    "spo_approval_ratio",
)

DREP_FIELDS: Tuple[str, ...] = (
    "drep_total_yes_stake",
    "drep_total_no_stake",
    "drep_total_abstain_stake",
    "drep_yes_vote_stake",
    "drep_no_vote_stake",
    "drep_abstain_vote_stake",
    "drep_no_confidence_stake",
    "drep_auto_abstain_stake",
    "drep_do_not_vote_stake",
    "drep_approval_ratio",
)

CC_FIELDS: Tuple[str, ...] = (
    "cc_yes",
    "cc_no",
    "cc_do_not_vote",
    "cc_abstain",
    "cc_approval_ratio",
)

ALL_FIELDS: Tuple[str, ...] = SPO_FIELDS + DREP_FIELDS + CC_FIELDS

BODY_FIELDS: Mapping[str, Tuple[str, ...]] = {
    "spo": SPO_FIELDS,
    "drep": DREP_FIELDS,
    "cc": CC_FIELDS,
}

NO_CONFIDENCE = "NO_CONFIDENCE"
NEW_COMMITTEE = "NEW_COMMITTEE"
NEW_CONSTITUTION = "NEW_CONSTITUTION"
HARD_FORK_INITIATION = "HARD_FORK_INITIATION"
TREASURY_WITHDRAWALS = "TREASURY_WITHDRAWALS"
PARAMETER_CHANGE = "PARAMETER_CHANGE"
INFO_ACTION = "INFO_ACTION"

ACTION_TYPES: Tuple[str, ...] = (
    NO_CONFIDENCE,
    NEW_COMMITTEE,
    NEW_CONSTITUTION,
    HARD_FORK_INITIATION,
    TREASURY_WITHDRAWALS,
    PARAMETER_CHANGE,
    INFO_ACTION,
)

_ACTION_TYPE_ALIASES = {
    "NOCONFIDENCE": NO_CONFIDENCE,
    "NOCONFIDENCEACTION": NO_CONFIDENCE,
    "NEWCOMMITTEE": NEW_COMMITTEE,
    "NEWCOMMITTEEACTION": NEW_COMMITTEE,
    "UPDATECOMMITTEE": NEW_COMMITTEE,
    "UPDATECOMMITTEEACTION": NEW_COMMITTEE,
    "NEWCONSTITUTION": NEW_CONSTITUTION,
    "NEWCONSTITUTIONACTION": NEW_CONSTITUTION,
    "HARDFORKINITIATION": HARD_FORK_INITIATION,
    "HARDFORKINITIATIONACTION": HARD_FORK_INITIATION,
    "TREASURYWITHDRAWAL": TREASURY_WITHDRAWALS,
    "TREASURYWITHDRAWALS": TREASURY_WITHDRAWALS,
    "TREASURYWITHDRAWALSACTION": TREASURY_WITHDRAWALS,
    "PARAMETERCHANGE": PARAMETER_CHANGE,
    "PARAMETERCHANGEACTION": PARAMETER_CHANGE,
    "INFOACTION": INFO_ACTION,
}

EXPECTED_BODIES: Mapping[str, Tuple[str, ...]] = {
    NO_CONFIDENCE: ("drep", "spo"),
    NEW_COMMITTEE: ("drep", "spo"),
    NEW_CONSTITUTION: ("drep", "cc"),
    HARD_FORK_INITIATION: ("drep", "spo", "cc"),
    TREASURY_WITHDRAWALS: ("drep", "cc"),
    PARAMETER_CHANGE: ("drep", "spo", "cc"),
    INFO_ACTION: ("drep", "spo", "cc"),
}

_HASH_RE = re.compile(r"^[0-9a-fA-F]{64}$")
_NON_NEGATIVE_INTEGER_RE = re.compile(r"^(0|[1-9][0-9]*)$")
_SCHEMA_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_$]*$")
_RATIO_QUANTUM = Decimal("0.0001")

NETWORK_BASE_URLS: Mapping[str, str] = {
    "mainnet": "https://adastat.net/api/rest/v2",
    "preprod": "https://preprod.adastat.net/api/rest/v2",
    "preview": "https://preview.adastat.net/api/rest/v2",
}

DEFAULT_ADASTAT_TIMEOUT = 20.0
DEFAULT_ADASTAT_RETRIES = 3
DEFAULT_ADASTAT_DELAY = 1.1
ADASTAT_USER_AGENT = "yaci-store-voting-stats-verifier/1.0"


class ReferenceContractError(ValueError):
    """The reference cannot be mapped safely to Yaci voting statistics."""


class ResponseSchemaError(ReferenceContractError):
    """The AdaStat response does not satisfy the expected response schema."""


class IdentityMismatchError(ReferenceContractError):
    """The AdaStat response identifies a different governance action."""


class UnsupportedActionIndex(ReferenceContractError):
    """The action index cannot be represented by AdaStat's 66-hex route."""


class ReferenceUnavailableError(RuntimeError):
    """AdaStat has no usable reference for the selected governance action."""


class ReferenceHttpError(RuntimeError):
    """AdaStat retrieval failed or returned an unexpected HTTP outcome."""


class StoreDataError(RuntimeError):
    """Selected Yaci Store rows do not satisfy the verifier contract."""


@dataclass(frozen=True)
class YaciProposal:
    epoch: int
    tx_hash: str
    index: int
    action_type: str
    status: str
    voting_stats: Mapping[str, Any]


@dataclass(frozen=True)
class AdaStatDRepInputs:
    total: Optional[int]
    yes: Optional[int]
    no: Optional[int]
    abstain: Optional[int]
    always_abstain: Optional[int]
    always_no_confidence: Optional[int]
    inactive: Optional[int]


@dataclass(frozen=True)
class AdaStatSPOInputs:
    total: Optional[int]
    yes: Optional[int]
    no: Optional[int]
    abstain: Optional[int]
    always_abstain: Optional[int]
    always_no_confidence: Optional[int]


@dataclass(frozen=True)
class AdaStatCCInputs:
    total: Optional[int]
    yes: Optional[int]
    no: Optional[int]
    abstain: Optional[int]


@dataclass(frozen=True)
class AdaStatReference:
    tx_hash: str
    index: int
    action_type: str
    bootstrap_period: bool
    submission_epoch: Optional[int]
    expiry_epoch: Optional[int]
    ratified_epoch: Optional[int]
    enacted_epoch: Optional[int]
    expired_epoch: Optional[int]
    dropped_epoch: Optional[int]
    tip_epoch: int
    tip_slot: Optional[int]
    drep: AdaStatDRepInputs
    spo: AdaStatSPOInputs
    cc: AdaStatCCInputs


@dataclass(frozen=True)
class Eligibility:
    eligible: bool
    reason: Optional[str]


@dataclass(frozen=True)
class NormalizedReference:
    reference: AdaStatReference
    values: Mapping[str, Union[int, Decimal]]
    unavailable: Mapping[str, str]
    diagnostics: Mapping[str, Mapping[str, Any]]

    @property
    def compared_fields(self) -> int:
        return len(self.values)


@dataclass(frozen=True)
class FetchedResponse:
    raw_text: str
    url: str
    fetched_at: str
    attempts: int


@dataclass(frozen=True)
class VerifierSettings:
    network: str
    adastat_base_url: str
    adastat_timeout: float
    adastat_retries: int
    adastat_delay: float
    store_url: str
    store_schema: str
    reports_dir: str
    logs_dir: str
    quiet: bool
    max_mismatches: int
    fail_on_inconclusive: bool
    report_dir: Optional[str]
    result_json: Optional[str]


class AdaStatClient:
    """Sequential AdaStat client with deterministic throttling and retries."""

    def __init__(
        self,
        network: str,
        base_url: Optional[str] = None,
        timeout: float = DEFAULT_ADASTAT_TIMEOUT,
        retries: int = DEFAULT_ADASTAT_RETRIES,
        min_interval: float = DEFAULT_ADASTAT_DELAY,
        opener: Callable[..., Any] = urlopen,
        sleep: Callable[[float], None] = time.sleep,
        monotonic: Callable[[], float] = time.monotonic,
        now: Callable[[], datetime] = lambda: datetime.now(timezone.utc),
    ):
        normalized_network = str(network).lower()
        if normalized_network not in NETWORK_BASE_URLS:
            raise ValueError("network must be one of: mainnet, preprod, preview")
        if (
            isinstance(timeout, bool)
            or not isinstance(timeout, (int, float))
            or not math.isfinite(timeout)
            or timeout <= 0
        ):
            raise ValueError("AdaStat timeout must be positive")
        if isinstance(retries, bool) or not isinstance(retries, int) or retries < 0:
            raise ValueError("AdaStat retries must be a non-negative integer")
        if (
            isinstance(min_interval, bool)
            or not isinstance(min_interval, (int, float))
            or not math.isfinite(min_interval)
            or min_interval < 0
        ):
            raise ValueError("AdaStat delay must be non-negative")

        self.network = normalized_network
        self.base_url = validate_adastat_base_url(
            base_url or NETWORK_BASE_URLS[normalized_network]
        )
        self.timeout = float(timeout)
        self.retries = int(retries)
        self.min_interval = float(min_interval)
        self._opener = opener
        self._sleep = sleep
        self._monotonic = monotonic
        self._now = now
        self._last_request_at: Optional[float] = None
        self._cache: Dict[Tuple[str, str], FetchedResponse] = {}
        self.requests = 0
        self.retry_count = 0
        self.cache_hits = 0

    def fetch(self, tx_hash: str, index: int) -> FetchedResponse:
        action_id = encode_adastat_action_id(tx_hash, index)
        cache_key = (self.network, action_id)
        cached = self._cache.get(cache_key)
        if cached is not None:
            self.cache_hits += 1
            return cached

        url = f"{self.base_url}/gov_actions/{action_id}.json?{urlencode({'currency': 'usd'})}"
        request = Request(
            url,
            headers={
                "Accept": "application/json",
                "Accept-Encoding": "identity",
                "User-Agent": ADASTAT_USER_AGENT,
            },
            method="GET",
        )

        max_attempts = self.retries + 1
        for attempt in range(1, max_attempts + 1):
            self._throttle()
            self.requests += 1
            try:
                response = self._opener(request, timeout=self.timeout)
                try:
                    content_encoding = (response.headers.get("Content-Encoding") or "").lower()
                    if content_encoding not in ("", "identity"):
                        raise ReferenceHttpError(
                            f"unsupported AdaStat content encoding: {content_encoding}"
                        )
                    raw_bytes = response.read()
                finally:
                    response.close()
                raw_text = raw_bytes.decode("utf-8")
            except HTTPError as exc:
                if exc.code in (400, 404):
                    raise ReferenceUnavailableError(
                        f"AdaStat reference unavailable (HTTP {exc.code}): {url}"
                    ) from exc
                if self._is_retryable_status(exc.code) and attempt < max_attempts:
                    self._retry(attempt, exc.headers.get("Retry-After") if exc.headers else None)
                    continue
                raise ReferenceHttpError(
                    f"AdaStat request failed after {attempt} attempt(s) "
                    f"(HTTP {exc.code}): {url}"
                ) from exc
            except (TimeoutError, socket.timeout, URLError) as exc:
                if attempt < max_attempts:
                    self._retry(attempt, None)
                    continue
                raise ReferenceHttpError(
                    f"AdaStat request failed after {attempt} attempt(s): {url}"
                ) from exc
            except UnicodeDecodeError as exc:
                raise ReferenceHttpError("AdaStat response is not valid UTF-8") from exc

            wrapper_code = _response_wrapper_code(raw_text)
            if wrapper_code in (400, 404):
                raise ReferenceUnavailableError(
                    f"AdaStat reference unavailable (response code {wrapper_code}): {url}"
                )
            if wrapper_code is not None and wrapper_code != 200:
                if self._is_retryable_status(wrapper_code) and attempt < max_attempts:
                    self._retry(attempt, None)
                    continue
                raise ReferenceHttpError(
                    f"AdaStat response code {wrapper_code} after {attempt} attempt(s): {url}"
                )

            fetched = FetchedResponse(
                raw_text=raw_text,
                url=url,
                fetched_at=self._now().astimezone(timezone.utc).isoformat(timespec="seconds"),
                attempts=attempt,
            )
            self._cache[cache_key] = fetched
            return fetched

        raise AssertionError("AdaStat retry loop ended unexpectedly")

    def _throttle(self) -> None:
        now = self._monotonic()
        if self._last_request_at is not None:
            remaining = self.min_interval - (now - self._last_request_at)
            if remaining > 0:
                self._sleep(remaining)
                now = self._monotonic()
        self._last_request_at = now

    def _retry(self, attempt: int, retry_after: Optional[str]) -> None:
        self.retry_count += 1
        delay = _retry_after_seconds(retry_after, self._now())
        if delay is None:
            delay = min(2 ** (attempt - 1), 30)
        self._sleep(float(delay))

    @staticmethod
    def _is_retryable_status(status: int) -> bool:
        return status == 429 or 500 <= status <= 599


def validate_adastat_base_url(value: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError("AdaStat base URL must be a non-empty string")
    normalized = value.rstrip("/")
    parsed = urlparse(normalized)
    if (
        parsed.scheme not in ("http", "https")
        or not parsed.hostname
        or parsed.username is not None
        or parsed.password is not None
        or parsed.query
        or parsed.fragment
    ):
        raise ValueError(
            "AdaStat base URL must be an HTTP(S) URL without credentials, query, or fragment"
        )
    return normalized


def resolve_verifier_settings(args: Any) -> VerifierSettings:
    """Resolve Store-only verifier settings with defaults < config < CLI."""

    defaults: Dict[str, Any] = {
        "network": None,
        "adastat_base_url": None,
        "adastat_timeout": DEFAULT_ADASTAT_TIMEOUT,
        "adastat_retries": DEFAULT_ADASTAT_RETRIES,
        "adastat_delay": DEFAULT_ADASTAT_DELAY,
        "store_url": DEFAULT_STORE_URL,
        "store_user": None,
        "store_password": None,
        "store_schema": DEFAULT_STORE_SCHEMA,
        "reports_dir": os.path.join(TOOL_DIR, "reports"),
        "logs_dir": os.path.join(TOOL_DIR, "logs"),
        "quiet": False,
        "max_mismatches": 0,
        "fail_on_inconclusive": False,
        "report_dir": None,
        "result_json": None,
    }
    path_keys = {"reports_dir", "logs_dir", "report_dir", "result_json"}
    config_path = getattr(args, "config", None)

    if config_path:
        loaded = load_config(config_path)
        if not isinstance(loaded, dict):
            raise ValueError("config file must contain a JSON object")
        config_dir = os.path.dirname(os.path.abspath(config_path))
        for key in defaults:
            if key not in loaded:
                continue
            value = loaded[key]
            defaults[key] = resolve_path(value, config_dir) if key in path_keys else value

    for key in defaults:
        cli_value = getattr(args, key, None)
        if cli_value is None:
            continue
        defaults[key] = (
            resolve_path(cli_value, os.getcwd()) if key in path_keys else cli_value
        )

    network = str(defaults["network"] or "").lower()
    if network not in NETWORK_BASE_URLS:
        raise ValueError("network is required and must be one of: mainnet, preprod, preview")

    timeout = _config_float(defaults["adastat_timeout"], "AdaStat timeout")
    retries = _config_non_negative_integer(defaults["adastat_retries"], "AdaStat retries")
    delay = _config_float(defaults["adastat_delay"], "AdaStat delay")
    max_mismatches = _config_non_negative_integer(
        defaults["max_mismatches"] or 0,
        "max mismatches",
    )
    if timeout <= 0:
        raise ValueError("AdaStat timeout must be positive")
    if retries < 0:
        raise ValueError("AdaStat retries must be non-negative")
    if delay < 0:
        raise ValueError("AdaStat delay must be non-negative")
    if max_mismatches < 0:
        raise ValueError("max mismatches must be non-negative")

    for key in ("quiet", "fail_on_inconclusive"):
        if not isinstance(defaults[key], bool):
            raise ValueError(f"{key} must be a boolean")

    store_url = apply_credentials(
        defaults["store_url"],
        defaults["store_user"],
        defaults["store_password"],
    )
    parsed_store_url = urlparse(store_url or "")
    if parsed_store_url.scheme not in ("postgres", "postgresql") or not parsed_store_url.hostname:
        raise ValueError("Store URL must be a postgres:// or postgresql:// URL")

    store_schema = defaults["store_schema"]
    if not isinstance(store_schema, str) or not _SCHEMA_RE.fullmatch(store_schema):
        raise ValueError("Store schema must be a valid PostgreSQL identifier")

    base_url = validate_adastat_base_url(
        defaults["adastat_base_url"] or NETWORK_BASE_URLS[network]
    )
    return VerifierSettings(
        network=network,
        adastat_base_url=base_url,
        adastat_timeout=timeout,
        adastat_retries=retries,
        adastat_delay=delay,
        store_url=store_url,
        store_schema=store_schema,
        reports_dir=resolve_path(defaults["reports_dir"], os.getcwd()),
        logs_dir=resolve_path(defaults["logs_dir"], os.getcwd()),
        quiet=defaults["quiet"],
        max_mismatches=max_mismatches,
        fail_on_inconclusive=defaults["fail_on_inconclusive"],
        report_dir=resolve_path(defaults["report_dir"], os.getcwd()),
        result_json=resolve_path(defaults["result_json"], os.getcwd()),
    )


def load_yaci_proposals(
    connection: Any,
    start_epoch: int,
    end_epoch: Optional[int] = None,
    proposal_filter: Optional[Tuple[str, int]] = None,
) -> Sequence[YaciProposal]:
    """Load each proposal's globally latest status when its epoch is in scope."""

    if (
        isinstance(start_epoch, bool)
        or not isinstance(start_epoch, int)
        or start_epoch < 0
    ):
        raise ValueError("start epoch must be a non-negative integer")
    if end_epoch is None:
        end_epoch = start_epoch
    if (
        isinstance(end_epoch, bool)
        or not isinstance(end_epoch, int)
        or end_epoch < start_epoch
    ):
        raise ValueError("end epoch must be an integer >= start epoch")

    query = """
        WITH latest_status AS (
            SELECT DISTINCT ON (gov_action_tx_hash, gov_action_index)
                   gov_action_tx_hash,
                   gov_action_index,
                   type,
                   status,
                   voting_stats::text AS voting_stats,
                   epoch
            FROM gov_action_proposal_status
    """
    params: list[Any] = []
    if proposal_filter is not None:
        tx_hash, index = proposal_filter
        tx_hash = normalize_hash(tx_hash, "proposal tx hash")
        if isinstance(index, bool) or not isinstance(index, int) or index < 0:
            raise ValueError("proposal index must be a non-negative integer")
        query += " WHERE gov_action_tx_hash = %s AND gov_action_index = %s"
        params.extend((tx_hash, index))
    query += """
            ORDER BY gov_action_tx_hash, gov_action_index, epoch DESC
        )
        SELECT gov_action_tx_hash,
               gov_action_index,
               type,
               status,
               voting_stats,
               epoch
        FROM latest_status
        WHERE epoch BETWEEN %s AND %s
        ORDER BY epoch, gov_action_tx_hash, gov_action_index
    """
    params.extend((start_epoch, end_epoch))

    try:
        with connection.cursor() as cursor:
            cursor.execute(query, tuple(params))
            rows = cursor.fetchall()
    except Exception as exc:
        raise StoreDataError(
            f"failed to load latest Store rows for epochs "
            f"{start_epoch}..{end_epoch}: {exc}"
        ) from exc

    proposals = []
    seen = set()
    for row in rows:
        if not isinstance(row, (tuple, list)) or len(row) != 6:
            raise StoreDataError("Store query returned an unexpected row shape")
        raw_hash, raw_index, raw_type, raw_status, raw_stats, raw_epoch = row
        try:
            tx_hash = normalize_hash(raw_hash, "Store gov_action_tx_hash")
            index = _parse_non_negative_integer(raw_index, "Store gov_action_index")
            row_epoch = _parse_non_negative_integer(raw_epoch, "Store epoch")
            action_type = normalize_action_type(raw_type)
            status = str(raw_status).upper()
            if status not in ("ACTIVE", "RATIFIED", "EXPIRED"):
                raise StoreDataError(f"unsupported Store status: {raw_status!r}")
            voting_stats = parse_yaci_voting_stats(raw_stats)
        except ReferenceContractError as exc:
            raise StoreDataError(
                f"invalid latest Store row in epochs {start_epoch}..{end_epoch}: {exc}"
            ) from exc

        key = (tx_hash, index)
        if key in seen:
            raise StoreDataError(
                f"duplicate latest Store proposal row: {tx_hash}#{index}"
            )
        if row_epoch < start_epoch or row_epoch > end_epoch:
            raise StoreDataError(
                f"Store returned latest epoch {row_epoch} outside "
                f"{start_epoch}..{end_epoch}"
            )
        seen.add(key)
        proposals.append(
            YaciProposal(
                epoch=row_epoch,
                tx_hash=tx_hash,
                index=index,
                action_type=action_type,
                status=status,
                voting_stats=voting_stats,
            )
        )
    return proposals


def save_normalized_reference(
    report_dir: str,
    network: str,
    proposal: YaciProposal,
    fetched: FetchedResponse,
    normalized: NormalizedReference,
) -> str:
    """Persist a credential-free, reproducible normalized reference."""

    action_id = encode_adastat_action_id(proposal.tx_hash, proposal.index)
    reference_dir = os.path.join(report_dir, "references")
    os.makedirs(reference_dir, exist_ok=True)
    path = os.path.join(reference_dir, f"{action_id}.json")
    payload = {
        "network": network,
        "action_id": action_id,
        "source_url": fetched.url,
        "fetched_at": fetched.fetched_at,
        "attempts": fetched.attempts,
        "proposal": {
            "tx_hash": proposal.tx_hash,
            "index": proposal.index,
            "epoch": proposal.epoch,
            "type": proposal.action_type,
            "status": proposal.status,
        },
        "adastat": {
            "tip_epoch": normalized.reference.tip_epoch,
            "tip_slot": normalized.reference.tip_slot,
            "ratified_epoch": normalized.reference.ratified_epoch,
            "expired_epoch": normalized.reference.expired_epoch,
            "bootstrap_period": normalized.reference.bootstrap_period,
        },
        "expected_values": _json_safe(normalized.values),
        "unavailable": dict(normalized.unavailable),
        "diagnostics": _json_safe(normalized.diagnostics),
    }
    with open(path, "w", encoding="utf-8") as output:
        json.dump(payload, output, separators=(",", ":"), sort_keys=True)
        output.write("\n")
    return path


def normalize_action_type(value: Any) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ResponseSchemaError("action type must be a non-empty string")
    token = re.sub(r"[^A-Za-z0-9]", "", value).upper()
    normalized = _ACTION_TYPE_ALIASES.get(token)
    if normalized is None:
        raise ResponseSchemaError(f"unsupported governance action type: {value!r}")
    return normalized


def normalize_hash(value: Any, field: str = "tx_hash") -> str:
    if not isinstance(value, str) or not _HASH_RE.fullmatch(value):
        raise ResponseSchemaError(f"{field} must be a 64-character hexadecimal string")
    return value.lower()


def encode_adastat_action_id(tx_hash: str, index: int) -> str:
    normalized_hash = normalize_hash(tx_hash)
    if isinstance(index, bool) or not isinstance(index, int):
        raise UnsupportedActionIndex("governance action index must be an integer")
    if index < 0 or index > 255:
        raise UnsupportedActionIndex("AdaStat 66-hex action index must be in range 0..255")
    return normalized_hash + format(index, "02x")


def parse_legacy_proposal_id(value: str) -> Tuple[str, int]:
    if not isinstance(value, str) or "#" not in value:
        raise ReferenceContractError("proposal must use TX_HASH#INDEX format")
    tx_hash, index_text = value.rsplit("#", 1)
    normalized_hash = normalize_hash(tx_hash, "proposal tx hash")
    if not _NON_NEGATIVE_INTEGER_RE.fullmatch(index_text):
        raise ReferenceContractError("proposal index must be a non-negative decimal integer")
    index = int(index_text)
    encode_adastat_action_id(normalized_hash, index)
    return normalized_hash, index


def parse_yaci_voting_stats(raw: Any) -> Dict[str, Any]:
    if isinstance(raw, str):
        try:
            parsed = json.loads(raw, parse_float=Decimal, parse_int=int)
        except (json.JSONDecodeError, ValueError) as exc:
            raise ReferenceContractError(f"invalid Yaci voting_stats JSON: {exc}") from exc
    else:
        parsed = raw
    if not isinstance(parsed, dict):
        raise ReferenceContractError("Yaci voting_stats must be a JSON object")
    return dict(parsed)


def parse_adastat_response(raw: Any) -> AdaStatReference:
    if isinstance(raw, (str, bytes, bytearray)):
        try:
            payload = json.loads(raw, parse_float=Decimal, parse_int=int)
        except (json.JSONDecodeError, UnicodeDecodeError, ValueError) as exc:
            raise ResponseSchemaError(f"invalid AdaStat JSON: {exc}") from exc
    else:
        payload = raw

    if not isinstance(payload, dict):
        raise ResponseSchemaError("AdaStat response must be a JSON object")

    code = _required_integer(payload, "code")
    if code != 200:
        raise ResponseSchemaError(f"AdaStat response code is {code}, expected 200")

    data = _required_object(payload, "data")
    tip = _required_object(payload, "tip")

    tx_hash = normalize_hash(_required_value(data, "tx_hash"), "data.tx_hash")
    index = _parse_non_negative_integer(_required_value(data, "index"), "data.index")
    action_type = normalize_action_type(_required_value(data, "type"))

    bootstrap_period = _required_value(data, "bootstrap_period")
    if not isinstance(bootstrap_period, bool):
        raise ResponseSchemaError("data.bootstrap_period must be a boolean")

    return AdaStatReference(
        tx_hash=tx_hash,
        index=index,
        action_type=action_type,
        bootstrap_period=bootstrap_period,
        submission_epoch=_optional_integer(data, "submission_epoch"),
        expiry_epoch=_optional_integer(data, "expiry_epoch"),
        ratified_epoch=_optional_integer(data, "ratified_epoch"),
        enacted_epoch=_optional_integer(data, "enacted_epoch"),
        expired_epoch=_optional_integer(data, "expired_epoch"),
        dropped_epoch=_optional_integer(data, "dropped_epoch"),
        tip_epoch=_required_integer(tip, "epoch_no"),
        tip_slot=_optional_integer(tip, "slot_no"),
        drep=AdaStatDRepInputs(
            total=_optional_integer(data, "drep_total_stake"),
            yes=_optional_integer(data, "drep_yes_stake"),
            no=_optional_integer(data, "drep_no_stake"),
            abstain=_optional_integer(data, "drep_abstain_stake"),
            always_abstain=_optional_integer(data, "drep_always_abstain_stake"),
            always_no_confidence=_optional_integer(data, "drep_always_no_confidence_stake"),
            inactive=_optional_integer(data, "drep_inactive_stake"),
        ),
        spo=AdaStatSPOInputs(
            total=_optional_integer(data, "pool_total_stake"),
            yes=_optional_integer(data, "pool_yes_stake"),
            no=_optional_integer(data, "pool_no_stake"),
            abstain=_optional_integer(data, "pool_abstain_stake"),
            always_abstain=_optional_integer(data, "pool_always_abstain_stake"),
            always_no_confidence=_optional_integer(data, "pool_always_no_confidence_stake"),
        ),
        cc=AdaStatCCInputs(
            total=_optional_integer(data, "cc_total"),
            yes=_optional_integer(data, "cc_yes"),
            no=_optional_integer(data, "cc_no"),
            abstain=_optional_integer(data, "cc_abstain"),
        ),
    )


def check_eligibility(proposal: YaciProposal, reference: AdaStatReference) -> Eligibility:
    if normalize_hash(proposal.tx_hash, "Yaci tx hash") != reference.tx_hash:
        raise IdentityMismatchError("AdaStat tx hash does not match the Yaci row")
    if isinstance(proposal.index, bool) or int(proposal.index) != reference.index:
        raise IdentityMismatchError("AdaStat action index does not match the Yaci row")

    yaci_type = normalize_action_type(proposal.action_type)
    if yaci_type != reference.action_type:
        raise IdentityMismatchError(
            f"AdaStat action type {reference.action_type} does not match Yaci {yaci_type}"
        )

    status = str(proposal.status).upper()
    if status == "ACTIVE":
        return Eligibility(False, "INCONCLUSIVE_LIVE")
    if status == "RATIFIED":
        if reference.ratified_epoch == proposal.epoch:
            return Eligibility(True, None)
        return Eligibility(False, "INCONCLUSIVE_EPOCH_MISMATCH")
    if status == "EXPIRED":
        if reference.expired_epoch == proposal.epoch:
            return Eligibility(True, None)
        return Eligibility(False, "INCONCLUSIVE_EPOCH_MISMATCH")
    return Eligibility(False, "INCONCLUSIVE_UNSUPPORTED_STATUS")


def build_expected_stats(reference: AdaStatReference) -> NormalizedReference:
    values: Dict[str, Union[int, Decimal]] = {}
    unavailable: Dict[str, str] = {}
    diagnostics: Dict[str, Mapping[str, Any]] = {}
    expected_bodies = set(EXPECTED_BODIES[reference.action_type])

    for body, fields in BODY_FIELDS.items():
        if body not in expected_bodies:
            unavailable.update({field: "BODY_UNAVAILABLE" for field in fields})
            diagnostics[body] = {"available": False, "reason": "BODY_UNAVAILABLE"}
            continue

        if body == "drep":
            body_values, body_diagnostics = compute_drep_stats(reference.drep, reference.action_type)
        elif body == "spo":
            body_values, body_diagnostics = compute_spo_stats(
                reference.spo,
                reference.action_type,
                reference.bootstrap_period,
            )
        else:
            body_values, body_diagnostics = compute_cc_stats(reference.cc)

        if body_values is None:
            unavailable.update({field: "BODY_UNAVAILABLE" for field in fields})
            diagnostics[body] = {"available": False, "reason": "BODY_UNAVAILABLE"}
            continue

        values.update(body_values)
        diagnostics[body] = body_diagnostics

    field_decisions = set(values) | set(unavailable)
    if field_decisions != set(ALL_FIELDS):
        missing = sorted(set(ALL_FIELDS) - field_decisions)
        raise ReferenceContractError(f"missing field coverage decisions: {missing}")
    if set(values) & set(unavailable):
        raise ReferenceContractError("a field cannot be both comparable and unavailable")

    return NormalizedReference(
        reference=reference,
        values=values,
        unavailable=unavailable,
        diagnostics=diagnostics,
    )


def compute_drep_stats(
    drep: AdaStatDRepInputs,
    action_type: str,
) -> Tuple[Optional[Dict[str, Union[int, Decimal]]], Mapping[str, Any]]:
    components = (
        drep.total,
        drep.yes,
        drep.no,
        drep.abstain,
        drep.always_abstain,
        drep.always_no_confidence,
        drep.inactive,
    )
    if _body_is_unavailable(components, "DRep"):
        return None, {}

    total, yes, no, abstain, always_abstain, no_confidence, inactive = components
    assert total is not None
    assert yes is not None
    assert no is not None
    assert abstain is not None
    assert always_abstain is not None
    assert no_confidence is not None
    assert inactive is not None

    do_not_vote = total - yes - no - abstain - always_abstain - no_confidence - inactive
    if do_not_vote < 0:
        raise ReferenceContractError(f"negative DRep do-not-vote remainder: {do_not_vote}")

    total_yes = yes + no_confidence if action_type == NO_CONFIDENCE else yes
    total_no = no + do_not_vote
    if action_type != NO_CONFIDENCE:
        total_no += no_confidence
    total_abstain = abstain + always_abstain

    values: Dict[str, Union[int, Decimal]] = {
        "drep_total_yes_stake": total_yes,
        "drep_total_no_stake": total_no,
        "drep_total_abstain_stake": total_abstain,
        "drep_yes_vote_stake": yes,
        "drep_no_vote_stake": no,
        "drep_abstain_vote_stake": abstain,
        "drep_no_confidence_stake": no_confidence,
        "drep_auto_abstain_stake": always_abstain,
        "drep_do_not_vote_stake": do_not_vote,
        "drep_approval_ratio": approval_ratio(total_yes, total_no),
    }
    diagnostics = {
        "available": True,
        "total_stake": total,
        "yes": yes,
        "no": no,
        "abstain": abstain,
        "always_abstain": always_abstain,
        "always_no_confidence": no_confidence,
        "inactive": inactive,
        "derived_do_not_vote": do_not_vote,
    }
    return values, diagnostics


def compute_spo_stats(
    spo: AdaStatSPOInputs,
    action_type: str,
    bootstrap_period: bool,
) -> Tuple[Optional[Dict[str, Union[int, Decimal]]], Mapping[str, Any]]:
    components = (
        spo.total,
        spo.yes,
        spo.no,
        spo.abstain,
        spo.always_abstain,
        spo.always_no_confidence,
    )
    if _body_is_unavailable(components, "SPO"):
        return None, {}

    total, yes, no, abstain, always_abstain, no_confidence = components
    assert total is not None
    assert yes is not None
    assert no is not None
    assert abstain is not None
    assert always_abstain is not None
    assert no_confidence is not None

    do_not_vote = total - yes - no - abstain - always_abstain - no_confidence
    if do_not_vote < 0:
        raise ReferenceContractError(f"negative SPO do-not-vote remainder: {do_not_vote}")

    total_yes = yes
    total_abstain = abstain
    if action_type != HARD_FORK_INITIATION:
        if bootstrap_period:
            total_abstain += always_abstain + no_confidence + do_not_vote
        else:
            total_abstain += always_abstain
            if action_type == NO_CONFIDENCE:
                total_yes += no_confidence

    unclamped_total_no = total - total_yes - total_abstain
    total_no = max(unclamped_total_no, 0)

    values: Dict[str, Union[int, Decimal]] = {
        "spo_total_yes_stake": total_yes,
        "spo_total_no_stake": total_no,
        "spo_total_abstain_stake": total_abstain,
        "spo_yes_vote_stake": yes,
        "spo_no_vote_stake": no,
        "spo_abstain_vote_stake": abstain,
        "spo_do_not_vote_stake": do_not_vote,
        "spo_approval_ratio": approval_ratio(total_yes, total_no),
    }
    diagnostics = {
        "available": True,
        "total_stake": total,
        "yes": yes,
        "no": no,
        "abstain": abstain,
        "always_abstain": always_abstain,
        "always_no_confidence": no_confidence,
        "derived_do_not_vote": do_not_vote,
        "total_no_before_clamp": unclamped_total_no,
        "total_no_clamped": unclamped_total_no < 0,
    }
    return values, diagnostics


def compute_cc_stats(
    cc: AdaStatCCInputs,
) -> Tuple[Optional[Dict[str, Union[int, Decimal]]], Mapping[str, Any]]:
    components = (cc.total, cc.yes, cc.no, cc.abstain)
    if _body_is_unavailable(components, "CC"):
        return None, {}

    total, yes, no, abstain = components
    assert total is not None
    assert yes is not None
    assert no is not None
    assert abstain is not None

    do_not_vote = total - yes - no - abstain
    if do_not_vote < 0:
        raise ReferenceContractError(f"negative CC do-not-vote remainder: {do_not_vote}")

    values: Dict[str, Union[int, Decimal]] = {
        "cc_yes": yes,
        "cc_no": no,
        "cc_do_not_vote": do_not_vote,
        "cc_abstain": abstain,
        "cc_approval_ratio": approval_ratio(yes, no + do_not_vote),
    }
    diagnostics = {
        "available": True,
        "total": total,
        "yes": yes,
        "no": no,
        "abstain": abstain,
        "derived_do_not_vote": do_not_vote,
    }
    return values, diagnostics


def approval_ratio(yes: int, no: int) -> Decimal:
    if yes == 0:
        return Decimal("0").quantize(_RATIO_QUANTUM)
    denominator = yes + no
    if denominator <= 0:
        raise ReferenceContractError("approval-ratio denominator must be positive")
    return (Decimal(yes) / Decimal(denominator)).quantize(
        _RATIO_QUANTUM,
        rounding=ROUND_HALF_UP,
    )


def normalize_actual_value(field: str, value: Any) -> Union[int, Decimal]:
    if field not in ALL_FIELDS:
        raise ReferenceContractError(f"unknown voting_stats field: {field}")
    if field.endswith("_approval_ratio"):
        if isinstance(value, bool) or value is None:
            raise ReferenceContractError(f"{field} must be numeric")
        try:
            decimal_value = value if isinstance(value, Decimal) else Decimal(str(value))
        except (ValueError, TypeError) as exc:
            raise ReferenceContractError(f"{field} must be numeric") from exc
        return decimal_value.quantize(_RATIO_QUANTUM, rounding=ROUND_HALF_UP)
    return _parse_non_negative_integer(value, field)


def _body_is_unavailable(values: Sequence[Optional[int]], body: str) -> bool:
    missing_count = sum(value is None for value in values)
    if missing_count == len(values):
        return True
    if missing_count:
        raise ResponseSchemaError(f"{body} voting body is only partially populated")
    return False


def _required_object(container: Mapping[str, Any], key: str) -> Mapping[str, Any]:
    value = _required_value(container, key)
    if not isinstance(value, dict):
        raise ResponseSchemaError(f"{key} must be a JSON object")
    return value


def _required_value(container: Mapping[str, Any], key: str) -> Any:
    if key not in container:
        raise ResponseSchemaError(f"missing required AdaStat field: {key}")
    return container[key]


def _required_integer(container: Mapping[str, Any], key: str) -> int:
    return _parse_non_negative_integer(_required_value(container, key), key)


def _optional_integer(container: Mapping[str, Any], key: str) -> Optional[int]:
    value = _required_value(container, key)
    if value is None:
        return None
    return _parse_non_negative_integer(value, key)


def _parse_non_negative_integer(value: Any, field: str) -> int:
    if isinstance(value, bool) or isinstance(value, float) or isinstance(value, Decimal):
        raise ResponseSchemaError(f"{field} must be an integer or decimal integer string")
    if isinstance(value, int):
        parsed = value
    elif isinstance(value, str) and _NON_NEGATIVE_INTEGER_RE.fullmatch(value):
        parsed = int(value)
    else:
        raise ResponseSchemaError(f"{field} must be an integer or decimal integer string")
    if parsed < 0:
        raise ResponseSchemaError(f"{field} must be non-negative")
    return parsed


def _response_wrapper_code(raw_text: str) -> Optional[int]:
    try:
        payload = json.loads(raw_text)
    except (json.JSONDecodeError, TypeError):
        return None
    if not isinstance(payload, dict):
        return None
    code = payload.get("code")
    if isinstance(code, bool) or not isinstance(code, int):
        return None
    return code


def _retry_after_seconds(value: Optional[str], now: datetime) -> Optional[float]:
    if not value:
        return None
    stripped = value.strip()
    if _NON_NEGATIVE_INTEGER_RE.fullmatch(stripped):
        return float(stripped)
    try:
        retry_at = parsedate_to_datetime(stripped)
    except (TypeError, ValueError, OverflowError):
        return None
    if retry_at.tzinfo is None:
        retry_at = retry_at.replace(tzinfo=timezone.utc)
    current = now if now.tzinfo is not None else now.replace(tzinfo=timezone.utc)
    return max((retry_at - current).total_seconds(), 0.0)


def _json_safe(value: Any) -> Any:
    if isinstance(value, Decimal):
        return format(value, "f")
    if isinstance(value, Mapping):
        return {key: _json_safe(item) for key, item in value.items()}
    if isinstance(value, (tuple, list)):
        return [_json_safe(item) for item in value]
    return value


def _config_float(value: Any, field: str) -> float:
    if isinstance(value, bool):
        raise ValueError(f"{field} must be a finite number")
    try:
        parsed = float(value)
    except (TypeError, ValueError) as exc:
        raise ValueError(f"{field} must be a finite number") from exc
    if not math.isfinite(parsed):
        raise ValueError(f"{field} must be a finite number")
    return parsed


def _config_non_negative_integer(value: Any, field: str) -> int:
    if isinstance(value, bool):
        raise ValueError(f"{field} must be a non-negative integer")
    if isinstance(value, int):
        parsed = value
    elif isinstance(value, str) and _NON_NEGATIVE_INTEGER_RE.fullmatch(value):
        parsed = int(value)
    else:
        raise ValueError(f"{field} must be a non-negative integer")
    if parsed < 0:
        raise ValueError(f"{field} must be a non-negative integer")
    return parsed
