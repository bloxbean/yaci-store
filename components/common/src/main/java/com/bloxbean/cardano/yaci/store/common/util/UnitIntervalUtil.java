package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UnitIntervalUtil {

    /**
     * Converts a rational value to BigDecimal using the same rule yaci 0.3.x applied while parsing:
     * exact division, or scale 10 with HALF_UP when the decimal expansion does not terminate.
     * Keeping this rule leaves stored protocol param values unchanged across yaci versions.
     */
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
