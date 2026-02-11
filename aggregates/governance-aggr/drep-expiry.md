# DRep Expiry in yaci-store (`governance-aggr`)

## Overview

DRep (Delegated Representative) expiry is a Cardano Conway-era governance mechanism that automatically marks inactive DReps so their delegated stake does not count toward governance thresholds. yaci-store recalculates DRep expiry **every epoch** as part of the DRep stake distribution snapshot, storing two complementary values — `expiry` and `active_until` — in the `drep_dist` table.

## Why DRep Expiry Matters

- DReps must vote or update their registration periodically to remain ACTIVE.
- **Accurate stake distribution**: Only ACTIVE DReps' stake is included when evaluating governance proposal thresholds.

## Related Concepts

| Concept | Description                                                                                                                            |
|---|----------------------------------------------------------------------------------------------------------------------------------------|
| **`dRepActivity`** | Protocol parameter — the number of epochs a DRep remains active after their last activity (registration, vote, or update).             |
| **Dormant epoch** | An epoch with **no active governance proposals** after proposal status evaluation. Dormant epochs do not count toward DRep inactivity. |
| **`dormantEpochCount`** | Running counter of **consecutive** dormant epochs (resets to 0 when an epoch has active proposals).                                    |
| **Activity (interaction)** | A vote cast (`VotingProcedure`) or a DRep certificate update (`UPDATE_DREP_CERT`).                                                     |
| **V9 bonus** | The additional bonus epochs added to the expiry calculation for DReps registered under protocol version 9.                             |

### Expiry formula (simplified)

```
expiry = lastActivityEpoch + dRepActivity + dormantCount [+ v9Bonus]
```

Where `lastActivityEpoch` is `max(registrationEpoch, lastInteractionEpoch)`.

### `active_until` vs `expiry`

`drep_dist` stores two expiry-related fields for each DRep:

- `active_until`: Internal expiry value, equivalent to cardano-ledger's `drepExpiry`.
  This is the value stored after subtracting dormant epochs during an ongoing dormant period.
  This value must be used for determining if a DRep is `ACTIVE` or `INACTIVE`.
  Formula (PV10+): `currentEpoch + activity - dormantEpochsSinceLastActivity`

- `expiry`: Effective expiry value, equivalent to cardano-ledger's `vsActualDRepExpiry`.
  This is the actual epoch when the DRep will expire, including dormant epoch adjustments.
  Use for display/query purposes only.
  Formula: `active_until + dormantEpochCount` (during ongoing dormant period)

Why two fields?

During dormant periods (epochs with no active proposals), DReps should not become inactive.
The ledger achieves this by:

1. Storing `drepExpiry` with dormant epochs subtracted
2. When a new proposal is submitted, bumping `drepExpiry` by adding dormant epochs back
3. Resetting the dormant counter to `0`

yaci-store mimics this by:

1. Storing `active_until` (equivalent to `drepExpiry` after subtracting dormant)
2. Storing `expiry` (equivalent to `actualExpiry = drepExpiry + dormantEpochs`)
3. Recalculating both values at each epoch boundary


## yaci-store Data Model

### `drep_dist` table (partitioned by epoch)

| Column | Type | Description |
|---|---|---|
| `drep_hash` | VARCHAR(56) | DRep credential hash (PK) |
| `drep_type` | VARCHAR(40) | `ADDR_KEYHASH` or `SCRIPTHASH` (PK) |
| `drep_id` | VARCHAR(255) | Bech32 DRep identifier |
| `amount` | BIGINT | Delegated stake (lovelace) |
| `epoch` | INT | Snapshot epoch (PK) |
| `active_until` | INT | Adjusted expiry used for status decisions |
| `expiry` | INT | Effective expiry including dormant compensation |

### `gov_epoch_activity` table

| Column | Type | Description |
|---|---|---|
| `epoch` | INT (PK) | Epoch number |
| `dormant` | BOOLEAN | Whether this epoch is dormant |
| `dormant_epoch_count` | INT | Consecutive dormant epoch counter |

### Key entities

- `DRepDistEntity` — JPA entity for `drep_dist`
- `GovEpochActivityEntity` — JPA entity for `gov_epoch_activity`

## Expiry Computation and Update Flow

```
StakeSnapshotTakenEvent
 |
 v
ProposalStateProcessor.handleProposalState()          -- [1]
 |
 +---> DRepDistService.takeStakeSnapshot(epoch)        -- [2]
 |      |
 |      +---> Insert DRep stake rows into drep_dist
 |      +---> DRepExpiryService                        -- [3]
 |             .calculateAndUpdateExpiryForEpoch(epoch)
 |
 +---> Evaluate proposal statuses (ratify/expire)
 |
 +---> Publish ProposalStatusCapturedEvent              -- [4]
        |
        v
       GovEpochActivityProcessor                       -- [5]
        .handleProposalStatusCapturedEvent()
        |
        +---> Determine if epoch is dormant
        +---> Update gov_epoch_activity
```

### Status determination (query time)

```
DRepStorageReaderImpl:
  if RETIRED in drep table -> RETIRED
  else if active_until IS NULL OR active_until >= queryEpoch -> ACTIVE
  else -> INACTIVE
```

Stake-weight queries (`DRepDistRepository`) use the same condition to exclude inactive DReps.

### Event ordering within an epoch boundary

```
StakeSnapshotTakenEvent
  -> DRep dist snapshot + expiry calc (uses dormant data from previous epochs)
  -> Proposal status evaluation
  -> ProposalStatusCapturedEvent
       -> Dormant epoch detection for THIS epoch (available for next epoch's expiry calc)
```

Note: the dormant status for epoch N is determined **after** the expiry calculation for epoch N, so epoch N's expiry uses dormant data up to epoch N-1.
