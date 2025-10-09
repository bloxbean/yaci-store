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
      └──> 4. ROLLBACK (RollbackEvent)
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
    
    cbor_hex TEXT NOT NULL,
    
    status VARCHAR(20) NOT NULL,
    -- Values: SUBMITTED, CONFIRMED, SUCCESS, FAILED, ROLLED_BACK
    
    submitted_at TIMESTAMP NOT NULL,
    metadata JSONB,
    
    confirmed_at TIMESTAMP,
    confirmed_slot BIGINT,
    confirmed_block_number BIGINT,
    success_at TIMESTAMP,
    
    error_message TEXT,
    
    update_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

```

### Schema Rationale

| Field | Purpose | Why Included |
|-------|---------|--------------|
| `tx_hash` | Unique identifier | Primary key |
| `cbor_hex` | Transaction data | For resubmission if needed |
| `status` | Lifecycle state | Track current state |
| `metadata` | Custom tags | User-defined metadata |
| `submitted_at` | Submission timestamp | Track when submitted |
| `confirmed_at` | Confirmation timestamp | When appeared in block |
| `confirmed_slot` | Confirmation slot | For rollback detection |
| `confirmed_block_number` | Block number | For depth calculation |
| `success_at` | Success timestamp | When reached SUCCESS state |
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
               ┌───→│  CONFIRMED  │ ← Appears in block
               │    └──────┬──────┘
               │           │
               │           │ No rollback after N blocks
               │           ↓
               │    ┌─────────────┐
               │    │   SUCCESS   │ ← Immutable (final success)
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
- `CONFIRMED` → `SUCCESS`: After N blocks without rollback (N configurable)
- `CONFIRMED` → `ROLLED_BACK`: Via RollbackEvent
- `ROLLED_BACK` → `CONFIRMED`: Can re-confirm after rollback

---


## Acceptance Criteria

- [ ] Users can submit transactions with tracking
- [ ] System detects confirmation via TransactionEvent
- [ ] System detects and handles rollbacks
- [ ] System marks transactions as SUCCESS after N blocks
- [ ] Users can query transaction status by txHash
- [ ] StatusUpdateEvent is published for every status change
- [ ] (Optional) WebSocket provides real-time updates
- [ ] Configuration allows toggling detection methods
- [ ] Documentation is complete
---

**Version:** 1.0
**Created:** 2025-10-07  
**Status:** Proposed - Under Discussion  

