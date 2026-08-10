#!/usr/bin/env python3
"""Compare /metadata/txs endpoints: Yaci vs Blockfrost."""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

ENDPOINTS = [
    ("/metadata/txs/labels",               True, []),
    ("/metadata/txs/labels/{label}",       True, ["label"]),
    ("/metadata/txs/labels/{label}/cbor",  True, ["label"]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    a = ap.parse_args()
    run_module(a.network, "metadata", ENDPOINTS, a.strict, a.depth, a.throttle)
