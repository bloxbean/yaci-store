#!/usr/bin/env python3
"""Compare /epochs endpoints: Yaci vs Blockfrost.

Usage:
  export BF_PROJECT_ID_PREPROD=preprod...
  python3 compare_epoch.py --network preprod
  python3 compare_epoch.py --network mainnet --strict
  python3 compare_epoch.py --network preprod --csv reports/epoch_preprod.csv
  python3 compare_epoch.py --network preprod --orders asc desc --pages 1 2 3
  python3 compare_epoch.py --network preprod --orders desc --pages 1 --count 20
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

ENDPOINTS = [
    ("/epochs/latest",                      False, []),
    ("/epochs/latest/parameters",           False, []),
    ("/epochs/{epoch}",                     False, ["epoch"]),
    ("/epochs/{epoch}/parameters",          False, ["epoch"]),
    ("/epochs/{epoch}/next",                True,  ["epoch"]),
    ("/epochs/{epoch}/previous",            True,  ["epoch"]),
    ("/epochs/{epoch}/stakes",              True,  ["epoch"]),
    ("/epochs/{epoch}/stakes/{pool_id}",    True,  ["epoch", "pool_id"]),
    ("/epochs/{epoch}/blocks",              True,  ["epoch"]),
    ("/epochs/{epoch}/blocks/{pool_id}",    True,  ["epoch", "pool_id"]),
]

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
    a = ap.parse_args()
    run_module(a.network, "epoch", ENDPOINTS, a.strict, a.depth, a.throttle,
               csv_out=a.csv, orders=a.orders, pages=a.pages, count=a.count)
