package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EpochPartitionTest {

    /** Preprod: four Byron epochs of 21,600 slots, Shelley from slot 86,400 with 432,000-slot epochs. */
    private EraService preprod() {
        EraService eraService = mock(EraService.class);
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(4));
        when(eraService.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(eraService.getFirstNonByronSlot()).thenReturn(86400L);
        when(eraService.getShelleyAbsoluteSlot(4, 0)).thenReturn(86400L);
        when(eraService.getShelleyAbsoluteSlot(5, 0)).thenReturn(518400L);
        return eraService;
    }

    @Test
    void byronEpochsUseByronSlotsAndEndAtTheFirstNonByronSlot() {
        EraService eraService = preprod();
        assertEquals(new SlotRange(0, 21600), PartitionValue.ofEpoch(0).toSlotRange(eraService));
        assertEquals(new SlotRange(64800, 86400), PartitionValue.ofEpoch(3).toSlotRange(eraService));
    }

    @Test
    void shelleyEpochsKeepShelleySlotArithmetic() {
        assertEquals(new SlotRange(86400, 518400), PartitionValue.ofEpoch(4).toSlotRange(preprod()));
    }
}
