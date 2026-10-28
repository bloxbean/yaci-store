# assets-ext mainnet regression tests

End-to-end tests for the assets-ext token metadata API (`/api/v1/tokens/subject/{subject}`, `/api/v1/tokens/subject/query`,
`/api/v1/tokens/cip26/{subject}`), run over HTTP against a yaci-store synced to the mainnet tip. They need no other
service: the expected values are committed fixtures.

## Run

```console
cd extensions/assets-ext/regression-tests
API_BASE_URL=http://localhost:8080 uv run pytest
```

`API_BASE_URL` is the yaci-store host (default `http://localhost:8080`). The CIP-26 tests need the off-chain sync done
(`assetStoreOffchainSync` UP), the CIP-68 tests need the indexer at the tip. Run one group with a marker:
`-m cip26`, `-m cip68`, `-m rft`, `-m validation`, `-m batch`, `-m deletion`.

## Fixtures

Copied on 2026-10-28 from [cf-token-metadata-registry](https://github.com/cardano-foundation/cf-token-metadata-registry)
`regression-tests/mainnet/fixtures/`, which serves the same tokens, so both projects test against the same baseline.
The tests are copied too, with the endpoint paths changed; V1 and CIP-113 tests are not copied (assets-ext has no V1 API
and does not index CIP-113).

| Fixture | Tokens | Where it comes from |
|---|---|---|
| `cip26_tokens.json` | 1,000 | CIP-26 mappings of the cardano-token-registry (a snapshot of the registry's database) |
| `cip26_removed_tokens.json` | 60 | mappings deleted from the cardano-token-registry, mined from its git history |
| `cip68_tokens.json` | 675 | CIP-68 fungible tokens (333), a reviewed snapshot of the registry's database |
| `cip68_rft_tokens.json` | 479 | CIP-68 rich fungible tokens (444), from the chain via Koios |
| `cip68_validation_errors_tokens.json` | 4 | hand-picked tokens that break CIP-68, with the errors they must report |

Where assets-ext deliberately differs from the registry, the copy says so:

- `cip68_validation_errors_tokens.json`: a `version` CIP-68 does not define is `invalid_version` (the registry reports
  `undefined_version`).
- `test_batch_query.py`: a batch holds at most 100 subjects, so the large batch is sent 100 at a time, and a batch of
  101 is a 400.

**The fixtures are a frozen baseline: never regenerate them during a test run.** When a test fails because a token's
datum changed on chain, correct that one entry deliberately, with the reason in the commit.

`scripts/` holds a different check, a byte-for-byte comparison with a running cf-token-metadata-registry; it is not
needed to run these tests.
