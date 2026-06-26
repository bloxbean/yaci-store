package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model;

/**
 * Aiken {@code Credential} discriminator for CIP-113 transfer-logic-script fields.
 * <p>
 * The on-chain Aiken type is {@code Credential = VerificationKey(hash) | Script(hash)}.
 * Both variants carry a 28-byte Blake2b-224 hash, so the hash column alone cannot
 * distinguish them after persistence — this enum captures the constructor variant so
 * downstream consumers can validate transfer logic correctly (vkey signatures vs
 * script-witness execution have very different verification semantics).
 */
public enum Cip113CredentialType {
    /** {@code VerificationKey(hash)} — Constr alternative 0. The hash is a vkey hash. */
    VKEY,
    /** {@code Script(hash)} — Constr alternative 1. The hash is a Plutus script hash. */
    SCRIPT
}
