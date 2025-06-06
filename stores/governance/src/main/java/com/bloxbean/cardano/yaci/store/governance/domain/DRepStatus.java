package com.bloxbean.cardano.yaci.store.governance.domain;

import java.util.HashMap;
import java.util.Map;

public enum DRepStatus {
    REGISTERED("registered"),
    UPDATED("updated"),
    RETIRED("retired");

    private final String value;
    private static final Map<String, DRepStatus> dRepStatusMap = new HashMap<>();

    public static DRepStatus fromValue(String value) {
        return dRepStatusMap.get(value);
    }

    public String getValue() {
        return this.value;
    }

    DRepStatus(String value) {
        this.value = value;
    }

    static {
        DRepStatus[] arr = values();
        for (DRepStatus type : arr) {
            dRepStatusMap.put(type.value, type);
        }
    }
}
