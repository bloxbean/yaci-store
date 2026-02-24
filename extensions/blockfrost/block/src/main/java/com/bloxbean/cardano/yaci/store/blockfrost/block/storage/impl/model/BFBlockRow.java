package com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model;

import java.math.BigInteger;

public record BFBlockRow(
        Long time,
        Long height,
        String hash,
        Long slot,
        Integer epoch,
        Integer epochSlot,
        String slotLeader,
        Long size,
        Integer txCount,
        BigInteger output,
        BigInteger fees,
        String blockVrf,
        String opCert,
        Long opCertCounter,
        String previousBlock,
        String nextBlock,
        Long confirmations
) {
}
