# ADR 0008: A reference NFT answers for its 333 and 444 subjects (the lookup is not changed)

## Status

Accepted. Nothing is implemented: this record is the decision not to change the lookup. The change that was tried, PR #1294 (a table of the 333 and
444 tokens seen on chain), was closed without merging, and the issue, #1292, was closed as "won't fix".

## Date

2026-10-23

## Context

CIP-68 keeps the metadata of a token in a **reference NFT** (asset name label `000643b0`). The user tokens it describes have the same name after
another label: `000de140` (222, an NFT), `0014df10` (333, fungible), `001bc280` (444, rich fungible). The API (ADR 0003, ADR 0006) serves the 333 and
444 subjects, and finds the metadata by replacing the label with `000643b0`.

That mapping only looks at the label. It does not check that the user token was ever minted, and it does not check which label the stored datum was
read for. So any 333 or 444 subject with the name of a reference NFT answers `200` with its metadata, including the 333 and 444 subjects of NFTs and
of tokens that were never minted. On preprod (full sync, 2026-10-22), 53,383 of 55,619 possible 333 subjects and 55,064 of 55,619 possible 444
subjects answered `200` while the token did not exist. The same was reported for the cf-token-metadata-registry V2 API (#1292), and the answer was not
flagged (an NFT datum had no validation errors, so `?validation=STRICT` accepted it). Since the review of PR #1220 a subject is checked for its own label
when it is served (ADR 0006 decision 4): the 333 subject of an NFT without a description has a `MANDATORY` error and is left out by
`?validation=MANDATORY_ONLY`; one whose datum has what a 333 needs still passes.

Options considered in #1292: (1) serve a subject only if the stored label matches; (2) serve it only if the user token was seen on chain (a small
`cip68_user_token` table filled by the CIP-68 processor, with rollback); (3) both; (4) do nothing and document it. Option 2 was built and verified on
preprod (every seen token answered `200`, every made-up subject `404`) before the decision below.

## Decision

### 1. The lookup stays as it is: the metadata API does not say whether a token exists

A 333 or 444 subject is answered from the newest row of the reference NFT with that policy and name, whatever its label, as before. A `200` means
"there is metadata attached to this name", not "this token exists". A client that needs to know whether the token exists asks the chain.

### 2. Why

- **History has value.** The metadata is kept after the token is burned, and explorers and wallets show historical transactions, where the token no
  longer exists on chain but its name, logo and decimals still matter. A rule that serves only tokens that exist (or that were seen) either hides
  them, or needs a burn-aware rule that the index cannot get right (a token minted and burned in the same transaction never appears in an output).
  What looks like a bug, a subject answering for a token that is not there, is also this feature.
- **Wallets are not affected.** A wallet asks for tokens it holds. A token that was never minted is in nobody's wallet, so it is never asked for.
- **Impersonation is not possible.** The reference NFT has to be minted under the same policy ID as the token, and only the owner of the policy can
  mint under it. An attacker cannot publish metadata for someone else's future token. (The first text of the issue named this as a risk; it applies
  only to the owner of the policy.)
- **The API answers a different question.** It returns the metadata attached to a subject, and cf-token-metadata-registry works the same way. The
  effect on a made-up unit, `200` instead of `404`, is a small inaccuracy.
- **The cost is not small.** The table needs a migration for three dialects, a rollback handler, an upsert that is safe for blocks processed in
  parallel (the first version failed on a duplicate key within an hour), a check on three endpoints, and a resync. It would also make this module differ
  from the registry.

### 3. What is documented

The README of the module says that a reference NFT also answers for its 333 and 444 subjects, also after the token was burned, and that checking
whether a token exists is up to the client.

## Consequences

- The cases of #1292 keep answering `200`: NFTs asked as 333 or 444, and tokens that were never minted.
- A burned token keeps its metadata in the API, which is what explorers and wallets need for history.
- The module and cf-token-metadata-registry V2 behave the same way here.
- No new table, migration or rollback entry. `invalid_token` (ADR 0007) is the only table of ADR 0006's work besides the new columns.

## Alternatives considered

- **Serve a subject only when its token was seen on chain (option 2, PR #1294).** Built, verified on preprod, and not merged: it takes away the
  history of burned tokens and costs more than the inaccuracy it removes (decision 2). The design is in the history of #1294 if the decision changes: a
  `cip68_user_token` table (`unit`, `slot`, `tx_hash`) filled by the processor with a database upsert that keeps the smallest slot, deleted on rollback
  for `slot >` the rollback slot.
- **Check the stored label only (option 1).** Cheap, but it hides tokens that exist: one reference NFT can serve user tokens of several labels, and
  the label is inferred from the datum when the user token is minted in a later transaction (ADR 0002).
- **Query the utxo or assets stores.** `assets-ext` does not depend on them, and the assets store is disabled in some deployments.

## Open questions

- If a client needs "does this token exist", should a flag be added to the response (derived from the chain data of the deployment, when it has it)? Not
  planned.

## References

- #1292 (closed, won't fix), #1294 (closed), #1220
- ADR 0002 (the label of a row), ADR 0003 (the read model), ADR 0006 (validation errors), ADR 0007 (`invalid_token`)
