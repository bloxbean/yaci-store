# ADR 0002: CIP-68 on-chain datum handling

## Status

Proposed. Rewritten on 2026-10-20 for ADR 0006 (validation errors are reported, not enforced); it replaces the version that
dropped a token or a property when it broke the CIP. Implemented on branch `feat/assets-ext-validation-errors`. Decisions 1, 2 and 5 are
implemented in PR #1220 (open, not merged; issues #1221, #1222, #1225 to #1228, #1231 to #1238 and #1240) and carry over
unchanged; the rest follows ADR 0006. Decision 9 describes behaviour that is on `main` in part.

## Date

2026-10-07, rewritten 2026-10-20

## Context

CIP-68 metadata is read from the inline datum of a reference NFT, and the datum is written by the
token's minter. Nothing checks it before it reaches the chain, so the module reads data that is
sometimes malformed, sometimes hostile, and often not what the CIP says: live mainnet tokens have an
empty `image`, `iagon://` URIs, free text where a URI belongs, constructors inside the metadata map,
logos split into chunks, versions 0 and 100.

The module indexes what is on-chain, as written, and says what is wrong with it (ADR 0006). This ADR records how a
datum is read and what is reported. It does not restate CIP-68. CIP-26 is covered in
[ADR 0001](0001-cip-26-offchain-registry-ingestion.md).

The general rule, the same for fungible tokens, NFTs and RFTs:

- **A datum that can be decoded is indexed and served as written.** Whatever departs from CIP-68 is kept, not repaired, and
  described by a `validation_errors` entry (decision 4): a missing mandatory property, a value of the wrong type, a `uri` with another
  scheme than `https`, `ipfs`, `ar` or `data`, a version CIP-68 does not define, a Plutus constructor in a property.
- **Only what is plainly an attack on the indexer is dropped**, with a WARN (decision 6): a datum too deep to decode, or a property
  nested more than 100 levels.

"Mandatory" and "optional" are the ones in the CIP-68 definition of the label (decision 3): `name` and `description`
for 333, `name` and `image` for 222 and 444, everything else optional. The error entry says which one a field is
(`field_category`).

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
  indexed, each resolved to its own entry in the map. A reference NFT with no entry is indexed with no metadata and a
  `no_nested_entry` error (decision 4).
- **Flat datum** (versions 1 to 3, or version 4 without `"721"`) (#1236): **every reference NFT of the output is
  indexed with that datum**, and one INFO line names the output, the count and the units (a valid case, so not a
  warning; see decision 8). The CIP's retrieval steps
  find the output the reference NFT is locked in and read its datum, whatever else the output holds, and only the
  nested map selects an entry, so a flat datum is the metadata of each reference NFT in the output.
  *Trade-off:* some of those tokens may have been given metadata that is not theirs, which is what any client
  following the CIP shows for them too; the INFO line lists them. *Considered and rejected:* indexing none, which hides
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

**Rolling back by hand with the admin CLI.** The `rollback-data` command of the admin CLI does not use the processor. It
deletes from the tables listed in `components/dbutils/src/main/resources/rollback.yml`, each with its own condition, and
`cip68_metadata` is listed there with the same rule, `DELETE ... WHERE slot > :slot` (#1132, #1133). The two paths
therefore agree on the boundary: the row at the rollback slot stays, later rows go. The CLI path depends on that file, so
a table that is added to this module later and has a `slot` column must be added to `rollback.yml` too, otherwise the
CLI leaves its rows behind while the runtime rollback removes them. `cip26_metadata` and `cip26_sync_state` are not listed,
which is correct for the reason in ADR 0001: they come from Git, not from the chain.

### 2. The label comes from the paired user tokens, and from the datum when there is none (#1222, #1225, #1238)

The label (222 NFT, 333 FT, 444 RFT) is not in the datum. It is read from the user token minted with
the reference NFT: the asset in the same transaction with **the same policy and the same base name**
(reference NFT `000643b0 + base`, user token `<label prefix> + base`). If several are paired, the row is
stored with the first of them in the order 222, 333, 444, **and the datum is checked against the requirements of every
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
wrong for an NFT, and 333 requires a description, so a description-less NFT minted that way got a `description` error.

*Why infer and not look the user token up across transactions:* the lookup is exact but needs a UTxO or
asset lookup at indexing time, which depends on other stores being enabled and costs a query per reference
NFT. It was not done. *Trade-offs and known gaps:*

- The inference can be wrong for a datum that mixes both kinds of fields. How many reference NFTs have no
  paired token, and how many the guess would label differently from their real user token, is not measured.
- With several paired user tokens (USDCx/USDrf LP has both a 222 and a 333 on-chain), the row keeps one label,
  but the datum is checked against **all** of them (#1238): it needs a `name`, a `description` if 333 is among
  them, and an `image` with an allowed scheme if 222 or 444 is. Whatever it lacks for a label is a validation error that
  names that label (`sub_standard`); the datum is indexed either way, since the metadata is shared by every token of that
  reference NFT. Before, only the first label was checked, and under the strict rules a datum that failed one label was not
  indexed at all. A multi-label token is logged at INFO when it is indexed, and counted in
  `yaci.store.assets.cip68.datums.multi_label{labels}` (decision 8). *Considered and not done:* recording every label (a
  `labels` column or a row per label): nothing reads it today, and a row per label would duplicate the datum; it can be added
  later with a resync.
- The opposite error (an NFT stored as 333) was possible before and is not measured.
- Rows already stored keep their label until their datum is processed again.

What the label does and does not decide: it chooses the fields that are mandatory for the token (decision 3) and the set a
row belongs to (the API serves fungible and rich fungible tokens, see decision 9). It does **not** change what the
API returns for a subject, which reads the latest row of the reference NFT whatever its label.

### 3. What each label requires, reported as errors (#1221)

| Label | Mandatory | Optional |
|---|---|---|
| 333 FT | `name` and `description` | everything else (`logo`, `ticker`, `url`, `decimals`) |
| 222 NFT, 444 RFT | `name` and `image` (decision 4) | `description`, and everything else |

The processor derives the label first and then checks the datum against it, because the requirements depend on it. A datum that lacks
a mandatory field is indexed, with a `MANDATORY` error for that field and that label (`sub_standard`): for example a 333 token with no
description has `{"cip": "CIP-68", "sub_standard": "333", "field_name": "description", "field_category": "MANDATORY",
"code": "missing_value", "error": "description field is missing a mandatory value"}`. A value that is present but of the wrong type (a `name` that
is an integer) is left out and reported as `wrong_type`; for `name` that error is mandatory, so it is not reported as missing too.
NFTs and RFTs without a description have no error, which the spec allows. Before #1221 the same rule was applied to every label and valid NFTs
were dropped silently; on mainnet at least 203 datums were affected (a lower bound).

The optional properties report as `OPTIONAL` errors, and keep their value:

| Optional property | What is wrong | Error code |
|---|---|---|
| `logo` | not a `uri` with an allowed scheme (a bare IPFS hash, `iagon://`) | `invalid_uri_scheme` |
| `files` | an entry that is not a map, has no `mediaType` or no `src`, or whose `src` is not a `uri` with an allowed scheme; the entries are kept as written, with one error per bad entry | `invalid_files_entry` |
| `decimals` | outside [0, 255] (stored as written), not an integer or beyond a `long` (stored as no value) | `out_of_range`, `wrong_type` |
| text fields | not valid UTF-8 (stored as hex, decision 7) | `not_utf8` |
| `ticker`, `url`, `mediaType`, `description` (222, 444) | not a byte string (left out) | `wrong_type` |
| additional properties | a constructor anywhere in the value (stored as `{"constructor", "fields"}`), a key with no text form, two keys that read the same (decision 7), a value nested deeper than 100 levels (dropped, decision 6) | `plutus_constructor`, `unsupported_key`, `key_collision`, `too_deep` |

### 4. `image` (222, 444) and `logo` (333): kept as written, with an error when the scheme is not one the CIP allows (#1221, #1234)

CIP-68 requires `image` for 222 and 444, and says the URI scheme of `image` and of the 333 `logo` must be one of `https`, `ipfs`, `ar` or
`data`. The module reports both, and keeps the value:

- A 222 or 444 token with no image, or an empty or blank one, is indexed with a `MANDATORY` `missing_value` error on `image` for that label.
- A 222 or 444 token whose image is not a URI with one of those four schemes (compared without regard to case, with something after the colon)
  is indexed with the image as written, and a `MANDATORY` `invalid_uri_scheme` error that gives the value (cut at 60 characters). That covers
  `iagon://` and free text.
- The check runs on the joined value of a chunked image (decision 5).
- A 333 token has no `image`. Its `logo` is optional, so a missing, empty or blank one is fine. One that is present and not a URI with one of
  those schemes (a bare IPFS hash such as `Qm...`, or `iagon://`) is kept as written, with an `OPTIONAL` `invalid_uri_scheme` error. The module
  does not repair the value: adding `ipfs://` would be a guess, and the index would then state something that is not on-chain. The check does not
  depend on the label, so its error has no `sub_standard`.
- **`files` is optional, and each entry needs a `mediaType` and a `src` that is a `uri`.** Every entry is kept as written. One that breaks
  that (not a map, no `mediaType`, no `src`, an empty `src`, a `src` that is not a URI with an allowed scheme) has its own
  `OPTIONAL` `invalid_files_entry` error. A `src` given as a list of chunks is joined and checked as one value.
- A reference NFT whose nested (`"721"`) datum has no usable entry for it (another policy id, another asset name, or an entry that is not a
  map) is indexed with no metadata and a `MANDATORY` `no_nested_entry` error. In an output with several reference NFTs, each one the
  datum does not list is indexed and reported the same way.

*Why:* wallets and explorers that follow the CIP cannot show a token whose image they cannot read, so a wallet that wants to be strict
rejects such a token, and now it can tell why. The index does not hide it. If a project wants its scheme (Iagon's `iagon://`) accepted, the way
is the CIP process: a proposal to add `iagon` and to make `image` optional for some uses is open as cardano-foundation/CIPs#1288, and when the CIP
changes, so does the check (the error disappears; no data is lost).

*History, measured under the earlier, strict rule* (which dropped the token), on a full mainnet index (build `ebb9daa37`, read on 2026-10-14): the
`image` rule skipped 4,683 datums of **3,113 distinct tokens from 6 policies**, all label 222. One policy accounts for 3,091 of them (a collection
whose `image` is a bare IPFS CID, `Qm...`); 3,102 of the 3,113 have a bare CID in `image`. The other 11 are 6 `iagon://` wine NFTs, 3 free-text values,
`asdas` and one placeholder text. Another 100 tokens from 38 policies have no `image` at all (mostly test tokens such as `SecurityTest` and
`NFT1`). 260 datums of 217 fungible tokens had a `logo` that is not a URI with an allowed scheme (216 a bare IPFS CID, `Qm...` or `baf...`; one a raw base64
PNG), and 86 tokens from 16 policies (89 datums) a `files` entry that breaks the definition: 38 with no usable `src`, 30 with a `src` that is not a URI with
an allowed scheme, 15 with no `mediaType` and 3 with an entry that is not a map. All of these are indexed now, with the errors above.

*Considered and rejected:* dropping the token, or only the property, as the strict rule did (ADR 0006); and repairing the value.

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

### 6. Only what looks like an attack is dropped, and nothing is capped (#1185, #1227, #1240)

There are no length caps on any value: the columns are `TEXT`, and a datum is inline in a transaction, so it is at most `maxTxSize` (16,384 bytes
at epoch 660). What is stored is therefore bounded without a rule of ours, and **`description` has no bound of its own, on purpose**: the CIP
defines it as `bounded_bytes` and gives no maximum length (`bounded_bytes` is how Plutus Data writes a byte string, chunked in pieces of at most 64
bytes; a longer description is a chunked byte string and the decoder joins the chunks, as it does for FLDT). (CIP-26 limits `description` to 500
characters; CIP-68 has no equivalent, and a CIP-26 limit is reported, not enforced.)

| Input | Rule | Effect |
|---|---|---|
| `version` | an integer that does not fit a `long` | the datum is indexed with no version and an `out_of_range` error (narrowing it would wrap `2^64 + 1` into 1) |
| `version` | not 1 to 4 | indexed as written, with an `undefined_version` error (decision 7 of the earlier version of this ADR, reversed) |
| nesting depth of the datum | the CBOR decoder recurses; a `StackOverflowError` is caught | **dropped** with a WARN that says it looks like an attack |
| a datum that cannot be decoded | | **dropped** with a WARN with the datum, so the case can be reproduced (`parse_failure`) |
| nesting depth of an additional property (#1240) | at most 100 levels of lists, maps and constructors | the **property is dropped** with a WARN and `properties.dropped{kind="too_deep"}`, the datum is indexed, with a `too_deep` error |

*Why these:* a deeply nested datum would crash the processor, and a valid on-chain datum can be nested deeper than the stack allows; they are the
obvious attack. The depth guard is a temporary workaround until the decoder stops recursing (cardano-client-lib#681).

The 100-level limit on a property value is separate from the datum depth. The value goes to a JSON column, and Jackson refuses to write or read a document
nested deeper than 1000 levels. Before the limit, a datum with a property nested 1100 levels (about 1 KB of datum, so anyone can mint one) failed on flush
inside the block transaction and stopped the sync (#1240). It is a real input: a transaction with such a datum (2,449 bytes) was accepted and included in a
block by `cardano-node` 11.0.1 on a local devnet (Yaci DevKit). The failure is Jackson's, in Hibernate's conversion of the map to JSON before the statement is
sent (`Could not serialize object of java type: java.util.Map`); the database never sees the value, so it is not a PostgreSQL limit, and it was reproduced on
H2 only. 100 is far above anything real, and far below 1000 even with the levels the parser wraps around the value (`properties`, `additional_properties` or
`files`, the entry). The check is bounded, so it runs before the value is unwrapped, which recurses as deep as the value goes. Isolating any other failure of
the listener from the sync is ADR 0004 (#1243); the `properties` are also written as JSON while the row is built, so a value Jackson refuses skips that output
instead of stopping the sync.

### 7. Text is text when it is UTF-8, hex when it is not; a constructor is kept; map keys (#1159, #1226, #1233, #1235)

CIP-68 says text is UTF-8, and Plutus has one byte-string type for text and binary data alike. Its retrieval steps say to "encode all string entries to
UTF-8 if possible, otherwise leave them in hex"; the module follows that rule. Every text value (the typed fields `name`, `description`, `ticker`, `url`,
`mediaType`, `logo` and `image`, and the values in additional properties) is stored as text if the bytes are valid UTF-8, and as **hex** otherwise. Null
characters are stripped from text. A typed field that is stored as hex has an `OPTIONAL` `not_utf8` error; a binary value in an additional property (a hash) is
normal and has none. Before #1226 the typed fields were decoded with replacement characters, so bytes that are not UTF-8 became `U+FFFD` and were lost (97 rows on
a preprod sync, about 0.4%; not measured on mainnet).

Keys other than the typed ones go to the `properties` column (`files` and `additional_properties`). The generic CIP-68 definition allows a metadata value to be a
map, a list, an integer or a byte string, and Plutus data of any kind only in `extra`. **A property whose value holds a constructor, however deep, is not valid
metadata, but it is kept**: it is stored as `{"constructor": n, "fields": [...]}` (the form #1209 introduced) and reported once, per property, with an `OPTIONAL`
`plutus_constructor` error. The same applies to a property of a `files` entry. The full datum stays in `cip68_metadata.datum`. History: #1209 fixed #1159 (a
constructor made the parser throw and dropped the whole token: 448 warnings for 434 distinct datums on a mainnet sync of the earlier code) by storing the
constructor in this form; #1233 then dropped such properties with a WARN, as the CIP does not define the form; and this decision reverses that, since the
error now tells the owner, and nothing on-chain is hidden. On mainnet 142 indexed tokens under 11 policies are affected (132 PBG voucher tokens with
`owner`, 8 wrapped assets with `seed`, two with `contractData` and `isDyn`), and a further 203 datums of NFT collections carry `contractData`. How the CIP
could define a JSON form, or move such data to `extra`, is being discussed in cardano-foundation/CIPs#1289.

**Map keys (#1235).** The CDDL allows any metadata as a map key, and JSON needs text keys. A byte string key is text or hex as above; an **integer key is kept as
its decimal string** (`7` becomes `"7"`, bignums too), which is what Lucid and Blockfrost do; a list, map or constructor key has no text form, so that entry is
left out, with an `unsupported_key` error. When a byte string key and an integer key read the same (`"1"` and `1`) the byte string key stays and a
`key_collision` error says so, whatever order the map is iterated in. This applies to additional properties, nested maps and `files` entries. *Trade-off:*
`7` and `"7"` can no longer be told apart in `properties`.

*Trade-offs:* a string such as `deadbeef` can be text or hex, and the reader cannot tell. The API returns the hex string for the few tokens whose text field is
not UTF-8, where it used to return garbled text.

### 8. What is reported, in the row, the log and the metrics (#1159, #1228, #1237)

- **The errors are the report**, and they are returned by the API (decision 9, ADR 0006). A datum indexed with errors is a DEBUG line, not a WARN: there is
  one per token, and mainnet has tens of thousands.
- A WARN is for what is dropped: a parse failure (one line with the exception and the datum hex, so the case can be reproduced; the stack trace at DEBUG
  only), a datum nested too deeply to decode, and a property nested more than 100 levels.
- An output with a flat datum and several reference NFTs is one **INFO** line with the number found and their units (decision 1). It is a valid case that the CIP
  does not call an error, and it is common (8,918 outputs on mainnet), so at WARN it would bury the lines that matter.
- A flat datum is parsed once for the whole output, so what is wrong with the datum itself (a wrong type, a constructor, a bad `logo`, an undefined version) is
  **counted once per output**, not once per reference NFT; the errors that depend on a label, the rows and the `indexed` count stay per token. Without this a datum
  that is wrong was counted once for each of its reference NFTs (178 times for the largest output on mainnet), which says nothing about how many datums are bad.
- A datum that does not have the CIP-68 shape at all (not a constructor, no metadata map, no integer version) is **not** logged: reference NFT outputs can
  carry arbitrary datums, and it would be noise.

**Metrics (#1237).** Micrometer counters, read from `/actuator/prometheus`: `yaci.store.assets.cip68.datums.indexed` (tag `label`: 222, 333, 444);
`yaci.store.assets.cip68.datums.validation_errors` (tags `sub_standard`, the label or `none`, `code` and `category`; the field is not a tag, because property
names come from the chain); `yaci.store.assets.cip68.datums.skipped` (tags `label` and `reason`, only `parse_failure` and `processing_failure`, both with label
`unknown`); `yaci.store.assets.cip68.datums.multi_label` (tag `labels`, for example `222+333`); and `yaci.store.assets.cip68.properties.dropped` (tag `kind`,
only `too_deep`). The tags have a small fixed set of values. They are registered with the CIP-68 processor and need no property; without a `MeterRegistry` bean
they go to a private registry and nothing is exposed. *Why metrics and not a health indicator:* a token with errors is not a failing service, and mainnet will
always have some, so a health indicator would be permanently degraded or need an arbitrary threshold. `processing_failure` is the one to alert on: it means
something nobody foresaw. *Trade-offs:* the counters start at zero on every restart and count datums processed, so a block replayed after a chain rollback is
counted again; they suit `rate()` and `increase()`, not a historical total.

### 9. Read model

- The API serves **fungible and rich fungible tokens**. A subject with the `0014df10` or `001bc280` prefix is mapped to its reference NFT, and the latest row
  of that reference NFT is returned whatever its label. A rich fungible token has no `logo`: its `image` is served as `metadata.logo`, and its `description`
  is optional. NFT rows (222) are stored but not exposed.
- For one subject, CIP-26 and CIP-68 are combined **per field**: the first non-null value in priority order wins, and the default order is CIP-68 then CIP-26.
  The `validation_errors` of both are returned, each naming its `cip`.
- A subject is returned when any standard has something indexed for it, **whatever is wrong with it**; there is no longer a requirement that the merged
  result has a `name` and a `description` (ADR 0003, ADR 0006).

*Why per field:* a token can have a name on chain and a logo only in the registry. *Trade-off:* the fields of one response can come from two sources. Each
property in the response names its `source`, and `show_cips_details` adds the raw metadata of each source (ADR 0003).

**An update is never rejected.** The API reads the latest stored row (decision 1), and every datum that can be decoded writes one, so the newest datum is the one
served, with its errors. This replaces the earlier behaviour, in which a skipped datum left the earlier row in place and served it as if it were current, with
no trace in the API (on a full mainnet index, 2,508 of the 3,113 tokens skipped for a bad `image` still had an older row).

## Consequences

- Every token that can be decoded is in the table and in the API, with the list of what is wrong with it. Owners can fix a datum, and a wallet can reject tokens
  with a `MANDATORY` error.
- A token that follows its CIP has `validation_errors: []`; a client that treated a `404` as "no valid metadata", or relied on `name` and `description`, has to
  adapt (ADR 0006).
- NFTs are stored but not served; their rules can change later without a breaking API change.
- Greenland Reserve Coin (version 0), the SaveCard tokens, NFTs with an `iagon://` or empty image and tokens with a bare-IPFS logo are returned, with errors.
- Anything corrected in a rule changes the errors of rows already stored, which are recomputed by a resync from before the affected slots; so is the first
  deployment of this decision, since the tokens that were dropped exist nowhere. That is accepted: the module has no known users yet, and no migration or backfill
  is planned. This should be revisited once the module is in use.

## Open questions

Decisions that are still open:

- One label per row, although the CIP allows several user tokens for one reference NFT: should the table record every matching label? Nothing reads the label
  today; the number of such tokens (below) will show whether it matters.
- Should the CIP fix its 444 definition (`3 / 4`), which contradicts its changelog and mainnet, and say whether a consumer should accept a datum whose version
  it does not know? The module accepts it and reports `undefined_version`; neither point is raised in the CIPs repository yet.

Measured on a full mainnet index built with the strict rules (`ebb9daa37`, read on 2026-10-14; it has to be measured again with this decision):

- 8,918 outputs hold several reference NFTs with a flat datum: 6,645 hold two, the largest holds 178.
- 10 datums were skipped because their reference NFT is paired with user tokens of labels 222 and 333; none of that kind was indexed, so the
  one-label-per-row question above has no live case yet.

Not measured yet:

- Whether any real nested (`"721"`) datum exists on-chain: none of the 250,876 rows is one (the two datums that contain the bytes `721` are flat, with `Shrine
  #0721` in the token name), and the nested case is covered by synthetic datums only. A nested datum with no entry for its token is now an error, so the next full
  index will show whether there are any.
- How good the datum-shape inference is for a reference NFT with no paired token, and whether an exact cross-transaction lookup is worth its cost.

## References

- #1159, #1185, #1202, #1208, #1209, #1233: parser hardening, version 4, constructor values
- #1221, #1222, #1225, #1226, #1227, #1228, #1231, #1232, #1233, #1234, #1235, #1236, #1237, #1238, #1240, PR #1220: required fields per label, label pairing and
  inference, text and hex, skip warnings (now errors), the image, several reference NFTs in one output, the layout by structure
- ADR 0006: validation errors are reported, not enforced
- #1132, #1133: the `cip68_metadata` table in the admin CLI rollback configuration (`rollback.yml`)
- cardano-foundation/cf-token-metadata-registry#104 and #110: backport of these fixes, and rich fungible tokens
- `Cip68Processor`, `Cip68DatumParser`, `Cip68Validator`, `Cip68TokenService`, `TokenQueryService`
