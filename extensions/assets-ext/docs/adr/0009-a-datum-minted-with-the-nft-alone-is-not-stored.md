# ADR 0009: A CIP-68 datum minted with the 222 NFT alone is not stored

## Status

Proposed. Implemented in PR #1220 (`0b382b706`) and in its 2.0.x backport, PR #1242 (`d4261bde2`). Amends ADR 0002 (decision 2, what is
indexed) and ADR 0008 (its consequence that NFTs keep answering for their 333 and 444 subjects).

## Date

2026-10-29

## Context

The API serves the 333 (`0014df10`) and 444 (`001bc280`) subjects of a reference NFT from its newest row, whatever label that row was stored with,
and checks each subject for its own label when it is served (ADR 0002 decision 2, ADR 0008). The processor stored every datum it could decode,
including the datum of an NFT: a reference NFT minted with its 222 token (`000de140`) and no 333 or 444 token. So every NFT also answered `200` for a
333 and a 444 subject that was never minted.

ADR 0008 kept that on purpose for tokens that were never minted, for the reasons it gives (history of burned tokens, a wallet only asks for what it
holds, no impersonation). It also counted NFTs among the cases that keep answering `200`. What it did not weigh was how many there are and what
they look like:

- **Mainnet at the tip (2026-10-29): 152,408 of the 155,302 reference NFTs in `cip68_metadata` have only label 222 rows.** For a sample, both the
  333 and the 444 subject of each answered `200`: a never-minted fungible token for nearly every NFT on chain.
- An NFT's datum has no `description`, and usually no `decimals` or `ticker`, so its 333 subject is served with `MANDATORY` errors and `valid: false`.
  The default `?validation=ALL` returns it.
- **Junk follows the NFT in.** A later datum of a reference NFT that is already stored is always stored, so that an owner who clears the metadata
  is not served the old one (ADR 0002 decision 2). For 30 mainnet NFTs (29 under policy `ce0bc877...`, names such as `@DAVID`) a later datum was a
  dApp profile with none of the CIP-68 token fields (`{"mainnet-testing": "this is a test profile."}`), sent without a user token. It was stored
  because the NFT already had rows, guessed as a 333 from its shape, and served as a fungible token with every field null.
- ADR 0008 says the module and cf-token-metadata-registry V2 behave the same way here. They did not: the registry does not store a datum whose
  transaction has the 222 NFT and no 333 or 444 token (its ADR-019), so it answers `404` for all of these. Found by comparing the two APIs at the
  mainnet tip, 2026-10-29.

Nothing in the module reads an NFT row. They were kept "for future use" (the schema README).

## Decision

### 1. A datum whose transaction has the 222 NFT and no 333 or 444 token is not stored, unless its reference NFT is already stored

`Cip68Processor` skips it at DEBUG and counts it as `yaci.store.assets.cip68.datums.skipped{reason="nft_only"}`. "In the transaction" is the pairing
of ADR 0002 decision 2: the same policy and the same base name.

- **Stored as before:** a datum whose transaction also has a 333 or 444 token (a 222 and a 333 minted together are both served, each checked for its
  label); a datum without any user token in its transaction, when it has CIP-68 token fields (its 333 or 444 token can be minted in another
  transaction, before or after it); and every later datum of a reference NFT that is stored.
- **Not stored:** the NFT's datum, and therefore the junk that would otherwise follow it in, since the reference NFT is never stored. A datum
  without a user token and without token fields was already skipped (`not_token_metadata`).
- It is not an `invalid_token` row (ADR 0007): nothing is wrong with an NFT, it is just not what this module serves.

### 2. Why at indexing, and not in the read path

- The read path cannot tell an NFT from a fungible token by its rows: a datum minted with a 222 and a 333 token together is stored with label 222
  (the first of the paired labels, ADR 0002 decision 2), and a datum without a user token gets a guessed label. A rule "serve a subject only if
  a row was stored for a 333 or 444" would need the stored label to mean something it does not.
- Skipping the NFT's datum keeps the reference NFT out of the table altogether, which also keeps the later datums of the NFT (the junk above) out.
- It is the same rule as cf-token-metadata-registry's, so the two serve the same subjects.

## Consequences

- An NFT's 333 and 444 subjects are a `404`: on mainnet about 152,000 reference NFTs answer for no subject any more.
- What ADR 0008 decided still holds for tokens: a stored reference NFT answers for its 333 and 444 subjects, also after the token was burned and
  also when one of the two was never minted. Only the reference NFTs of NFTs are no longer stored.
- The module no longer keeps NFT metadata. If a use for it comes, it needs its own decision, and a resync to fill it.
- A resync is needed to remove the rows already stored; PR #1220 requires one anyway (ADR 0006).
- The table is smaller: on mainnet the 222 rows are most of it.
- An NFT whose reference NFT is updated later without a user token, with token fields, is stored from that datum on, guessed as a 333 or 444 by
  its shape, and served. This is the case of a reference NFT whose 333 or 444 token is minted apart from it, which cannot be told apart without a
  cross-transaction lookup (ADR 0002 decision 2). Not measured.
- **Known issue: "already stored" depends on the processing order during a parallel sync.** With
  `store.executor.enable-parallel-processing=true`, the blocks of a batch run in partitions at the same time, so the check can miss an earlier row
  that is not stored yet, or see a row that a later block stored first. A later datum is then skipped (House of Titans #1001, #3377 and #4102 on
  mainnet kept their old metadata after it was cleared), or an NFT's datum is stored. This also applies to `not_token_metadata` (ADR 0002 decision 2).
  At the tip the check is exact. The fix and its alternatives are discussed in #1309; the proposed one, #1310, indexes the blocks of a batch in
  chain order on `PreCommitEvent`. That event is internal and is never published to a remote consumer (`store.remote.consumer-enabled`), so with
  #1310 the module indexes nothing there, like the other stores that finish their work on `PreCommitEvent` or `CommitEvent`
  (`AccountBalanceProcessor`, `DatumStorageImpl`, ...; #1311). There are no known users of remote consumers.

## Alternatives considered

- **Keep storing NFT datums and filter in the read path.** Keeps NFT data for a future use, but the stored label cannot drive the filter
  (decision 2), and a separate "has a 333 or 444 been paired" flag would be a new column and a migration for a use nobody has.
- **Keep ADR 0008 as it is and document it.** Leaves about 152,000 never-minted fungible tokens with `MANDATORY` errors in the default response,
  and the junk datums served as tokens.
- **Look up whether a 333 or 444 token exists.** That is ADR 0008's option 2 (PR #1294), rejected there for its cost and for the history of burned
  tokens; it is not needed for this case, which the minting transaction already shows.

## References

- ADR 0002 (decision 2: the stored label and what is indexed), ADR 0007 (`invalid_token`), ADR 0008 (a reference NFT answers for its 333 and 444
  subjects)
- PR #1220, PR #1242; review of PR #1220 (2026-10-26, point 1: the stored label is a guess)
- cardano-foundation/cf-token-metadata-registry ADR-019 and #121: the same rule, and a datum without a user token stored whatever its shape
- `Cip68Processor.indexDatum`, `Cip68Metrics.NFT_ONLY`
