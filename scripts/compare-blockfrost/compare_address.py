#!/usr/bin/env python3
"""Compare /addresses endpoints: Yaci vs Blockfrost."""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

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
    a = ap.parse_args()
    run_module(a.network, "address", ENDPOINTS, a.strict, a.depth, a.throttle)
