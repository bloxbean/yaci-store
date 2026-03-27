package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Constants;

public enum SyncStatusEnum {

    SYNC_NOT_STARTED(Constants.SYNC_NOT_STARTED),
    SYNC_IN_PROGRESS(Constants.SYNC_IN_PROGRESS),
    SYNC_DONE(Constants.SYNC_DONE),
    SYNC_ERROR(Constants.SYNC_ERROR),
    SYNC_IN_EXTRA_JOB(Constants.SYNC_IN_EXTRA_JOB);

    private final String text;

    SyncStatusEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
