package com.bloxbean.cardano.yaci.store.common.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtil {
    protected static final ObjectMapper mapper;

    public JsonUtil() {
    }

    public static String getPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        } else {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            } catch (JsonProcessingException var2) {
                log.error("Json parsing error", var2);
                return obj.toString();
            }
        }
    }

    public static String getPrettyJson(String jsonStr) {
        if (jsonStr == null) {
            return null;
        } else {
            try {
                Object json = mapper.readValue(jsonStr, Object.class);
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } catch (Exception var2) {
                return jsonStr;
            }
        }
    }

    public static String getJson(Object obj) {
        if (obj == null) {
            return null;
        } else {
            try {
                return mapper.writeValueAsString(obj);
            } catch (JsonProcessingException var2) {
                log.error("Json parsing error", var2);
                return obj.toString();
            }
        }
    }

    public static JsonNode parseJson(String jsonContent) throws JsonProcessingException {
        return mapper.readTree(jsonContent);
    }

    public static ObjectMapper getMapper() {
        return mapper;
    }

    static {
        mapper = (new ObjectMapper()).enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY) // Make all fields visible
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }
}

