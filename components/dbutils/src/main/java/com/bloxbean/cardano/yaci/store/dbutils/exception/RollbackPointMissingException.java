package com.bloxbean.cardano.yaci.store.dbutils.exception;

public class RollbackPointMissingException extends RuntimeException {
    private final int epoch;

    public RollbackPointMissingException(int epoch) {
        super(String.format("Block table not available and manual rollback point not provided for epoch %d. " +
                "Please provide block number and block hash using CLI options (--block, --block-hash) " +
                "or configure them in application properties (store.rollback.point.block, store.rollback.point.block_hash)", epoch));
        this.epoch = epoch;
    }

    public int getEpoch() {
        return epoch;
    }
}

