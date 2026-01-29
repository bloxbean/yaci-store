package com.bloxbean.cardano.yaci.store.adminui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusDto {
    private long block;
    private long slot;
    private int epoch;
    private String era;
    private String blockHash;
    private double syncPercentage;
    private long networkBlock;
    private long networkSlot;
    private boolean synced;
    private long protocolMagic;
}
