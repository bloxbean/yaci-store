package com.bloxbean.cardano.yaci.store.account.util;

public enum ConfigStatus {
    //For batch aggregation
    BATCH_AGGR_IN_PROGRESS,
    BATCH_AGGR_STOPPED,
    BATCH_AGGR_REQUEST_TO_STOP,

    //For regular sync
    IN_SYNC,
    OUT_OF_SYNC,
}
