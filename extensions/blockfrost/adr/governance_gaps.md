# Governance Module — Gaps & Open Items

**PR:** #865 | **Status:** 🔄 Open PR | **Endpoints:** 17 / 17

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /governance/dreps` | ✅ | |
| `GET /governance/dreps/{drep_id}` | ✅ | Retired DReps return `null` `active_epoch` to match BF |
| `GET /governance/dreps/{drep_id}/delegators` | ✅ | Optimized with indexed window functions |
| `GET /governance/dreps/{drep_id}/metadata` | ⚠️ | Returns 200 instead of 404 when off-chain fetch fails |
| `GET /governance/dreps/{drep_id}/updates` | ✅ | |
| `GET /governance/dreps/{drep_id}/votes` | ✅ | |
| `GET /governance/proposals` | ✅ | |
| `GET /governance/proposals/{tx_hash}/{cert_index}` | ⚠️ | `ratified_epoch`/`expired_epoch` null without aggr |
| `GET /governance/proposals/{tx_hash}/{cert_index}/parameters` | ✅ | Returned as strings matching BF format |
| `GET /governance/proposals/{tx_hash}/{cert_index}/withdrawals` | ✅ | |
| `GET /governance/proposals/{tx_hash}/{cert_index}/votes` | ✅ | |
| `GET /governance/proposals/{tx_hash}/{cert_index}/metadata` | ⚠️ | Off-chain fetch returns 200 vs BF 404 |
| `GET /governance/proposals/{tx_hash}/{cert_index}/dreps` | ✅ | |
| `GET /governance/proposals/{tx_hash}/{cert_index}/pools` | ✅ | |
| `GET /governance/proposals/{tx_hash}/{cert_index}/committees` | ✅ | |
| `GET /governance/votes/{tx_hash}/{cert_index}` | ✅ | |
| `GET /governance/proposals/{tx_hash}/{cert_index}/linked_proposals` | ✅ | |

## Open Gaps

### Accepted Limitations

- **Special DReps not indexed:** `drep_always_abstain` and `drep_always_no_confidence` are
  CIP-1694 predefined options — not registered on-chain, cannot be indexed. Queries targeting
  these virtual DReps return empty/null, matching Blockfrost behavior.

- **`ratified_epoch`/`expired_epoch` null:** When `store.governance-aggr.enabled=false`, these
  fields are not computed and return `null`. Enable governance aggregation for full parity.

- **Off-chain metadata status code mismatch:** When off-chain fetch fails, yaci-store returns
  HTTP 200 with partial response while Blockfrost returns HTTP 404. Requires a fetch + caching
  layer to align. Currently out of scope.

- **Delegator UTXO timing:** Small differences in delegator counts between syncs due to UTXO
  observation timing. Not a code issue.

## Indexes

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_drep_registration_drep_hash
    ON drep_registration (drep_hash);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_voting_procedure_drep_hash_type
    ON voting_procedure (drep_hash, voter_type);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.governance=true` | All governance endpoints |
| `store.governance.enabled=true` | Governance data |
| `store.governance-aggr.enabled=true` | `ratified_epoch`, `expired_epoch` fields |
| `store.cardano.n2c-host=<node-ip>` | N2C live DRep data |
| `store.cardano.n2c-port=31001` | N2C live DRep data |

## Release Notes

No hard blockers. Main gaps:
1. Off-chain metadata status code mismatch (accepted for now).
2. `ratified_epoch`/`expired_epoch` require `governance-aggr` module (document as prerequisite).
Module is in review with reviewers assigned.
