package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.storage.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.EraStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EraServiceTest {

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

    @InjectMocks
    private EraService eraService;

    @BeforeEach
    void setup() {
        eraService = new EraService(eraStorage, cursorStorage, epochConfig, genesisConfig, storeProperties);
    }

    @Test
    void blockTime_preprod_shelley_blk_99() {
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.getStartTime(any(Long.class))).thenReturn(1654041600L);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(86400L).build()));

        long blockTime = eraService.blockTime(Era.Shelley, 87460);
        assertEquals(1655770660, blockTime);
    }

    @Test
    void blockTime_preprod_shelley_blk_366010() {
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.getStartTime(any(Long.class))).thenReturn(1654041600L);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(86400L).build()));

        long blockTime = eraService.blockTime(Era.Shelley, 14621989);
        assertEquals(1670305189, blockTime);
    }

    @Test
    void blockTime_preprod_byron_blk_12() {
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.getStartTime(any(Long.class))).thenReturn(1654041600L);

        long blockTime = eraService.blockTime(Era.Byron, 23761);
        assertEquals(1654516820, blockTime);
    }

    @Test
    void blockTime_preview_alonzo_blk_21() {
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.getStartTime(any(Long.class))).thenReturn(1666656000L);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Alonzo).startSlot(0L).build()));

        long blockTime = eraService.blockTime(Era.Alonzo, 420);
        assertEquals(1666656420, blockTime);
    }

    @Test
    void blockTime_preview_alonzo_blk_13011() {
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.getStartTime(any(Long.class))).thenReturn(1666656000L);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Alonzo).startSlot(0L).build()));

        long blockTime = eraService.blockTime(Era.Alonzo, 259180);
        assertEquals(1666915180, blockTime);
    }

    @Test
    void blockTime_preview_babbage_blk_13011() {
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        when(genesisConfig.slotDuration(Era.Byron)).thenReturn(20.0);
        when(genesisConfig.getStartTime(any(Long.class))).thenReturn(1666656000L);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Alonzo).startSlot(0L).build()));

        long blockTime = eraService.blockTime(Era.Babbage, 264811);
        assertEquals(1666920811, blockTime);
    }
}
