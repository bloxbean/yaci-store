package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test for EraService.slotFromTime() method.
 * Verifies time-to-slot conversion for both Byron and Shelley/post-Shelley eras.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EraServiceSlotFromTimeTest {

    @Mock
    private EraStorage eraStorage;

    @Mock
    private CursorStorage cursorStorage;

    @Mock
    private EpochConfig epochConfig;

    @Mock
    private GenesisConfig genesisConfig;

    @Mock
    private StoreProperties storeProperties;

    private EraService eraService;

    // Mainnet constants
    private static final long MAINNET_GENESIS_TIME = 1506203091L; // Sep 23, 2017 21:44:51 UTC
    private static final long SHELLEY_START_SLOT = 4492800L;
    private static final double BYRON_SLOT_DURATION = 20.0; // seconds
    private static final double SHELLEY_SLOT_DURATION = 1.0; // seconds

    @BeforeEach
    void setUp() {
        eraService = new EraService(eraStorage, cursorStorage, epochConfig, genesisConfig, storeProperties);

        // Mock mainnet genesis configuration
        when(genesisConfig.getStartTime(0L)).thenReturn(MAINNET_GENESIS_TIME);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(BYRON_SLOT_DURATION);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(SHELLEY_SLOT_DURATION);
        when(storeProperties.getProtocolMagic()).thenReturn(0L);

        // Mock Shelley era start
        CardanoEra shelleyEra = CardanoEra.builder()
                .era(Era.Shelley)
                .startSlot(SHELLEY_START_SLOT)
                .build();
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(shelleyEra));
    }

    @Test
    void testSlotFromTime_ShelleyEra_Test1() {
        // Oct 31, 2025 9:27:32 PM Singapore timezone (Asia/Singapore)
        long epochSeconds = ZonedDateTime.of(2025, 10, 31, 21, 27, 32, 0,
                ZoneId.of("Asia/Singapore")).toEpochSecond();

        long slot = eraService.slotFromTime(epochSeconds);

        assertEquals(170350961L, slot,
                "Oct 31, 2025 9:27:32 PM SGT should convert to slot 170350961");
    }

    @Test
    void testSlotFromTime_ShelleyEra_Test2() {
        // Nov 25, 2023 6:37:34 AM Singapore timezone (Asia/Singapore)
        long epochSeconds = ZonedDateTime.of(2023, 11, 25, 6, 37, 34, 0,
                ZoneId.of("Asia/Singapore")).toEpochSecond();

        long slot = eraService.slotFromTime(epochSeconds);

        assertEquals(109299163L, slot,
                "Nov 25, 2023 6:37:34 AM SGT should convert to slot 109299163");
    }

    @Test
    void testSlotFromTime_ByronEra_Test1() {
        // May 17, 2019 2:05:51 AM Singapore timezone (Asia/Singapore)
        long epochSeconds = ZonedDateTime.of(2019, 5, 17, 2, 5, 51, 0,
                ZoneId.of("Asia/Singapore")).toEpochSecond();

        long slot = eraService.slotFromTime(epochSeconds);

        assertEquals(2591343L, slot,
                "May 17, 2019 2:05:51 AM SGT should convert to slot 2591343");
    }

    @Test
    void testSlotFromTime_ByronEra_Test2() {
        // Sep 27, 2018 8:05:11 AM Singapore timezone (Asia/Singapore)
        long epochSeconds = ZonedDateTime.of(2018, 9, 27, 8, 5, 11, 0,
                ZoneId.of("Asia/Singapore")).toEpochSecond();

        long slot = eraService.slotFromTime(epochSeconds);

        assertEquals(1590181L, slot,
                "Sep 27, 2018 8:05:11 AM SGT should convert to slot 1590181");
    }

    @Test
    void testSlotFromTime_RoundTrip_Byron() {
        // Test that converting slot -> time -> slot gives the same result for Byron era
        long originalSlot = 2000000L;
        long blockTime = eraService.blockTime(Era.Byron, originalSlot);
        long convertedSlot = eraService.slotFromTime(blockTime);

        assertEquals(originalSlot, convertedSlot,
                "Converting Byron slot to time and back should yield the same slot");
    }

    @Test
    void testSlotFromTime_RoundTrip_Shelley() {
        // Test that converting slot -> time -> slot gives the same result for Shelley era
        long originalSlot = 100000000L;
        long blockTime = eraService.blockTime(Era.Shelley, originalSlot);
        long convertedSlot = eraService.slotFromTime(blockTime);

        assertEquals(originalSlot, convertedSlot,
                "Converting Shelley slot to time and back should yield the same slot");
    }

    @Test
    void testSlotFromTime_ShelleyNotYetKnown_UsesByronFormula() {
        // Simulate mainnet during initial Byron-era sync: no Shelley era in DB yet
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.empty());
        EraService byronOnlyService = new EraService(eraStorage, cursorStorage, epochConfig, genesisConfig, storeProperties);

        // Nov 13, 2017 00:00:00 UTC (Byron era date)
        long epochSeconds = ZonedDateTime.of(2017, 11, 13, 0, 0, 0, 0,
                ZoneId.of("UTC")).toEpochSecond();

        long slot = byronOnlyService.slotFromTime(epochSeconds);

        // Byron formula: round((epochSeconds - genesisTime) / 20)
        long expected = Math.round((epochSeconds - MAINNET_GENESIS_TIME) / BYRON_SLOT_DURATION);
        assertEquals(expected, slot,
                "When Shelley is not yet known, Byron formula (20s slots) should be used");
    }

    @Test
    void testSlotFromTime_ShelleyAtSlotZero_UsesShelleyFormula() {
        // Simulate preprod/preview where chain starts in Shelley/post-Shelley at slot 0
        long preprodGenesisTime = 1654041600L; // 2022-06-01T00:00:00Z
        when(genesisConfig.getStartTime(1L)).thenReturn(preprodGenesisTime);
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        CardanoEra shelleyAtZero = CardanoEra.builder()
                .era(Era.Babbage)
                .startSlot(0L)
                .build();
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(shelleyAtZero));

        EraService preprodService = new EraService(eraStorage, cursorStorage, epochConfig, genesisConfig, storeProperties);

        // Jan 15, 2024 00:00:00 UTC
        long epochSeconds = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0,
                ZoneId.of("UTC")).toEpochSecond();

        long slot = preprodService.slotFromTime(epochSeconds);

        // Shelley formula: 0 + round((epochSeconds - genesisTime) / 1)
        long expected = Math.round((epochSeconds - preprodGenesisTime) / SHELLEY_SLOT_DURATION);
        assertEquals(expected, slot,
                "When Shelley starts at slot 0 (preprod/preview), Shelley formula (1s slots) should be used");
    }
}
