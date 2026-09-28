#!/usr/bin/env python3
"""Compare /pools endpoints: Yaci vs Blockfrost.

Pool selection precedence:
  1. --pool-id, an explicit bech32 pool id (highest priority)
  2. config/pools.json, the network's pool_id (picked up by run_module)
  3. auto-discovery, the first entry of the local /pools list

Usage:
  export BF_PROJECT_ID_PREPROD=preprod...
  python3 compare_pools.py --network preprod
  python3 compare_pools.py --network mainnet --strict
  python3 compare_pools.py --network preprod --pool-id pool1...
  python3 compare_pools.py --network preprod --csv reports/pools_preprod.csv
  python3 compare_pools.py --network preprod --orders desc --pages 1 --count 20
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, load_config, http_get, NETWORKS, LOCAL_PREFIX, LIST_QUERY

ENDPOINTS = [
    ("/pools",                      True,  []),
    ("/pools/extended",             True,  []),
    ("/pools/retired",              True,  []),
    ("/pools/retiring",             True,  []),
    ("/pools/{pool_id}",            False, ["pool_id"]),
    ("/pools/{pool_id}/metadata",   False, ["pool_id"]),
    ("/pools/{pool_id}/relays",     False, ["pool_id"]),
    ("/pools/{pool_id}/blocks",     True,  ["pool_id"]),
    ("/pools/{pool_id}/updates",    True,  ["pool_id"]),
    ("/pools/{pool_id}/votes",      True,  ["pool_id"]),
    ("/pools/{pool_id}/history",    True,  ["pool_id"]),
    ("/pools/{pool_id}/delegators", True,  ["pool_id"]),
]


def seed_pool_id(net):
    """First pool id from the local /pools list, or None when unreachable or empty."""
    cfg = NETWORKS[net]
    st, pools = http_get(cfg["local"] + LOCAL_PREFIX + f"/pools?{LIST_QUERY}")
    if st == 200 and isinstance(pools, list) and pools:
        return pools[0]
    print(f"  ! cannot seed a pool id from local /pools: {pools}", file=sys.stderr)
    return None


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network",  required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",   action="store_true")
    ap.add_argument("--depth",    type=int,   default=20)
    ap.add_argument("--throttle", type=float, default=0.2)
    ap.add_argument("--csv",      metavar="FILE", default=None,
                    help="Append results to a CSV file (created if absent)")
    ap.add_argument("--orders",   nargs="+", default=["asc", "desc"],
                    choices=["asc", "desc"],
                    help="Order values to test on list endpoints (default: asc desc)")
    ap.add_argument("--pages",    nargs="+", type=int, default=[1, 2],
                    help="Page numbers to test on list endpoints (default: 1 2)")
    ap.add_argument("--count",    type=int, default=100,
                    help="Page size for list endpoints (default: 100)")
    ap.add_argument("--pool-id",  type=str, default="",
                    help="explicit pool bech32 id to test "
                         "(overrides config/pools.json and auto-discovery)")
    a = ap.parse_args()

    param_sets = None
    if a.pool_id.strip():
        # 1. Explicit CLI value wins.
        param_sets = [{"pool_id": a.pool_id.strip()}]
    elif not load_config("pools", a.network).get("pool_id"):
        # 3. Zero-config fallback: take the first pool from the local /pools list.
        # (2. runs when config/pools.json has a pool_id: run_module seeds it itself.)
        seeded = seed_pool_id(a.network)
        if seeded:
            print(f"  seeded pool_id {seeded} from local /pools")
            param_sets = [{"pool_id": seeded}]
        else:
            print("  no pool_id available; per-pool endpoints will be skipped")

    run_module(a.network, "pools", ENDPOINTS, a.strict, a.depth, a.throttle,
               csv_out=a.csv, orders=a.orders, pages=a.pages, count=a.count,
               param_sets=param_sets)
