package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatePartitionTest {

    @Test
    void toSlotRange_shouldIncludeGenesisSlot_whenStartOfDayIsBeforeGenesisTime() {
        // Simulate mainnet genesis day (2017-09-23). Midnight UTC is before genesis time
        // (21:44:51 UTC), so slotFromTime returns a large negative number.
        EraService eraService = mock(EraService.class);
        when(eraService.slotFromTime(anyLong()))
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
        // so slotFromTime returns 0 for start of day.
        EraService eraService = mock(EraService.class);
        when(eraService.slotFromTime(anyLong()))
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
        when(eraService.slotFromTime(anyLong()))
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
        when(eraService.slotFromTime(anyLong()))
                .thenReturn(-172800L)  // start of day
                .thenReturn(-86400L);  // end of day

        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2017, 9, 22));
        SlotRange slotRange = partition.toSlotRange(eraService);

        // startSlot clamped to -1, endSlot clamped to 0
        assertThat(slotRange.startSlot()).isEqualTo(-1);
        assertThat(slotRange.endSlot()).isEqualTo(0);
    }

    @Test
    void toPathSegment_shouldReturnHiveStyleDateFormat() {
        PartitionValue partition = PartitionValue.ofDate(LocalDate.of(2024, 1, 15));

        assertThat(partition.toPathSegment()).isEqualTo("date=2024-01-15");
    }
}
