#!/usr/bin/env python3
"""Compare /assets endpoints: Yaci vs Blockfrost."""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

ENDPOINTS = [
    ("/assets",                      True,  []),
    ("/assets/{asset}",              False, ["asset"]),
    ("/assets/{asset}/history",      True,  ["asset"]),
    ("/assets/{asset}/txs",          True,  ["asset"]),
    ("/assets/{asset}/transactions", True,  ["asset"]),
    ("/assets/{asset}/addresses",    True,  ["asset"]),
    ("/assets/policy/{policy_id}",   True,  ["policy_id"]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    a = ap.parse_args()
    run_module(a.network, "asset", ENDPOINTS, a.strict, a.depth, a.throttle)
