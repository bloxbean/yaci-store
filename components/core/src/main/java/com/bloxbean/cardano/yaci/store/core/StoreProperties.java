package com.bloxbean.cardano.yaci.store.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StoreProperties {
    private Long eventPublisherId;
    private boolean syncAutoStart;

    private String cardanoHost;
    private int cardanoPort;
    private long protocolMagic;
    private String n2cNodeSocketPath;
    private String n2cHost;
    private int n2cPort;

    private String submitApiUrl;
    private String mempoolMonitoringEnabled;

    private long syncStartSlot;
    private String syncStartBlockhash;
    private String syncStopBlockhash;

    private String byronGenesisFile;
    private String shelleyGenesisFile;
}
