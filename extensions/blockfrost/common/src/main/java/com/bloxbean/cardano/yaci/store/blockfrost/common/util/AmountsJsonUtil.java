package com.bloxbean.cardano.yaci.store.blockfrost.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public final class AmountsJsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AmountsJsonUtil() {
    }

    public static Map<String, BigInteger> toQuantityByUnit(String amountsJson) {
        Map<String, BigInteger> quantitiesByUnit = new HashMap<>();
        if (amountsJson == null || amountsJson.isBlank()) {
            return quantitiesByUnit;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(amountsJson);
            if (!root.isArray()) {
                return quantitiesByUnit;
            }

            for (JsonNode amountNode : root) {
                String unit = amountNode.path("unit").asText(null);
                if (unit == null || unit.isBlank()) {
                    continue;
                }

                BigInteger quantity = parseQuantity(amountNode.path("quantity"));
                if (quantity != null) {
                    quantitiesByUnit.merge(unit, quantity, BigInteger::add);
                }
            }
        } catch (Exception e) {
            return quantitiesByUnit;
        }

        return quantitiesByUnit;
    }

    public static BigInteger findQuantity(String amountsJson, String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        return toQuantityByUnit(amountsJson).get(unit);
    }

    private static BigInteger parseQuantity(JsonNode quantityNode) {
        if (quantityNode == null || quantityNode.isMissingNode() || quantityNode.isNull()) {
            return null;
        }

        try {
            if (quantityNode.isNumber()) {
                return quantityNode.decimalValue().toBigInteger();
            }

            String value = quantityNode.asText();
            if (value == null || value.isBlank()) {
                return null;
            }

            return new BigDecimal(value).toBigInteger();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
