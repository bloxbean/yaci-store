#!/usr/bin/env python3
"""
Cross-check yaci-store's stored transaction counts against Koios.

Catches blocks whose transactions were dropped during decoding, fully or partly.
Koios is an independent implementation, so an exact match is real evidence that
no transactions are missing.

Epoch mode (default). Compares per-epoch tx and block counts using one Koios
request. The last epoch on stdin is skipped by default, because it is usually
still in progress on one side or the other.

  db.sh --host H --container C --csv -- \
    "select epoch, sum(no_of_txs) txs, count(*) blks
       from block where era >= 2 group by epoch order by epoch;" \
  | koios_tx_count_check.py --network mainnet

Block mode (--blocks). Compares per block, to pinpoint the block(s) in an epoch
that epoch mode flagged. Koios is paged 1000 blocks per request.

  db.sh --host H --container C --csv -- \
    "select number, no_of_txs from block where epoch = 660 order by number;" \
  | koios_tx_count_check.py --network mainnet --blocks

Use `era >= 2`: yaci-store records no_of_txs = 0 for every Byron block by design.

Exit 0 = all compared counts agree, 1 = at least one mismatch, 2 = usage/fetch error.
"""
import argparse, csv, json, sys, urllib.request, urllib.error

BASES = {
    "mainnet": "https://api.koios.rest/api/v1",
    "preprod": "https://preprod.koios.rest/api/v1",
    "preview": "https://preview.koios.rest/api/v1",
}
PAGE = 1000  # Koios maximum rows per response


def get_json(url, timeout):
    """GET a Koios URL as JSON.

    Tries urllib first, then falls back to curl. Python on macOS ships its own
    CA bundle and frequently cannot verify public certs, while curl uses the
    system store — so the fallback is the common path there, not an edge case.
    """
    try:
        import ssl
        ctx = None
        try:
            import certifi
            ctx = ssl.create_default_context(cafile=certifi.where())
        except ImportError:
            pass
        req = urllib.request.Request(url, headers={"Accept": "application/json"})
        with urllib.request.urlopen(req, timeout=timeout, context=ctx) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        sys.exit(f"koios HTTP {e.code} from {url}: {e.reason}")
    except Exception:
        import shutil, subprocess
        if not shutil.which("curl"):
            sys.exit(f"koios fetch failed from {url} and curl is unavailable")
        try:
            out = subprocess.run(
                ["curl", "-sSf", "--max-time", str(timeout), "-H", "Accept: application/json", url],
                capture_output=True, text=True, check=True).stdout
            return json.loads(out)
        except subprocess.CalledProcessError as e:
            sys.exit(f"koios fetch failed from {url}: curl: {e.stderr.strip()}")
        except json.JSONDecodeError as e:
            sys.exit(f"koios returned non-JSON from {url}: {e}")


def get_paged(url, timeout):
    rows, offset = [], 0
    while True:
        page = get_json(f"{url}&limit={PAGE}&offset={offset}", timeout)
        rows.extend(page)
        if len(page) < PAGE:
            return rows
        offset += PAGE


def check_epochs(rows, base, timeout, include_last):
    yaci = {int(r["epoch"]): (int(r["txs"]), int(r["blks"])) for r in rows}
    last = max(yaci)
    if not include_last:
        del yaci[last]
        print(f"epoch {last}: skipped as possibly in progress (use --include-last to compare it)")
    if not yaci:
        sys.exit("no closed epochs left to compare")

    lo, hi = min(yaci), max(yaci)
    koios = {int(e["epoch_no"]): (int(e["tx_count"]), int(e["blk_count"]))
             for e in get_paged(f"{base}/epoch_info?select=epoch_no,tx_count,blk_count"
                                f"&epoch_no=gte.{lo}&epoch_no=lte.{hi}&order=epoch_no.asc", timeout)}

    bad = skipped = 0
    for ep in sorted(yaci):
        if ep not in koios:
            print(f"epoch {ep}: not in Koios — skipped")
            skipped += 1
            continue
        (yt, yb), (kt, kb) = yaci[ep], koios[ep]
        if (yt, yb) != (kt, kb):
            bad += 1
            print(f"DIFF epoch {ep}: txs yaci={yt} koios={kt} ({yt - kt:+d})  "
                  f"blocks yaci={yb} koios={kb} ({yb - kb:+d})")
    compared = len(yaci) - skipped
    total = sum(yaci[e][0] for e in yaci if e in koios)
    print(f"\nepochs {lo}..{hi}: compared={compared} txs={total}")
    if bad:
        print("next: rerun with --blocks for each DIFF epoch to find the block(s)")
    return bad, skipped


def check_blocks(rows, base, timeout):
    yaci = {int(r["number"]): int(r["no_of_txs"]) for r in rows}
    lo, hi = min(yaci), max(yaci)
    koios = {int(b["block_height"]): int(b["tx_count"])
             for b in get_paged(f"{base}/blocks?select=block_height,tx_count"
                                f"&block_height=gte.{lo}&block_height=lte.{hi}"
                                f"&order=block_height.asc", timeout)}

    bad = 0
    for n in sorted(set(yaci) | set(koios)):
        if n not in yaci:
            print(f"MISSING block {n}: in Koios ({koios[n]} txs), not in yaci")
            bad += 1
        elif n not in koios:
            print(f"EXTRA   block {n}: in yaci ({yaci[n]} txs), not in Koios (rolled back, or Koios lag?)")
            bad += 1
        elif yaci[n] != koios[n]:
            print(f"DIFF    block {n}: txs yaci={yaci[n]} koios={koios[n]} ({yaci[n] - koios[n]:+d})")
            bad += 1
    print(f"\nblocks {lo}..{hi}: yaci={len(yaci)} koios={len(koios)} "
          f"txs yaci={sum(yaci.values())} koios={sum(koios.values())}")
    return bad, 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", default="mainnet", choices=sorted(BASES))
    ap.add_argument("--blocks", action="store_true", help="per-block mode: stdin has number,no_of_txs")
    ap.add_argument("--include-last", action="store_true", help="epoch mode: also compare the last epoch")
    ap.add_argument("--timeout", type=int, default=60)
    a = ap.parse_args()

    key = "number" if a.blocks else "epoch"
    rows = [r for r in csv.DictReader(sys.stdin) if r.get(key, "").strip().lstrip("-").isdigit()]
    if not rows:
        sys.exit(f"no yaci rows on stdin (expected db.sh --csv output with a '{key}' column)")

    base = BASES[a.network]
    print(f"network={a.network}  mode={'blocks' if a.blocks else 'epochs'}  yaci_rows={len(rows)}\n")
    bad, skipped = (check_blocks(rows, base, a.timeout) if a.blocks
                    else check_epochs(rows, base, a.timeout, a.include_last))
    print(f"result: {'PASS' if bad == 0 else 'FAIL'}  mismatches={bad}  skipped={skipped}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
