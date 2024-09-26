package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import java.util.HashMap;
import java.util.Map;

public enum GovActionStatus {

    OPEN_BALLOT("OPEN_BALLOT"),
    RATIFIED("RATIFIED"),
    ENACTED("ENACTED"),
    EXPIRED("EXPIRED");

    private final String value;
    private static final Map<String, GovActionStatus> govActionStatusMap = new HashMap<>();

    public static GovActionStatus fromValue(String value) {
        return govActionStatusMap.get(value);
    }

    public String getValue() {
        return this.value;
    }

    GovActionStatus(String value) {
        this.value = value;
    }

    static {
        GovActionStatus[] arr = values();
        for (GovActionStatus type : arr) {
            govActionStatusMap.put(type.value, type);
        }
    }
}
