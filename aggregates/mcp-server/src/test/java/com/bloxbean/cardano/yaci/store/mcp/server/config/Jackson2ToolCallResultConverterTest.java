package com.bloxbean.cardano.yaci.store.mcp.server.config;

import com.bloxbean.cardano.yaci.store.mcp.server.model.TimestampSlotInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Jackson2ToolCallResultConverterTest {
    private final Jackson2ToolCallResultConverter converter = new Jackson2ToolCallResultConverter();

    @Test
    void keepsSnakeCaseAndDeclarationOrderAsInSpringAi1() {
        TimestampSlotInfo info = new TimestampSlotInfo(1L, 2L, 3L, 4L, "SHELLEY", "ok", "note");

        String json = converter.convert(info, TimestampSlotInfo.class);

        assertThat(json).isEqualTo("{\"original_timestamp\":1,\"timestamp_seconds\":2,\"timestamp_millis\":3,"
                + "\"slot\":4,\"era\":\"SHELLEY\",\"validation_note\":\"ok\",\"conversion_note\":\"note\"}");
    }

    @Test
    void voidResultIsDone() {
        assertThat(converter.convert(null, Void.TYPE)).isEqualTo("\"Done\"");
    }
}
