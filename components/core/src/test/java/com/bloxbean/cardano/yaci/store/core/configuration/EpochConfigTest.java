package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EpochConfigTest {

    private GenesisConfig genesisConfig;
    private EpochConfig epochConfig;

    private final Long mainnetShellyStartSlot = 4492800L;
    private final Long preprodShellyStartSlot = 86400L;

    void preprodSetup() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(NetworkType.PREPROD.getProtocolMagic());
        genesisConfig = new GenesisConfig(storeProperties, new ObjectMapper());
        epochConfig = new EpochConfig(genesisConfig);
    }

    void mainnetSetup() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(NetworkType.MAINNET.getProtocolMagic());
        genesisConfig = new GenesisConfig(storeProperties, new ObjectMapper());
        epochConfig = new EpochConfig(genesisConfig);
    }

    @Nested
    class PreprodEpochs {
        @Test
        void epochFromSlot__firstShelley_epochIs4() {
            preprodSetup();
            int epoch = epochConfig.epochFromSlot(preprodShellyStartSlot, Era.Shelley, 86400);
            assertEquals(4, epoch);
        }

        @Test
        void epochFromSlot_epochIs6() {
            preprodSetup();
            int epoch = epochConfig.epochFromSlot(preprodShellyStartSlot, Era.Shelley, 1006073);
            assertEquals(6, epoch);
        }

        @Test
        void epochFromSlot_epochIs8() {
            preprodSetup();
            int epoch = epochConfig.epochFromSlot(preprodShellyStartSlot, Era.Shelley, 2048749);
            assertEquals(8, epoch);
        }

        @Test
        void epochFromSlot_epochIs36() {
            preprodSetup();
            int epoch = epochConfig.epochFromSlot(preprodShellyStartSlot, Era.Shelley, 13782223);
            assertEquals(35, epoch);
        }

        @Test
        void epochFromSlot_epochIs57() {
            preprodSetup();
            int epoch = epochConfig.epochFromSlot(preprodShellyStartSlot, Era.Shelley, 23368840);
            assertEquals(57, epoch);
        }
    }

    @Nested
    class MainnetEpochs {

        @Test
        void epochFromSlot__firstShelley_epochIs208() {
            mainnetSetup();
            int epoch = epochConfig.epochFromSlot(mainnetShellyStartSlot, Era.Shelley, 4492800);
            assertEquals(208, epoch);
        }

        @Test
        void epochFromSlot__firstShelley_epochIs208_2() {
            mainnetSetup();
            int epoch = epochConfig.epochFromSlot(mainnetShellyStartSlot, Era.Shelley, 4492800);
            assertEquals(208, epoch);
        }

        @Test
        void epochFromSlot__firstShelley_epochIs208_3() {
            mainnetSetup();
            int epoch = epochConfig.epochFromSlot(mainnetShellyStartSlot, Era.Shelley, 4492880);
            assertEquals(208, epoch);
        }

        @Test
        void epochFromSlot_epochIs398() {
            mainnetSetup();
            int epoch = epochConfig.epochFromSlot(mainnetShellyStartSlot, Era.Shelley, 86707137);
            assertEquals(398, epoch);
        }

        @Test
        void epochFromSlot_epochIs350() {
            mainnetSetup();
            int epoch = epochConfig.epochFromSlot(mainnetShellyStartSlot, Era.Shelley, 66095599);
            assertEquals(350, epoch);
        }

        @Test
        void epochFromSlot_epochIs254() {
            mainnetSetup();
            int epoch = epochConfig.epochFromSlot(mainnetShellyStartSlot, Era.Shelley, 24791234);
            assertEquals(254, epoch);
        }

        @Test
        void epochSlot_1() {
            mainnetSetup();
            long absoluteSlot = 94807439L;
            long epochSlot = epochConfig.shelleyEpochSlot(mainnetShellyStartSlot, absoluteSlot);
            assertEquals(26639, epochSlot);
        }

        @Test
        void epochSlot_2() {
            mainnetSetup();
            long absoluteSlot = 94348799L;
            long epochSlot = epochConfig.shelleyEpochSlot(mainnetShellyStartSlot, absoluteSlot);
            assertEquals(431999, epochSlot);
        }

        @Test
        void epochSlot_3() {
            mainnetSetup();
            long absoluteSlot = 94348926L;
            long epochSlot = epochConfig.shelleyEpochSlot(mainnetShellyStartSlot, absoluteSlot);
            assertEquals(126, epochSlot);
        }
    }
}
