#!/usr/bin/env python3
"""
Generate the CIP-113 fixture of a network from the chain, through Koios.

The expected values come from the chain, not from the registry's database, so the suite checks the indexer against an
independent source. For each live node of the official CIP-113 registry, the generator reads the RegistryNode datum the
way assets-ext's Cip113RegistryNodeParser does, and lists the tokens minted under the registered policy, which the API
serves as PROGRAMMABLE.

Usage:
    uv run python shared/generate_cip113_fixtures.py mainnet
    uv run python shared/generate_cip113_fixtures.py preprod

Writes <network>/fixtures/cip113_tokens.json. Regenerate it on purpose, when a policy is registered or a registration
changes, never as part of a test run: the committed file is the baseline.

Environment variables:
    MAX_SUBJECTS_PER_POLICY (default: 25)
"""

import json
import os
import sys
import time
import urllib.request
from datetime import datetime, timezone

NETWORKS = {
    # Registry NFT policy ids of the official deployments, as bundled in src/main/resources/cip113/
    "mainnet": ("https://api.koios.rest/api/v1", "484e733d122af44e6101988bcc47ed261a7af5c43c797e89d60e3075"),
    "preprod": ("https://preprod.koios.rest/api/v1", "3083d387537f9318b6ff5aeab7de5f5629d26495319b8fafd409222a"),
}
HASH_HEX_LEN = 56
REFERENCE_NFT_PREFIX = "000643b0"
MAX_SUBJECTS_PER_POLICY = int(os.getenv("MAX_SUBJECTS_PER_POLICY", "25"))


def koios(base, path, body=None):
    for attempt in range(5):
        try:
            data = json.dumps(body).encode() if body is not None else None
            request = urllib.request.Request(f"{base}/{path}", data=data,
                                             headers={"content-type": "application/json", "accept": "application/json"})
            with urllib.request.urlopen(request, timeout=120) as response:
                return json.load(response)
        except Exception as e:  # Koios is a public service: retry a few times on a transient error
            if attempt == 4:
                raise
            print(f"  retrying {path} after {e}", file=sys.stderr)
            time.sleep(2 * (attempt + 1))


class InvalidDatum(Exception):
    pass


def optional_credential(field, name):
    """A credential field: a Credential Constr (alt 0 or 1) over a 28-byte hash, or empty bytes for none."""
    if "bytes" in field:
        if field["bytes"] == "":
            return None
        raise InvalidDatum(f"{name} is non-empty bytes")
    if field.get("constructor") not in (0, 1) or len(field.get("fields", [])) != 1 or "bytes" not in field["fields"][0]:
        raise InvalidDatum(f"{name} is not a Credential")
    value = field["fields"][0]["bytes"].lower()
    if value == "":
        return None
    if len(value) != HASH_HEX_LEN:
        raise InvalidDatum(f"{name} is not a 28-byte hash")
    return value


def parse_registry_node(value):
    """The RegistryNode datum, Constr 0 [key, next, 4 credentials, global_state_cs], as the API serves it."""
    if value.get("constructor") != 0 or len(value.get("fields", [])) != 7:
        raise InvalidDatum("not a 7-field RegistryNode")
    fields = value["fields"]
    if "bytes" not in fields[0] or "bytes" not in fields[1] or fields[1]["bytes"] == "":
        raise InvalidDatum("bad key or next")
    global_state = fields[6].get("bytes")
    if global_state is None or len(global_state) not in (0, HASH_HEX_LEN):
        raise InvalidDatum("bad global_state_cs")
    return fields[0]["bytes"].lower(), {
        "minting_logic_script": optional_credential(fields[2], "minting_logic_script"),
        "transfer_logic_script": optional_credential(fields[3], "transfer_logic_script"),
        "third_party_transfer_logic_script": optional_credential(fields[4], "third_party_logic_script"),
        "unfracking_logic_script": optional_credential(fields[5], "unfracking_logic_script"),
        "global_state_policy_id": global_state.lower() or None,
    }


def metadata_hints(base, policy_id, assets):
    """
    The subjects of a policy, each with whether Koios sees CIP-26 or CIP-68 metadata for it: a subject without any is
    served with the companion_metadata error (shared/cip113_suite.py).
    """
    info = {}
    for i in range(0, len(assets), 50):
        for a in koios(base, "asset_info", {"_asset_list": [[policy_id, x["asset_name"]] for x in assets[i:i + 50]]}):
            info[a["asset_name"]] = a
    return [{
        "subject": policy_id + a["asset_name"],
        "koios_cip26": bool(info.get(a["asset_name"], {}).get("token_registry_metadata")),
        "koios_cip68_labels": sorted((info.get(a["asset_name"], {}).get("cip68_metadata") or {}).keys()),
    } for a in assets]


def main():
    network = sys.argv[1] if len(sys.argv) > 1 else "mainnet"
    base, registry_policy = NETWORKS[network]

    nodes = [a for a in koios(base, f"policy_asset_info?_asset_policy={registry_policy}") if int(a["total_supply"]) > 0]
    # in chunks: Koios answers a long _asset_list with 413 Request Entity Too Large
    utxos = []
    for i in range(0, len(nodes), 25):
        utxos += koios(base, "asset_utxos", {"_asset_list": [[registry_policy, n["asset_name"]] for n in nodes[i:i + 25]],
                                             "_extended": True})
    policies, skipped = [], []
    for utxo in sorted(utxos, key=lambda u: (u["tx_hash"], u["tx_index"])):
        datum = (utxo.get("inline_datum") or {}).get("value")
        try:
            key, cip113 = parse_registry_node(datum or {})
        except InvalidDatum as e:
            skipped.append({"utxo": f"{utxo['tx_hash']}#{utxo['tx_index']}", "reason": str(e)})
            continue
        if len(key) != HASH_HEX_LEN:
            continue  # head or tail sentinel, not a registered policy
        # A CIP-68 reference NFT is never a subject of the API: its fungible token is
        assets = [a for a in koios(base, f"policy_asset_info?_asset_policy={key}")
                  if int(a.get("total_supply") or 0) > 0 and not a["asset_name"].startswith(REFERENCE_NFT_PREFIX)]
        assets = sorted(assets, key=lambda a: a["asset_name"])[:MAX_SUBJECTS_PER_POLICY]
        policies.append({"policy_id": key, "registry_utxo": f"{utxo['tx_hash']}#{utxo['tx_index']}",
                         "cip113": cip113, "subjects": metadata_hints(base, key, assets)})
        print(f"  {key}: {len(assets)} live tokens", file=sys.stderr)

    fixture = {
        "network": network,
        "registry_policy_id": registry_policy,
        "generated_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "source": base,
        "policies": sorted(policies, key=lambda p: p["policy_id"]),
        "skipped_registry_nodes": skipped,
    }
    path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), network, "fixtures",
                        "cip113_tokens.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(fixture, f, indent=2)
        f.write("\n")
    print(f"{network}: {len(policies)} registered policies, {sum(len(p['subjects']) for p in policies)} subjects, "
          f"{len(skipped)} malformed nodes -> {path}")


if __name__ == "__main__":
    main()
