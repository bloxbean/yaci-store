package com.bloxbean.cardano.yaci.store.adminui.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerStateStatusDto {
    private int currentEpoch;
    private int lastProcessedEpoch;
    private boolean jobRunning;
    private String lastJobStatus;
    private String lastJobError;
    private Long lastJobTimestamp;
}
