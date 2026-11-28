# ADR 0012: CIP-113 in the read model and API

## Status

Accepted. Implemented in PR #1433. It amends ADR 0003 (decisions 1, 3 and 6) and ADR 0006 (decisions 2 and 3) for CIP-113. ADR 0011 says
how the registry nodes are stored.

## Date

2026-11-26

## Context

ADR 0003 defines the read model for two sources, CIP-26 and CIP-68, merged per property. CIP-113 is different from both: a registry
node holds **no display metadata**. It says that a policy is programmable and which scripts govern its tokens. The name, ticker, decimals
and logo of a programmable token still come from CIP-26 or CIP-68, its *companion* metadata.

The module must serve the same data as cf-token-metadata-registry's V2 API, so that clients can move between the two (ADR 0011, Context).
yaci-store has one subject API, under `${apiPrefix}/tokens` (`/api/v1` by default): it serves the shape of CF's V2 API, there is no
separate V1 or V2 version of it.

## Decisions

### 1. A subject has a `type`, and CIP-113 data goes under `extensions`

Two fields are added to the merged `Subject`:

| Field | Content |
|---|---|
| `type` | Always present. `PROGRAMMABLE` when the subject's policy has a current node in a configured registry, `NATIVE` otherwise |
| `extensions` | A map keyed by CIP. Today only `cip113`, with `minting_logic_script`, `transfer_logic_script`, `third_party_transfer_logic_script`, `unfracking_logic_script` and `global_state_policy_id`; each is `null` when the node has none. Left out when the map is empty, so a native token has no `extensions` |

- **The policy ID** is the first 56 characters of the subject, matched in lower case, since the nodes' keys are stored as the datum
  holds them.
- **`extensions` is a map, not a `cip113` field,** so that a later CIP can add its own key without changing the shape. `Extension` is an
  interface; its OpenAPI schema lists the implementations (`oneOf`).
- **CIP-113 is not part of the merge.** It has no property to merge, so it has no place in `query_priority`. `standards` (with
  `show_cips_details`) stays CIP-26 and CIP-68 only.
- **`property` and `show_cips_details` do not filter `type` or `extensions`.** They are about the token, not about a metadata property.
- **A token whose policy is not registered is `NATIVE`,** even when it sits at the programmable-logic address (ADR 0007).
- **When CIP-113 is off** (`store.assets.ext.cip113.enabled=false`), or no registry is known for the network, every token is `NATIVE`.

*Why:* this is the shape of CF's V2 API. *Trade-off:* `type` is a new required field. It is additive, so a client that does not know it
ignores it.

### 2. A programmable token without companion metadata is served, with a `MANDATORY` error

This amends ADR 0003 decision 3. A subject is now **found** if CIP-26 or CIP-68 has something indexed for it, **or** its policy has a current
registry node. A registered policy with no CIP-26 or CIP-68 metadata is returned with:

- no metadata properties (`metadata` is only `{ "valid": false }`);
- one `validation_errors` entry: `cip` `CIP-113`, `field_name` `companion_metadata`, `field_category` `MANDATORY`, `code` `missing_value`.

The error is added after the `property` filter, so it is always reported, as `no_nested_entry` is (ADR 0003, Consequences): with no
companion metadata, every property is missing. `?validation=MANDATORY_ONLY` and `STRICT` leave such a token out, as for any `MANDATORY`
error. A programmable token that has companion metadata gets no CIP-113 error: its errors are those of CIP-26 and CIP-68.

*Why:* it follows ADR 0006: the module serves what it has indexed, and says what is wrong. A client that only wants complete tokens uses
`MANDATORY_ONLY`. *Trade-off:* a subject that was a 404 before CIP-113 can now be a 200 with an error, like the change of ADR 0003
decision 3.

### 3. No per-standard endpoint for CIP-113

ADR 0003 decision 1 gives each standard its own endpoint (`/cip26/**`, `/cip68/ft/**`), so that an operator can see what one source holds
without the merge. CIP-113 has none.

*Why:* those endpoints exist to see a standard's metadata without the merge, and CIP-113 has no metadata and takes no part in the merge.
`extensions.cip113` on the merged endpoint is already the node as stored, unchanged.
*Trade-off:* only the current node is served. The history of a node, and the nodes of a registry that is no longer configured, are only
in the table (ADR 0011 decision 3). An endpoint can be added if operators need it.

### 4. A batch reads the nodes in one more query

This amends ADR 0003 decision 6. `POST /subject/query` reads the current nodes of all the policies of the batch in one query, so a batch takes
**at most four queries** whatever its size: the CIP-26 rows, the CIP-26 logos (only if `logo` is requested), the CIP-68 rows and the
CIP-113 nodes. The single-subject path takes one more indexed lookup. Both read only the configured registries (ADR 0011 decision 3).

## Amendments to ADR 0006

- **Decision 2, the contract:** `cip` can also be `CIP-113`. Its only error is `companion_metadata` (decision 2 above), with the existing
  code `missing_value`. It has no `sub_standard`.
- **Decision 3, what is dropped:** an output that holds a registry NFT but is not a real node is dropped and recorded in `invalid_token`,
  as `registry_node_unparseable` or `registry_node_spoofed` (ADR 0011 decision 4, ADR 0007). This is not metadata that breaks its CIP, which
  ADR 0006 keeps and reports: the registry's validator only creates well-formed nodes at its address with the NFT of their key, so such an
  output was not written by the registry. Spoofed outputs are the attack case the list already allows. An unparseable one holds a real
  registry NFT, but a datum that is not a node, so there is nothing to serve.

ADR 0005 decision 1 ("one rule for both standards") applies to CIP-26 and CIP-68, the standards that carry metadata. CIP-113 adds no
metadata rule: its only check on what is served is decision 2 above.

## Consequences

- **A client can tell a programmable token from a native one** with `type`, without knowing the registry, and gets the scripts that govern
  it in `extensions.cip113`.
- **The answer depends on the configuration.** Switching CIP-113 off, or changing the configured registries, changes `type` and
  `extensions` at once, without a resync (ADR 0011 decision 3).
- **A token registered before the sync's start is served as `NATIVE`.** The node must have been indexed (ADR 0011, Consequences).
- **The regression suite** (`regression-tests`, marker `cip113`) compares `type`, `extensions.cip113` and the `companion_metadata` error with
  cf-token-metadata-registry, for the programmable tokens of its fixtures on mainnet and preprod (generated from the chain).

## Open questions

- Should there be a per-standard CIP-113 endpoint (decision 3), for example `GET /cip113/{policyId}` with the node's history?

## References

- ADR 0003 (read model and API), ADR 0005 (validation policy), ADR 0006 (validation errors), ADR 0007 (`invalid_token`), ADR 0011 (CIP-113
  storage)
- `TokenQueryService`, `Subject`, `TokenType`, `Extension`, `ProgrammableTokenCip113`, `Cip113Validation`, `Cip113StorageReader`
- cf-token-metadata-registry V2 API and its ADR-016 (CIP-113)
