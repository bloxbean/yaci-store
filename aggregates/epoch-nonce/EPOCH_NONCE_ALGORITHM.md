# Epoch Nonce Computation Algorithm

This document describes how the Cardano epoch nonce is computed, as implemented in the `epoch-nonce` module.

## Overview

Each Cardano epoch has an **epoch nonce** that provides randomness for the leader schedule of a future epoch.
The nonce is derived from VRF outputs of blocks and evolves continuously across epoch boundaries.

## Algorithm

### Per-block processing (continuous across epochs — no reset)

For every block in slot order:

```
vrfOutput = block's VRF output bytes

# Era-aware eta derivation:
if era <= Alonzo (TPraos, era value <= 5):
    eta = Blake2b_256(vrfOutput)
if era >= Babbage (Praos, era value >= 6):
    eta = Blake2b_256(Blake2b_256("N" || vrfOutput))

# Update evolving nonce:
evolvingNonce = Blake2b_256(evolvingNonce || eta)

# Snapshot candidate if within stability window:
firstSlotNextEpoch = firstSlotOfCurrentEpoch + epochLength
if (slot + stabilityWindow < firstSlotNextEpoch):
    candidateNonce = evolvingNonce

# Track previous block hash:
labNonce = prevHash(block)
```

### At epoch boundary (TICKN transition)

When transitioning from epoch N to epoch N+1:

```
# 1. Process all blocks from completed epoch N (updates evolving, candidate, labNonce)

# 2. Compute the new epoch nonce using the ⭒ (star/combine) operator:
epochNonce = candidateNonce ⭒ ticknPrevHashNonce

# 3. Carry forward the labNonce for the NEXT epoch's TICKN:
ticknPrevHashNonce' = labNonce

# 4. evolving/candidate are NOT reset — they continue into the next epoch
```

### The ⭒ (star) operator

```
Nonce(a) ⭒ Nonce(b)    = Blake2b_256(a || b)
x        ⭒ NeutralNonce = x
NeutralNonce ⭒ x        = x
```

### Genesis initialization

At the first Shelley epoch:
```
genesisHash = Blake2b_256(raw bytes of shelley-genesis.json)
evolvingNonce = genesisHash
candidateNonce = genesisHash
labNonce = null (NeutralNonce)
ticknPrevHashNonce = null (NeutralNonce)
```

**Important**: The genesis hash is computed over the exact raw bytes of the official `shelley-genesis.json` file.
Whitespace and formatting matter — the file must be byte-for-byte identical to the canonical version.

## Key Parameters

| Parameter | Formula | Pre-Conway | Conway+ |
|-----------|---------|------------|---------|
| stabilityWindow (for nonce) | see below | floor(3k/f) = 129,600 | ceiling(4k/f) = 172,800 |
| epochLength | from genesis | 432,000 | 432,000 |
| securityParam (k) | from genesis | 2,160 | 2,160 |
| activeSlotsCoeff (f) | from genesis | 0.05 | 0.05 |

### Era-dependent stability window

The stability window used for the candidate nonce freeze cutoff changed at the Conway hard fork:

| Era | Haskell function | Formula | Value (k=2160, f=0.05) |
|-----|---------|---------|------------------------|
| Shelley through Babbage | `computeStabilityWindow` | `floor(3k/f)` | 129,600 (36h) |
| Conway+ | `computeRandomnessStabilisationWindow` | `ceiling(4k/f)` | 172,800 (48h) |

This change was introduced in **ouroboros-consensus v0.15.0.0** (backwards-incompatible), which set
Conway's `praosRandomnessStabilisationWindow` to `computeRandomnessStabilisationWindow` (ceiling(4k/f))
instead of the previous `computeStabilityWindow` (3k/f). See also **cardano-ledger erratum 17.3**.

For pre-Conway eras, the consensus layer's `PraosParams.praosRandomnessStabilisationWindow` field was
populated with `computeStabilityWindow` (3k/f) despite the naming suggesting otherwise.

## State Model

The `EpochNonce` record stores:

| Field | Description |
|-------|-------------|
| `nonce` | The computed epoch nonce for this epoch |
| `evolvingNonce` | Post-processing evolving nonce (continuous, never reset) |
| `candidateNonce` | Post-processing candidate nonce (continuous, never reset) |
| `labNonce` | prevHash of the last block in the completed epoch |
| `lastEpochBlockNonce` | The `ticknPrevHashNonce` — labNonce from the previous epoch, used in TICKN |

## Haskell Reference Code

The implementation follows these Haskell modules in the Cardano codebase:

- **TPraos VRF nonce** (era <= 5): [ouroboros-consensus/.../Protocol/TPraos.hs](https://github.com/IntersectMBO/ouroboros-consensus/blob/main/ouroboros-consensus-protocol/src/ouroboros-consensus-protocol/Ouroboros/Consensus/Protocol/TPraos.hs)
  - `tickChainDepState` — updates the TICKN state at epoch boundary
  - `updateChainDepState` — processes each block's VRF output

- **Praos VRF nonce** (era >= 6): [ouroboros-consensus/.../Protocol/Praos.hs](https://github.com/IntersectMBO/ouroboros-consensus/blob/main/ouroboros-consensus-protocol/src/ouroboros-consensus-protocol/Ouroboros/Consensus/Protocol/Praos.hs) and [Praos/VRF.hs](https://github.com/IntersectMBO/ouroboros-consensus/blob/main/ouroboros-consensus-protocol/src/ouroboros-consensus-protocol/Ouroboros/Consensus/Protocol/Praos/VRF.hs)

- **TICKN state machine**: [cardano-ledger/.../Shelley/Rules/Tick.hs](https://github.com/IntersectMBO/cardano-ledger/blob/master/eras/shelley/impl/src/Cardano/Ledger/Shelley/Rules/Tick.hs) — `TICKN` rule applies `candidateNonce ⭒ prevHashNonce`

- **Nonce combine (⭒)**: [cardano-ledger/.../BaseTypes.hs](https://github.com/IntersectMBO/cardano-ledger/blob/master/libs/cardano-ledger-core/src/Cardano/Ledger/BaseTypes.hs) — `Nonce` type and `(⭒)` operator

- **Stability windows**: [cardano-ledger/.../Shelley/StabilityWindow.hs](https://github.com/IntersectMBO/cardano-ledger/blob/master/eras/shelley/impl/src/Cardano/Ledger/Shelley/StabilityWindow.hs) — `computeRandomnessStabilisationWindow = ceiling(4k/f)`, `computeStabilityWindow = ceiling(3k/f)`

- **Evolving nonce (Updn)**: [cardano-ledger/.../Shelley/Rules/Updn.hs](https://github.com/IntersectMBO/cardano-ledger/blob/master/eras/shelley/impl/src/Cardano/Ledger/Shelley/Rules/Updn.hs) — evolving and candidate nonce updates

## Verified Against

Epoch nonce values have been empirically verified against Cardano dbsync for preprod epochs 4–163+
(spanning pre-Conway and Conway eras).
