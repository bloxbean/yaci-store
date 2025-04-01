package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

class UnitIntervalUtilTest {

    @Test
    void testSafeRatio_ValidInput() {
        BigInteger numerator = BigInteger.valueOf(10);
        BigInteger denominator = BigInteger.valueOf(2);

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.valueOf(5), result, "The ratio should be 10 / 2 = 5");
    }

    @Test
    void testSafeRatio_NullNumerator() {
        BigInteger numerator = null;
        BigInteger denominator = BigInteger.valueOf(2);

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.ZERO, result, "The ratio should be 0 when numerator is null");
    }

    @Test
    void testSafeRatio_NullDenominator() {
        BigInteger numerator = BigInteger.valueOf(10);
        BigInteger denominator = null;

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.ZERO, result, "The ratio should be 0 when denominator is null");
    }

    @Test
    void testSafeRatio_ZeroDenominator() {
        BigInteger numerator = BigInteger.valueOf(10);
        BigInteger denominator = BigInteger.ZERO;

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.ZERO, result, "The ratio should be 0 when denominator is 0");
    }

    @Test
    void testSafeRatio_NegativeValues() {
        BigInteger numerator = BigInteger.valueOf(-10);
        BigInteger denominator = BigInteger.valueOf(2);

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.valueOf(-5), result, "The ratio should be -10 / 2 = -5");
    }

    @Test
    void testSafeRatio_BothNegativeValues() {
        BigInteger numerator = BigInteger.valueOf(-10);
        BigInteger denominator = BigInteger.valueOf(-2);

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.valueOf(5), result, "The ratio should be -10 / -2 = 5");
    }

    @Test
    void testSafeRatio_ZeroNumerator() {
        BigInteger numerator = BigInteger.ZERO;
        BigInteger denominator = BigInteger.valueOf(10);

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator);

        assertEquals(BigDecimal.ZERO, result, "The ratio should be 0 when numerator is 0");
    }

    @Test
    void testSafeRatio_LargeValues() {
        BigInteger numerator = new BigInteger("12345678901234567890");
        BigInteger denominator = new BigInteger("9876543210987654321");

        BigDecimal result = UnitIntervalUtil.safeRatio(numerator, denominator)
                .round(new MathContext(3, RoundingMode.UP));

        BigDecimal expected = new BigDecimal("1.25");
        assertEquals(0, result.compareTo(expected), "The ratio of large values should be correct");
    }

    @Test
    void testMarginToNumeratorDenominator_PositiveNumber() {
        BigDecimal margin = new BigDecimal("12.345");

        Tuple<BigInteger, BigInteger> result = UnitIntervalUtil.marginToNumeratorDenominator(margin);

        assertEquals(new BigInteger("12345"), result._1, "Numerator should be 12345");
        assertEquals(new BigInteger("1000"), result._2, "Denominator should be 1000");
    }

    @Test
    void testMarginToNumeratorDenominator_NegativeNumber() {
        BigDecimal margin = new BigDecimal("-7.89");

        Tuple<BigInteger, BigInteger> result = UnitIntervalUtil.marginToNumeratorDenominator(margin);

        assertEquals(new BigInteger("-789"), result._1, "Numerator should be -789");
        assertEquals(new BigInteger("100"), result._2, "Denominator should be 100");
    }

    @Test
    void testMarginToNumeratorDenominator_Zero() {
        BigDecimal margin = BigDecimal.ZERO;

        Tuple<BigInteger, BigInteger> result = UnitIntervalUtil.marginToNumeratorDenominator(margin);

        assertEquals(BigInteger.ZERO, result._1, "Numerator should be 0");
        assertEquals(BigInteger.ONE, result._2, "Denominator should be 1");
    }

    @Test
    void testMarginToNumeratorDenominator_HighPrecision() {
        BigDecimal margin = new BigDecimal("0.000123456");

        Tuple<BigInteger, BigInteger> result = UnitIntervalUtil.marginToNumeratorDenominator(margin);

        assertEquals(new BigInteger("123456"), result._1, "Numerator should be 123456");
        assertEquals(new BigInteger("1000000000"), result._2, "Denominator should be 1000000000");
    }

    @Test
    void testMarginToNumeratorDenominator_LargeNumber() {
        BigDecimal margin = new BigDecimal("9876543210.123456789");

        Tuple<BigInteger, BigInteger> result = UnitIntervalUtil.marginToNumeratorDenominator(margin);

        assertEquals(new BigInteger("9876543210123456789"), result._1, "Numerator should be 9876543210123456789");
        assertEquals(new BigInteger("1000000000"), result._2, "Denominator should be 1000000000");
    }

    @Test
    void testDecimalToUnitInterval_Zero() {
        BigDecimal decimal = BigDecimal.ZERO;

        UnitInterval result = UnitIntervalUtil.decimalToUnitInterval(decimal);

        assertEquals(BigInteger.ZERO, result.getNumerator(), "Numerator should be 0");
        assertEquals(BigInteger.ONE, result.getDenominator(), "Denominator should be 1");
    }

    @Test
    void testDecimalToUnitInterval_LargeNumber() {
        BigDecimal margin = new BigDecimal("9876543210.123456789");

        UnitInterval result = UnitIntervalUtil.decimalToUnitInterval(margin);

        assertEquals(new BigInteger("9876543210123456789"), result.getNumerator(), "Numerator should be 9876543210123456789");
        assertEquals(new BigInteger("1000000000"), result.getDenominator(), "Denominator should be 1000000000");
    }

    @Test
    void testDecimalToUnitInterval_HighPrecision() {
        BigDecimal margin = new BigDecimal("0.000123456");

        UnitInterval result = UnitIntervalUtil.decimalToUnitInterval(margin);

        assertEquals(new BigInteger("123456"), result.getNumerator(), "Numerator should be 123456");
        assertEquals(new BigInteger("1000000000"), result.getDenominator(), "Denominator should be 1000000000");
    }
}
