package com.bloxbean.cardano.yaci.store.core.util;

public class StringUtil {

    public static String nonNull(String input) {
        if (input == null)
            return "";
        else
            return input;
    }
}
