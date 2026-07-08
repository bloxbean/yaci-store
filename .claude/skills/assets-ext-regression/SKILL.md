---
name: assets-ext-regression
description: Run CIP-26 and CIP-68 wire-shape regression tests for the assets-ext extension against the cf-token-metadata-registry V2 API on mainnet. Use when a user wants to verify yaci-store's API output is byte-equivalent to CF, after a code change to assets-ext, or when investigating reported metadata divergences.
---

# assets-ext regression skill

End-to-end regression for `extensions/assets-ext`. Compares yaci-store's
CIP-26 and CIP-68 FT API responses against the cf-token-metadata-registry
**V2 API** on Cardano mainnet, byte-for-byte.

CF V1 endpoints are intentionally not exercised — yaci-store does not persist
off-chain signatures, so a faithful V1 comparison is impossible.

## When to invoke

- After a code change in `extensions/assets-ext/` that could touch the read path
  (`AssetsReader`, `Cip26StorageReader`, `Cip68StorageReader`, `TokenQueryService`,
  any repository or DTO under `cip26/` or `cip68/`).
- When a user reports a metadata divergence ("token X looks wrong on yaci vs
  tokens.cardano.org").
- As a pre-release smoke check before tagging an assets-ext build.

## Inputs you need from the user

Ask only if not already obvious from the conversation context:

1. **Where yaci-store is running** — local port 8081 by default. If it's remote,
   ask for either an SSH host (so you can suggest a tunnel) or a direct URL.
2. **Which CF source to compare against**:
   - **Public** `https://tokens.cardano.org` — works for anyone with internet,
     but rate-limited (~1 request per 1.5 s; HTTP 429 above that).
   - **Internal CF mirror** — fast, no rate limit; needs an SSH tunnel. Ask if
     they have access.
3. **Do they have SSH+Postgres access to the yaci-store host?** Determines which
   scripts you can run:
   - Yes → use the fast DB-enumerated `sweep_cip26_full.py` + `sweep_cip68_ft_full.py`
     (a few minutes total).
   - No  → use `verify_against_cf_registry.py` (slower, no DB needed).

Default assumptions if the user just says "run the regression" with no extra
context: yaci at `http://127.0.0.1:8081`, internal mirror at `http://127.0.0.1:8082`,
DB host `mczeladka` (matches the developer's existing setup).

## Step-by-step

All commands run from the repository root.

### Step 1 — Confirm yaci is reachable

```sh
curl -s --max-time 5 http://127.0.0.1:8081/actuator/health | head -c 300
```

Expect `"status":"UP"` with `assetStoreOffchainSync` present. If the response is
empty or 404, ask the user how to reach their yaci-store and adjust `--yaci-url`
on subsequent commands.

### Step 2 — Confirm the CF source is reachable

The cLBC FT subject is a stable mainnet token used throughout these scripts:
`376efee6953a6af08c41903b781a316553ad279387b8ff1bf5d7c94d0014df10634c4243`.

```sh
curl -s -w " HTTP %{http_code}\n" -o /dev/null \
  "${CF_URL}/api/v2/subjects/376efee6953a6af08c41903b781a316553ad279387b8ff1bf5d7c94d0014df10634c4243"
```

`HTTP 200` means the V2 endpoint is up. `204` means the CF source doesn't have
that subject (wrong host or non-mainnet). `429` means you're being rate-limited.

### Step 3 — Run the sweeps

**Preferred (DB access + CF mirror — fastest)**:

```sh
cd extensions/assets-ext/scripts

python3 sweep_cip26_full.py \
    --yaci-url http://127.0.0.1:8081 \
    --cf-url   http://127.0.0.1:8082 \
    --workers  16

python3 sweep_cip68_ft_full.py \
    --yaci-url http://127.0.0.1:8081 \
    --cf-url   http://127.0.0.1:8082 \
    --workers  16
```

Each script prints a one-line summary at the end and exits `0` if all subjects
agree, `1` if any diverged. Mainnet baseline: 7,932 CIP-26 + 7,647 CIP-68 FT,
all agreeing.

**Fallback (no DB access)**:

```sh
cd extensions/assets-ext/scripts
python3 verify_against_cf_registry.py --network mainnet \
    --yaci-url http://127.0.0.1:8081
```

Enumerates from the CF GitHub registry (clones it once, fast-forwards on
subsequent runs). Use this when the QA engineer cannot reach the yaci-store DB.

**Public-CF variant (slow but no internal access needed)** — swap `--cf-url` to
`https://tokens.cardano.org` and add `--cf-throttle-ms 1500 --workers 1`. A
mainnet full sweep that way takes hours.

### Step 4 — Interpret the result

A passing run looks like:

```
[setup] yaci=http://127.0.0.1:8081  cf=http://127.0.0.1:8082  workers=16
[setup] subjects=7932

total=7932  agree=7932  diverged=0  skip=0  elapsed=137.4s
```

If there are divergences, the script prints up to 20 of them with per-field
diffs (`field: yaci=… cf=…`). Tell the user:

1. The total and pass rate.
2. Whether any divergence appears in CIP-26 vs CIP-68 (different bug classes).
3. **Whether the divergence is in a known-acceptable category** before flagging
   it as a regression (see "Acceptable divergences" below).
4. The first few non-acceptable diffs verbatim.

### Step 5 (only if regressions found) — Triage

Common root causes when something fails:

- **All divergences are `logo` field** — almost certainly the spec-vs-CF encoding
  difference. Not a regression; document and move on.
- **All divergences point to a single field across many subjects** — likely a
  serializer or wire-shape bug. Look at `Cip26TokenMetadata.from` / the V2 DTO
  builders.
- **Divergences are CIP-68 only, with stale metadata** — historically caused by
  the `findFirstByPolicyIdAndAssetNameAndLabelOrderBySlotDesc` label filter
  (fixed). If it resurfaces, check `Cip68MetadataRepository` for a reintroduced
  `label = :label` clause.
- **Many `skip` results with `yaci=200,cf=204`** — CF mirror doesn't have the
  subject. Often means the mirror is on a different network (preprod vs
  mainnet) or behind on indexing.

## Acceptable divergences

Both scripts surface these by design — they are **not** regressions:

1. **`logo.value` encoding** — CIP-26 spec says base64; CF V2 returns hex.
   yaci-store passes the spec base64 through unchanged. If a divergence list
   contains only logo fields, this is expected.
2. **Multiple registry files claiming the same subject** — affects 3 testnet
   subjects (BCoin, wUSDT, one other); 0 on mainnet. yaci picks deterministically
   by filename matching the subject; CF picks via `File.listFiles()` order.

## Notes for the agent

- **Always** report scripts' exit codes back to the user — they're the
  authoritative pass/fail signal.
- Do not throttle internal-mirror requests; the mirror is local, throttling just
  wastes time.
- The two `sweep_*` scripts use `ssh ... psql` to enumerate from the DB. Pass
  the right `--db-host` if it's not the default (`mczeladka`).
- If the user wants to investigate one specific subject, fall back to a direct
  side-by-side curl:
  ```sh
  curl -s "${YACI_URL}/api/v1/tokens/subject/${SUBJ}?show_cips_details=true" | python3 -m json.tool
  curl -s "${CF_URL}/api/v2/subjects/${SUBJ}?show_cips_details=true"      | python3 -m json.tool
  ```
- Scripts and README live at `extensions/assets-ext/scripts/`. The README is the
  authoritative reference for flag details and is intentionally consistent with
  this skill — keep them in sync if either changes.
