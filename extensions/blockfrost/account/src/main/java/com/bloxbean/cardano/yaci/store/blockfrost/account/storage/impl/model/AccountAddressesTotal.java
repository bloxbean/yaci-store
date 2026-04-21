package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;
import java.util.Map;

public record AccountAddressesTotal(
        String stakeAddress,
        Map<String, BigInteger> receivedSum,
        Map<String, BigInteger> sentSum,
        long txCount
) {}
