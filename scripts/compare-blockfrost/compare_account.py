#!/usr/bin/env python3
"""Compare /accounts endpoints: Yaci vs Blockfrost.

NOTE: requires store.account + store.adapot synced from genesis.
      Fill stake_address/pool_id in bf_compare.PARAMS for each network.
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

ENDPOINTS = [
    ("/accounts/{stake_address}",                       False, ["stake_address"]),
    ("/accounts/{stake_address}/rewards",               True,  ["stake_address"]),
    ("/accounts/{stake_address}/history",               True,  ["stake_address"]),
    ("/accounts/{stake_address}/delegations",           True,  ["stake_address"]),
    ("/accounts/{stake_address}/registrations",         True,  ["stake_address"]),
    ("/accounts/{stake_address}/withdrawals",           True,  ["stake_address"]),
    ("/accounts/{stake_address}/mirs",                  True,  ["stake_address"]),
    ("/accounts/{stake_address}/addresses",             True,  ["stake_address"]),
    ("/accounts/{stake_address}/addresses/assets",      True,  ["stake_address"]),
    ("/accounts/{stake_address}/addresses/total",       False, ["stake_address"]),
    ("/accounts/{stake_address}/utxos",                 True,  ["stake_address"]),
    ("/accounts/{stake_address}/transactions",          True,  ["stake_address"]),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    a = ap.parse_args()
    run_module(a.network, "account", ENDPOINTS, a.strict, a.depth, a.throttle)
