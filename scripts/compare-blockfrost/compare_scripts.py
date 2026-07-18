#!/usr/bin/env python3
"""Compare /scripts endpoints: Yaci vs Blockfrost.

NOTE: fill script_hash and datum_hash in bf_compare.PARAMS for each network.
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

ENDPOINTS = [
    ("/scripts/{script_hash}",            False, ["script_hash"]),
    ("/scripts/{script_hash}/json",       False, ["script_hash"]),
    ("/scripts/{script_hash}/cbor",       False, ["script_hash"]),
    ("/scripts/{script_hash}/redeemers",  True,  ["script_hash"]),
    ("/scripts/datum/{datum_hash}",       False, ["datum_hash"]),
    ("/scripts/datum/{datum_hash}/cbor",  False, ["datum_hash"]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    a = ap.parse_args()
    run_module(a.network, "scripts", ENDPOINTS, a.strict, a.depth, a.throttle)
