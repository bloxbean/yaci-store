package com.bloxbean.cardano.yaci.store.common.util;

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
}
