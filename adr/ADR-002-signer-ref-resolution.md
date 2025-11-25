# ADR-002: Signer reference resolution for TxPlan / QuickTx

## Status
Proposed

## Context
- QuickTx YAML (`TxPlan`) supports `from_ref`, `fee_payer_ref`, `collateral_payer_ref`, and `signer` references. These are resolved through `SignerRegistry` when building or signing transactions.
- The submit component currently builds unsigned tx bodies from YAML without any registry, so any `_ref` values would fail (`TxBuildException: ... no SignerRegistry configured`).
- We want clients to author YAML using stable references (e.g., `account://alice`, `wallet://ops`, `policy://nft`, `remote://alice`) while the server maps those to concrete signers/addresses.
- Some signers may live on remote services (e.g., dripdropz/remote-signer in Rust) rather than local key material.

## Goals
- Define how signer references are configured and resolved on the server.
- Allow `from_ref` to auto-populate sender/change/collateral addresses and signers during build/sign.
- Support remote signing endpoints as a signer type (e.g., dripdropz remote-signer).
- Keep YAML portable and free of sensitive key material.

## Non-Goals
- Define the full key management story (HSM, KMS) — only the reference and resolution model.
- Change TxPlan YAML schema; we will leverage existing `*_ref` and `signers` fields.

## Decision / Proposal
- Introduce a `SignerRegistry` bean in the submit module, wired into `QuickTxBuilder.compose(plan, registry)`, so TxPlanBuildService and future sign flows can resolve references.
- Registry entries are configured server-side (Spring config/yaml). Each entry binds a `ref` URI to a resolver that can:
  - Provide a preferred address (for `from_ref`, `fee_payer_ref`, `collateral_payer_ref`).
  - Provide a signer for one or more scopes (`payment`, `stake`, `drep`, `committeeCold`, `committeeHot`, `policy`).
  - Optionally provide a wallet object when available to let QuickTx set sender/change addresses automatically.
- Supported signer types (initial):
  - `local-key`: payment/stake/policy keys stored locally (file/keystore/env); produces a `TxSigner`.
  - `local-wallet`: HD wallet seed or xprv/xpub; can yield both address and signer.
  - `remote-signer`: HTTP client that delegates signing to an external service (e.g., dripdropz/remote-signer). Needs endpoint, auth, and scope mapping. The signer implementation should call the remote service with the CBOR to sign and return witnesses.
  - `address-only`: supplies an address for `from_ref`/payers without signing (useful for build-only flows).
- Example configuration (shape to be refined in code):

```yaml
store:
  submit:
    signer-registry:
      entries:
        - ref: account://alice
          type: local-wallet
          scopes: [payment, stake]
          wallet:
            mnemonic: ${ALICE_MNEMONIC}
            accountIndex: 0
        - ref: policy://nft
          type: local-key
          scopes: [policy]
          keys:
            paymentSkey: file:/keys/policy.skey
        - ref: remote://ops
          type: remote-signer
          scopes: [payment, stake, policy]
          remote:
            baseUrl: https://remote-signer.example.com
            authToken: ${REMOTE_SIGNER_TOKEN}
            keyId: ops-key-1
        - ref: wallet://treasury
          type: address-only
          scopes: [payment]
          address: addr1...
```

- For remote signer support:
  - Define an interface (e.g., `RemoteSignerClient`) so implementations can target dripdropz/remote-signer or other services.
  - Map signer scope to remote key id or role; include auth headers.
  - Ensure we never ship private keys to clients; all signing happens server-side or remote-side.

## Notes on behavior
- If a TxPlan uses `_ref` fields, TxPlanBuildService must call `quickTxBuilder.compose(plan, signerRegistry)` instead of `compose(plan)`.
- Build-only flows can still work with `address-only` entries; signing flows will require actual signer-capable types.
- Missing or unresolved references should fail fast with a clear error naming the ref and scope.
- We should log reference resolution at debug level, but never log key material or tokens.

## Remote signer integration options
- Treat `remote://*` refs as `TxSigner` producers that delegate signing to a remote service. Server-side registry entries map the ref to a client bean plus routing metadata (key id, scopes, auth).
- Initial target (per PR discussion https://github.com/bloxbean/cardano-client-lib/pull/542#issuecomment-3468098461): DripDropz [`remote-signer`](https://github.com/dripdropz/remote-signer).
  - gRPC bidirectional stream (`RemoteSigner.ConnectToServer` in `protos/remote_signer.proto`).
  - Handshake: client sends `address + vkey + challenge + challenge_signature`; server replies with a signed challenge to confirm the host master key (mitm protection) before any signing happens.
  - Signing: server streams `TransactionMessage {request_id, cbor}` where `cbor` is the tx body; client signs the tx hash with the configured key and replies with `SignatureMessage {request_id, signature}`.
  - Keepalive: ping/pong on the same stream.
- Wrap the gRPC client in a `RemoteSignerClient` abstraction so we can plug in other remote schemes (HTTP, different protos) without changing registry wiring.
- Security: enforce bearer/JWT auth, verify the server master key from config, and pin allowed scopes per registry entry. Never log tokens or key material; emit structured errors/metrics instead.

## Implementation sketch / next steps
- Add a Spring-configured `SignerRegistry` bean in the submit component that loads entries from `store.submit.signer-registry`.
- Model entry types for `local-wallet`, `local-key`, `address-only`, and `remote-signer` (endpoint, auth token/credentials, key id, allowed scopes).
- Wire `TxPlanBuildService` to prefer `quickTxBuilder.compose(plan, signerRegistry)` when the registry exists (already supported by QuickTx).
- Provide a `RemoteSignerClient` SPI plus a DripDropz implementation using the proto above (Netty gRPC, TLS, bearer auth, host-key pin). Map `remote://ref` → `(client, keyId)` → `TxSigner`.
- Validate and fail fast on unknown refs or unsupported scopes; once the registry is wired, document YAML examples in `components/submit/README.md`.

## Open Questions
- How to store secrets (mnemonics, skeys, remote tokens) securely in deployments (Vault/K8s secrets)?
- Should we allow per-signer policy restricting scopes (e.g., remote signer only for policy keys)?
- Do we need hot-reload of registry entries without restart?
- What wire format does the remote signer expect/return (CBOR tx body, witness set)? Align with dripdropz API before implementation.
