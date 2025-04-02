package com.bloxbean.cardano.yaci.store.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public class BigNumberUtils {

    public final static MathContext mathContext = new MathContext(35);

    public static BigInteger add(BigInteger a, BigInteger b) {
        return a.add(b);
    }

    public static BigInteger multiplyAndFloor(BigInteger a, BigDecimal b, BigDecimal c) {
        return new BigDecimal(a).multiply(b).multiply(c).round(new MathContext(0, RoundingMode.FLOOR)).toBigInteger();
    }

    public static BigInteger multiplyAndFloor(BigInteger a, BigDecimal b) {
        return new BigDecimal(a).multiply(b)
                .round(new MathContext(0, RoundingMode.FLOOR)).toBigInteger();
    }

    public static BigInteger multiply(BigInteger a, BigInteger b, BigInteger c) {
        return a.multiply(b).multiply(c);
    }

    public static BigDecimal multiply(BigInteger a, BigDecimal b, BigDecimal c) {
        return new BigDecimal(a).multiply(b).multiply(c);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b);
    }

    public static BigDecimal add(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).add(b);
    }

    public static BigDecimal add(BigDecimal a, double b) {
        return a.add(BigDecimal.valueOf(b));
    }

    public static BigDecimal add(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b));
    }

    public static BigDecimal add(long a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b));
    }

    public static BigDecimal add(double a, long b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b);
    }

    public static BigInteger subtract(BigInteger a, BigInteger b) {
        return a.subtract(b);
    }

    public static BigDecimal subtract(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).subtract(b);
    }

    public static BigDecimal subtract(BigDecimal a, double b) {
        return a.subtract(BigDecimal.valueOf(b));
    }

    public static BigDecimal subtract(BigInteger a, double b) {
        return new BigDecimal(a).subtract(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b);
    }

    public static BigDecimal multiply(BigInteger a, BigDecimal b) {
        return new BigDecimal(a).multiply(b);
    }

    public static BigDecimal multiply(BigInteger a, double b) {
        return new BigDecimal(a).multiply(new BigDecimal(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b, BigDecimal c) {
        return a.multiply(b).multiply(c);
    }

    public static BigDecimal multiply(BigDecimal a, double b, BigDecimal c) {
        return a.multiply(BigDecimal.valueOf(b)).multiply(c);
    }

    public static BigDecimal multiply(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).multiply(b);
    }

    public static BigDecimal multiply(BigDecimal a, double b) {
        return a.multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(double a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b));
    }

    public static BigInteger multiply(int a, BigInteger b) {
        return BigInteger.valueOf(a).multiply(b);
    }

    public static BigDecimal multiply(double a, BigInteger b) {
        return BigDecimal.valueOf(a).multiply(new BigDecimal(b));
    }

    public static BigDecimal multiply(long a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(double a, long b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, mathContext);
    }

    public static BigDecimal divide(BigInteger a, BigDecimal b) {
        return new BigDecimal(a).divide(b, mathContext);
    }

    public static BigDecimal divide(double a, BigInteger b) {
        return BigDecimal.valueOf(a).divide(new BigDecimal(b), mathContext);
    }

    public static BigDecimal divide(BigInteger a, BigInteger b) {
        return new BigDecimal(a).divide(new BigDecimal(b), mathContext);
    }

    public static BigDecimal divide(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).divide(b, mathContext);
    }

    public static BigDecimal divide(BigDecimal a, double b) {
        return a.divide(BigDecimal.valueOf(b), mathContext);
    }

    public static BigDecimal divide(double a, double b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), mathContext);
    }

    public static BigDecimal divide(long a, double b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), mathContext);
    }

    public static BigDecimal divide(long a, long b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), mathContext);
    }

    public static BigDecimal divide(double a, long b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), mathContext);
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.min(b);
    }

    public static BigInteger floor(BigDecimal a) {
        return a.round(new MathContext(0, RoundingMode.FLOOR)).toBigInteger();
    }

    public static boolean isLowerOrEquals(BigInteger a, BigInteger b) {
        return a.compareTo(b) <= 0;
    }

    public static boolean isLower(BigInteger a, BigInteger b) {
        return a.compareTo(b) < 0;
    }

    public static boolean isLower(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) < 0;
    }

    public static boolean isHigher(BigInteger a, BigInteger b) {
        return a.compareTo(b) > 0;
    }

    public static boolean isHigher(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) > 0;
    }

    public static boolean isHigherOrEquals(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0;
    }

    public static boolean isZero(BigDecimal a) {
        return a.compareTo(BigDecimal.ZERO) == 0;
    }
}
