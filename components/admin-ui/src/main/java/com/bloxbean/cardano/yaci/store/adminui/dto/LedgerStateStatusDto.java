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
    private boolean enabled;
    private int currentEpoch;
    private int lastProcessedEpoch;
    private boolean jobRunning;
    private String lastJobStatus;
    private String lastJobError;
    private Integer lastErrorEpoch;
    private Long lastJobTimestamp;
}
