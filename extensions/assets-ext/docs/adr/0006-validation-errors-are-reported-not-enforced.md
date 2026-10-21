# ADR 0006: Validation errors are reported, not enforced

## Status

Proposed. Implemented in PR #1220 (open). It is meant for yaci-store 3 and is not backported to 2.x (see decision 8).

## Date

2026-10-20

## Context

Until now the module enforced CIP-26 and CIP-68 by **dropping**: a CIP-68 datum without a mandatory property was not indexed, an
optional property that did not fit was left out, a datum with a version CIP-68 does not define was skipped, and a CIP-26 entry
that failed the registry's validator was not stored (the behaviour of the module before this decision). The API
then refused to serve a token whose merged metadata had no `name` and `description`.

The team decided, after a call, that the module should not enforce standards that way:

- The module is an index of what is on-chain and in the registry. Enforcing the CIP by deleting tokens hides them from the people
  who most need to see them: the owners who have to fix a datum, and the wallets that want to decide for themselves.
- A dropped token is invisible. Its owner gets nothing from the API, only a WARN in the log of one node, and cannot tell why their
  token does not show. A token with a list of what is wrong can be fixed, and a wallet can be strict and reject it.
- The numbers show how much was dropped: on the full mainnet sync of the #1220 branch (build `ebb9daa37`), several thousand tokens
  were skipped for a missing name, description or image, or an image scheme such as `iagon://`; Greenland Reserve Coin, a live token
  whose datum has version 0, and 217 fungible tokens with a logo that is not a URI with an allowed scheme were affected too.

## Decision

### 1. Index everything, serve it as indexed, and say what is wrong

A CIP-26 entry that can be read and a CIP-68 datum that can be decoded are **stored as written and served**. What departs from
the CIP is not repaired and not dropped: it is described in a list of **validation errors** that is stored with the row and returned
by the API. Values are kept as they are: a `logo` with a bad scheme, an `image` that is `iagon://…`, a `files` entry without a
`mediaType`, a `decimals` of 300, a Plutus constructor in a property (stored as `{"constructor": n, "fields": […]}`), a version 0.

*Why:* the index records the chain; judging it is for the consumer. *Trade-off:* the API returns tokens that a strict reader of the
CIP would not accept, and a client that relied on `name` and `description` always being there must now look at the errors.

### 2. The contract: `validation_errors`

Every response that carries token metadata has a `validation_errors` array, empty when the token follows its CIP. An entry has:

| Field | Meaning |
|---|---|
| `cip` | `CIP-26` or `CIP-68`. |
| `sub_standard` | For CIP-68, the label the rule was checked for (`222`, `333`, `444`). A reference NFT with user tokens of several labels is checked against each, and each error names its label. Absent for CIP-26 and for a rule that does not depend on a label (for example a bad `logo`). |
| `field_name` | The field, for example `description`, `image`, `version`, `files`, `properties.owner`. |
| `field_category` | `MANDATORY` if the CIP requires the field (for CIP-68, as the label requires it; `name` and `description` for CIP-26), `OPTIONAL` otherwise. |
| `code` | A stable reason for programs: `missing_value`, `wrong_type`, `invalid_uri_scheme`, `invalid_length`, `out_of_range`, `undefined_version`, `plutus_constructor`, `not_utf8`, `invalid_files_entry`, `no_nested_entry`, `too_deep`, `unsupported_key`, `key_collision`, `invalid_base64`, `too_large`. |
| `error` | A message for people, for example `description field is missing a mandatory value`. |

The field is **additive**: no existing field is renamed or removed. `metadata.valid`, which the response always had, stays and is now
`true` exactly when `validation_errors` is empty. It is on the merged `Subject` (the union of the errors of every
standard that has the token, in query priority order), on `standards.cip26` and `standards.cip68`, on `/cip68/ft/**` and on
`/cip26/**`. The wire name is snake_case, as in the team's example; the rest of the module's JSON is camelCase, so the name is
set explicitly. The `code` and the `sub_standard` were added to the team's example so that a program does not parse messages; new
codes may be added, existing ones are not changed.

### 3. What is still dropped: only what looks like an attack, or cannot be stored

A datum or entry is dropped, with one WARN that says it looks like an attack, only when it is obviously not metadata:

- a datum nested so deep that it cannot be decoded (`StackOverflowError`), or that cannot be decoded at all;
- a property whose value is nested more than 100 levels (ADR 0002 decision 6, issue #1240); the property is dropped and the rest of
  the datum is indexed, with a `too_deep` error;
- a CIP-26 entry whose file name differs from its inner `subject` (the registry has thousands of these on testnets), and one whose
  `subject` cannot be a key (not even-length hex, not 56 to 120 characters);
- a CIP-26 logo far beyond any real size (over about 1 MiB; the CIP's limit is 64 KiB, so anything between is kept with an error);
- an output whose indexing throws something unexpected (`processing_failure`, ADR 0004).

What is dropped is recorded in the `invalid_token` table (ADR 0007).

There are no length caps on text: a datum is at most one transaction (16 KB), so what is stored is bounded, and the columns are
`TEXT`. A version that is an integer outside the range of a `BIGINT` is stored as no version, with an error.

### 4. Persisted, computed when the row is written

The errors are computed by the parser, the validator and the processor, and stored in a new `validation_errors` JSON column of
`cip68_metadata` and `cip26_metadata` (`JSONB`, `JSON`, `TEXT` on H2; nullable). Reading a row costs nothing extra, and the batch
API makes no new query. A row written before the column existed has `NULL`, which means *never checked* and is not the same as `[]` (*checked, no errors*): the API
leaves `validation_errors` out for it, and `metadata.valid` falls back to the rule it had before (name and description both present). The
migration is additive (`V0_1700_2`, with `V0_1700_3`) and rewrites no data, so a node upgrades in place and keeps working. The tokens that were
dropped before exist nowhere, and the old rows have no errors, so a **full resync** is needed for complete results; there is no backfill. When
a token merges several standards, `validation_errors` is left out only if a standard has none recorded and no other reports any.

*Alternative rejected:* computing the errors at read time from the stored datum or mapping. It needs no migration, but it runs the
parser on every request, and it cannot bring back what was never stored.

### 5. The API serves what is indexed

The read-model gate of ADR 0003 decision 3 is gone: a merged result no longer needs `name` and `description`, a property filter no
longer has to include them, and a token with errors is `200`, not `404`. A subject is `404` only when no standard has anything indexed
for it. A token whose datum has no usable entry (a nested `"721"` datum with none for the token) is indexed with no metadata and a
`no_nested_entry` error. A rejected update no longer leaves the earlier row in place (ADR 0003 decision 5): the newest datum is the
one served, with its errors.

A client that wants a stricter view asks for it with `?validation=ALL|MANDATORY_ONLY|STRICT` (default `ALL`): `MANDATORY_ONLY` returns only
tokens with no `MANDATORY` error and `STRICT` only tokens with no error at all. A token that is not accepted is a `404` (left out of a
batch); the errors of a returned token are never filtered. The check is made in Java on the row already read, so it needs no native query.

### 6. Rich fungible tokens are served

A `001bc280` subject maps to its reference NFT like a `0014df10` one. A rich fungible token has no `logo`; its `image` is served as
`metadata.logo`, and `description` is optional for it (a token without one is not an error). NFTs (label 222) are still indexed
but not served. This follows cf-token-metadata-registry ADR-018, since for now both implementations are kept.

### 7. How it is reported

- The errors are the report: they are in the API. A datum that is indexed with errors is not a WARN (there would be a line per
  token, and tens of thousands on mainnet); it is a DEBUG line. A WARN is for what is dropped.
- Metrics: `datums.indexed{label}` counts every stored datum; `datums.validation_errors{sub_standard,code,category}` counts the
  errors (the field is not a tag, since property names come from the chain); `datums.skipped{reason}` is only for `parse_failure` and
  `processing_failure`; `properties.dropped{kind}` is only `too_deep`. Errors that depend on the datum, not on a label, are counted
  once per output when several reference NFTs share a flat datum.

### 8. Where it ships

yaci-store 3. The migrations are additive and the API tells "never checked" (`NULL`, field left out, the old `valid` rule) from "no errors"
(`[]`), so a later 2.x backport of the field, the migrations and `invalid_token` can upgrade a database in place without a resync, with the rows
written before it unchecked. The policy that serves tokens a strict reader would drop stays with 3.x for now; the backport PR #1242 of rules
that drop a token is on hold.

## Consequences

- Owners and wallets see why a token is not what the CIP asks. A wallet can reject tokens with a `MANDATORY` error.
- A client that treated `404` as "no valid metadata", or relied on `name` and `description`, has to adapt. This is the one
  behaviour change, and it is listed in the README.
- The index is larger than before (the tokens that were dropped, several thousand on mainnet) and every row carries a small JSON list.
- `cf-token-metadata-registry` and the module no longer agree on which tokens exist: the registry hides what it cannot validate, the
  module serves it with errors. This is accepted while both exist.
- A change to a rule changes the errors of rows already stored; they are recomputed by a resync.
- More odd values now reach the database (long names, constructors in properties). Issue #1265, a database failure that still stops the
  sync, matters more.

## Alternatives considered

- **Keep dropping (ADRs 0001 to 0005).** Rejected for the reasons above.
- **A switch between strict and lenient.** Rejected: it gives every operator a decision they are not in a position to make, and two
  behaviours to test.
- **Repair what is wrong (a bare IPFS hash to `ipfs://…`).** Rejected: the index would then state something that is not on-chain.

## Open questions

- Whether the `code` list should be fixed in the API documentation as an enumeration.
- Whether the errors of a property filter (`property=ticker`) should be limited to the fields asked for. Today all errors are returned.
- Whether CIP-26 entries whose file name differs from the subject should also be indexed under the file name with an error. They are
  dropped today, since the registry has thousands of them and last-write-wins would make the data ambiguous.

## References

- ADRs that follow from this decision: 3 to 5 of ADR 0001; 3 to 9 of ADR 0002; 2, 3 and 5 of ADR 0003; 1, 2 and 4 of ADR 0005
- cf-token-metadata-registry ADR-018 (rich fungible tokens) and PR #110
- Issues #1221 to #1243, #1265
- `Cip68DatumParser`, `Cip68Validator`, `Cip26MetadataValidator`, `ValidationError`
