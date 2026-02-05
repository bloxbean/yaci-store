package com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model;

import java.math.BigInteger;
import java.util.Map;

public record BFAddressTotal(Map<String, BigInteger> receivedSum,
                             Map<String, BigInteger> sentSum,
                             long txCount) {
}
