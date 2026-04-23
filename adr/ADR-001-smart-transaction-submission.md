# ADR-001: Smart Transaction Submission with Lifecycle Tracking

## Status
**Proposed** - Under Discussion

## Context

The `submit` module currently only submits transactions without tracking their lifecycle. Users cannot monitor transaction status after submission, leading to:
- No visibility into transaction confirmation
- No rollback detection
- No success/failure tracking
- Users must use external tools for monitoring

## Decision

We will implement **Smart Transaction Submission** - an enhanced submission service that automatically tracks the transaction lifecycle with a **simplified, incremental approach**.

This feature enables users to:
- Submit transactions and automatically track their status
- Query transaction status at any time
- Receive notifications on status changes (optional)
- Detect rollbacks and handle them appropriately

---

## High-Level Architecture 

```
┌────────────────────────────────────────────────────────────────┐
│              TRANSACTION LIFECYCLE ARCHITECTURE                │
└────────────────────────────────────────────────────────────────┘

User submits TX
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ 1. SUBMISSION                                               │
│    - Submit via Ogmios/Local Node/Submit API                │
│    - Save to DB: status = SUBMITTED                         │
│    - Publish StatusUpdateEvent                              │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. CONFIRMATION (Yaci TransactionEvent)                     │
│    - Listen to TransactionEvent from Yaci                   │
│    - OR: Periodic check in transaction table                │
│    - Match txHash with submitted_transaction                │
│    - Update: status = CONFIRMED                             │
│    - Publish StatusUpdateEvent                              │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. SUCCESS (No rollback after N blocks)                     │
│    - Scheduled job checks block depth                       │
│    - If no rollback after N blocks: status = SUCCESS        │
│    - Publish StatusUpdateEvent                              │
└─────────────────────────────────────────────────────────────┘
      │
      ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. FINALIZED (After 2,160 blocks)                           │
│    - Scheduled job checks if SUCCESS for 2,160+ blocks      │
│    - Update: status = FINALIZED                             │
│    - Publish StatusUpdateEvent                              │
└─────────────────────────────────────────────────────────────┘
      │
      └──> 5. ROLLBACK (RollbackEvent)
           - Can rollback from CONFIRMED or SUCCESS
           - Update: status = ROLLED_BACK
           - Publish StatusUpdateEvent

┌─────────────────────────────────────────────────────────────┐
│ 📡 OPTIONAL: WebSocket Notification Service                 │
│    @EventListener on StatusUpdateEvent                      │
│    Enable: store.submit.lifecycle.websocket.enabled=true    │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Schema (Version 1 - Simplified)

### Core Table: `submitted_transaction`

```sql
CREATE TABLE submitted_transaction (
    -- Primary Key
    tx_hash VARCHAR(64) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    -- Values: SUBMITTED, CONFIRMED, SUCCESS, FINALIZED, FAILED, ROLLED_BACK
    submitted_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    confirmed_slot BIGINT,
    confirmed_block_number BIGINT,
    success_at TIMESTAMP,
    finalized_at TIMESTAMP,
    error_message TEXT,
    update_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

```

### Schema Rationale

| Field | Purpose | Why Included |
|-------|---------|--------------|
| `tx_hash` | Unique identifier | Primary key |
| `status` | Lifecycle state | Track current state |
| `submitted_at` | Submission timestamp | Track when submitted |
| `confirmed_at` | Confirmation timestamp | When appeared in block |
| `confirmed_slot` | Confirmation slot | For rollback detection |
| `confirmed_block_number` | Block number | For depth calculation |
| `success_at` | Success timestamp | When reached SUCCESS state (after N blocks) |
| `finalized_at` | Finalized timestamp | When reached FINALIZED state (after 2160 blocks) |
| `error_message` | Error details | For failed submissions |

---

## Transaction State Machine

```
                    ┌─────────────┐
                    │  SUBMITTED  │ ← Initial state after submission
                    └──────┬──────┘
                           │
                           │ TransactionEvent detected OR
                           │ Periodic check finds tx in transaction table
                           ↓
                    ┌─────────────┐
               ┌───→│  CONFIRMED  │ ← Appears in block (re-submission target)
               │    └──────┬──────┘
               │           │
               │           │ No rollback after N blocks (configurable)
               │           ↓
               │    ┌─────────────┐
               │    │   SUCCESS   │ ← Practically safe (e.g., 15 blocks)
               │    └──────┬──────┘
               │           │
               │           │ After 2,160 blocks (security parameter)
               │           ↓
               │    ┌─────────────┐
               │    │  FINALIZED  │ ← Mathematically immutable (final state)
               │    └─────────────┘
               │
               │    ┌──────────────┐
               └────│ ROLLED_BACK  │ ← RollbackEvent detected
                    └──────┬───────┘
                           │
                           │ Can return to
                           ↓
                    ┌─────────────┐
                    │  CONFIRMED  │
                    └─────────────┘

Special States:
┌─────────┐
│ FAILED  │ ← Submission rejected
└─────────┘
```

**State Transitions:**
- `SUBMITTED` → `CONFIRMED`: Via TransactionEvent or periodic check
- `CONFIRMED` → `SUCCESS`: After N blocks without rollback (N configurable, e.g., 15 blocks)
- `SUCCESS` → `FINALIZED`: After 2,160 blocks (Cardano security parameter - mathematical finality)
- `CONFIRMED` → `ROLLED_BACK`: Via RollbackEvent (before SUCCESS)
- `SUCCESS` → `ROLLED_BACK`: Via RollbackEvent (rare, but possible before 2,160 blocks)
- `ROLLED_BACK` → `CONFIRMED`: Re-confirm after re-submission

**Note on Rollback & Re-confirmation:**

According to Cardano protocol and Cardano Wallet Transaction Lifecycle specification:

1. **Rollback Behavior**: When a transaction is rolled back due to chain reorganization (fork), the transaction is removed from the blockchain database (`deleteBySlotGreaterThan`).

2. **Transaction Fate**: The rolled-back transaction is no longer on the main chain. However:
   - The transaction **may reappear** in the winning fork if nodes re-include it
   - The transaction **must be re-submitted** if it doesn't naturally reappear

3. **State Transition**: 
   - Cardano Wallet spec: rolled-back transactions move from `in_ledger` → `pending`
   - Yaci Store: `CONFIRMED` → `ROLLED_BACK` → (after re-submission) → `CONFIRMED`

4. **Re-submission Strategy**:
   - **Automatic re-submission**: System can automatically re-submit `ROLLED_BACK` transactions
   - **Manual re-submission**: User can manually re-submit via the same submission endpoint
   - Both approaches will transition the transaction back to `SUBMITTED` → `CONFIRMED`

**References:**
- [Cardano Wallet - Transaction Lifecycle](https://cardano-foundation.github.io/cardano-wallet/design/concepts/transaction-lifecycle.html)
- [Cardano Stack Exchange - Best practice for handling rollbacks](https://cardano.stackexchange.com/questions/4614/best-practice-for-handling-rollbacks)
- Yaci Store Implementation: `TransactionRollbackProcessor.deleteBySlotGreaterThan()`

---


## Acceptance Criteria

- [ ] Users can submit transactions with tracking
- [ ] System detects confirmation via TransactionEvent
- [ ] System detects and handles rollbacks
- [ ] System marks transactions as SUCCESS after N blocks (configurable)
- [ ] System marks transactions as FINALIZED after 2,160 blocks (security parameter)
- [ ] Users can query transaction status by txHash
- [ ] StatusUpdateEvent is published for every status change
- [ ] (Optional) WebSocket provides real-time updates
- [ ] Configuration allows toggling detection methods
- [ ] Efficient transition from SUCCESS to FINALIZED (periodic check)
- [ ] Documentation is complete
---

**Version:** 1.0
**Created:** 2025-10-07  
**Status:** Proposed - Under Discussion  

