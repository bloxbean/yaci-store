# ADR 0002: CIP-68 on-chain datum handling

## Status

Proposed

The behaviour in decisions 2 to 6, 8 and 9 is implemented in PR #1220 (draft, not merged at the time of
writing; issues #1221, #1222, #1225, #1226, #1227 and #1228). Decisions 1, 7 and 10 describe behaviour
that is on `main`.

## Date

2026-10-07

## Context

CIP-68 metadata is read from the inline datum of a reference NFT, and the datum is written by the
token's minter. Nothing checks it before it reaches the chain, so the module reads data that is
sometimes malformed, sometimes hostile, and often not what the CIP says: live mainnet tokens have an
empty `image`, `iagon://` URIs, free text where a URI belongs, constructors inside the metadata map,
logos split into chunks, versions 0 and 100.

The module therefore has to decide, field by field, when to be strict (drop the datum or the value)
and when to be lenient (keep it, and say so). This ADR records those decisions. It does not restate
CIP-68. CIP-26 is covered in [ADR 0001](0001-cip-26-offchain-registry-ingestion.md).

The general rule the module follows: **be strict about safety, lenient about content.** A value that
could break an insert, exhaust the stack or wrap a number is rejected or dropped. A value that is
merely unusual or not spec-compliant is stored as written, because dropping it hides a real token and
a stored value can be filtered later while a dropped one needs a resync to recover.

## Decisions

### 1. What is indexed, and how history is kept

A reference NFT is an output holding exactly one unit of an asset whose name starts with `000643b0`
and an inline datum. Each such output becomes a row in `cip68_metadata`. The key includes `tx_hash`,
so every metadata update is a new row and the table keeps the full history. The latest row for a
reference NFT is the one with the highest `(slot, tx_index)`. A rollback deletes the rows after the
rollback slot.

*Trade-off:* the table grows with updates. In exchange, a rollback needs no reconstruction and the
history of a token is queryable.

### 2. The label comes from the paired user token, and from the datum when there is none (#1222, #1225)

The label (222 NFT, 333 FT, 444 RFT) is not in the datum. It is read from the user token minted with
the reference NFT: the asset in the same transaction with **the same policy and the same base name**
(reference NFT `000643b0 + base`, user token `<label prefix> + base`). If several are paired, the order
is 222, then 333, then 444.

The earlier rule took the prefixes of every asset in the transaction, whatever its policy or name, with
222 winning. A transaction that also contained any unrelated 222 NFT then labelled every reference
NFT in it 222. On mainnet, FLDT, KWIC and Shards (each minted next to its own 333 token) and Wrapped
SILVER and Wrapped pUSDC were stored as 222 for that reason, and 68 tokens whose latest row is 222 or
444 carry fungible-token fields (77 if tokens that a later update relabelled 333 are counted). 357
reference NFTs have rows with more than one label over their history.

**When no user token is paired** (it was minted in another transaction), the label is **inferred from the
fields the datum carries**:

| Datum has | Label |
|---|---|
| `ticker` or `logo` (only the fungible token defines them) | 333 |
| `image`, `mediaType` or `files`, and `decimals` | 444 |
| `image`, `mediaType` or `files`, no `decimals` | 222 |
| none of these | 333 |

`decimals` alone does not decide, since the fungible token and the RFT both define it. A paired user token
always wins over this guess.

*Why pair first, and guess only as a fallback:* the pairing is how CIP-68 defines the relation and it does
not guess; the datum shape is a heuristic and is used only when the pairing is not available. Before the
inference, such a reference NFT was labelled 333. That is right for a fungible token (Wrapped SILVER) and
wrong for an NFT, and 333 requires a description, so a description-less NFT minted that way was dropped.

*Why infer and not look the user token up across transactions:* the lookup is exact but needs a UTxO or
asset lookup at indexing time, which depends on other stores being enabled and costs a query per reference
NFT. It was not done. *Trade-offs and known gaps:*

- The inference can be wrong for a datum that mixes both kinds of fields. How many reference NFTs have no
  paired token, and how many the guess would label differently from their real user token, is not measured.
- With both a 222 and a 333 paired, 222 wins (USDCx/USDrf LP has both on-chain). Nobody has decided that
  this is right; it keeps the previous order.
- The opposite error (an NFT stored as 333) was possible before and is not measured.
- Rows already stored keep their label until their datum is processed again.

What the label does and does not decide: it chooses the required fields (decision 3) and the set a
row belongs to (the API reads fungible tokens only, see decision 10). It does **not** change what the
API returns for a fungible subject, which reads the latest row of the reference NFT whatever its label.

### 3. Required fields per label: strict for fungible tokens, lenient for NFTs and RFTs (#1221)

| Label | Required to index | Spec |
|---|---|---|
| 333 FT | `name` and `description` | both required |
| 222 NFT, 444 RFT | `name` | `description` is optional, `image` is required |

The processor derives the label first and then validates, because the check depends on it.

A fungible-token datum without a description is **not indexed**. This is a deliberate choice: fungible
metadata without a description has little to show, the spec requires it, and the read path requires
a description in the merged result (decision 10), so a CIP-68 row without one would only be useful if
CIP-26 supplies it. How many such tokens exist on-chain has not been measured. NFTs and RFTs are kept
without a description, which the spec allows. Before #1221 the same rule was applied to every label and
valid NFTs were dropped silently; on mainnet at least 203 datums were affected (a lower bound).

### 4. `image` is lenient: index, warn, do not validate

CIP-68 requires `image` for 222 and 444, and lists `https`, `ipfs`, `ar` and `data` as URI schemes.
The module follows neither strictly:

- A 222 or 444 token with no image, or an empty one, **is indexed, with a WARN** naming the policy and
  asset. A 333 token is not checked, it uses `logo`.
- `image` and `logo` are stored **as written**. The URI scheme is never validated, so `iagon://` URIs
  and free text are kept.

*Considered and rejected:* enforcing the spec. On mainnet that would remove 12 genuine NFTs whose datum
has `mediaType: image/svg+xml` and an empty `image` (the "DID registration" sensor tokens) and 9 more
whose image is not a spec URI (6 `iagon://` wine NFTs and 3 free-text values), for no benefit to any
consumer: NFT images are not exposed by the API today. `iagon://` support is to be proposed to the CIP
separately; until then it is accepted like any other value. *Trade-off:* the table can hold images that
no client can open, and nothing is validated.

### 5. A logo or image given as a list of chunks is joined as bytes (#1226)

A Plutus byte string is at most 64 bytes, so a longer URI (a `data:` URI, a long URL) can only be stored
as a list of byte strings. The CIP defines this as `uri = bounded_bytes / [* bounded_bytes]`. The module
joins the chunks for `logo` and `image`. The chunks are joined as **bytes** and decoded once, so a
multi-byte character cut by a chunk boundary survives (decoding each chunk on its own corrupted it).
Elements that are not byte strings are ignored. The other typed fields (`name`, `description`, `ticker`,
`url`) are plain `bounded_bytes` in the CIP and are not joined.

On preprod, 240 of the 1,968 fungible tokens (181 with 2 chunks, 42 with 3, 14 with 4, 3 with 21) store
their logo this way (not measured on mainnet). The CF token metadata registry does not read them and
returns no logo (see cardano-foundation/cf-token-metadata-registry#104).

### 6. Untrusted values are bounded; the rest of the datum is kept where possible (#1185, #1227)

| Input | Rule | Effect |
|---|---|---|
| `version` | must fit in a `long` | the datum is skipped, with a WARN |
| `decimals` | must be in [0, 255] and fit in a `long` | the value is dropped, the datum is kept |
| `name`, `ticker`, `url`, `mediaType` | at most 255, 32, 250 and 255 characters, counted in UTF-16 units | the value is dropped; a dropped `name` skips the datum (decision 3) |
| `logo`, `image` | at most 64 KiB, measured on the joined bytes | the value is dropped with a WARN, the datum is kept |
| nesting depth | the CBOR decoder recurses; a `StackOverflowError` is caught | the datum is skipped, with a WARN |

*Why:* a datum integer is unbounded, so narrowing it can wrap a large value into a plausible one; an
over-long value would fail the insert and stop the sync; a deeply nested datum would crash the
processor, and a valid on-chain datum can be nested deeper than the stack allows. Characters are counted
in UTF-16 units because H2 counts that way, and the bound must be safe on every supported database.
*Trade-off:* a long non-BMP name that PostgreSQL alone could store is rejected. The depth guard is a
temporary workaround until the decoder stops recursing (cardano-client-lib#681).

The `logo` and `image` limit is the one the CIP-26 logo already has; the CIP-68 spec gives none. It cannot
trigger on current data: the ledger limits a transaction to `maxTxSize` (16,384 bytes at epoch 660), so a
datum cannot reach 64 KiB. It is there so the module does not rely on that parameter staying where it is.
`description` is `TEXT` and still has no size bound.

The other bounds did not trigger on a full mainnet or preprod sync.

### 7. The version is stored as written, with no allow-list

The version is the second element of the datum and must be an integer. Any value that fits a `long` is
stored: mainnet has 0, 1, 2, 3 and 100. Versions 4 and above are read as a candidate for the nested
format `{"721": {policy: {asset name: metadata}}}`: if the `"721"` key is present the entry for the
reference NFT is used (without a reference NFT it is used only if it is the single entry), and if the key
is absent the map is read as a flat one. A nested entry that cannot be resolved skips the datum.

*Why no allow-list:* the CIP adds versions, and a fixed list would drop a token the day a new one
appears. *Trade-off:* an unknown version above 4 whose map happens to contain `"721"` is read as nested.
Nothing like that has been seen on mainnet; preprod has 59 version-4 rows, one of them nested.

### 8. Text is text when it is UTF-8, hex when it is not; what cannot be represented is left out (#1159, #1226)

CIP-68 says text is UTF-8, and Plutus has one byte-string type for text and binary data alike. Every text
value (the typed fields `name`, `description`, `ticker`, `url`, `mediaType`, `logo` and `image`, and the
values in additional properties) is stored as text if the bytes are valid UTF-8, and as **hex** otherwise.
Null characters are stripped from text. Before #1226 the typed fields were decoded with replacement
characters, so bytes that are not UTF-8 became `U+FFFD` and were lost (97 rows on a preprod sync, about
0.4%; not measured on mainnet).

Keys other than the typed ones go to the `properties` column (`files` and `additional_properties`). In it,
a Plutus constructor is stored as `{"constructor": n, "fields": [...]}`. A value with no JSON form is left
out on its own, not the whole datum: one unrepresentable property used to drop a token entirely (448
warnings for 434 distinct datums on a mainnet sync of the earlier code).

*Trade-offs:* a string such as `deadbeef` can be text or hex, and the reader cannot tell. The API returns
the hex string for the few tokens whose text field is not UTF-8, where it used to return garbled text.
Hex is twice as long as the bytes, so a non-UTF-8 `name` of more than 127 bytes (or a `ticker` of more than
16, a `url` of more than 125) now exceeds the length bound of decision 6 and is dropped, where it used to
be kept with replacement characters.

### 9. A datum that is not indexed is reported (#1159, #1228)

- A parse failure is one WARN line with the exception and the datum hex, so the case can be reproduced,
  and the stack trace is logged at DEBUG only, because a sync can hit many.
- A datum that parses but is skipped for a missing required field (no `name`, or no `description` on a
  fungible token) is one WARN line with the policy, the asset name, the label and the reason.
- A datum that does not have the CIP-68 shape at all (not a constructor, no metadata map, no integer
  version) is **not** logged: reference NFT outputs can carry arbitrary datums, and it would be noise.

### 10. Read model

- The API serves **fungible tokens** only. A subject with the `0014df10` prefix is mapped to its
  reference NFT, and the latest row of that reference NFT is returned whatever its label. NFT and RFT
  rows are stored but not exposed.
- For one subject, CIP-26 and CIP-68 are combined **per field**: the first non-null value in priority
  order wins, and the default order is CIP-68 then CIP-26.
- The merged result is returned only if it has both `name` and `description`; otherwise the subject is
  treated as not found.

*Why per field:* a token can have a name on chain and a logo only in the registry. *Trade-off:* the
fields of one response can come from two sources, and `show_cips_details` is the way to see which.

## Consequences

- Every skip caused by a missing required field, a parse failure or an over-limit value leaves a WARN.
  Only a datum that does not have the CIP-68 shape is silent.
- Because NFTs and RFTs are stored without being served, their rules can change later without a
  breaking API change.
- Anything corrected in a rule needs a resync from before the affected slots to take effect on old rows.

## Open questions

- Both 222 and 333 paired under one policy and name: is 222 first the right order?
- How good is the datum-shape inference for a reference NFT with no paired token, and is an exact
  cross-transaction lookup worth its cost? Not measured.
- A size bound for `description`, which is still unbounded `TEXT`.
- A counter or health indicator for skipped datums, instead of only log lines.
- Whether a relabel or backfill job is wanted after a label fix, instead of a resync.

## References

- #1159, #1185, #1202, #1208, #1209: parser hardening, version 4, constructor values
- #1221, #1222, #1225, #1226, #1227, #1228, PR #1220: required fields per label, label pairing and
  inference, text and hex, size cap, skip warnings, lenient image
- cardano-foundation/cf-token-metadata-registry#104: backport of these fixes
- `Cip68Processor`, `Cip68DatumParser`, `Cip68TokenService`, `TokenQueryService`
