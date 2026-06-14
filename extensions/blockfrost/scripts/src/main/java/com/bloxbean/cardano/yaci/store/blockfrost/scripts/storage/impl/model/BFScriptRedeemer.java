package com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model;

/**
 * Raw storage model for a redeemer row joined from transaction_scripts.
 * The {@code fee} field is computed from execution units and protocol parameters:
 * {@code fee = ceil(unitMem * priceMem + unitSteps * priceStep)}.
 */
public record BFScriptRedeemer(
        String txHash,
        Integer txIndex,        // redeemer_index from transaction_scripts
        String purpose,        // raw DB value e.g. "SPEND", "MINT", "Spend", "Mint"
        Long   unitMem,
        Long   unitSteps,
        String fee,            // pre-computed execution fee in lovelace (as string)
        String redeemerDataHash,
        String datumHash
) {}
