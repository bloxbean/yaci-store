package com.bloxbean.cardano.yaci.store.common.util;

public class ControllerPageUtil {

    /// Adjust page number for API requests. Convert 1-based page number to 0-based.
    public static int adjustPage(int page) {
        int p = page;
        if (p > 0)
            p = p - 1;
        return p;
    }
}
