#!/usr/bin/env python3
"""Compare /assets endpoints: Yaci vs Blockfrost.

By default this seeds several distinct assets from the local chain so the
comparison has multiple datasets (more cases / more evidence). Use --samples to
change how many, or --assets to test an explicit comma-separated list of units.

The network-global ``/assets`` list endpoint is asset-independent, so it is
exercised once (not per sample). All other endpoints are asset/policy-scoped and
run once per seeded asset.
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, seed_assets, NETWORKS

# Asset/policy-scoped endpoints — run once per seeded asset.
ENDPOINTS = [
    ("/assets/{asset}",              False, ["asset"]),
    ("/assets/{asset}/history",      True,  ["asset"]),
    ("/assets/{asset}/txs",          True,  ["asset"]),
    ("/assets/{asset}/transactions", True,  ["asset"]),
    ("/assets/{asset}/addresses",    True,  ["asset"]),
    ("/assets/policy/{policy_id}",   True,  ["policy_id"]),
]

# Network-global list endpoint — asset-independent, tested once.
GLOBAL_ENDPOINTS = [
    ("/assets",                      True,  []),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    ap.add_argument("--samples", type=int,   default=5,
                    help="number of assets to auto-discover and test (default 5)")
    ap.add_argument("--assets", type=str, default="",
                    help="comma-separated explicit asset units to test "
                         "(overrides --samples auto-discovery)")
    ap.add_argument("--no-list", action="store_true",
                    help="skip the network-global /assets list endpoint")
    a = ap.parse_args()

    if a.assets.strip():
        param_sets = [{"asset": x.strip(), "policy_id": x.strip()[:56]}
                      for x in a.assets.split(",") if x.strip()]
    else:
        param_sets = seed_assets(a.network, NETWORKS[a.network],
                                 want=a.samples, depth=a.depth)
        if not param_sets:
            sys.exit("Could not seed any assets from the local chain.")

    # Per-asset endpoints across all seeded datasets.
    run_module(a.network, "asset", ENDPOINTS, a.strict, a.depth, a.throttle,
               param_sets=param_sets)

    # Global /assets list once (unless suppressed).
    if not a.no_list:
        run_module(a.network, "asset", GLOBAL_ENDPOINTS, a.strict, a.depth, a.throttle)
