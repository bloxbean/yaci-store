package com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model;

import java.math.BigInteger;

public record AccountWithdrawal(
        String txHash,
        BigInteger amount
) {}
