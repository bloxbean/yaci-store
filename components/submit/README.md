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
  "tx_hash": "1234567890abcdef...",
  "status": "SUBMITTED",
  "tx_body_cbor": "84a400818258201234...5678",
  "submitted_at": "2025-10-12T10:30:00Z"
}
```

### Get Transaction Status

**GET** `/api/v1/tx/lifecycle/{txHash}`

Query the current status and details of a submitted transaction.

**Response:**
```json
{
  "tx_hash": "1234567890abcdef...",
  "status": "SUCCESS",
  "submitted_at": "2025-10-12T10:30:00Z",
  "confirmed_at": "2025-10-12T10:31:00Z",
  "confirmed_slot": 123456789,
  "confirmed_block_number": 9000000,
  "confirmations": 20,
  "success_at": "2025-10-12T10:36:00Z"
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

## Build unsigned tx from TxPlan (QuickTx)

**POST** `/api/v1/tx/lifecycle/build`

Request (YAML):
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

Response:
```json
{
  "txBodyCbor": "84a40081825820..."
}
```

Requires Ogmios (`store.cardano.ogmios-url`) plus local UTXO and Epoch modules (`store.utxo.enabled`, `store.epoch.enabled`).

### Signer Registry

Enable TxPlan `_ref` fields (`from_ref`, `fee_payer_ref`, `signers`) by binding them to accounts, address-only refs, or remote signers.

#### Configuration

```yaml
store:
  submit:
    signer-registry:
      enabled: true
      entries:
        - ref: account://alice
          type: account
          scopes: [payment, stake]    # Or comma-separated: "payment,stake"
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
            endpoint: https://remote-signer.example.com/sign
            auth-token: ${REMOTE_SIGNER_TOKEN}
            key-id: ops-key-1
            verification-key: ${REMOTE_SIGNER_VKEY}
            address: ${REMOTE_SIGNER_ADDRESS}
            timeout-ms: 5000
```

#### Signer Types

| Type | Description | Use Case |
|------|-------------|----------|
| `account` | Local signing with mnemonic, bech32 private key, or root key | Development, self-custody |
| `address_only` | Build-only, provides address without signing capability | Multi-sig workflows, external signing |
| `remote_signer` | Delegates signing to external HTTP service | Production, HSM, key management services |

#### Account Signer Properties

| Property | Required | Description |
|----------|----------|-------------|
| `account.mnemonic` | One of three | 24-word mnemonic phrase |
| `account.bech32-private-key` | One of three | Bech32-encoded private key |
| `account.root-key-hex` | One of three | Hex-encoded root key |
| `account.account` | No | HD derivation account index (default: 0) |
| `account.index` | No | HD derivation address index (default: 0) |

#### Remote Signer Properties

| Property | Required | Description |
|----------|----------|-------------|
| `remote.endpoint` | Yes | URL of the remote signing service |
| `remote.key-id` | Yes | Key identifier sent to remote service |
| `remote.auth-token` | No | Bearer token for authentication |
| `remote.verification-key` | No | Fallback verification key (hex) if not returned by service |
| `remote.address` | No | Preferred address for `from_ref` resolution |
| `remote.timeout-ms` | No | HTTP request timeout in milliseconds |

#### Remote Signer HTTP Protocol

The default `HttpRemoteSignerClient` communicates via HTTP POST:

**Request:**
```http
POST {endpoint}/sign
Content-Type: application/json
Authorization: Bearer {auth-token}

{
  "keyId": "ops-key-1",
  "scope": "payment",
  "txBody": "84a400818258...",
  "address": "addr_test1...",
  "verificationKey": "5820..."
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `keyId` | Yes | Key identifier from configuration |
| `scope` | Yes | Signing scope: `payment`, `stake`, `policy`, `drep`, `committeecold`, `committeehot` |
| `txBody` | Yes | Hex-encoded CBOR transaction body |
| `address` | No | Address hint (if configured) |
| `verificationKey` | No | Verification key hint (if configured) |

**Response:**
```json
{
  "signature": "845820...",
  "verificationKey": "5820..."
}
```

| Field | Required | Description |
|-------|----------|-------------|
| `signature` | Yes | Hex-encoded Ed25519 signature |
| `verificationKey` | Conditional | Hex-encoded public key (required if not pre-configured) |

**Notes:**
- If endpoint doesn't end with `/sign`, it's automatically appended
- Verification key fallback: response → configured `verification-key` → error
- Scopes can be comma-separated in TxPlan (e.g., `scope: payment,stake`)

#### Local Development Stub

For local development and E2E testing, enable the built-in stub signer:

```yaml
store:
  submit:
    local-remote-signer:
      enabled: true
```

This exposes `POST /local-remote-signer/sign` which produces **real Ed25519 signatures** using test accounts.

**Key ID Mapping:**

| Key ID | Account | Default Scope | Address |
|--------|---------|---------------|---------|
| `ops-key-1` | 0 | payment | `addr_test1qryvgass5dsrf2kxl3vgfz76uhp83kv5lagzcp29tcana68ca5aqa6swlq6llfamln09tal7n5kvt4275ckwedpt4v7q48uhex` |
| `stake-key-1` | 1 | stake | (stake operations) |
| `policy-key-1` | 2 | payment | (minting operations) |
| `payment-key-1` | 3 | payment | (additional payments) |

**Test Mnemonic:** `test test test test test test test test test test test test test test test test test test test test test test test sauce`

⚠️ **Warning:** Never use this stub or test mnemonic in production.

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

## References

- [ADR-001: Smart Transaction Submission](../../adr/ADR-001-smart-transaction-submission.md)
- [ADR-002: Signer Reference Resolution](../../adr/ADR-002-signer-ref-resolution.md)
- [Cardano Wallet - Transaction Lifecycle](https://cardano-foundation.github.io/cardano-wallet/design/concepts/transaction-lifecycle.html)

## License

Same as Yaci Store main project.
