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

  # Required for TxPlan builder (QuickTx)
  utxo:
    enabled: true
  epoch:
    enabled: true
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

### Build Unsigned Transaction from TxPlan (YAML)

**POST** `/api/v1/tx/plan/build`

Build an unsigned transaction from a YAML TxPlan definition using QuickTx.

**Request (YAML):**
```yaml
version: 1.0
context:
  fee_payer: addr_test1qp...
transaction:
  - tx:
      from: addr_test1qp...
      intents:
        - type: payment
          to: addr_test1zr...
          amount:
            unit: lovelace
            quantity: 5000000
```

**Response:**
```json
{
  "txBodyCbor": "84a40081825820..."
}
```

> Requires `store.cardano.ogmios-url` plus the local UTXO and Epoch modules (`store.utxo.enabled`, `store.epoch.enabled`). Signer references still require explicit addresses until a signer registry is configured.

### Signer registry (experimental)

Enable refs in TxPlan YAML (`from_ref`, `fee_payer_ref`, `signers`) by binding them to local accounts, remote signers, or address-only placeholders:

```yaml
store.submit.signer-registry.enabled: true
store.submit.signer-registry.entries:
  - ref: account://alice
    type: account
    scopes: [payment, stake]
    account:
      mnemonic: "${ALICE_MNEMONIC}"
      account: 0
      index: 0
  - ref: address://treasury
    type: address_only
    scopes: [payment]
    address:
      address: addr_test1...
  - ref: remote://ops
    type: remote_signer
    scopes: [payment, stake, policy]
    remote:
      key-id: ops-key-1
      endpoint: ${REMOTE_SIGNER_ENDPOINT:https://remote-signer.example.com}
      auth-token: ${REMOTE_SIGNER_TOKEN:}
      verification-key: ${REMOTE_SIGNER_VKEY:}
      address: ${REMOTE_SIGNER_ADDRESS:}
```

Notes:
- `account` bindings resolve senders/fee payers and can produce signers for payment/stake/drep/committee scopes.
- `address_only` bindings expose only an address (no signing) for build-only flows.
- `remote_signer` delegates signing to a `RemoteSignerClient` bean; bring your own implementation (e.g., DripDropz remote-signer gRPC client) and wire it into Spring.

## WebSocket Real-time Notifications (Broadcast Mode)

🎯 **Broadcast Mode**: WebSocket automatically broadcasts ALL transaction status updates to all connected clients. No subscription needed!

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

### Receive Status Updates (Automatic)

After connecting, you'll automatically receive ALL transaction updates:

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

### Benefits of Broadcast Mode

- ✅ **Simple**: Just connect - no subscription messages needed
- ✅ **Comprehensive**: Receive updates for all transactions in the system
- ✅ **Perfect for**: Monitoring dashboards, audit systems, real-time displays
- ✅ **Flexible**: Filter on client-side if you only care about specific transactions

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
