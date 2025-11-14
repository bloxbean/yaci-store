package com.bloxbean.cardano.yaci.store.analytics.state;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExportStateSimpleTest {

    @Test
    public void testExportStatusEnum() {
        assertThat(ExportStatus.PENDING).isNotNull();
        assertThat(ExportStatus.IN_PROGRESS).isNotNull();
        assertThat(ExportStatus.COMPLETED).isNotNull();
        assertThat(ExportStatus.FAILED).isNotNull();
    }

    @Test
    public void testExportStateIdCreation() {
        ExportStateId id = new ExportStateId("test_table", "2024-10-30");
        assertThat(id.getTableName()).isEqualTo("test_table");
        assertThat(id.getPartitionValue()).isEqualTo("2024-10-30");
    }

    @Test
    public void testExportStateConvenienceConstructor() {
        ExportState state = new ExportState("test_table", "2024-10-30");
        assertThat(state.getId().getTableName()).isEqualTo("test_table");
        assertThat(state.getId().getPartitionValue()).isEqualTo("2024-10-30");
        assertThat(state.getStatus()).isEqualTo(ExportStatus.PENDING);
        assertThat(state.getRetryCount()).isEqualTo(0);
    }
}
