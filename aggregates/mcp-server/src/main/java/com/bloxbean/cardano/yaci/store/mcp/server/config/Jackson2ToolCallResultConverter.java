package com.bloxbean.cardano.yaci.store.mcp.server.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;

import java.lang.reflect.Type;

/**
 * Tool results as JSON with Jackson 2, as Spring AI 1.x wrote them. Spring AI 2 serializes with Jackson 3, which
 * ignores the Jackson 2 annotations on the result models (for example {@code @JsonNaming(SnakeCaseStrategy)} on
 * {@code TimestampSlotInfo}) and sorts the properties, so the tool output seen by MCP clients would change.
 * Images and {@code void} results are left to Spring AI's {@link DefaultToolCallResultConverter}.
 */
public class Jackson2ToolCallResultConverter implements ToolCallResultConverter {
    //Same settings as Spring AI 1.x JsonParser
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .findAndAddModules()
            .build();

    private final DefaultToolCallResultConverter defaultConverter = new DefaultToolCallResultConverter();

    @Override
    public String convert(Object result, Type returnType) {
        if (returnType == Void.TYPE || result instanceof java.awt.image.RenderedImage)
            return defaultConverter.convert(result, returnType);

        //As Spring AI 1.x: a String that already is JSON is returned as is
        if (result instanceof String str && isValidJson(str))
            return str;

        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Conversion from Object to JSON failed", e);
        }
    }

    private static boolean isValidJson(String input) {
        try {
            OBJECT_MAPPER.readTree(input);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
