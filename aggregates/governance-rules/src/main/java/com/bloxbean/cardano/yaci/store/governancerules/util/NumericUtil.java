package com.bloxbean.cardano.yaci.store.governancerules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NumericUtil {
    private NumericUtil() {
    }

    public static double toDouble(BigDecimal value) {
        if (value == null) {
            return 0;
        }

        return value.doubleValue();
    }

    public static double toDouble(BigInteger value) {
        if (value == null) {
            return 0;
        }

        return value.doubleValue();
    }
}
