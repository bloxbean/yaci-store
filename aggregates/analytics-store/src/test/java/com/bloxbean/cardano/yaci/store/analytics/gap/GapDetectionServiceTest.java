package com.bloxbean.cardano.yaci.store.analytics.gap;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Export end date = UTC date of (latest block time − finality window − margin) − buffer-days.
 */
class GapDetectionServiceTest {

    private GenesisConfig genesisConfig;
    private BlockStorageReader blockStorageReader;
    private AnalyticsStoreProperties properties;
    private GapDetectionService service;

    @BeforeEach
    void setUp() {
        genesisConfig = mock(GenesisConfig.class);
        blockStorageReader = mock(BlockStorageReader.class);
        properties = new AnalyticsStoreProperties();
        service = new GapDetectionService(genesisConfig, blockStorageReader,
                mock(ExportStateService.class), properties, mock(EraService.class));
        mainnetGenesis();
    }

    private void mainnetGenesis() {
        when(genesisConfig.getSecurityParam()).thenReturn(2160);
        when(genesisConfig.getActiveSlotsCoeff()).thenReturn(0.05);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
    }

    private void tipAt(String isoInstant) {
        Block block = Block.builder().blockTime(Instant.parse(isoInstant).getEpochSecond()).build();
        when(blockStorageReader.findRecentBlock()).thenReturn(Optional.of(block));
    }

    @Test
    void finalityWindowIsDerivedFromGenesisPlusMargin() {
        assertEquals(Duration.ofHours(13), service.getFinalityWindow());          // 2160 × 20 s = 12 h + 1 h

        properties.getContinuousSync().setFinalityMarginHours(3);
        assertEquals(Duration.ofHours(15), service.getFinalityWindow());

        when(genesisConfig.getSecurityParam()).thenReturn(432);                    // preview
        properties.getContinuousSync().setFinalityMarginHours(1);
        assertEquals(Duration.ofSeconds(432 * 20 + 3600), service.getFinalityWindow());
    }

    @Test
    void overrideReplacesTheDerivedWindowAndMissingGenesisFallsBackToMarginOnly() {
        properties.getContinuousSync().setFinalityWindowHours(6);
        assertEquals(Duration.ofHours(7), service.getFinalityWindow());

        properties.getContinuousSync().setFinalityWindowHours(0);
        when(genesisConfig.getSecurityParam()).thenReturn(0);
        assertEquals(Duration.ofHours(1), service.getFinalityWindow());
    }

    @Test
    void aDayIsExportableOnlyOnceTheFinalizedTipHasLeftIt() {
        // buffer-days=1, window 13 h: day D-1 becomes eligible at 13:00 UTC of day D
        tipAt("2026-08-19T12:59:00Z");
        assertEquals(LocalDate.parse("2026-08-17"), service.getExportEndDate());

        tipAt("2026-08-19T13:00:00Z");
        assertEquals(LocalDate.parse("2026-08-18"), service.getExportEndDate());

        // just after midnight the previous day is NOT yet exportable (would be with the old rule)
        tipAt("2026-08-20T00:05:00Z");
        assertEquals(LocalDate.parse("2026-08-18"), service.getExportEndDate());

        // buffer-days=2 adds a further day
        properties.getContinuousSync().setBufferDays(2);
        tipAt("2026-08-19T13:00:00Z");
        assertEquals(LocalDate.parse("2026-08-17"), service.getExportEndDate());
    }

    @Test
    void negativeMarginIsTreatedAsZeroAndNeverMovesTheFinalizedTipForward() {
        properties.getContinuousSync().setFinalityMarginHours(-13);
        service.validateAndLogFinalitySettings();
        assertEquals(0, properties.getContinuousSync().getFinalityMarginHours());
        assertEquals(Duration.ofHours(12), service.getFinalityWindow());
        // 23:00 UTC of D: with margin 0 the finalized tip is 11:00 of D -> end date D-1, never D
        tipAt("2026-08-19T23:00:00Z");
        assertEquals(LocalDate.parse("2026-08-18"), service.getExportEndDate());

        // even without the startup clamp, getFinalityWindow() floors the margin at 0
        properties.getContinuousSync().setFinalityMarginHours(-5);
        assertEquals(Duration.ofHours(12), service.getFinalityWindow());
    }

    @Test
    void bufferDaysBelowOneIsRaisedToOne() {
        properties.getContinuousSync().setBufferDays(0);
        service.validateAndLogFinalitySettings();
        assertEquals(1, properties.getContinuousSync().getBufferDays());
        tipAt("2026-08-19T13:00:00Z");
        assertEquals(LocalDate.parse("2026-08-18"), service.getExportEndDate());
    }

    @Test
    void noBlocksYetFallsBackToGenesisMinusBuffer() {
        when(blockStorageReader.findRecentBlock()).thenReturn(Optional.empty());
        when(genesisConfig.getStartTime()).thenReturn(Instant.parse("2022-06-01T00:00:00Z").getEpochSecond());
        assertEquals(LocalDate.parse("2022-05-31"), service.getExportEndDate());
    }
}
