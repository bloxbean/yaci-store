package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * <p>
 * Utility class to handle UnitInterval
 * </p>
 *
 * CDDL specification for unit_interval :
 *
 * <p>The unit_interval is defined as:
 * <pre>
 *   unit_interval = #6.30([1, 2])
 *   ; unit_interval = #6.30([uint, uint])
 * </pre>
 *
 * <p>Comment above depicts the actual definition for `unit_interval`.</p>
 *
 * <p>A unit interval is a number in the range between 0 and 1, which
 * imposes two extra constraints:</p>
 * <ul>
 *   <li>Numerator ≤ Denominator</li>
 *   <li>Denominator > 0</li>
 * </ul>
 *
 * <p>Note: The relation between numerator and denominator cannot be expressed
 * in CDDL. This poses a problem for testing because we need to generate random valid
 * data for testing the implementation of our encoders/decoders. Therefore,
 * the actual definition is not used here, and the value is hardcoded to 1/2.</p>
 */
public class UnitIntervalUtil {
    private static final Logger log = LoggerFactory.getLogger(UnitIntervalUtil.class);
    private static MathContext mathContext = new MathContext(35);

    /**
     * Safely calculates the ratio of a given numerator and denominator as a BigDecimal.
     * If the numerator or denominator is null, or if the denominator is zero, it returns BigDecimal.ZERO.
     *
     * @param numerator the numerator of the ratio represented as a BigInteger
     * @param denominator the denominator of the ratio represented as a BigInteger
     * @return the calculated ratio as a BigDecimal, or BigDecimal.ZERO if input is null or the denominator is zero
     */
    public static BigDecimal safeRatio(BigInteger numerator, BigInteger denominator) {
        if (isInvalidUnitInterval(numerator, denominator)) {
            return BigDecimal.ZERO;
        }

        var numeratorBD = new BigDecimal(numerator);
        var denominatorBD = new BigDecimal(denominator);

        return numeratorBD.divide(denominatorBD, mathContext);
    }

    private static boolean isInvalidUnitInterval(BigInteger numerator, BigInteger denominator) {
        boolean denominatorIsZero = denominator != null && denominator.equals(BigInteger.ZERO);
        return numerator == null || denominator == null || denominatorIsZero;

        //Ideally, we should also check numerator <= denominator and throw exception
        //But, the caller is expected to pass the correct values to the safeRatio method
    }

    /**
     * Safely calculates the ratio of a UnitInterval object's numerator and denominator as a BigDecimal.
     * If the UnitInterval is null or contains invalid values (e.g., null numerator or denominator, or denominator is zero),
     * it returns a specific fallback value: null or BigDecimal.ZERO based on the condition.
     *
     * @param unitInterval the UnitInterval instance containing numerator and denominator values to calculate the ratio.
     *                      It can be null.
     * @return the calculated ratio as a BigDecimal, null if the input UnitInterval is null, or BigDecimal.ZERO if the
     *         UnitInterval has invalid values.
     */
    public static BigDecimal safeRatio(UnitInterval unitInterval) {
        if (unitInterval == null) {
            return null;
        }

        if (isInvalidUnitInterval(unitInterval.getNumerator(), unitInterval.getDenominator())) {
            return BigDecimal.ZERO;
        }

        var numeratorBD = new BigDecimal(unitInterval.getNumerator());
        var denominatorBD = new BigDecimal(unitInterval.getDenominator());

        return numeratorBD.divide(denominatorBD, mathContext);
    }


    /**
     * Converts a string representation of a rational number into a tuple containing the numerator and denominator.
     * The input string is expected to have the format "numerator/denominator".
     * If the input string is null or invalid, the method returns a tuple with both values set to BigInteger.ZERO.
     *
     * @param numberStr the string representation of a rational number, expected in the format "numerator/denominator"
     * @return a Tuple containing the numerator and denominator as BigInteger instances.
     *         Returns (BigInteger.ZERO, BigInteger.ZERO) if the input string is null or invalid.
     */
    public static Tuple<BigInteger, BigInteger> stringToNumeratorDenominator(String numberStr) {
        if (numberStr == null)
            return new Tuple<>(BigInteger.ZERO, BigInteger.ZERO);

        String[] tokens = numberStr.split("/");

        if (tokens.length == 2) {
            var numerator = new BigInteger(tokens[0]);
            var denominator = new BigInteger(tokens[1]);

            return new Tuple<>(numerator, denominator);
        } else {
            return new Tuple<>(BigInteger.ZERO, BigInteger.ZERO);
        }
    }

    /**
     * Converts a given BigDecimal margin into its numerator and denominator representation as integers.
     * The method determines the scale of the BigDecimal and calculates the numerator and denominator
     * necessary to represent the margin as a rational number.
     *
     * @param margin the BigDecimal value representing the margin to be converted. It must be a non-null value.
     * @return a Tuple containing the numerator and denominator as BigInteger instances. The first element (_1)
     *         is the numerator, and the second element (_2) is the denominator.
     */
    public static Tuple<BigInteger, BigInteger> marginToNumeratorDenominator(BigDecimal margin) {
        BigInteger scaleFactor = BigInteger.TEN.pow(margin.scale());
        BigInteger numerator = margin.unscaledValue();
        BigInteger denominator = scaleFactor;

        return new Tuple<>(numerator, denominator);
    }

    /**
     * Converts a given BigDecimal to a UnitInterval object, representing the decimal value as a ratio of numerator
     * and denominator. If the input decimal is null, the method returns null.
     *
     * @param decimal the BigDecimal value to be converted to a UnitInterval. It can be a null value.
     * @return a UnitInterval instance containing the numerator and denominator representation of the input decimal,
     *         or null if the input decimal is null.
     */
    public static UnitInterval decimalToUnitInterval(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }

        BigInteger scaleFactor = BigInteger.TEN.pow(decimal.scale());
        BigInteger numerator = decimal.unscaledValue();
        BigInteger denominator = scaleFactor;

        return new UnitInterval(numerator, denominator);
    }

    /**
     * Converts the given BigDecimal to a NonNegativeInterval object by representing the decimal value as
     * a ratio of a numerator and denominator. If the input decimal is null, the method returns null.
     *
     * @param decimal the BigDecimal value to be converted to a NonNegativeInterval. It may be null.
     * @return a NonNegativeInterval instance containing the numerator and denominator representation
     *         of the input decimal, or null if the input decimal is null.
     */
    public static NonNegativeInterval decimalToNonNegativeInterval(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }

        //check decimal is not negative
        if (decimal.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Decimal value cannot be negative");
        }


        BigInteger scaleFactor = BigInteger.TEN.pow(decimal.scale());
        BigInteger numerator = decimal.unscaledValue();
        BigInteger denominator = scaleFactor;

        return new NonNegativeInterval(numerator, denominator);
    }
}
