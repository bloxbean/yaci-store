# ADR 0002: Scalus Transaction Evaluation

## Status

Proposed

## Date

2026-05-13

## Context

Yaci Store currently exposes a Blockfrost-compatible transaction evaluation
endpoint at `/utils/txs/evaluate`. The endpoint is enabled only when
`OgmiosService` is available, which requires `store.cardano.ogmios-url`.

This makes transaction evaluation dependent on an external Ogmios instance even
when Yaci Store already has the local indexed data needed to resolve UTxOs,
protocol parameters, and reference scripts.

Scalus exposes `scalus.bloxbean.ScalusTransactionEvaluator`, an implementation
of Cardano Client Lib's `TransactionEvaluator`. It can evaluate Plutus scripts
from a transaction CBOR when given protocol parameters, a UTxO supplier, a
script supplier, and a correct `SlotConfig`.

## Decision

Add Scalus as an alternative transaction evaluator in the submit module.

Evaluation backend selection will be controlled by:

```properties
store.submit.tx-evaluator-mode=ogmios
```

Supported values:

- `ogmios`: default; preserve current behavior.
- `scalus`: use Scalus even if `store.cardano.ogmios-url` is configured.

Bind this property as an enum-backed Spring `@ConfigurationProperties` value so
unknown values fail during application startup instead of surfacing as a
request-time transaction evaluation error.

The existing endpoint paths and Blockfrost-compatible response shape will remain
unchanged.

## Implementation Notes

Add Scalus `0.17.0` to `gradle/libs.versions.toml` and reference it from
`components/submit/build.gradle`. Scalus must be used with the repository
managed Cardano Client Lib version; do not bump CCL as part of this change
unless compilation or runtime validation proves it is required. Exclude
transitive Cardano Client Lib dependencies from Scalus if they conflict with the
repository-managed CCL version.

Do not make the submit module depend directly on epoch, UTxO, or script store
modules. Use client interfaces:

- `UtxoClient` for UTxO lookup.
- new minimal `EpochParamClient` for latest protocol parameters.

`EpochParamClient` returns Cardano Client Lib `ProtocolParams`, because Scalus'
Java-facing evaluator consumes CCL protocol params directly. The local
implementation lives in `stores:epoch` as `EpochParamClientImpl`, is enabled by
default when that module is on the classpath, and maps Yaci Store
domain `ProtocolParams` to CCL. Do not add a remote epoch parameter client or
`store.epoch-param-client-url` for the initial release because there is no
current deployment use case for it.

Do not add a `ScriptClient` for the initial implementation. Reference scripts
are stored on UTxO outputs as `AddressUtxo.scriptRef` with
`referenceScriptHash`, and the UTxO APIs already return those fields.

However, Scalus' CCL interop cannot read `AddressUtxo.scriptRef` directly
because Cardano Client Lib's `Utxo` model only carries `referenceScriptHash`.
Scalus resolves reference scripts by calling the supplied
`scalus.bloxbean.ScriptSupplier` with that hash. Therefore, implement a custom
`ScriptSupplier` backed by resolved `AddressUtxo.scriptRef` values and pass it
to `ScalusTransactionEvaluator`. Do not use Scalus' `NoScriptSupplier` for
transactions that may contain reference scripts.

The Scalus UTxO supplier should bridge both concerns: when it resolves an
`AddressUtxo`, it should map the output to a CCL `Utxo` and register any
`scriptRef` on the custom `ScriptSupplier`, keyed by `referenceScriptHash`.
The script supplier can deserialize the stored script reference using
`ScriptReferenceUtil.deserializeScriptRef(...)` from `components:common` and
return the resulting CCL Plutus script. Add a separate `ScriptClient` later only
if there is a concrete evaluation case where required scripts are not reachable
through transaction witnesses or resolved UTxOs.

Protocol parameter mapping from Yaci Store domain objects to Cardano Client Lib
`ProtocolParams` is part of the implementation. This mapping must include cost
models by language version, including Plutus V1, V2, and V3 where present.
Cost model arrays should be exposed to CCL as numeric string keys (`"0"`,
`"1"`, ...) to preserve ledger array order.

Scalus must be run in `EvaluatorMode.EVALUATE_AND_COMPUTE_COST` for this
endpoint. `EvaluatorMode.VALIDATE` is not the target behavior because the
Blockfrost-compatible endpoint is expected to compute script execution units.

Scalus `EvaluationResult` values must be mapped to the same purpose labels that
the current Ogmios JSON-RPC path passes through for the v5-compatible formatter:

- `Spend` -> `spend`
- `Mint` -> `mint`
- `Cert` -> `publish`
- `Reward` -> `withdraw`
- `Voting` -> `vote`
- `Proposing` -> `propose`

Do not add a separate purpose mapping to the Ogmios path. The existing Ogmios
path should continue to lower-case the purpose returned by Ogmios and use that
value in the Blockfrost-compatible response key.

The `evaluate/utxos` request's `additionalUtxoSet` should be honored in the
Scalus path by passing the supplied UTxOs into
`ScalusTransactionEvaluator.evaluateTx(byte[], Set<Utxo>)`. The existing Ogmios
path can remain unchanged for compatibility, but the API documentation should be
updated so the "additional UTxOs are ignored" note applies only to Ogmios mode,
not to Scalus mode.

Do not model `additionalUtxoSet` as an unstructured `Object`. The
Blockfrost-compatible endpoint documents this value as an array of `[TxIn,
TxOut]` tuples, and the Blockfrost SDK references the Ogmios additional UTxO set
format. Use a typed `OgmiosUtxo` request object that can parse the tuple shape
and normalize both common value encodings:

- Blockfrost SDK shape: `TxIn={txId,index}` and
  `TxOut.value={coins,assets}`.
- Ogmios schema/docs shape: transaction reference fields such as
  `transaction.id` plus `index`/`output.index`, and
  `TxOut.value={ada:{lovelace}, <policyId>:{<assetName>: quantity}}`.

Keep parsing mode-aware. Ogmios mode ignores `additionalUtxoSet` today, so the
controller should keep the raw JSON payload and only parse/validate it when
Scalus mode is selected. Missing required `TxIn`/`TxOut` fields in Scalus mode
must fail fast as request validation errors, not become null CCL `Utxo` fields.

If `TxOut.script` contains a Plutus reference script, derive the script hash,
register the script in the per-evaluation `ScriptSupplier`, and put the derived
hash on the CCL `Utxo`. Native reference scripts may be ignored by this
Plutus-focused supplier unless a concrete Scalus evaluation case proves they are
required.

The Scalus UTxO supplier is intended for transaction-input lookup by tx hash and
output index. Address-page lookup should be treated as unsupported for
transaction evaluation unless a real Scalus evaluator path needs it later,
because address-page responses do not currently carry enough script reference
material to populate the custom `ScriptSupplier`.

The controller should not remain conditional on `OgmiosService`. It should be
available when the submit component is enabled and delegate to
`TxEvaluationService`, which performs runtime backend selection. If
`store.submit.tx-evaluator-mode=ogmios` is selected without
`store.cardano.ogmios-url`, evaluation should fail with a clear configuration
error rather than hiding the Scalus implementation from native images.

## SlotConfig

Scalus must be constructed with an explicit network-aware `SlotConfig`. The
constructors that default to `SlotConfig.mainnet` must not be used.

Use Yaci Store's existing era and genesis logic:

```text
zeroSlot   = EraService.getFirstNonByronSlot()
zeroTime   = EraService.shelleyEraStartTime() * 1000
slotLength = GenesisConfig.slotDuration(Era.Shelley) * 1000
```

This matters because Shelley genesis `systemStart` is not sufficient for all
networks. For mainnet, Yaci's bundled Shelley genesis has `systemStart` at Byron
start time, while Scalus needs the Shelley transition time.

Expected known values include:

- mainnet: `zeroSlot=4492800`, `zeroTime=1596059091000`, `slotLength=1000`
- preview: `zeroSlot=0`, `zeroTime=1666656000000`, `slotLength=1000`
- preprod: `zeroSlot=86400`, `zeroTime=1655769600000`, `slotLength=1000`

The derived `SlotConfig` may be cached per application instance because these
values are network constants for the running store.

## Native Image Considerations

The evaluator selection should be runtime-native-image friendly.

Avoid property-conditional bean registration for Scalus-only beans. In Spring
AOT/native builds, property conditions may be evaluated during image generation,
which can make runtime property switching unreliable.

Instead, create the required evaluator components so they are reachable in the
native image, and choose the backend at runtime inside a delegating
`TxEvaluationService`.

Add native hints only if Scalus, Scala, or Jackson reflection failures appear
during native testing.

## Consequences

### Positive

- Transaction evaluation no longer requires Ogmios.
- Operators can explicitly choose Ogmios or Scalus.
- Existing public API paths remain stable.
- The submit module remains decoupled from store implementation modules.
- Native image runtime switching is preserved.

### Negative

- Scalus evaluation depends on correctly indexed UTxOs, protocol params,
  reference scripts embedded in UTxOs, and era data.
- A new client abstraction is required for protocol params.
- Native image validation is required because Scalus brings Scala dependencies.
- Evaluation behavior must be compared carefully against Ogmios for compatibility.
- The submit artifact includes Scalus and Scala dependencies even for
  Ogmios-only deployments so that runtime evaluator switching remains reachable
  in native images.

## Validation Plan

Add tests for:

- evaluator mode selection;
- Ogmios mode preserving current behavior;
- Scalus mode winning even when Ogmios URL exists;
- invalid evaluator mode failing during configuration binding;
- `SlotConfig` derivation for mainnet, preview, and preprod;
- assertions that derived `SlotConfig` values match the known constants listed
  above for bundled mainnet, preview, and preprod genesis data;
- UTxO mapping from Yaci domain objects to CCL UTxOs;
- `additionalUtxoSet` handling in Scalus mode;
- typed parsing of Blockfrost/Ogmios `[TxIn, TxOut]` additional UTxO tuples,
  including lovelace, native assets, inline datum, and Plutus reference scripts;
- compatibility check that Ogmios mode still ignores malformed
  `additionalUtxoSet` payloads;
- 400 response mapping for malformed Scalus-mode `additionalUtxoSet` payloads;
- protocol parameter mapping to CCL `ProtocolParams`, spot-validated against a
  real epoch parameter dump with Plutus cost models;
- reference script resolution through the custom `ScriptSupplier` populated from
  `AddressUtxo.scriptRef`, without a separate `ScriptClient`;
- Scalus `EvaluationResult` mapping to the existing Blockfrost-compatible v5
  response, including non-spend redeemer purpose names;
- explicit failure for unsupported address-page UTxO lookup in the Scalus
  supplier.

Run:

```bash
./gradlew :components:client:test
./gradlew :components:submit:test
./gradlew :stores:epoch:compileJava
./gradlew :stores-api:epoch-api:compileJava
./gradlew :starters:spring-boot-starter:compileJava
./gradlew :starters:submit-spring-boot-starter:compileJava
./gradlew :applications:all:compileJava
./gradlew :applications:utxo-indexer:compileJava
```

Also build and run a native image with:

```properties
store.submit.tx-evaluator-mode=scalus
```

## Final Position

Scalus should be added as an explicit alternative transaction evaluator, not as a
hidden fallback only. The release-safe design is to keep the submit module
decoupled, use existing or minimal client interfaces for ledger context, pass an
explicit `SlotConfig`, and perform evaluator selection at runtime for native
image compatibility.
