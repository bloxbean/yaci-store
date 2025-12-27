package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RollbackContext {
    private boolean rollbackLedgerState;
    private int epoch;
    private long eventPublisherId;
    private Long rollbackPointBlock;
    private String rollbackPointBlockHash;
    private Long rollbackPointSlot;
    private Integer rollbackPointEra;
}
