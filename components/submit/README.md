# Smart Transaction Submission with Lifecycle Tracking

## Overview

The `submit` component provides enhanced transaction submission with automatic lifecycle tracking. This feature enables users to submit transactions and automatically track their status through the entire lifecycle from submission to finalization.

## Features

✅ **Automatic Lifecycle Tracking**: Track transactions through states: `SUBMITTED` → `CONFIRMED` → `SUCCESS` → `FINALIZED`  
✅ **Rollback Detection**: Automatically detect and handle chain reorganizations  
✅ **Multiple Submission Methods**: Support for Ogmios, Local Node (N2C), and Submit API  
✅ **REST API**: Query transaction status and statistics  
✅ **WebSocket Support**: Optional real-time status notifications  
✅ **Configurable Finality**: Customize block depth for SUCCESS state

## Transaction States

| State | Description | Transition |
|-------|-------------|------------|
| `SUBMITTED` | Transaction submitted to network | Initial state |
| `CONFIRMED` | Transaction appeared in a block | After detection in blockchain |
| `SUCCESS` | Transaction has N confirmations | After configurable blocks (default: 15) |
| `FINALIZED` | Mathematical finality reached | After 2,160 blocks (security parameter) |
| `FAILED` | Submission rejected | If submission fails |
| `ROLLED_BACK` | Transaction rolled back | If chain reorganization occurs |

## Configuration

### Enable Lifecycle Tracking

```yaml
store:
  submit:
    lifecycle:
      # Enable transaction lifecycle tracking
      enabled: true
      
      # Block depth for SUCCESS state (configurable per application)
      success-block-depth: 15
      
      # Block depth for FINALIZED state (Cardano security parameter)
      finalized-block-depth: 2160
      
      # SUCCESS status check interval (milliseconds)
      success-check-interval-ms: 20000
      
      # FINALIZED status check interval (milliseconds)
      finalized-check-interval-ms: 3600000
      
      # WebSocket notifications (OPTIONAL)
      websocket:
        enabled: true
        endpoint: /ws/tx-lifecycle
```

## REST API Endpoints

### Submit Transaction with Tracking

**POST** `/api/v1/tx/lifecycle/submit`

Submit a transaction with automatic lifecycle tracking.

**Request (Hex String):**
```bash
curl -X POST http://localhost:8080/api/v1/tx/lifecycle/submit \
  -H "Content-Type: text/plain" \
  -d "84a400818258201234...5678"
```

**Response:**
```json
{
  "txHash": "1234567890abcdef...",
  "status": "SUBMITTED",
  "cborHex": "84a400818258201234...5678",
  "submittedAt": "2025-10-12T10:30:00Z"
}
```

### Get Transaction Status

**GET** `/api/v1/tx/lifecycle/{txHash}`

Query the current status and details of a submitted transaction.

**Response:**
```json
{
  "txHash": "1234567890abcdef...",
  "status": "SUCCESS",
  "submittedAt": "2025-10-12T10:30:00Z",
  "confirmedAt": "2025-10-12T10:31:00Z",
  "confirmedSlot": 123456789,
  "confirmedBlockNumber": 9000000,
  "confirmations": 20,
  "successAt": "2025-10-12T10:36:00Z"
}
```

### Get Transactions by Status

**GET** `/api/v1/tx/lifecycle/status/{status}?page=0&size=20`

Get all transactions with a specific status (paginated).

**Example:**
```bash
curl http://localhost:8080/api/v1/tx/lifecycle/status/CONFIRMED?page=0&size=10
```

### Get Statistics

**GET** `/api/v1/tx/lifecycle/stats`

Get transaction lifecycle statistics.

**Response:**
```json
{
  "submitted": 5,
  "confirmed": 12,
  "success": 45,
  "finalized": 1234,
  "failed": 2,
  "rolled_back": 1
}
```

## WebSocket Real-time Notifications

When WebSocket is enabled, clients can subscribe to real-time status updates.

### Connect

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/tx-lifecycle');

ws.onopen = () => {
  console.log('Connected to transaction lifecycle WebSocket');
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received:', data);
};
```

### Subscribe to Transaction

```javascript
ws.send(JSON.stringify({
  action: 'subscribe',
  txHash: '1234567890abcdef...'
}));
```

### Receive Status Updates

```json
{
  "type": "status_update",
  "txHash": "1234567890abcdef...",
  "previousStatus": "CONFIRMED",
  "newStatus": "SUCCESS",
  "timestamp": 1697112300000,
  "message": "Transaction reached SUCCESS state",
  "confirmedAt": "2025-10-12T10:31:00Z",
  "successAt": "2025-10-12T10:36:00Z"
}
```

### Unsubscribe

```javascript
ws.send(JSON.stringify({
  action: 'unsubscribe',
  txHash: '1234567890abcdef...'
}));
```

## Architecture

### High-Level Flow

```
User submits TX
      │
      ↓
1. SUBMISSION → Save to DB (status = SUBMITTED)
      │
      ↓
2. CONFIRMATION → Listen to TransactionEvent
      │           Update status = CONFIRMED
      ↓
3. SUCCESS → Scheduled job checks block depth
      │      Update status = SUCCESS (after N blocks)
      ↓
4. FINALIZED → Scheduled job checks block depth
               Update status = FINALIZED (after 2160 blocks)

      └──> ROLLBACK → Listen to RollbackEvent
                       Update status = ROLLED_BACK
```

### Components

- **TxLifecycleService**: Core service for managing transaction lifecycle
- **TxLifecycleProcessor**: Unified processor that handles:
  - Listen to `TransactionEvent` to mark transactions as CONFIRMED
  - Listen to `RollbackEvent` to handle rollbacks
  - Scheduled job to transition CONFIRMED → SUCCESS (every 20s)
  - Scheduled job to transition SUCCESS → FINALIZED (every 1 hour)
- **TxLifecycleController**: REST API endpoints
- **TxLifecycleWebSocketHandler**: WebSocket handler for real-time notifications (optional)

## Database Schema

```sql
CREATE TABLE submitted_transaction (
    tx_hash                 VARCHAR(64) PRIMARY KEY,
    cbor_hex                TEXT NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    submitted_at            TIMESTAMP NOT NULL,
    confirmed_at            TIMESTAMP,
    confirmed_slot          BIGINT,
    confirmed_block_number  BIGINT,
    success_at              TIMESTAMP,
    finalized_at            TIMESTAMP,
    error_message           TEXT,
    update_datetime         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Rollback Handling

According to [Cardano Wallet Transaction Lifecycle](https://cardano-foundation.github.io/cardano-wallet/design/concepts/transaction-lifecycle.html):

1. When a transaction is rolled back due to chain reorganization, it is removed from the blockchain database
2. The transaction **may reappear** in the winning fork if nodes re-include it
3. If the transaction doesn't naturally reappear, it **must be re-submitted**

### Re-submission Strategy

- **Automatic**: System can automatically re-submit `ROLLED_BACK` transactions
- **Manual**: Users can manually re-submit via the same submission endpoint
- Both approaches will transition the transaction back to `SUBMITTED` → `CONFIRMED`

## Example Usage

### Java Client

```java
@Autowired
private SmartTxSubmissionService smartSubmissionService;

// Submit transaction
SubmittedTransaction result = smartSubmissionService.submitTransaction(cborBytes);

System.out.println("Status: " + result.getStatus());
System.out.println("TxHash: " + result.getTxHash());
```

### Check Status

```java
@Autowired
private TxLifecycleService lifecycleService;

Optional<SubmittedTransaction> tx = lifecycleService.getTransaction(txHash);
tx.ifPresent(t -> {
    System.out.println("Status: " + t.getStatus());
    System.out.println("Confirmations: " + t.getConfirmations(currentBlock));
});
```

## Benefits

- ✅ **No External Tools Required**: Built-in tracking without needing external monitoring services
- ✅ **Automatic Rollback Handling**: Detects and handles chain reorganizations
- ✅ **Flexible Finality**: Configurable SUCCESS state for different application needs
- ✅ **Real-time Updates**: Optional WebSocket support for instant notifications
- ✅ **Production-Ready**: Optimized scheduled jobs and indexed queries

## References

- [ADR-001: Smart Transaction Submission](../../adr/ADR-001-smart-transaction-submission.md)
- [Cardano Wallet - Transaction Lifecycle](https://cardano-foundation.github.io/cardano-wallet/design/concepts/transaction-lifecycle.html)
- [Cardano Stack Exchange - Rollback Best Practices](https://cardano.stackexchange.com/questions/4614/best-practice-for-handling-rollbacks)

## License

Same as Yaci Store main project.

