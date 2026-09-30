#!/usr/bin/env python3
"""
Cross-check yaci-store's adapot (ada pot) values against Koios.

Reads yaci rows as CSV on stdin (produced by db.sh --csv) and compares them to
Koios /totals for the same epochs. Independent source, so a match is real
evidence the ledger-state pot maths is right.

  db.sh --host H --container C --csv -- \
    "select epoch,treasury,reserves,fees,deposits_stake,circulation
       from adapot where epoch between 630 and 638 order by epoch;" \
  | koios_adapot_check.py --network mainnet

Exit 0 = all compared fields agree, 1 = at least one mismatch, 2 = usage/fetch error.
"""
import argparse, csv, json, sys, urllib.request, urllib.error

BASES = {
    "mainnet": "https://api.koios.rest/api/v1",
    "preprod": "https://preprod.koios.rest/api/v1",
    "preview": "https://preview.koios.rest/api/v1",
}

# Fields that mean the same thing on both sides and must match exactly.
DIRECT = ["treasury", "reserves", "fees", "deposits_stake"]

# yaci `circulation` is NOT Koios `circulation`. Verified against mainnet:
# yaci.circulation[N] == koios.supply[N-1] == 45e15 - koios.reserves[N-1].
# yaci reports the value as at the START of epoch N (= end of N-1); Koios
# `circulation` is a different quantity again (supply minus rewards/deposits).
# Comparing the two directly produces a false alarm — hence the shift.
SHIFTED = {"circulation": ("supply", -1)}


def fetch(base, timeout):
    """Fetch all epoch totals in one request.

    Tries urllib first, then falls back to curl. Python on macOS ships its own
    CA bundle and frequently cannot verify public certs, while curl uses the
    system store — so the fallback is the common path there, not an edge case.
    """
    url = f"{base}/totals"
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
            data = json.load(r)
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
            data = json.loads(out)
        except subprocess.CalledProcessError as e:
            sys.exit(f"koios fetch failed from {url}: curl: {e.stderr.strip()}")
        except json.JSONDecodeError as e:
            sys.exit(f"koios returned non-JSON from {url}: {e}")
    return {int(x["epoch_no"]): x for x in data}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--network", default="mainnet", choices=sorted(BASES))
    ap.add_argument("--timeout", type=int, default=30)
    a = ap.parse_args()

    rows = [r for r in csv.DictReader(sys.stdin) if r.get("epoch", "").strip().isdigit()]
    if not rows:
        sys.exit("no yaci rows on stdin (expected db.sh --csv output)")

    koios = fetch(BASES[a.network], a.timeout)
    print(f"network={a.network}  yaci_epochs={len(rows)}  koios_epochs={len(koios)}\n")

    bad = miss = 0
    for r in rows:
        ep = int(r["epoch"])
        hdr = f"epoch {ep}"
        if ep not in koios:
            print(f"{hdr}: not in Koios yet — skipped")
            miss += 1
            continue
        out = []
        for f in DIRECT:
            if f not in r:
                continue
            y, k = str(r[f]).strip(), str(koios[ep].get(f, "")).strip()
            ok = y == k
            bad += not ok
            out.append((f, y, k, ok))
        for yf, (kf, shift) in SHIFTED.items():
            if yf not in r:
                continue
            src = koios.get(ep + shift)
            if src is None:
                continue
            y, k = str(r[yf]).strip(), str(src.get(kf, "")).strip()
            ok = y == k
            bad += not ok
            out.append((f"{yf} (vs koios {kf}[{ep+shift}])", y, k, ok))

        print(hdr)
        for f, y, k, ok in out:
            mark = "ok  " if ok else "DIFF"
            print(f"  {mark} {f:<34} yaci={y:>20}  koios={k:>20}")
            if not ok:
                try:
                    print(f"       delta = {int(y) - int(k):+d}")
                except ValueError:
                    pass
        print()

    print(f"result: {'PASS' if bad == 0 else 'FAIL'}  mismatches={bad}  skipped={miss}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
