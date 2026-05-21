package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;
import java.util.Map;

public record AccountUtxo(
        String address,
        String txHash,
        int outputIndex,
        Map<String, BigInteger> amounts,
        String blockHash,
        String dataHash,
        String inlineDatum,
        String referenceScriptHash
) {}
