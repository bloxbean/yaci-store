#!/usr/bin/env python3
"""Compare /governance/dreps and /governance/proposals: Yaci vs Blockfrost.

Blockfrost project IDs and endpoint overrides are loaded from .env by
bf_compare. Governance identifiers come from config/governance.json first and
are otherwise discovered from the local Yaci governance list endpoints.
"""
import argparse
import sys

sys.path.insert(0, __file__.rsplit("/", 1)[0])

from bf_compare import LOCAL_PREFIX, NETWORKS, http_get, load_config, run_module


# The optional fourth tuple item is the canonical label written to terminal and
# CSV output. It keeps proposal-category seed keys out of user-facing paths.
ENDPOINTS = [
    ("/governance/dreps", True, []),
    ("/governance/dreps/{drep_id}", False, ["drep_id"]),
    ("/governance/dreps/{drep_id}/delegators", True, ["drep_id"]),
    ("/governance/dreps/{drep_id}/metadata", False, ["drep_id"]),
    ("/governance/dreps/{drep_id}/updates", True, ["drep_id"]),
    ("/governance/dreps/{drep_id}/votes", True, ["drep_id"]),
    ("/governance/proposals", True, []),
    (
        "/governance/proposals/{proposal_tx_hash}/{proposal_cert_index}",
        False,
        ["proposal_tx_hash", "proposal_cert_index"],
        "/governance/proposals/{tx_hash}/{cert_index}",
    ),
    (
        "/governance/proposals/{proposal_gov_action_id}",
        False,
        ["proposal_gov_action_id"],
        "/governance/proposals/{gov_action_id}",
    ),
    (
        "/governance/proposals/{parameter_tx_hash}/{parameter_cert_index}/parameters",
        False,
        ["parameter_tx_hash", "parameter_cert_index"],
        "/governance/proposals/{tx_hash}/{cert_index}/parameters",
    ),
    (
        "/governance/proposals/{parameter_gov_action_id}/parameters",
        False,
        ["parameter_gov_action_id"],
        "/governance/proposals/{gov_action_id}/parameters",
    ),
    (
        "/governance/proposals/{withdrawal_tx_hash}/{withdrawal_cert_index}/withdrawals",
        True,
        ["withdrawal_tx_hash", "withdrawal_cert_index"],
        "/governance/proposals/{tx_hash}/{cert_index}/withdrawals",
    ),
    (
        "/governance/proposals/{withdrawal_gov_action_id}/withdrawals",
        True,
        ["withdrawal_gov_action_id"],
        "/governance/proposals/{gov_action_id}/withdrawals",
    ),
    (
        "/governance/proposals/{proposal_tx_hash}/{proposal_cert_index}/votes",
        True,
        ["proposal_tx_hash", "proposal_cert_index"],
        "/governance/proposals/{tx_hash}/{cert_index}/votes",
    ),
    (
        "/governance/proposals/{proposal_gov_action_id}/votes",
        True,
        ["proposal_gov_action_id"],
        "/governance/proposals/{gov_action_id}/votes",
    ),
    (
        "/governance/proposals/{metadata_tx_hash}/{metadata_cert_index}/metadata",
        False,
        ["metadata_tx_hash", "metadata_cert_index"],
        "/governance/proposals/{tx_hash}/{cert_index}/metadata",
    ),
    (
        "/governance/proposals/{metadata_gov_action_id}/metadata",
        False,
        ["metadata_gov_action_id"],
        "/governance/proposals/{gov_action_id}/metadata",
    ),
]


def _proposal_ref(value):
    """Normalize a proposal config/list item to the three path identifiers."""
    if not isinstance(value, dict):
        return {}
    return {
        "tx_hash": value.get("tx_hash"),
        "cert_index": value.get("cert_index"),
        "gov_action_id": value.get("gov_action_id") or value.get("id"),
    }


def _has_ref(ref):
    return bool(ref.get("tx_hash")) and ref.get("cert_index") is not None and bool(ref.get("gov_action_id"))


def _put_ref(params, prefix, ref):
    if ref.get("tx_hash"):
        params[f"{prefix}_tx_hash"] = ref["tx_hash"]
    if ref.get("cert_index") is not None:
        params[f"{prefix}_cert_index"] = ref["cert_index"]
    if ref.get("gov_action_id"):
        params[f"{prefix}_gov_action_id"] = ref["gov_action_id"]


def _local_list(net, path, pages=1):
    """Read one or more descending pages from a local governance list."""
    base = NETWORKS[net]["local"] + LOCAL_PREFIX
    rows = []
    for page in range(1, pages + 1):
        status, body = http_get(base + path + f"?count=100&page={page}&order=desc")
        if status != 200 or not isinstance(body, list):
            print(f"  ! cannot seed {path} page {page}: status={status} body={str(body)[:240]}",
                  file=sys.stderr)
            break
        rows.extend(item for item in body if isinstance(item, dict))
        if len(body) < 100:
            break
    return rows


def seed_governance(net, seed_pages=10):
    """Load pinned governance IDs, filling missing samples from local lists."""
    config = load_config("governance", net)
    params = {"sample": "governance"}

    drep_id = config.get("drep_id")
    if not drep_id:
        dreps = _local_list(net, "/governance/dreps")
        if dreps:
            drep_id = dreps[0].get("drep_id") or dreps[0].get("hex")
    if drep_id:
        params["drep_id"] = drep_id

    refs = {
        "proposal": _proposal_ref(config.get("proposal")),
        "parameter": _proposal_ref(config.get("parameter_proposal")),
        "withdrawal": _proposal_ref(config.get("withdrawal_proposal")),
        "metadata": _proposal_ref(config.get("metadata_proposal")),
    }

    if not all(_has_ref(ref) for ref in refs.values()):
        proposals = _local_list(net, "/governance/proposals", pages=seed_pages)
        for item in proposals:
            ref = _proposal_ref(item)
            if not _has_ref(ref):
                continue
            governance_type = str(item.get("governance_type") or "").lower()
            if not _has_ref(refs["proposal"]):
                refs["proposal"] = ref
            if governance_type == "parameter_change" and not _has_ref(refs["parameter"]):
                refs["parameter"] = ref
            if governance_type == "treasury_withdrawals" and not _has_ref(refs["withdrawal"]):
                refs["withdrawal"] = ref

    # Metadata exists conceptually for every proposal anchor. A pinned metadata
    # sample can override this when a known successfully-fetched anchor is needed.
    if not _has_ref(refs["metadata"]) and _has_ref(refs["proposal"]):
        refs["metadata"] = refs["proposal"]

    for prefix, ref in refs.items():
        _put_ref(params, prefix, ref)

    drep_display = str(params.get("drep_id") or "")
    proposal_display = str(params.get("proposal_gov_action_id") or "")
    print(f"  governance seed: drep={drep_display[:24]}…  proposal={proposal_display[:24]}…")
    if not _has_ref(refs["parameter"]):
        print("  ! no parameter_change proposal found; parameter endpoints will be skipped")
    if not _has_ref(refs["withdrawal"]):
        print("  ! no treasury_withdrawals proposal found; withdrawal endpoints will be skipped")
    return params


def _positive_int(value):
    parsed = int(value)
    if parsed < 1:
        raise argparse.ArgumentTypeError("must be at least 1")
    return parsed


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict", action="store_true")
    ap.add_argument("--depth", type=int, default=20)
    ap.add_argument("--throttle", type=float, default=0.2)
    ap.add_argument("--csv", metavar="FILE", default=None,
                    help="append results to a CSV file (created if absent)")
    ap.add_argument("--orders", nargs="+", default=["asc", "desc"],
                    choices=["asc", "desc"],
                    help="order values to test on list endpoints (default: asc desc)")
    ap.add_argument("--pages", nargs="+", type=_positive_int, default=[1, 2],
                    help="page numbers to test on list endpoints (default: 1 2)")
    ap.add_argument("--count", type=_positive_int, default=100,
                    help="page size for list endpoints (default: 100)")
    ap.add_argument("--seed-pages", type=_positive_int, default=10,
                    help="local proposal pages to scan for parameter/withdrawal samples (default: 10)")
    ap.add_argument("--drep-id", default="",
                    help="override the DRep sample from config/auto-discovery")
    ap.add_argument("--tx-hash", default="",
                    help="override the generic proposal transaction hash")
    ap.add_argument("--cert-index", type=int, default=None,
                    help="override the generic proposal certificate index")
    ap.add_argument("--gov-action-id", default="",
                    help="override the generic CIP-0129 governance action ID")
    args = ap.parse_args()

    governance_params = seed_governance(args.network, args.seed_pages)
    if args.drep_id:
        governance_params["drep_id"] = args.drep_id

    proposal_overridden = bool(args.tx_hash or args.gov_action_id or args.cert_index is not None)
    if proposal_overridden:
        for key in ("proposal_tx_hash", "proposal_cert_index", "proposal_gov_action_id"):
            governance_params.pop(key, None)
        if args.tx_hash:
            governance_params["proposal_tx_hash"] = args.tx_hash
        if args.cert_index is not None:
            governance_params["proposal_cert_index"] = args.cert_index
        if args.gov_action_id:
            governance_params["proposal_gov_action_id"] = args.gov_action_id

    if args.count > 100:
        ap.error("--count must not exceed Blockfrost's maximum of 100")

    run_module(
        args.network,
        "governance",
        ENDPOINTS,
        args.strict,
        args.depth,
        args.throttle,
        csv_out=args.csv,
        orders=args.orders,
        pages=args.pages,
        count=args.count,
        param_sets=[governance_params],
    )
