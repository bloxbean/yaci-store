# ADR 0008: A 333 or 444 subject is served only when its user token was seen on chain

## Status

Proposed. Implemented in PR #1294 (open, stacked on #1220). The issue is #1292.

## Date

2026-10-22

## Context

CIP-68 keeps the metadata of a token in a **reference NFT** (asset name label `000643b0`). The user tokens it describes have the same name after
another label: `000de140` (222, an NFT), `0014df10` (333, fungible), `001bc280` (444, rich fungible). The API (ADR 0003, ADR 0006) serves the 333 and
444 subjects, and finds the metadata by replacing the label with `000643b0`.

That mapping only looks at the label. It does not check that the user token was ever minted, and it does not check which label the stored datum was
read for. So any 333 or 444 subject with the name of a reference NFT answered `200` with its metadata, including the 333 and 444 subjects of NFTs.
On preprod (full sync, 2026-10-22), 53,383 of 55,619 possible 333 subjects and 55,064 of 55,619 possible 444 subjects answered `200` while the token
did not exist; the same problem was reported for the cf-token-metadata-registry V2 API (#1292).

The answer is not flagged either (an NFT datum has no validation errors), so `?validation=STRICT` accepts it. And a project's future token name can be
impersonated by publishing a reference NFT before the token exists.

## Decision

### 1. Record the 333 and 444 user tokens that are seen, and serve a subject only if its token was seen

The CIP-68 processor records every unit whose asset name starts with `0014df10` or `001bc280` and has a name after it, the first time it appears in an
output, in a small table `cip68_user_token` (`unit`, `slot`, `tx_hash`; migration `V0_1700_4`). The storage reader answers `404` (single), leaves the
subject out (batch) or answers `404` (`/cip68/ft/{policyId}/{rawAssetName}`, which accepts the 333 or the 444 token of that name) when the token is not in
the table.

The write is made in the block transaction. It is **bounded** (only two labels, a unit of at most 120 characters) and **idempotent** (a token already
recorded is not written again), and it is an **upsert that the database resolves**, in a fixed order, that **keeps the smallest slot**. A rollback to an
earlier slot deletes the rows with `slot >` the rollback slot (a listener, and an entry in `rollback.yml`).

*Why an upsert and not a read followed by an insert:* blocks can be processed in parallel (`store.executor.enable-parallel-processing`), each in its own
transaction. Two transactions that both see a token for the first time both read "not recorded" and both insert it, and the second fails with a duplicate
key **when it commits**, inside the block transaction, where an exception stops the whole sync (ADR 0004). That happened in the first minutes of the first
preprod run of this change. The database resolves the conflict (`ON CONFLICT` on PostgreSQL, `ON DUPLICATE KEY UPDATE` on MySQL, `MERGE` on H2), and the
fixed order stops two transactions that write the same tokens from waiting for each other. The row keeps the smallest slot because a later block can commit
first, and a rollback to a slot between the two must not delete a token that was already seen before it.

*What is not guaranteed:* a failed write is logged as a warning, but a database error that poisons the transaction (PostgreSQL) still reaches the commit,
as in ADR 0004 and #1265, so the write cannot be called harmless in every case.

*Why a table and not a query of the utxo or assets stores:* `assets-ext` does not depend on them, the assets store is disabled in some deployments, and
`address_utxo` has no index on the units it holds, so a lookup per request would scan it.

### 2. The stored label of the reference NFT is not a gate

A reference NFT can serve several user tokens (the CIP allows a 222 and a 333 together; there are 10 such datums on mainnet), and we store one label per
row. The label is also inferred from the datum when the user token is minted in a later transaction than the reference NFT (#1225). Serving a 333
subject only when the row says 333 would hide tokens that exist. Checking that the token exists cannot.

### 3. The error we prefer

There are two ways to be wrong: serve metadata for a token that does not exist (a false `200`), or hide one that does (a false `404`). A wallet or an
explorer asks about tokens it has seen, so a false `200` mostly hurts the ones who check existence or impersonation, and a false `404` breaks a real
token for the people who hold it. The rule can only err towards the false `200`: a token that was minted and burned in the same transaction never
appears in an output, and is not recorded.

## Consequences

- Subjects whose token was never seen are `404` where they were `200`. This is a behaviour change for clients, and it is in the README.
- A sync from genesis is needed, like the rest of the CIP-68 data. The table is empty after the migration and is filled by a resync, which #1220
  requires anyway (ADR 0006). A store started mid-chain would miss the tokens minted before it started, and would `404` a reference NFT that is
  updated later; the module already assumes a sync from genesis (older reference NFTs would be missing as well).
- The MySQL statement was written from the documentation and has not been run against a MySQL database; PostgreSQL and H2 were.
- The table grows with the number of 333 and 444 tokens, a few thousand on mainnet. An attacker could mint many units with these labels, but each costs
  transaction fees, and a row is small.
- A token that was minted and burned stays recorded and is still served: it existed.
- The validation errors of a row are computed for the label inferred when it was indexed. A 333 subject served from a row indexed as a 222 shows the 222
  errors, as before.

## Alternatives considered

- **Check the stored label only.** Cheap, but it hides tokens that exist (several user tokens for one reference NFT, a label inferred wrongly), which is
  the worse error.
- **Do nothing and document it,** as the registry's V2 does. Rejected: the 222 case alone makes tens of thousands of subjects answer.
- **Query the utxo or assets stores.** See decision 1.
- **Put the check in `TokenQueryService`.** The storage reader is the one place that maps a subject to a reference NFT, and the raw `/cip68/ft/**`
  endpoints do not go through the query service.

## Open questions

- Whether the same check should apply to a future NFT API (222 subjects). Nothing serves NFTs today (ADR 0003).
- Whether the 404 of a token that exists in `cip68_metadata` but was never seen should say so; today it is the same answer as an unknown subject.

## References

- #1292, #1294, #1220
- ADR 0003 (the read model), ADR 0004 (processing failures), ADR 0006 (validation errors), ADR 0007 (`invalid_token`)
- `Cip68UserTokenRecorder`, `Cip68UserTokenService`, `Cip68StorageReaderImpl`
