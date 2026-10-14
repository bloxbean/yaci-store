# ADR 0003: Token metadata read model and API

## Status

Proposed

This record describes behaviour that is on `main`. The effect of a rejected CIP-68 datum on what the API returns
(decision 5) follows from the rules of ADR 0002, which are implemented in PR #1220 (open).

## Date

2026-10-14

## Context

`assets-ext` stores token metadata from two unrelated sources: CIP-26 (an off-chain registry read from Git, ADR 0001) and
CIP-68 (on-chain datums, ADR 0002). A client does not care where a name or a logo came from. It asks for a token and
expects one answer. The module therefore has to decide how the two sources are combined, what counts as a token that
has metadata, which CIP-68 rows the API may serve, and which endpoints exist.

Two facts shape everything below:

- The sources overlap and disagree. A token can have a name on chain and a logo only in the registry, or the same
  `name` in both with different text.
- The module is optional. The CIP-26 side is off by default (`store.assets.ext.cip26.enabled=false`) because the registry is
  a separate trust source from the chain, and the CIP-68 side is on by default.

**Not covered: CIP-25.** The metadata of NFTs minted with CIP-25 (the transaction metadata standard, label 721) is not
indexed and not served. CIP-25 appears in the module's code only as a format that CIP-68 borrows: the nested `"721"` map
of a version 4 datum and the splitting of long strings into chunks of at most 64 bytes (ADR 0002 decisions 5 and 7). A
token with CIP-25 metadata and no CIP-26 or CIP-68 metadata has no answer from this API.

**NFT support is an initial version, not full support.** The module indexes and stores CIP-68 NFTs (label 222) and RFTs
(label 444), with the rules of ADR 0002, so that the data exists and the rules can change before anything depends on them.
It does not serve them (decision 4), and it has no NFT-specific DTO, service method or endpoint (the storage reader's own
comment says they are for a later release). This record is about
fungible tokens; read "token" in it as a fungible token unless it says otherwise.

## Decisions

### 1. One merged endpoint, plus one endpoint per standard

| Endpoint (under `${apiPrefix}/tokens`) | Returns |
|---|---|
| `GET /subject/{subject}` | The merged metadata of one subject |
| `POST /subject/query` | The merged metadata of up to 100 subjects |
| `GET /cip26/{subject}` and `/cip26/{policyId}/{assetName}` | The CIP-26 row only |
| `GET /cip68/ft/{subject}` and `/cip68/ft/{policyId}/{rawAssetName}` | The CIP-68 fungible token metadata only |

The merged endpoint is the one clients are meant to use. The per-standard endpoints exist so that a client, or an
operator who suspects a wrong answer, can see what one source holds without the merge. A subject is the policy id plus
the optional asset name, 56 to 120 hex characters; anything else is a 400 before any query runs. The `rawAssetName` of the CIP-68
endpoint is the asset name without the 4-byte label prefix, so at most 56 hex characters, and the module adds the label itself.

### 2. The sources are merged per property, in a priority order

The default order is CIP-68, then CIP-26 (`store.assets.ext.default-query-priority=CIP_68,CIP_26`). A request can set
another order with `query_priority`. For each property (`name`, `description`, `ticker`, `decimals`, `logo`, `url`) the
first source in the order that has a non-null value supplies it. Sources are not merged inside a property and a missing
value in the first source never blocks the second. `version` exists only in CIP-68.

Every property in the response carries its `source` (`CIP_68` or `CIP_26`), so a client can see where each value came from
without asking for more. `show_cips_details=true` additionally returns the raw metadata of each source (`standards`), which
is the way to compare what the two sources hold; it is off by default because it makes the response larger.

*Why per property and not per token:* a token with a CIP-68 `name` and `description` but no logo would otherwise lose its
registry logo. *Why CIP-68 first by default:* on-chain metadata is preferred (the wording of the code). The registry is an opt-in
trust source, separate from the chain, which is also why it is off by default. *Trade-off:* one response can mix values from
two sources, for example the name from the chain and the logo from the registry, and they need not agree with each other.
A client that needs one source asks for it with `query_priority` or uses the per-standard endpoint.

### 3. A token has metadata only if it has a name and a description

After the merge, a result without both `name` and `description` is treated as not found: the single endpoint answers 404,
and the batch endpoint leaves the subject out of its 200 response. `name` and `description` are therefore the two properties a
client can rely on. When a request filters the properties with `property`, it must include both or the answer is a 400, since
a filtered answer without them could never be valid.

*Why:* both CIP-26 and CIP-68 fungible tokens require them (ADR 0001, ADR 0002 decision 3), so a merged result without them is
a gap in the data, not a token with little metadata. *Trade-off:* a token that only has a `ticker` and `decimals` on chain is
reported as unknown instead of partially known.

### 4. Only fungible tokens from CIP-68 are served

The CIP-68 table stores rows for labels 222, 333 and 444, but the API reads fungible tokens only. A subject with the fungible
prefix `0014df10` is mapped to its reference NFT (prefix `000643b0`) and the latest row of that reference NFT is returned.
NFT and RFT rows are stored and not exposed.

The latest row is chosen **whatever its label**. A reference NFT can be updated in a transaction that also mints a user
token of another label, so its newest row may carry label 222 although the token is fungible; filtering by label would then
return an older row, which is worse than an unexpected label. The label decides which fields the datum must have when it is
indexed (ADR 0002 decision 3), not what the API returns.

*Why fungible only:* the public API surface is fungible-token focused today, and storing NFT and RFT rows while not exposing
them means their rules can change without a breaking API change (ADR 0002 consequences). *Trade-off:* clients that want NFT metadata
cannot get it from this module yet.

### 5. A skipped CIP-68 update leaves the previous row in the answer

The API serves the latest **stored** row. When a CIP-68 datum is skipped, nothing is written, so the row of the earlier datum
stays the latest and is served as if it were current. The response does not say that a newer datum was refused. The full
description, the mainnet numbers and the alternatives that were rejected are in ADR 0002 decision 10. This record only makes the
consequence visible from the read side: **the API answers "the last accepted metadata", not "the metadata of the newest datum"**.

### 6. A batch is read with a fixed number of queries

`POST /subject/query` takes 1 to 100 subjects and reads them with at most three queries whatever its size: the CIP-26 rows, the
CIP-26 logos (only if `logo` is requested) and the CIP-68 rows. The merge then runs in memory per subject. The CIP-26 logo is
a separate query in the single-subject path as well, and is only read when `logo` is requested, because it can be large.
Properties the request does not ask for are not fetched where that is cheap (the logo), and are filtered out of the response
otherwise.

*Why:* reading each subject with its own queries would make a 100-subject batch hundreds of round trips (the code names this
the N+1 pattern). *Trade-off:* the batch path and the single path share the merge but not the fetching code, so a change to one has to be made in
the other.

## Consequences

- A client can rely on `name` and `description` being present in any answer, and on each property naming its source.
- Two clients that ask with a different `query_priority` can get different answers for the same token. That is intended.
- The same data can answer differently before and after a CIP-26 sync, because the registry is read periodically
  (ADR 0001), while CIP-68 is updated block by block.
- NFTs and RFTs have no API. When one is added it needs its own rules for what is served and in which shape.
- Whatever makes a CIP-68 datum not indexed (ADR 0002) changes what this API returns only if the token had no earlier row. A
  token with an earlier row keeps answering with it (decision 5).

## Open questions

- Should the response say when the CIP-68 row it serves is older than a datum that was refused? The module has the
  information (the skip is logged and counted) but does not store it, so answering would need a record of refused datums.
- Should NFTs and RFTs be exposed, and under which endpoint? Nothing reads them today.
- Should CIP-25 metadata become a third source? It would need an indexer for transaction metadata, its own rules for what is
  valid (the standard has more than one version), a place in the priority order, and an ADR of its own. Nothing in the module prepares for it.
- Should the merged endpoint report which sources were consulted when a token is not found, so that a 404 for a registry that
  is switched off is distinguishable from a token that is in neither source?

## References

- ADR 0001 (CIP-26 ingestion) and ADR 0002 (CIP-68 datum handling): what is stored and when a row is skipped
- `TokenMetadataSubjectController`, `TokenQueryService`, `Metadata`, `Standards`, `Cip68StorageReaderImpl`,
  `Cip68MetadataRepository`
