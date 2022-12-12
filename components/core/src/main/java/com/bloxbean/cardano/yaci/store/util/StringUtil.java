package com.bloxbean.cardano.yaci.store.util;

public class StringUtil {

    public static String nonNull(String input) {
        if (input == null)
            return "";
        else
            return input;
    }
}
