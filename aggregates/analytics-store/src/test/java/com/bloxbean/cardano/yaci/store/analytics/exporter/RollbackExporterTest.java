package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.writer.StorageWriter;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RollbackExporterTest {

    private static final long MAINNET_GENESIS_TIME = 1506203091L; // 2017-09-23T21:44:51Z
    private static final long MAINNET_SHELLEY_SLOT = 4492800L;

    @Test
    void doesNotJoinTheBlockTheRollbackRemoved() {
        EraService eraService = mainnet(true);
        RollbackExporter exporter = exporter(eraService);
        PartitionValue day = PartitionValue.ofDate(LocalDate.of(2024, 3, 1));

        String query = exporter.buildQuery(day, day.toSlotRange(eraService));

        assertThat(query).doesNotContainIgnoringCase("join");
        assertThat(query).contains("r.current_slot >= " + day.toSlotRange(eraService).startSlot());
    }

    @Test
    void slotTimeMatchesTheEraArithmeticInBothEras() throws Exception {
        EraService eraService = mainnet(true);
        String expression = RollbackExporter.slotTimeSql("s", eraService);

        long[] slots = {0, 1, 406, MAINNET_SHELLEY_SLOT - 1, MAINNET_SHELLEY_SLOT, MAINNET_SHELLEY_SLOT + 1,
                116_000_000L};
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT CAST(" + expression + " AS BIGINT) FROM (SELECT CAST(? AS BIGINT) AS s)")) {
            for (long slot : slots) {
                stmt.setLong(1, slot);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    Era era = slot < MAINNET_SHELLEY_SLOT ? Era.Byron : Era.Shelley;
                    assertThat(rs.getLong(1)).as("slot %d", slot).isEqualTo(eraService.blockTime(era, slot));
                }
            }
        }
    }

    @Test
    void eachRollbackGetsATimeOnTheDayItIsFiledUnder() throws Exception {
        EraService eraService = mainnet(true);
        String expression = RollbackExporter.slotTimeSql("s", eraService);
        // A Byron day and a Shelley-era day: first and last slot of each
        for (LocalDate date : new LocalDate[]{LocalDate.of(2018, 1, 10), LocalDate.of(2024, 3, 1)}) {
            SlotRange range = PartitionValue.ofDate(date).toSlotRange(eraService);
            for (long slot : new long[]{range.startSlot(), range.endSlot() - 1}) {
                try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT CAST(" + expression + " AS BIGINT) FROM (SELECT CAST(? AS BIGINT) AS s)")) {
                    stmt.setLong(1, slot);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        LocalDate day = Instant.ofEpochSecond(rs.getLong(1)).atZone(ZoneOffset.UTC).toLocalDate();
                        assertThat(day).as("slot %d", slot).isEqualTo(date);
                    }
                }
            }
        }
    }

    @Test
    void beforeTheFirstNonByronEraIsKnownEverySlotIsByron() {
        String expression = RollbackExporter.slotTimeSql("s", mainnet(false));

        assertThat(expression).doesNotContain("CASE").contains(String.valueOf(MAINNET_GENESIS_TIME)).contains("* 20");
    }

    private static RollbackExporter exporter(EraService eraService) {
        StorageWriter writer = mock(StorageWriter.class);
        when(writer.getSourceSchema()).thenReturn("cardano");
        return new RollbackExporter(writer, mock(ExportStateService.class), eraService,
                new AnalyticsStoreProperties(), mock(AdaPotJobStorage.class));
    }

    private static EraService mainnet(boolean shelleyKnown) {
        GenesisConfig genesisConfig = mock(GenesisConfig.class);
        when(genesisConfig.getStartTime(0L)).thenReturn(MAINNET_GENESIS_TIME);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        StoreProperties storeProperties = mock(StoreProperties.class);
        when(storeProperties.getProtocolMagic()).thenReturn(0L);
        EraStorage eraStorage = mock(EraStorage.class);
        when(eraStorage.findFirstNonByronEra()).thenReturn(shelleyKnown
                ? Optional.of(CardanoEra.builder().era(Era.Shelley).startSlot(MAINNET_SHELLEY_SLOT).build())
                : Optional.empty());
        return new EraService(eraStorage, mock(CursorStorage.class), mock(EpochConfig.class),
                genesisConfig, storeProperties);
    }
}
