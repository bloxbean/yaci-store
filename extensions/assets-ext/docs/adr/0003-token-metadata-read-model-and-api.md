# ADR 0003: Token metadata read model and API

## Status

Proposed

Decisions 3, 4 and 5 and the `validation_errors` field apply ADR 0006 (validation errors are reported, not enforced) and are implemented in
PR #1220 (open).

## Date

2026-10-14

## Context

`assets-ext` stores token metadata from two unrelated sources: CIP-26 (an off-chain registry read from Git, ADR 0001) and
CIP-68 (on-chain datums, ADR 0002). A client does not care where a name or a logo came from. It asks for a token and
expects one answer. The module therefore has to decide how the two sources are combined, what counts as a token that
is known, which CIP-68 rows the API may serve, how it says that a token breaks its CIP, and which endpoints exist.

Two facts shape everything below:

- The sources overlap and disagree. A token can have a name on chain and a logo only in the registry, or the same
  `name` in both with different text.
- The module is optional. The CIP-26 side is off by default (`store.assets.ext.cip26.enabled=false`) because the registry is
  a separate trust source from the chain, and the CIP-68 side is on by default.

**Not covered: CIP-25.** The metadata of NFTs minted with CIP-25 (the transaction metadata standard, label 721) is not
indexed and not served. CIP-25 appears in the module's code only as a format that CIP-68 borrows: the nested `"721"` map
of a version 4 datum and the splitting of long strings into chunks of at most 64 bytes (ADR 0002 decisions 1 and 5). A
token with CIP-25 metadata and no CIP-26 or CIP-68 metadata has no answer from this API.

**NFT support is an initial version, not full support.** The module indexes and stores CIP-68 NFTs (label 222) and RFTs
(label 444), with the rules of ADR 0002. It serves the RFTs, as fungible tokens (decision 4), and not the NFTs, so that the data
exists and the rules can change before anything depends on them; it has no NFT-specific DTO, service method or endpoint. This
record is about fungible tokens; read "token" in it as a fungible or rich fungible token unless it says otherwise.

## Decisions

### 1. One merged endpoint, plus one endpoint per standard

| Endpoint (under `${apiPrefix}/tokens`) | Returns |
|---|---|
| `GET /subject/{subject}` | The merged metadata of one subject |
| `POST /subject/query` | The merged metadata of up to 100 subjects |
| `GET /cip26/{subject}` and `/cip26/{policyId}/{assetName}` | The CIP-26 row only |
| `GET /cip68/ft/{subject}` and `/cip68/ft/{policyId}/{rawAssetName}` | The CIP-68 fungible and rich fungible token metadata only |

Every one of them returns `validation_errors` (decision 3).

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

The `validation_errors` are not merged per property: the errors of **every** source that has the token are returned, in the same
priority order, and each names its `cip` (and, for CIP-68, its `sub_standard`). A value from one source can come with an error
about the other source's value, which is what the client wants to know.

### 3. A token is returned as indexed, with the errors it has

After the merge, a subject is **found** if any standard has something indexed for it, and it is returned whatever is wrong with it:
no `name` or `description` is required, and a token that breaks its CIP is a 200, not a 404. The single endpoint answers 404 only when
no standard knows the subject, and the batch endpoint leaves such a subject out of its 200 response. A request that filters the
properties with `property` is no longer required to include `name` and `description` (it was a 400 before).

Every response has a **`validation_errors`** array (`[]` when the token follows its CIP; left out for a row indexed before the field existed, see below): on the merged `Subject`, on `standards.cip26` and
`standards.cip68`, and on the per-standard endpoints. Each entry has `cip`, `sub_standard` (the CIP-68 label; absent otherwise), `field_name`,
`field_category` (`MANDATORY` or `OPTIONAL`), `code` and `error` (ADR 0006 decision 2). A client that wants the old guarantee can reject any
token with a `MANDATORY` error. The `metadata.valid` flag stays in the JSON where it has always been. It used to be `name` and `description` both present, which was the gate, so it
was `true` for every token the API returned. It is now `true` when `validation_errors` is empty: the token follows its CIP completely, optional
fields included (an optional field that is broken is a defect the owner should fix too). It is set after the merge, from the errors of every
standard that has the token, so it can now be `false`. A client that only cares about the mandatory fields looks for a `MANDATORY` entry. The array is not filtered by `property`: it describes the token, not the fields asked for.

*Why:* an index that hides a token because its datum is wrong tells the owner nothing (ADR 0006). *Trade-off:* a client can no longer rely on
`name` and `description` being present, and a token that was a 404 is now a 200 with errors. This is the one change to the contract that is not
purely additive, and it is why the README lists it; the `validation_errors` field itself is additive.

### 4. Fungible and rich fungible tokens from CIP-68 are served

The CIP-68 table stores rows for labels 222, 333 and 444. A subject with the fungible prefix `0014df10`, or the rich fungible prefix `001bc280`,
is mapped to its reference NFT (prefix `000643b0`) and the latest row of that reference NFT is returned. NFT rows are stored and not exposed.
A rich fungible token has no `logo` in CIP-68; its `image` is served as `metadata.logo` (source `CIP_68`), as cf-token-metadata-registry does,
and its `description` is optional (not an error). An image whose scheme the CIP does not allow is served as written, with an `invalid_uri_scheme`
error, so a wallet can decide.

The latest row is chosen **whatever its label**. A reference NFT can be updated in a transaction that also mints a user
token of another label, so its newest row may carry label 222 although the token is fungible; filtering by label would then
return an older row, which is worse than an unexpected label. The label decides which fields are mandatory for the token (ADR 0002
decision 3), not what the API returns.

*Why these two:* the public API surface is fungible-token focused today, and the registry serves RFTs since cf-token-metadata-registry
#110, so the two implementations stay comparable. Storing NFT rows while not exposing them means their rules can change without a
breaking API change. *Trade-off:* clients that want NFT metadata cannot get it from this module yet.

### 5. The newest datum is the one served

The API serves the latest **stored** row, and every datum that can be decoded writes a row (ADR 0002), so the newest datum is the one
returned, with its own errors. An update is never rejected, so there is no longer a case where the row of an earlier datum is served as if
it were current. (A design that skipped bad datums would write nothing, and the API would answer "the last accepted metadata".) A datum that is dropped as an attack (ADR 0006 decision 3) still writes nothing, so the row of the earlier datum stays; that is the one
remaining case, and it is logged.

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

- A client can rely on each property naming its source and on `validation_errors` being there, but not on `name` and `description`:
  it checks the errors.
- Two clients that ask with a different `query_priority` can get different answers for the same token. That is intended.
- The same data can answer differently before and after a CIP-26 sync, because the registry is read periodically
  (ADR 0001), while CIP-68 is updated block by block.
- NFTs have no API. When one is added it needs its own rules for what is served and in which shape.
- Rows indexed before `validation_errors` existed have `NULL`, which means *never checked* and is not `[]`: the field is left out, and
  `metadata.valid` falls back to the rule it had before (name and description both present). A resync computes the errors and brings in the
  tokens that were dropped (ADR 0006 decision 4).
- **`?validation=ALL|MANDATORY_ONLY|STRICT`** (default `ALL`) chooses which tokens are returned: `MANDATORY_ONLY` those with no `MANDATORY` error,
  `STRICT` those with no error at all. A token that is not accepted is a `404`, or left out of a batch; the errors of a returned token are never
  filtered. It is applied in Java to the rows already read, so it needs no native query (ADR 0006 decision 5).

## Open questions

- Should the errors be limited to the fields a `property` filter asks for? Today all of them are returned.
- Should NFTs be exposed, and under which endpoint? Nothing reads them today.
- Should CIP-25 metadata become a third source? It would need an indexer for transaction metadata, its own rules for what is
  valid (the standard has more than one version), a place in the priority order, and an ADR of its own. Nothing in the module prepares for it.
- Should the merged endpoint report which sources were consulted when a token is not found, so that a 404 for a registry that
  is switched off is distinguishable from a token that is in neither source?

## References

- ADR 0001 (CIP-26 ingestion), ADR 0002 (CIP-68 datum handling) and ADR 0006 (validation errors): what is stored and what is reported
- `TokenMetadataSubjectController`, `TokenQueryService`, `Metadata`, `Standards`, `Cip68StorageReaderImpl`,
  `Cip68MetadataRepository`
