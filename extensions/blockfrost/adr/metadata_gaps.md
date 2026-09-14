# Metadata Module — Gaps & Open Items

**PR:** #872 | **Status:** 🔄 Open PR | **Endpoints:** 3 / 3

## Endpoint Status

| Endpoint | Match | Data | Notes |
|----------|-------|------|-------|
| `GET /metadata/txs/labels` | ⚠️ | Partial | `cip10` null; rare duplicate rows on reconnect |
| `GET /metadata/txs/labels/{label}` | ⚠️ | Partial | Ordering inside the same block may differ from Blockfrost |
| `GET /metadata/txs/labels/{label}/cbor` | ⚠️ | Partial | CBOR null when not stored at ingest time; ordering inside the same block may differ from Blockfrost |

## Open Gaps

### Accepted Limitations

- **`cip10` always null:** `BFMetadataLabelDto.cip10` is hardcoded to `null`. Populating it
  requires integration with the CIP-10 community registry (numeric label → human-readable name
  mapping). No integration planned at this time.

- **CBOR null when not stored:** `GET /metadata/txs/labels/{label}/cbor` returns `null` for
  `cbor_metadata` when the transaction's raw CBOR bytes were not captured at ingestion time.
  Re-encoding from stored JSON is not attempted. Affects older indexed data.

- **Raw JSON passthrough:** Uses `@JsonRawValue` for metadata content. Malformed stored metadata
  produces structurally invalid JSON responses. Acceptable as the source data is already invalid.

- **Numeric label sort workaround:** Labels cast to `BigDecimal` for sort ordering instead of
  native `BIGINT`. Functionally equivalent but less efficient. Low priority.

- **Ordering inside the same block:** Blockfrost orders metadata-by-label responses by chain
  position, including transaction index inside the block. Yaci Store currently sorts these rows
  by slot, but `transaction_metadata` does not store `tx_index`. When multiple transactions in
  the same block share a metadata label, `GET /metadata/txs/labels/{label}` and
  `GET /metadata/txs/labels/{label}/cbor` may return the same records in a different order than
  Blockfrost. This is accepted for now as long as the metadata label records are present.

### Tracked Issues

- **Duplicate rows on reconnect (low severity):** `GET /metadata/txs/labels` can theoretically
  return duplicate label rows after a node reconnection triggers re-indexing, if deduplication
  logic does not cover re-processed transactions. This is a rare edge case, hard to reproduce
  in practice. Track as a low-priority follow-up item; not a merge blocker.

- **Add transaction index to metadata rows:** Introduce `tx_index` on `transaction_metadata`, and
  other tables that need Blockfrost-compatible ordering inside a block, so metadata-by-label
  endpoints can sort by `slot, tx_index` without joining the transaction table at query time.

## Indexes

No additional indexes required beyond those provided by existing metadata infrastructure.
The `transaction_metadata` table is queried directly.

## Configuration

```yaml
store:
  extensions:
    blockfrost:
      metadata:
        enabled: true
  metadata:
    enabled: true

blockfrost:
  apiPrefix: /api/v1/blockfrost
```

## Release Notes

The remaining gaps are accepted limitations. The duplicate-rows-on-reconnect edge case is rare
and hard to reproduce. Metadata-by-label ordering inside the same block is also accepted for now;
future schema work should add `tx_index` to metadata rows so these endpoints can match
Blockfrost ordering without an expensive transaction-table join.
