package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums.SyncStatusEnum;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SyncStatus {

    private boolean isInitialSyncDone;
    private SyncStatusEnum status;

}
