#!/usr/bin/env python3
"""Compare /network + /genesis endpoints: Yaci vs Blockfrost.

The root endpoint ("/") is intentionally excluded. It reports this instance's own
url (hostname + apiPrefix) and a shim-local version string (see
BFNetworkController.getRoot / BFNetworkService), neither of which can match
Blockfrost's root response, so comparing it only ever yields a guaranteed-false
mismatch. The substantive comparisons are /genesis, /network, and /network/eras.
"""
import argparse, sys
sys.path.insert(0, __file__.rsplit("/", 1)[0])
from bf_compare import run_module, NETWORKS

# Every endpoint is a single-object (or fixed-array) response with no pagination,
# so all entries are is_list=False. "/network/eras" returns a fixed JSON array
# that Blockfrost does not paginate, so it is compared once (order/page/count do
# not apply). The root "/" endpoint is deliberately omitted (see module docstring).
ENDPOINTS = [
    ("/genesis",       False, []),
    ("/network",       False, []),
    ("/network/eras",  False, []),
]

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", required=True, choices=list(NETWORKS))
    ap.add_argument("--strict",  action="store_true")
    ap.add_argument("--depth",   type=int,   default=20)
    ap.add_argument("--throttle",type=float, default=0.2)
    a = ap.parse_args()
    run_module(a.network, "network", ENDPOINTS, a.strict, a.depth, a.throttle)
