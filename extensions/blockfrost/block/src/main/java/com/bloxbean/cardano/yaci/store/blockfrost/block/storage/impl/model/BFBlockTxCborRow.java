package com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model;

import java.util.Arrays;

public record BFBlockTxCborRow(
        String txHash,
        byte[] cborData,
        Integer txIndex
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BFBlockTxCborRow other)) return false;
        return java.util.Objects.equals(txHash, other.txHash)
                && Arrays.equals(cborData, other.cborData)
                && java.util.Objects.equals(txIndex, other.txIndex);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(txHash, txIndex);
        result = 31 * result + Arrays.hashCode(cborData);
        return result;
    }

    @Override
    public String toString() {
        return "BFBlockTxCborRow[txHash=" + txHash
                + ", cborData=" + Arrays.toString(cborData)
                + ", txIndex=" + txIndex + "]";
    }
}
