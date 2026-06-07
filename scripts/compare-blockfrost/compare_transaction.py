#!/usr/bin/env python3
"""Compare /txs endpoints: Yaci vs Blockfrost."""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

ENDPOINTS = [
    ("/txs/{tx_hash}",                   False, ["tx_hash"]),
    ("/txs/{tx_hash}/utxos",             False, ["tx_hash"]),
    ("/txs/{tx_hash}/metadata",          True,  ["tx_hash"]),
    ("/txs/{tx_hash}/redeemers",         True,  ["tx_hash"]),
    ("/txs/{tx_hash}/stakes",            True,  ["tx_hash"]),
    ("/txs/{tx_hash}/delegations",       True,  ["tx_hash"]),
    ("/txs/{tx_hash}/withdrawals",       True,  ["tx_hash"]),
    ("/txs/{tx_hash}/mirs",              True,  ["tx_hash"]),
    ("/txs/{tx_hash}/pool_updates",      True,  ["tx_hash"]),
    ("/txs/{tx_hash}/pool_retires",      True,  ["tx_hash"]),
    ("/txs/{tx_hash}/required_signers",  True,  ["tx_hash"]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    a = ap.parse_args()
    run_module(a.network, "transaction", ENDPOINTS, a.strict, a.depth, a.throttle)
