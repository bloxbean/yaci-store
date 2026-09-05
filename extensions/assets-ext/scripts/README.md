# assets-ext regression scripts

Three Python scripts that byte-compare yaci-store's assets-ext API output against
[`cf-token-metadata-registry`](https://github.com/cardano-foundation/cf-token-metadata-registry)'s
**V2 API** for CIP-26 and CIP-68. Use these whenever you want to verify that a
yaci-store build serves wire-shape parity with CF.

V1 endpoints are intentionally **not** exercised. CF V1 wraps each property with
off-chain signatures (`{value, signatures, sequenceNumber}`); yaci-store does not
persist signatures, so a faithful V1 comparison is impossible.

| Script | Enumerates from | Needs DB access? | Speed | Coverage |
|---|---|---|---|---|
| [`verify_against_cf_registry.py`](verify_against_cf_registry.py) | CF GitHub registry (clones the repo) | No | Slow (~30–60 min on mainnet — registry is large) | Every subject in the registry; CIP-26 byte-level, CIP-68 sub-tally for subjects that have both |
| [`sweep_cip26_full.py`](sweep_cip26_full.py) | yaci-store `cip26_metadata` table | Yes (SSH + Postgres) | Fast (~2 min mainnet at 16 workers against the CF mirror) | Every CIP-26 row in the DB |
| [`sweep_cip68_ft_full.py`](sweep_cip68_ft_full.py) | yaci-store `cip68_metadata` table (label=333) | Yes (SSH + Postgres) | Fast (~1.5 min mainnet at 16 workers against the CF mirror) | Every distinct CIP-68 fungible token whose history contains a label=333 row |

**Pick by access**: if you can reach yaci-store's database, use the two `sweep_*`
scripts — they're fast and exhaustive. If you only have HTTP access to yaci, use
`verify_against_cf_registry.py`.

---

## Prerequisites

- Python ≥ 3.10 (stdlib only — no `pip install` needed for any script).
- HTTP access to a running yaci-store (default expected at `http://127.0.0.1:8081`).
- HTTP access to a CF V2 API source — either:
  - **Public** `https://tokens.cardano.org/api/v2/subjects/{subject}` — rate-limited (~1 req per 1.5 s, AWS ELB returns HTTP 429 above that), or
  - **Internal CF mirror** at `http://<host>:8080/api/v2/subjects/{subject}` (typically reached via SSH tunnel to port 8082).
- For the `sweep_*` scripts: SSH access to the host running yaci-store's PostgreSQL instance, plus the DB credentials.
- For `verify_against_cf_registry.py`: `git` on `PATH` (used to clone/pull the registry).

---

## Step-by-step: QA workflow

> Goal: confirm that the yaci-store under test serves wire-shape parity with CF
> mainnet's V2 API for CIP-26 and CIP-68 FT.

### 1. Pick which yaci-store you are testing

The scripts expect yaci's HTTP API at `http://127.0.0.1:8081` by default. Either:
- Run yaci-store locally on port 8081, **or**
- Tunnel a remote yaci: `ssh -L 8081:localhost:8081 -N <yaci-host>`, **or**
- Pass `--yaci-url <url>` to each script.

Confirm yaci responds:
```sh
curl -s http://127.0.0.1:8081/actuator/health | head -c 200
```

### 2. Pick which CF V2 source to compare against

**Option A — Public CF mainnet** (no setup, but rate-limited):
```sh
CF_URL=https://tokens.cardano.org
THROTTLE=--cf-throttle-ms=1500
WORKERS=--workers=1
```

**Option B — Internal CF mirror** (requires SSH tunnel; no throttle, no rate limit):
```sh
ssh -L 8082:localhost:8080 -N <cf-mirror-host>
CF_URL=http://127.0.0.1:8082
THROTTLE=
WORKERS=
```

Confirm CF responds (cLBC subject is a stable mainnet token, expected `HTTP 200`):
```sh
curl -s -w "HTTP %{http_code}\n" -o /dev/null \
  "${CF_URL}/api/v2/subjects/376efee6953a6af08c41903b781a316553ad279387b8ff1bf5d7c94d0014df10634c4243"
```

### 3. Run the regressions

**Without DB access** (slower; uses the CF GitHub registry as the subject list):

```sh
cd extensions/assets-ext/scripts
python3 verify_against_cf_registry.py --network mainnet --yaci-url http://127.0.0.1:8081
```

This clones `cardano-foundation/cardano-token-registry`, iterates every subject,
and reports CIP-26 byte-level agreement plus CIP-68 sub-tally for subjects that
have both. Use `--help` for the full flag list.

**With DB access** (fast; exhaustive):

```sh
cd extensions/assets-ext/scripts

# CIP-26 — 7,932 subjects mainnet, ~2 min at 16 workers against the mirror
python3 sweep_cip26_full.py \
    --yaci-url http://127.0.0.1:8081 \
    --cf-url   http://127.0.0.1:8082 \
    --db-host  <ssh-host-with-pg-access>

# CIP-68 FT — 7,647 subjects mainnet, ~1.5 min at 16 workers
python3 sweep_cip68_ft_full.py \
    --yaci-url http://127.0.0.1:8081 \
    --cf-url   http://127.0.0.1:8082 \
    --db-host  <ssh-host-with-pg-access>
```

### 4. Interpret the result

Each script prints a summary line and exits with status `0` on full agreement,
`1` on any divergence, `2` on setup failure.

```
total=7932  agree=7932  diverged=0  skip=0  elapsed=137.4s
```

If `diverged > 0`, the first 20 diverging subjects are printed with a
per-field diff. Each row identifies the field, yaci's value, and CF's value.

### 5. Known acceptable divergences

These are **expected and not regressions** — both scripts will surface them:

1. **`logo.value` encoding** — CIP-26 spec says base64; CF V2 returns hex
   (decoded then re-encoded as hex, non-spec). yaci-store passes the spec base64
   through. If you see *only* logo-encoding differences, that's not a regression.
2. **Multiple registry files claiming the same subject** — affects 3 testnet
   subjects, 0 on mainnet. yaci-store deterministically picks the file whose
   name equals the subject; CF picks non-deterministically.

Any other divergence is worth investigating.

---

## Public CF rate limit

Public `tokens.cardano.org` is fronted by AWS ELB, which returns HTTP 429 after
roughly 183 requests in a 3-minute window with no `Retry-After` header. Use
`--cf-throttle-ms 1500 --workers 1` against it. The internal mirror has no rate
limit — prefer it when available.

---

## See also

- [`SKILL.md`](../../../.claude/skills/assets-ext-regression/SKILL.md) — Claude
  Code skill that wraps these scripts for one-command QA invocation.
- [`db/store/README.md`](../src/main/resources/db/store/README.md) — schema
  documentation for `cip26_metadata` / `cip26_sync_state` / `cip68_metadata` /
  `cip113_registry_node`.
