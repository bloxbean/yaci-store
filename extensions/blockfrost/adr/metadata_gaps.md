# Metadata Module — Gaps & Open Items

**PR:** #872 | **Status:** 🔄 Open PR | **Endpoints:** 3 / 3

## Endpoint Status

| Endpoint | Match | Data | Notes |
|----------|-------|------|-------|
| `GET /metadata/txs/labels` | ⚠️ | Partial | `cip10` null; duplicate rows on reconnect |
| `GET /metadata/txs/labels/{label}` | ✅ | Full | |
| `GET /metadata/txs/labels/{label}/cbor` | ⚠️ | Partial | CBOR null when not stored at ingest time |

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

### Tracked Issues

- **Duplicate rows on reconnect:** `GET /metadata/txs/labels` can return duplicate label rows
  after a node reconnection triggers re-indexing. Deduplication logic does not handle
  re-processed transactions. Should be resolved before merge.

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

The duplicate-rows-on-reconnect issue is the only item that should be resolved before merge.
All other gaps are accepted limitations. Minor code style review comments also outstanding.
