package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StakeRegistrationExporterTest {
    @Test
    void preservesExplicitDepositsAndNullRefunds() throws Exception {
        var writer = mock(StorageWriter.class);
        when(writer.getSourceSchema()).thenReturn("source");
        var exporter = new StakeRegistrationExporter(writer, null, null, new AnalyticsStoreProperties(), null);
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("CREATE SCHEMA source");
            st.execute("CREATE TABLE source.stake_registration(tx_hash VARCHAR, cert_index INTEGER, "
                    + "tx_index INTEGER, credential VARCHAR, cred_type VARCHAR, type VARCHAR, address VARCHAR, "
                    + "deposit BIGINT, epoch INTEGER, slot BIGINT, block_hash VARCHAR, block BIGINT, block_time BIGINT)");
            st.execute("INSERT INTO source.stake_registration VALUES "
                    + "('a',0,0,'c','KEY','STAKE_REGISTRATION','s',3000000,1,10,'h',1,1700000000),"
                    + "('b',0,0,'c','KEY','STAKE_DEREGISTRATION','s',NULL,1,20,'i',2,1700000001)");
            st.execute("CREATE MACRO postgres_query(db, sql) AS TABLE SELECT * FROM query(sql)");
            String sql = exporter.buildQuery(PartitionValue.ofDate(LocalDate.of(2023, 11, 14)),
                    new SlotRange(10, 30));
            try (var rs = st.executeQuery(sql)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong("deposit")).isEqualTo(3000000);
                assertThat(rs.next()).isTrue();
                assertThat(rs.getObject("deposit")).isNull();
                assertThat(rs.next()).isFalse();
            }
        }
    }
}
