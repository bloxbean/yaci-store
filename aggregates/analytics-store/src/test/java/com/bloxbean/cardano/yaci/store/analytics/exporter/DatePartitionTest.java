package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatePartitionTest {

    @Test
    void toSlotRange_shouldIncludeGenesisSlot_whenStartOfDayIsBeforeGenesisTime() {
        // Simulate mainnet genesis day (2017-09-23). Midnight UTC is before genesis time
        // (21:44:51 UTC), so slotAtOrAfterTime returns a large negative number.
        EraService eraService = mock(EraService.class);
        when(eraService.slotAtOrAfterTime(anyLong()))
                .thenReturn(-78291L)   // start of day -> large negative
                .thenReturn(8109L);    // end of day -> positive

        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2017, 9, 23));
        SlotRange slotRange = partition.toSlotRange(eraService);

        // startSlot should be clamped to -1 (not 0) to include genesis data
        assertThat(slotRange.startSlot()).isEqualTo(-1);
        assertThat(slotRange.endSlot()).isEqualTo(8109);
    }

    @Test
    void toSlotRange_shouldIncludeGenesisSlot_whenStartOfDayIsExactlyGenesisTime() {
        // Simulate preprod genesis day (2022-06-01). Genesis time is exactly midnight UTC,
        // so slotAtOrAfterTime returns 0 for start of day.
        EraService eraService = mock(EraService.class);
        when(eraService.slotAtOrAfterTime(anyLong()))
                .thenReturn(0L)        // start of day -> exactly 0
                .thenReturn(4320L);    // end of day -> positive

        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2022, 6, 1));
        SlotRange slotRange = partition.toSlotRange(eraService);

        // startSlot should be set to -1 (not 0) to include genesis data
        assertThat(slotRange.startSlot()).isEqualTo(-1);
        assertThat(slotRange.endSlot()).isEqualTo(4320);
    }

    @Test
    void toSlotRange_shouldNotAffectNormalDays() {
        // A normal day well after genesis — both slots are positive
        EraService eraService = mock(EraService.class);
        when(eraService.slotAtOrAfterTime(anyLong()))
                .thenReturn(1000L)     // start of day
                .thenReturn(87400L);   // end of day

        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2024, 1, 15));
        SlotRange slotRange = partition.toSlotRange(eraService);

        // No clamping — both are positive
        assertThat(slotRange.startSlot()).isEqualTo(1000);
        assertThat(slotRange.endSlot()).isEqualTo(87400);
    }

    @Test
    void toSlotRange_shouldClampEndSlotToZero_whenBothSlotsAreNegative() {
        // Edge case: a date entirely before genesis (should not happen in practice,
        // but verifies clamping behavior)
        EraService eraService = mock(EraService.class);
        when(eraService.slotAtOrAfterTime(anyLong()))
                .thenReturn(-172800L)  // start of day
                .thenReturn(-86400L);  // end of day

        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2017, 9, 22));
        SlotRange slotRange = partition.toSlotRange(eraService);

        // startSlot clamped to -1, endSlot clamped to 0
        assertThat(slotRange.startSlot()).isEqualTo(-1);
        assertThat(slotRange.endSlot()).isEqualTo(0);
    }

    @Test
    void toSlotRange_shouldKeepTheByronSlotBeforeMidnightInItsOwnDay() {
        // Mainnet Byron slots are 20s from genesis 21:44:51 UTC, so a slot starts at 23:59:51.
        // Its block_time is on the earlier day, so only that day's range may select it.
        EraService eraService = mainnet();
        LocalDate day = LocalDate.of(2018, 3, 1);
        long midnight = day.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long slotBeforeMidnight = (midnight - 9 - MAINNET_GENESIS_TIME) / 20;
        assertThat(eraService.blockTime(Era.Byron, slotBeforeMidnight)).isEqualTo(midnight - 9);

        SlotRange previousDay = PartitionValue.ofDate(day.minusDays(1)).toSlotRange(eraService);
        SlotRange thisDay = PartitionValue.ofDate(day).toSlotRange(eraService);

        assertThat(previousDay.endSlot()).isEqualTo(slotBeforeMidnight + 1);
        assertThat(thisDay.startSlot()).isEqualTo(slotBeforeMidnight + 1);
        assertThat(eraService.blockTime(Era.Byron, thisDay.startSlot())).isEqualTo(midnight + 11);
        // A Byron day holds 4320 slots
        assertThat(thisDay.endSlot() - thisDay.startSlot()).isEqualTo(4320);
    }

    @Test
    void toSlotRange_shouldStartTheGenesisDayAtTheGenesisSlotWithRealArithmetic() {
        SlotRange genesisDay = PartitionValue.ofDate(LocalDate.of(2017, 9, 23)).toSlotRange(mainnet());
        SlotRange nextDay = PartitionValue.ofDate(LocalDate.of(2017, 9, 24)).toSlotRange(mainnet());

        assertThat(genesisDay.startSlot()).isEqualTo(-1);
        // 2h15m09s from genesis to midnight: slots 0..405 start before midnight
        assertThat(genesisDay.endSlot()).isEqualTo(406);
        assertThat(nextDay.startSlot()).isEqualTo(406);
    }

    private static final long MAINNET_GENESIS_TIME = 1506203091L; // 2017-09-23T21:44:51Z

    private static EraService mainnet() {
        GenesisConfig genesisConfig = mock(GenesisConfig.class);
        when(genesisConfig.getStartTime(0L)).thenReturn(MAINNET_GENESIS_TIME);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        StoreProperties storeProperties = mock(StoreProperties.class);
        when(storeProperties.getProtocolMagic()).thenReturn(0L);
        EraStorage eraStorage = mock(EraStorage.class);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800L).build()));
        return new EraService(eraStorage, mock(CursorStorage.class), mock(EpochConfig.class),
                genesisConfig, storeProperties);
    }

    @Test
    void toPathSegment_shouldReturnHiveStyleDateFormat() {
        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2024, 1, 15));

        assertThat(partition.toPathSegment()).isEqualTo("date=2024-01-15");
    }
}
