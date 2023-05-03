package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EpochConfigTest {
    private GenesisConfig genesisConfig = new GenesisConfig();
    private EpochConfig epochConfig = new EpochConfig(genesisConfig);

    @Nested
    class PreprodEpochs {
        @Test
        void epochFromSlot_epochIsZero() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Byron, 19445);
            assertEquals(0, epoch);
        }

        @Test
        void epochFromSlot_epochIsOne() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Byron, 21600);
            assertEquals(1, epoch);
        }

        @Test
        void epochFromSlot_epochIsOne_2() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Byron, 23761);
            assertEquals(1, epoch);
        }

        @Test
        void epochFromSlot_epochIs3() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Byron, 79922);
            assertEquals(3, epoch);
        }

        @Test
        void epochFromSlot_epochIs3_2() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Byron, 84242);
            assertEquals(3, epoch);
        }

        @Test
        void epochFromSlot__firstShelley_epochIs4() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Shelley, 86400);
            assertEquals(4, epoch);
        }

        @Test
        void epochFromSlot_epochIs6() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Shelley, 1006073);
            assertEquals(6, epoch);
        }

        @Test
        void epochFromSlot_epochIs8() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Shelley, 2048749);
            assertEquals(8, epoch);
        }

        @Test
        void epochFromSlot_epochIs36() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Shelley, 13782223);
            assertEquals(35, epoch);
        }

        @Test
        void epochFromSlot_epochIs57() {
            int epoch = epochConfig.epochFromSlot(NetworkType.PREPROD.getProtocolMagic(), Era.Shelley, 23368840);
            assertEquals(57, epoch);
        }
    }

    @Nested
    class MainnetEpochs {
        @Test
        void epochFromSlot_epochIsZero() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 500);
            assertEquals(0, epoch);
        }

        @Test
        void epochFromSlot_epochIsZero_2() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 21599);
            assertEquals(0, epoch);
        }

        @Test
        void epochFromSlot_epochIsOne() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 21600);
            assertEquals(1, epoch);
        }

        @Test
        void epochFromSlot_epochIsOne_2() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 21600);
            assertEquals(1, epoch);
        }

        @Test
        void epochFromSlot_epochIsOne_3() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 23761);
            assertEquals(1, epoch);
        }

        @Test
        void epochFromSlot_epochIs3() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 79922);
            assertEquals(3, epoch);
        }

        @Test
        void epochFromSlot_epochIs3_2() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 84242);
            assertEquals(3, epoch);
        }

        @Test
        void epochFromSlot__firstShelley_epochIs208() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Shelley, 4492800);
            assertEquals(208, epoch);
        }

        @Test
        void epochFromSlot__firstShelley_epochIs208_2() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Shelley, 4492800);
            assertEquals(208, epoch);
        }

        @Test
        void epochFromSlot__firstShelley_epochIs208_3() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Shelley, 4492880);
            assertEquals(208, epoch);
        }

        @Test
        void epochFromSlot_epochIs398() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Shelley, 86707137);
            assertEquals(398, epoch);
        }

        @Test
        void epochFromSlot_epochIs_115() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Byron, 2492018);
            assertEquals(115, epoch);
        }

        @Test
        void epochFromSlot_epochIs350() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Shelley, 66095599);
            assertEquals(350, epoch);
        }

        @Test
        void epochFromSlot_epochIs254() {
            int epoch = epochConfig.epochFromSlot(NetworkType.MAINNET.getProtocolMagic(), Era.Shelley, 24791234);
            assertEquals(254, epoch);
        }
    }
}
