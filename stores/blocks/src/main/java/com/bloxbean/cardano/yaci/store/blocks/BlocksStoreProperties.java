package com.bloxbean.cardano.yaci.store.blocks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlocksStoreProperties {
    private boolean enabled;

    private boolean metricsEnabled = true;
    private long metricsUpdateInterval = 60000; // 60 seconds
}
