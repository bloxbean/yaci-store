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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    private EpochConfig epochConfig;

    @Mock
    private GenesisConfig genesisConfig;

    @Mock
    private StoreProperties storeProperties;

    @Mock
    private TipFinderService tipFinderService;

    @InjectMocks
    private EraService eraService;

    @BeforeEach
    void setup() {
        this.epochConfig = new EpochConfig(genesisConfig);
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

    @Test
    void getEraForEpoch_ByronEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(206);
        assertEquals(Era.Byron, era);
    }

    @Test
    void getEraForEpoch_ByronEpoch_onlyShelleyEraInList() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(207);
        assertEquals(Era.Byron, era);
    }

    @Test
    void getEraForEpoch_shelleyStartEpoch_onlyShelleyEraInList() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(208);
        assertEquals(Era.Shelley, era);
    }

    @Test
    void getEraForEpoch_shelleyEpoch_onlyShelleyEraInList() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(300);
        assertEquals(Era.Shelley, era);
    }

    @Test
    void getEraForEpoch_shelleyEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(235);
        assertEquals(Era.Shelley, era);
    }

    @Test
    void getEraForEpoch_allegraStartEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(236);
        assertEquals(Era.Allegra, era);
    }

    @Test
    void getEraForEpoch_allegraEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(250);
        assertEquals(Era.Allegra, era);
    }

    @Test
    void getEraForEpoch_maryEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(289);
        assertEquals(Era.Mary, era);
    }

    @Test
    void getEraForEpoch_alonzoStartEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(290);
        assertEquals(Era.Alonzo, era);
    }

    @Test
    void getEraForEpoch_alonzoEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(364);
        assertEquals(Era.Alonzo, era);
    }

    @Test
    void getEraForEpoch_babbageStartEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(365);
        assertEquals(Era.Babbage, era);
    }

    @Test
    void getEraForEpoch_babbageEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(400);
        assertEquals(Era.Babbage, era);
    }

    @Test
    void getEraForEpoch_conwayStartEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build(),
                CardanoEra.builder().era(Era.Conway).startSlot(133660855).block(10781331).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(507);
        assertEquals(Era.Conway, era);
    }

    @Test
    void getEraForEpoch_conwayEpoch() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Shelley).startSlot(4492800).block(4490511).build(),
                CardanoEra.builder().era(Era.Allegra).startSlot(16588800).block(5086524).build(),
                CardanoEra.builder().era(Era.Mary).startSlot(23068800).block(5406747).build(),
                CardanoEra.builder().era(Era.Alonzo).startSlot(39916975).block(6236060).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build(),
                CardanoEra.builder().era(Era.Conway).startSlot(133660855).block(10781331).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Shelley).startSlot(4492800).block(4490511).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(518);
        assertEquals(Era.Conway, era);
    }

    @Test
    void getEraForEpoch_alonzoEra_nonShelleyStartEra() {

        List<CardanoEra> eras = List.of(
                CardanoEra.builder().era(Era.Alonzo).startSlot(0).block(1).build(),
                CardanoEra.builder().era(Era.Babbage).startSlot(72316896).block(7791699).build(),
                CardanoEra.builder().era(Era.Conway).startSlot(133660855).block(10781331).build()
        );

        when(eraStorage.findAllEras()).thenReturn(eras);
        when(eraStorage.findFirstNonByronEra()).thenReturn(Optional.of(CardanoEra.builder()
                .era(Era.Alonzo).startSlot(0).block(1).build()));

        when(genesisConfig.slotsPerEpoch(Era.Byron)).thenReturn(21600L);
        when(genesisConfig.slotsPerEpoch(Era.Shelley)).thenReturn(432000L);

        Era era = eraService.getEraForEpoch(4);
        assertEquals(Era.Alonzo, era);
    }


}
