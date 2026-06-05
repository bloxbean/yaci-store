#!/usr/bin/env python3
"""Compare /addresses endpoints: Yaci vs Blockfrost.

By default this seeds several distinct addresses from the local chain so the
comparison has multiple datasets (more cases / more evidence). Use --samples to
change how many, or --addresses to test an explicit comma-separated list.
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, seed_addresses, load_config, NETWORKS

ENDPOINTS = [
    ("/addresses/{address}",              False, ["address"]),
    ("/addresses/{address}/extended",     False, ["address"]),
    ("/addresses/{address}/total",        False, ["address"]),
    ("/addresses/{address}/utxos",        True,  ["address"]),
    ("/addresses/{address}/utxos/{asset}", True, ["address", "asset"]),
    ("/addresses/{address}/transactions", True,  ["address"]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    ap.add_argument("--samples", type=int,   default=5,
                    help="number of addresses to auto-discover and test (default 5)")
    ap.add_argument("--addresses", type=str, default="",
                    help="comma-separated explicit addresses to test "
                         "(overrides --samples auto-discovery)")
    a = ap.parse_args()

    if a.addresses.strip():
        # 1. Explicit CLI list wins.
        param_sets = [{"address": x.strip()} for x in a.addresses.split(",") if x.strip()]
    else:
        cfg = load_config("address", a.network)
        cfg_addrs = cfg.get("addresses") or []
        if cfg_addrs:
            # 2. Addresses from config/address.json. Entries may be plain
            #    strings or objects; strings inherit the network's default asset.
            param_sets = []
            for item in cfg_addrs:
                if isinstance(item, dict):
                    param_sets.append(item)
                else:
                    ps = {"address": item}
                    if cfg.get("asset"):
                        ps["asset"] = cfg["asset"]
                    if cfg.get("policy_id"):
                        ps["policy_id"] = cfg["policy_id"]
                    param_sets.append(ps)
            print(f"  using {len(param_sets)} address(es) from config/address.json [{a.network}]")
        else:
            # 3. Zero-config fallback: auto-discover from the local chain.
            param_sets = seed_addresses(a.network, NETWORKS[a.network],
                                        want=a.samples, depth=a.depth)
            if not param_sets:
                sys.exit("Could not seed any addresses from the local chain.")

    run_module(a.network, "address", ENDPOINTS, a.strict, a.depth, a.throttle,
               param_sets=param_sets)
