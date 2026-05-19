package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;

public record AccountAddressAsset(
        String unit,
        BigInteger quantity
) {}
