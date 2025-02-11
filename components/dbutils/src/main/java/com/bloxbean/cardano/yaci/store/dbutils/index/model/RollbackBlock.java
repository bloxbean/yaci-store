package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RollbackBlock {
    private String hash;
    private Long number;
    private Long slot;
    private Integer epoch;
    private Integer epochSlot;
    private Integer era;
}
