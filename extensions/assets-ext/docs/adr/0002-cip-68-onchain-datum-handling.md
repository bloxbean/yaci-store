# ADR 0002: CIP-68 on-chain datum handling

## Status

Proposed

The behaviour in decisions 1 to 9 is implemented in PR #1220 (open, not merged at the time of writing;
issues #1221, #1222, #1225, #1226, #1227, #1228, #1231, #1232, #1233, #1234, #1235, #1236, #1237 and #1238). Decision 10 describes behaviour that is on
`main`.

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

The general rule the module follows, the same for fungible tokens, NFTs and RFTs:

- **A mandatory property that violates the CIP: the whole token is not indexed**, with a WARN that says why.
- **An optional property that violates the CIP: only that property is dropped**, the token is indexed, with a WARN
  and a count.

"Mandatory" and "optional" are the ones in the CIP-68 definition of the label (decision 3): `name` and `description`
for 333, `name` and `image` for 222 and 444, everything else optional. "Violates" means the value is missing, has
the wrong type, or is not what the CIP defines for it (a `uri` with another scheme than `https`, `ipfs`, `ar` or
`data`). The datum's `version` is also required and must be one the CIP defines (decision 7).

Two things the rule does not cover. A value that could break an insert, exhaust the stack or wrap a number is
rejected or dropped whatever the field (decision 6): when it is a mandatory property (a `name` over the column
width), the token is not indexed, as the rule says. And a value that is merely unusual, in a field the CIP does not
constrain (an additional property), is stored as written, because a stored value can be filtered later, while a
dropped one is gone from the table until the datum is processed again.

## Decisions

### 1. What is indexed, and how history is kept

A reference NFT is an asset with quantity one whose name starts with `000643b0`, in an output that has an
inline datum. Each reference NFT of such an output becomes a row in `cip68_metadata`. Every output of every
transaction is read, so a transaction that mints many tokens is indexed completely when each reference NFT
has its own output, which is the usual pattern.

An output can also hold **several reference NFTs** (#1231). The version 4 nested format exists for that: its
rationale says the metadata "can consolidate across multiple reference tokens" to reduce the minimum ADA of
large collections. The CIP does not spell out the layout, so this follows from the rationale. The module
handles it by the datum format:

- **Nested datum** (version 4 or above, with the `"721"` key): every reference NFT of the output is
  indexed, each resolved to its own entry in the map. A reference NFT with no entry is not indexed.
- **Flat datum** (versions 1 to 3, or version 4 without `"721"`) (#1236): **every reference NFT of the output is
  indexed with that datum**, and one WARN names the output, the count and the units. The CIP's retrieval steps
  find the output the reference NFT is locked in and read its datum, whatever else the output holds, and only the
  nested map selects an entry, so a flat datum is the metadata of each reference NFT in the output.
  *Trade-off:* some of those tokens may have been given metadata that is not theirs, which is what any client
  following the CIP shows for them too; the WARN lists them. *Considered and rejected:* indexing none, which hides
  every token of the output and treats as an error a case the CIP does not call one; and indexing only the first,
  which was arbitrary (the first asset in the output's list) and matches neither the CIP nor a strict reading.

The key includes `tx_hash`, so every metadata update is a new row and the table keeps the full history. The latest row for a
reference NFT is the one with the highest `(slot, tx_index)`. A rollback deletes the rows after the
rollback slot.

*Trade-off:* the table grows with updates. In exchange, a rollback needs no reconstruction and the
history of a token is queryable.

**Chain rollbacks.** Cardano can roll back a few blocks near the tip. `Cip68RollbackProcessor` listens for the
store's `RollbackEvent` and, in one transaction, deletes every row with a slot **greater than** the rollback
slot (a single bulk `DELETE`, which stays cheap for a deep rollback thanks to the index on `slot`). Rows at the
rollback slot stay, because that block stays on the chain. The row that was the previous datum then becomes the
latest again, a token created after the rollback point disappears, and when the new fork replays the blocks the
processor stores them again under the same key, with no duplicate. It is active together with the CIP-68
indexing (`store.assets.ext.cip68.enabled`), so one cannot be on without the other. The behaviour is pinned by
`Cip68RollbackProcessorH2IT` against the real table definition. It has not been exercised on a live chain yet:
a sync from genesis rarely meets a rollback.

### 2. The label comes from the paired user tokens, and from the datum when there is none (#1222, #1225, #1238)

The label (222 NFT, 333 FT, 444 RFT) is not in the datum. It is read from the user token minted with
the reference NFT: the asset in the same transaction with **the same policy and the same base name**
(reference NFT `000643b0 + base`, user token `<label prefix> + base`). If several are paired, the row is
stored with the first of them in the order 222, 333, 444, **and the datum has to satisfy the requirements of every
paired label** (see below).

Two remarks in the CIP's "Constraints and conditions" shape this rule. The user token does **not** have to
be minted in the same transaction as the reference NFT, so looking inside one transaction is a heuristic,
not the CIP's definition (hence the inference below). And there **may be several user tokens for one
reference NFT**, for example a 222 and a 333 with the same name, so one metadata record can legitimately
serve more than one token type. The table holds one label per row, so the stored label is the first in the
order above, a choice forced by the model that does not claim one of them is the "right" label; what is checked
is not limited to it.

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
- With several paired user tokens (USDCx/USDrf LP has both a 222 and a 333 on-chain), the row keeps one label,
  but the datum is validated against **all** of them (#1238): it needs a `name`, a `description` if 333 is among
  them, and an `image` with an allowed scheme if 222 or 444 is. A datum that fails any
  of them is not indexed, with a WARN that lists the labels and says which one it fails, because the metadata
  is shared by every token of that reference NFT and each of them is a token of its class. Before, only the
  first label was checked, so a datum without a description was indexed although it is also a fungible token,
  and one without an image was dropped although it was valid as a fungible token. *Cost:* some tokens that were
  indexed are now dropped (valid for one label and not for another). A multi-label token is logged at INFO when
  it is indexed, and counted in `yaci.store.assets.cip68.datums.multi_label{labels,outcome}` (decision 9).
  *Considered and not done:* recording every label (a `labels` column or a row per label): nothing reads it
  today, and a row per label would duplicate the datum; it can be added later with a resync.
- The opposite error (an NFT stored as 333) was possible before and is not measured.
- Rows already stored keep their label until their datum is processed again.

What the label does and does not decide: it chooses the required fields (decision 3) and the set a
row belongs to (the API reads fungible tokens only, see decision 10). It does **not** change what the
API returns for a fungible subject, which reads the latest row of the reference NFT whatever its label.

### 3. Required fields per label (#1221)

| Label | Required to index | Spec |
|---|---|---|
| 333 FT | `name` and `description` | both required |
| 222 NFT, 444 RFT | `name` and `image` (decision 4) | `description` is optional, `image` is required |

The processor derives the label first and then validates, because the check depends on it.

Everything else is optional, and a value that violates the CIP is dropped on its own, the token kept:

| Optional property | Violation | Decision |
|---|---|---|
| `logo` (333) | not a `uri` with an allowed scheme (a bare IPFS hash, `iagon://`), or over 64 KiB | 4, 6 |
| `files` (222, 444) | an entry that is not a map, has no `mediaType` or no `src`, or whose `src` is not a `uri` with an allowed scheme: the whole `files` property is dropped | 4 |
| `decimals` | out of range, or not an integer | 6 |
| `ticker`, `url`, `mediaType` | over the column width | 6 |
| `description` (222, 444) | not a byte string | none |
| additional properties | a constructor anywhere in the value, a key with no text form (decision 8), a value nested deeper than 100 levels (decision 6) | 6, 8 |

A fungible-token datum without a description is **not indexed**. This is a deliberate choice: fungible
metadata without a description has little to show, the spec requires it, and the read path requires
a description in the merged result (decision 10), so a CIP-68 row without one would only be useful if
CIP-26 supplies it. How many such tokens exist on-chain has not been measured. NFTs and RFTs are kept
without a description, which the spec allows. Before #1221 the same rule was applied to every label and
valid NFTs were dropped silently; on mainnet at least 203 datums were affected (a lower bound).

### 4. `image` (222, 444) must be a URI with a scheme the CIP allows: strict, drop and warn; a bad `logo` (333) is dropped alone (#1221, #1234)

CIP-68 requires `image` for 222 and 444, and says the URI scheme of `image` and of the 333 `logo` must be one
of `https`, `ipfs`, `ar` or `data`. The module enforces both, but differently, because `image` is required and
`logo` is not:

- A 222 or 444 token with no image, or an empty or blank one, **is not indexed**, and a WARN names the
  policy, the asset name, the label and the reason ("it has no image").
- A 222 or 444 token whose image is not a URI with one of those four schemes (the scheme is compared
  without regard to case, and something must follow the colon) **is not indexed**, and the WARN gives the
  reason and the value (cut at 60 characters). That covers `iagon://` and free text.
- The check runs on the joined value of a chunked image (decision 5), after the size cap (decision 6).
- A 333 token has no `image`. Its `logo` is optional (`? logo: uri`), so a missing, empty or blank one is fine.
  One that is present and not a URI with one of those schemes (a bare IPFS hash such as `Qm...` instead of
  `ipfs://Qm...`, or `iagon://`) is **dropped, and the token is indexed without it**. A WARN names the policy, the
  asset name and the value ("dropping the logo and keeping the rest"), and the counter
  `properties.dropped{kind="bad_logo_scheme"}` goes up. The module does not try to repair the value (adding
  `ipfs://` would be a guess). The check runs on the datum of any label, after the label and the required fields
  have been validated, so a bad logo never makes a datum invalid for any label, including the 333 of a multi-label
  token (decision 2).

*Why:* clients that follow the CIP cannot discover or show a token whose image they cannot read, so
indexing it would only produce rows no consumer can use. If a project wants its scheme (Iagon's
`iagon://`) accepted, the way is the CIP process; a proposal to add `iagon` and to make `image` optional
for some uses is open as cardano-foundation/CIPs#1288, and when the CIP changes, so does this rule.

*Why the `logo` does not drop the token:* a bad optional field should cost that field, not the required ones
(`name`, `description`) and the other optional ones (`decimals`, `ticker`). This is the rule everywhere else in the
module (an out-of-range `decimals`, an over-long `ticker`, a constructor property). *What it costs:* a stored token can
have no logo although the datum had one. The logo that is dropped is often a raw base64 string or a bare IPFS hash,
which CIP-68 excludes (`logo` "needs to be a valid URI and not a plain bytestring"), so no CIP-68 client could
show it. A full mainnet index with the earlier, stricter rule (the token dropped) found 260 datums of 217 fungible
tokens with such a logo (216 a bare IPFS CID, `Qm...` or `baf...`; one a raw base64 PNG); with this rule they are
indexed instead.

*Cost of the `image` rule:* on an earlier mainnet index, this rule would remove 12 NFTs whose datum has
`mediaType: image/svg+xml` and an empty `image` (the "DID registration" sensor tokens) and 9 more whose image
is not a spec URI (6 `iagon://` wine NFTs and 3 free-text values). They are real tokens, no longer in the
table, and a dropped token comes back only when its datum is processed again (a resync), which is cheap today because the module has no known users yet. The count was taken before
the rule existed and has not been re-measured with it.

- **`files` (222, 444) is optional (`? files : [* files_details]`), and each entry needs a `mediaType` and a `src`
  that is a `uri`.** If any entry breaks that (not a map, no `mediaType`, no `src`, an empty `src`, a `src` that
  is not a URI with one of the four schemes, a bare IPFS hash for example) the **whole `files` property is dropped**
  and the token is indexed without it, with one WARN that names the reason and the value, and the counter
  `properties.dropped{kind="invalid_files"}`. The valid entries of such a list are dropped too: the rule drops a
  property, not part of one, and a half-listed set of files would suggest the token has no others. A `src` given as a
  list of chunks is joined and checked as one value. On the mainnet index built with the earlier rule (no check),
  about 100 of 71,915 tokens with `files` have an entry like that (34 with an empty `src`, 12 with no `mediaType`,
  2 with no `src`, the rest a bare IPFS hash), all 222.

*Considered and tried, then reversed:* indexing such tokens with a WARN and storing the image as written. That
kept every token, but the table then held images no client can open, and it left the module more permissive than
the CIP it implements. For the `logo` two other options were tried: storing it as written without a check, and
dropping the whole token (the second was implemented first and reversed after the mainnet run showed 217 tokens
lost for an optional field).

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
| `logo` | a URI with scheme `https`, `ipfs`, `ar` or `data` (decision 4) | the value is dropped with a WARN, the datum is kept |
| `files` | every entry has a `mediaType` and a `src` that is such a URI (decision 4) | the whole `files` property is dropped with a WARN, the datum is kept |
| nesting depth of the datum | the CBOR decoder recurses; a `StackOverflowError` is caught | the datum is skipped, with a WARN |
| nesting depth of an additional property (#1240) | at most 100 levels of lists, maps and constructors | the property is dropped with a WARN and `properties.dropped{kind="too_deep"}`, the datum is kept |

*Why:* a datum integer is unbounded, so narrowing it can wrap a large value into a plausible one; an
over-long value would fail the insert and stop the sync; a deeply nested datum would crash the
processor, and a valid on-chain datum can be nested deeper than the stack allows. Characters are counted
in UTF-16 units because H2 counts that way, and the bound must be safe on every supported database.
*Trade-off:* a long non-BMP name that PostgreSQL alone could store is rejected. The depth guard is a
temporary workaround until the decoder stops recursing (cardano-client-lib#681).

The 100-level limit on a property value is separate from the datum depth. The value goes to a JSON column, and Jackson
refuses to write or read a document nested deeper than 1000 levels. Before the limit, a datum with a property nested
1100 levels (about 1 KB of datum, so anyone can mint one) failed on flush inside the block transaction and stopped the
sync (#1240). 100 is far above anything real, and far below 1000 even with the levels the parser wraps around the
value (`properties`, `additional_properties` or `files`, the entry). The check is bounded, so it runs before the
constructor check, which recurses as deep as the value goes. The key is dropped, not kept with a `null`: the API does not
expose `properties`, and the datum hex stays in `cip68_metadata.datum`. Isolating any other failure of the listener from
the sync is a separate change (#1243).

The `logo` and `image` limit is the one the CIP-26 logo already has; the CIP-68 spec gives none. It cannot
trigger on current data: the ledger limits a transaction to `maxTxSize` (16,384 bytes at epoch 660), so a
datum cannot reach 64 KiB. It is there so the module does not rely on that parameter staying where it is.
**`description` has no bound of its own, on purpose.** The CIP defines it as `bounded_bytes` and gives no maximum
length (`bounded_bytes` is how Plutus Data writes a byte string, chunked in pieces of at most 64 bytes; a longer
description is a chunked byte string and the decoder joins the chunks, as it does for FLDT). The only limit is the
one on the whole datum: it is inline in a transaction, so `maxTxSize` (16,384 bytes at epoch 660) caps it. It is
stored as `TEXT`, which holds that without risk, so a limit of our own would invent a rule the CIP does not have.
(CIP-26 limits `description` to 500 characters; CIP-68 has no equivalent.) If `maxTxSize` is raised a long
description still fits `TEXT`.

The other bounds did not trigger on a full mainnet or preprod sync.

### 7. The layout comes from the structure; only versions 1 to 4 are indexed (#1232)

The version is the second element of the datum and must be an integer. CIP-68 defines versions 1 to 4: its CDDL
lists them (`version = 1 / 2 / 3 / 4`) and says a change that is not backwards-compatible adds a new version.

**A datum with any other version is not indexed.** One WARN (`Skipping CIP-68 datum of <policy>/<asset>: version <n>
is not one CIP-68 defines (1 to 4)`) names the token and the version, and the datum is counted in the skipped metric
with the reason `invalid_version` (label `unknown`, since the label is derived later). A version that does not fit a
`long` is rejected before it can be narrowed (decision 6). The allowed set is one range, 1 to 4, for every label: the
444 definition says `3 / 4`, which contradicts the CIP's own changelog, where version 2 added the RFT, and mainnet,
where all 18 RFTs use version 1, so a set per label would drop real tokens.

*Why strict:* the layout of a version the CIP has not defined is a guess, and the module implements CIP-68 as it is
written, the same stance as for `image` (decision 4): if a project wants a new version, the way is the CIP process,
and the module adds it when the CIP does. *Considered and tried, then reversed, in both directions:* the first
version of this rule rejected versions outside 1 to 4; it was replaced by reading such datums by their structure with
a WARN, because the CIP never says to reject them and a live token would have been dropped; and then it was made
strict again, on the argument above.

**How a datum of version 1 to 4 is read does not depend on the version.** The CIP tells direct metadata from the
nested format by the `"721"` key (step 4 of "Retrieve metadata as 3rd party": "direct metadata (map without "721"
key) or nested map format (map with "721" key)"), so the module does the same: nested if the metadata map has the
`"721"` key with a map as its value, flat otherwise. With a nested datum the entry for the reference NFT is used
(without a reference NFT only if it is the single entry), and an entry that cannot be resolved skips the datum. Before
this decision a datum was nested only if its version was 4 or above, a rule the CIP does not state.

*Effect on mainnet.* Two reference NFTs have a datum with a version outside 1 to 4; every other token is at version
1, 2 or 3. Both datums are plain flat metadata maps, and **both are no longer indexed**:

- `HOSKY 10K NFT 0002` (version 100) is a test token. Its version 100 datum was replaced about 44,700 slots later by
  a valid version 1 datum, which is indexed, and the token was burned. Nothing is lost.
- `Greenland Reserve Coin` (version 0) is a live fungible token: its reference NFT has supply 1, the user token has a
  supply of 3,000,000,000,000 units, and both of its datums declare version 0. It disappears from the CIP-68 side of
  the API. It is also in the CIP-26 registry, so the API still serves it from there (name, ticker, decimals and logo
  from the registry; any field that only CIP-68 has is lost).

None of the 28,957 mainnet rows has a `"721"` key as an additional property, so reading by structure changes how no
existing token is read.

*Trade-offs.* The allowed range is a constant in the parser. When the CIP adds a version (a version 5 has been
suggested in the discussion of cardano-foundation/CIPs#1288 and #1289), tokens of that version are not indexed until
the range is changed and the index is rebuilt; the WARN and the `invalid_version` counter make the drop visible. A
version 1 to 3 datum with a `"721"` key is read as nested, as step 4 says. A flat map with an additional property named
`"721"` that holds a map would be misread as nested. Neither exists on mainnet.

### 8. Text is text when it is UTF-8, hex when it is not; a property that holds a constructor is dropped (#1159, #1226, #1233)

CIP-68 says text is UTF-8, and Plutus has one byte-string type for text and binary data alike. Its retrieval
steps say to "encode all string entries to UTF-8 if possible, otherwise leave them in hex"; the module follows
that rule, and before #1226 the typed fields did not. Every text
value (the typed fields `name`, `description`, `ticker`, `url`, `mediaType`, `logo` and `image`, and the
values in additional properties) is stored as text if the bytes are valid UTF-8, and as **hex** otherwise.
Null characters are stripped from text. Before #1226 the typed fields were decoded with replacement
characters, so bytes that are not UTF-8 became `U+FFFD` and were lost (97 rows on a preprod sync, about
0.4%; not measured on mainnet).

Keys other than the typed ones go to the `properties` column (`files` and `additional_properties`).
The generic CIP-68 definition allows a metadata value to be a map, a list, an integer or a byte string, and
Plutus data of any kind only in `extra`. **A property whose value holds a constructor, however deep (inside a
list or a map too), is not valid metadata: it is dropped, the token is kept, and one WARN per property names
the policy, the asset name and the property** (cut at 60 characters). The same applies to a property of a
`files` entry. The rest of the datum, and the token, are indexed as usual. The full datum stays in
`cip68_metadata.datum`, so the dropped value can be recovered.

This is a bounded change of an earlier choice. #1209 fixed #1159 (a constructor made the parser throw and
dropped the whole token: 448 warnings for 434 distinct datums on a mainnet sync of the earlier code) by storing
a constructor as `{"constructor": n, "fields": [...]}`. That form is not defined by the CIP, whose step 5 of the
retrieval steps covers strings only, so no other consumer reads it the same way, and nothing was logged.
On mainnet 142 indexed tokens under 11 policies are affected (132 PBG voucher tokens with `owner`, 8 wrapped
assets with `seed`, two with `contractData` and `isDyn`), and a further 203 datums of NFT collections, counted from
the datums, carry `contractData`.

*Considered and rejected:* dropping the whole token, the strictest reading of the CIP and consistent with
decision 4. It would remove working tokens, among them the wrapped assets the API serves, over a field no client
reads (the API does not expose `properties`). *Considered and rejected:* keeping the constructor in our JSON form,
which is harmless but keeps a format of ours and logs nothing.

*Trade-offs:* the dropped property is gone from `properties`, though recoverable from the datum. How the CIP could define a JSON form, or move such data to `extra`, is being discussed in
cardano-foundation/CIPs#1289; this rule follows the CIP as it is and changes if the CIP does.

**Map keys (#1235).** The CDDL allows any metadata as a map key, and JSON needs text keys. A byte string key is
text or hex as above; an **integer key is kept as its decimal string** (`7` becomes `"7"`, bignums too), which is
what Lucid and Blockfrost do; a list, map or constructor key has no text form, so that entry is left out with a
WARN. When a byte string key and an integer key read the same (`"1"` and `1`) the byte string key stays and a WARN
says so, whatever order the map is iterated in. This applies to additional properties, nested maps and `files`
entries. *Trade-off:* `7` and `"7"` can no longer be told apart in `properties`.

*Trade-offs:* a string such as `deadbeef` can be text or hex, and the reader cannot tell. The API returns
the hex string for the few tokens whose text field is not UTF-8, where it used to return garbled text.
Hex is twice as long as the bytes, so a non-UTF-8 `name` of more than 127 bytes (or a `ticker` of more than
16, a `url` of more than 125) now exceeds the length bound of decision 6 and is dropped, where it used to
be kept with replacement characters.

### 9. A datum that is not indexed is reported, in the log and in metrics (#1159, #1228, #1237)

- A parse failure is one WARN line with the exception and the datum hex, so the case can be reproduced,
  and the stack trace is logged at DEBUG only, because a sync can hit many.
- A datum that parses but is skipped because it breaks a requirement of its label (no `name`, no
  `description` on a fungible token, no `image` or an image with a scheme the CIP does not allow on a 222 or
  444 token) is one WARN line with the policy, the asset name, the label and the reason.
- A datum that is indexed but loses a property (a constructor value, a key that cannot be stored, a `logo` with a scheme
  the CIP does not allow, an invalid `files`) is one WARN line per property, with the policy, the asset name and the reason.
- An output with a flat datum and several reference NFTs is one WARN line with the number found and their units
  (decision 1).
- A datum that does not have the CIP-68 shape at all (not a constructor, no metadata map, no integer
  version) is **not** logged: reference NFT outputs can carry arbitrary datums, and it would be noise.

**Metrics (#1237).** The totals are also Micrometer counters, read from `/actuator/prometheus`:
`yaci.store.assets.cip68.datums.indexed` (tag `label`: 222, 333, 444),
`yaci.store.assets.cip68.datums.skipped` (tags `label` and `reason`: `no_name`, `no_description`, `no_image`,
`bad_image_scheme`, or `parse_failure` and `invalid_version`, both with label `unknown`) and
`yaci.store.assets.cip68.datums.multi_label` (tags `labels`, for example `222+333`, and `outcome`: `indexed` or
`skipped`; reference NFTs paired with user tokens of several labels) and
`yaci.store.assets.cip68.properties.dropped` (tag `kind`: `constructor`, `key_unsupported`, `key_collision`, `bad_logo_scheme`, `invalid_files`, `too_deep`). The
tags have a small fixed set of values. They are registered with the CIP-68 processor and need no property; without a
`MeterRegistry` bean they go to a private registry and nothing is exposed. The WARN lines stay as the per-token
audit trail.

*Why metrics and not a health indicator:* a skipped datum is not a failing service, and mainnet will always have
some bad tokens, so a health indicator on skips would be permanently degraded or need an arbitrary threshold. Health
stays for "is the sync running" (`assetStoreOffchainSync` does that for CIP-26). *Trade-offs:* the counters start at
zero on every restart and count datums processed, so a block replayed after a chain rollback is counted again; they
suit `rate()` and `increase()` (for example over a resync), not a historical total. Other drops (an out-of-range
`decimals`, a value over its column width, a `logo` or `image` over 64 KiB) and CIP-26 are not
counted yet.

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

- Every skip caused by a missing or invalid required field, a parse failure or an over-limit value leaves a WARN.
  Only a datum that does not have the CIP-68 shape is silent.
- A token can be indexed with fewer properties than its datum has: an optional property that breaks the CIP (a bad
  `logo`, an invalid `files`, a constructor value) is dropped and the token kept. The datum hex stays in
  `cip68_metadata.datum`, so the dropped value can be recovered.
- NFTs and RFTs without an acceptable image are not in the table at all; tokens that use another scheme
  (`iagon://`) appear only after the CIP allows it and the index is rebuilt.
- Because NFTs and RFTs are stored without being served, their rules can change later without a
  breaking API change.
- Anything corrected in a rule needs a resync from before the affected slots to take effect on old rows. That is accepted: the module has no known users yet, so a resync is the way to apply a rule change, and no migration or backfill is planned. This should be revisited once the module is in use.

## Open questions

Decisions that are still open:

- One label per row, although the CIP allows several user tokens for one reference NFT: should the table record
  every matching label (a consumer that asks for the NFTs or the fungible tokens would need it)? Nothing reads the
  label today; the number of such tokens (below) will show whether it matters.
- Should the CIP fix its 444 definition (`3 / 4`), which contradicts its changelog and mainnet, and say whether a
  consumer should ignore a datum whose version it does not know? The module ignores it and warns; neither point is
  raised in the CIPs repository yet.

Not measured yet (a full mainnet index with the current rules will answer them; none of them is a design
question until then):

- How many tokens the strict `image` rule removes in total on a current index (a full mainnet index with the earlier
  `logo` rule showed 217 fungible tokens with a bad logo; they are now kept without it).
- How many outputs hold several reference NFTs, and whether any real nested datum exists on-chain. The nested case
  is covered by synthetic datums only.
- How many tokens have several user tokens for one reference NFT.
- How good the datum-shape inference is for a reference NFT with no paired token, and whether an exact
  cross-transaction lookup is worth its cost.
- How many of the 203 `contractData` NFT datums survive the `image` rule.

## References

- #1159, #1185, #1202, #1208, #1209, #1233: parser hardening, version 4, constructor values (stored in #1209, dropped with a WARN in #1233)
- #1221, #1222, #1225, #1226, #1227, #1228, #1231, #1232, #1233, #1234, #1235, #1236, #1237, #1238, PR #1220: required fields per label, label pairing and
  inference, text and hex, size cap, skip warnings, strict image, several reference NFTs in one output, the layout by structure
- cardano-foundation/cf-token-metadata-registry#104: backport of these fixes
- `Cip68Processor`, `Cip68DatumParser`, `Cip68TokenService`, `TokenQueryService`
