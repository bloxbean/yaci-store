package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UnitIntervalUtil {

    // Same rule as yaci 0.3.x (exact, else scale 10 HALF_UP) so stored values don't change.
    public static BigDecimal toBigDecimal(UnitInterval value) {
        if (value == null)
            return null;

        BigDecimal numerator = new BigDecimal(value.getNumerator());
        BigDecimal denominator = new BigDecimal(value.getDenominator());
        try {
            return numerator.divide(denominator);
        } catch (ArithmeticException e) {
            return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
        }
    }
}
