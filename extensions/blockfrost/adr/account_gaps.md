# Account Module — Gaps & Open Items

**PR:** #836 | **Status:** ✅ Merged | **Endpoints:** 12 / 12 (118 / 120 field matches, 98.3%)

## Endpoint Status

| Endpoint | Match | Notes |
|----------|-------|-------|
| `GET /accounts/{stake_address}` | ✅ | |
| `GET /accounts/{stake_address}/rewards` | ⚠️ | May lag by 1 row on live data (AdaPot timing) |
| `GET /accounts/{stake_address}/history` | ✅ | |
| `GET /accounts/{stake_address}/delegations` | ✅ | |
| `GET /accounts/{stake_address}/registrations` | ✅ | |
| `GET /accounts/{stake_address}/withdrawals` | ✅ | |
| `GET /accounts/{stake_address}/mirs` | ✅ | |
| `GET /accounts/{stake_address}/addresses` | ✅ | |
| `GET /accounts/{stake_address}/addresses/assets` | ✅ | |
| `GET /accounts/{stake_address}/addresses/total` | ✅ | |
| `GET /accounts/{stake_address}/utxos` | ✅ | Cursor-based streaming to avoid heap pressure |
| `GET /accounts/{stake_address}/transactions` | ✅ | |

## Open Gaps

### Accepted Limitations

- **Reward row timing lag:** `GET /accounts/{stake_address}/rewards` may show 1 extra row vs.
  Blockfrost during live sync because AdaPot materializes reward snapshots one epoch behind
  Blockfrost's live reward exposure window. Inherent to AdaPot architecture, not a code bug.

### Tracked Issues

- **`withdrawable_amount` view (#897):** Issue #897 tracks creating an `account_rewards`
  database view for precise `withdrawable_amount` calculation with same-slot tie-breaking.
  Currently computed inline — may cause subtle discrepancies when withdrawal and reward rows
  share the same slot.

## Indexes

```sql
-- Already in index.yml — verify before applying:
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_address_utxo_owner_stake_addr
    ON address_utxo (owner_stake_addr);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_withdrawal_address
    ON withdrawal (address);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_withdrawal_address_slot
    ON withdrawal (address, slot);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_instant_reward_address_slot
    ON instant_reward (address, slot);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_reward_rest_address_slot
    ON reward_rest (address, slot);
```

## Configuration

| Property | Required For |
|----------|-------------|
| `store.extensions.blockfrost.account.enabled=true` | All account endpoints |
| `store.account.enabled=true` | Balance fields |
| `store.account.stake-address-balance-enabled=true` | `controlled_amount` field |
| `store.adapot.enabled=true` | Rewards, history data |
| `store.governance.enabled=true` | DRep delegation fields |

## Release Notes

No functional blockers. The reward timing lag is inherent to AdaPot architecture.
Issue #897 (`withdrawable_amount` view) is the only outstanding tracked item.
