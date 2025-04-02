package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.PoolParams;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisStaking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;
import static org.assertj.core.api.Assertions.assertThat;

class ShelleyGenesisTest {
    public static final String GENESIS_MAINNET_SHELLEY_GENESIS_JSON = "/genesis/mainnet-shelley-genesis.json";
    public static final String GENESIS_CUSTOM_SHELLEY_GENESIS_JSON = "/genesis/custom-shelley-genesis.json";

    @Test
    void parseShelleyGenesis_mainnet() {
        ShelleyGenesis shelleyGenesis = new ShelleyGenesis(this.getClass().getResourceAsStream(GENESIS_MAINNET_SHELLEY_GENESIS_JSON));

        assertThat(shelleyGenesis.getSlotLength()).isEqualTo(1);
        assertThat(shelleyGenesis.getSystemStart()).isEqualTo("2017-09-23T21:44:51Z");
        assertThat(shelleyGenesis.getActiveSlotsCoeff()).isEqualTo(0.05);
        assertThat(shelleyGenesis.getMaxLovelaceSupply()).isEqualTo(new BigInteger("45000000000000000"));
        assertThat(shelleyGenesis.getEpochLength()).isEqualTo(432000);
        assertThat(shelleyGenesis.getNetworkMagic()).isEqualTo(764824073L);
    }

    @Test
    void parseShelleyGenesis_customNetwork() {
        ShelleyGenesis shelleyGenesis = new ShelleyGenesis(this.getClass().getResourceAsStream(GENESIS_CUSTOM_SHELLEY_GENESIS_JSON));

        List<GenesisBalance> expectedInitialUtxos = new ArrayList<>();
        expectedInitialUtxos.add(new GenesisBalance("addr_test1qpefp65049pncyz95nyyww2e44sgumqr5kx8mcemm0fuumeftwv8zdtpqct0836wz8y56aakem2uejf604cee7cn2p3qp9p8te", "66dc6b2e628bf1fb6204797f1a07f8e949d9520a70e859ecbf3ea3076029871e", new BigInteger("300000000000")));
        expectedInitialUtxos.add(new GenesisBalance("addr_test1vpf8vv32c7yzgdqh8hxxgsvstannw6ym6vymdzkckds5lkq4jtrmx", "650aae802cf0f3f5b214b6669caf823c34ff3648d749a8367bceed4bf64b2ae6", new BigInteger("3000000000000000")));
        expectedInitialUtxos.add(new GenesisBalance("addr_test1vzaf27s0la5pvqsm9ta8jq97af506y8j678mtdjdurfr08qkeh3t3", "c59295f675c40a068596c0b816c997a8a2f564d6c090d5242444cf79d0ddec48", new BigInteger("3000000000000000")));
        expectedInitialUtxos.add(new GenesisBalance("addr_test1vzs0r2nae22szlq3ul3h8t4u7rz9dr850mqjh98caddm4zcypzjjt", "347e9d3c72ddc52bdb43cf1c44d31507983c102e870398b9205fb8ed125f7e64", new BigInteger("3000000000000000")));

        assertThat(shelleyGenesis.getSlotLength()).isEqualTo(1.0);
        assertThat(shelleyGenesis.getSystemStart()).isEqualTo("2022-09-15T04:09:11.577484Z");
        assertThat(shelleyGenesis.getActiveSlotsCoeff()).isEqualTo(1.0);
        assertThat(shelleyGenesis.getMaxLovelaceSupply()).isEqualTo(new BigInteger("20000000000000000"));
        assertThat(shelleyGenesis.getEpochLength()).isEqualTo(500);
        assertThat(shelleyGenesis.getNetworkMagic()).isEqualTo(42);

        assertThat(shelleyGenesis.getInitialFunds()).containsAll(expectedInitialUtxos);

        assertThat(shelleyGenesis.getGenesisStaking()).isNotNull();
        assertThat(shelleyGenesis.getGenesisStaking().getPools()).hasSize(1);
        assertThat(shelleyGenesis.getGenesisStaking().getStakes()).hasSize(1);

        PoolParams poolParams = shelleyGenesis.getGenesisStaking().getPools().get(0);
        GenesisStaking.Stake stake = shelleyGenesis.getGenesisStaking().getStakes().get(0);

        String rewardAcc = new Address(HexUtil.decodeHexString(poolParams.getRewardAccount())).toBech32();

        assertThat(poolParams.getOperator()).isEqualTo("7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57");
        assertThat(poolParams.getCost()).isEqualTo(new BigInteger("340000000"));
        assertThat(safeRatio(poolParams.getMargin())).isEqualTo(BigDecimal.ZERO);
        assertThat(poolParams.getPoolMetadataUrl()).isNull();
        assertThat(poolParams.getPoolOwners()).isEmpty();
        assertThat(poolParams.getPledge()).isEqualTo(BigInteger.ZERO);
        assertThat(rewardAcc).isEqualTo("stake_test1uqg6znklwwcg5z38ewvt93t7kd78sr033l8u7eu9a4wlsjszpckek");
        assertThat(poolParams.getVrfKeyHash()).isEqualTo("c2b62ffa92ad18ffc117ea3abeb161a68885000a466f9c71db5e4731d6630061");

        assertThat(stake.getStakeKeyHash()).isEqualTo("295b987135610616f3c74e11c94d77b6ced5ccc93a7d719cfb135062");
        assertThat(stake.getPoolHash()).isEqualTo("7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57");
    }

    @Test
    void parseShelleyGenesis_mainnet_bundled() {
        ShelleyGenesis shelleyGenesis = new ShelleyGenesis(NetworkType.MAINNET.getProtocolMagic());
        ProtocolParams protocolParams = shelleyGenesis.getProtocolParams();

        assertThat(shelleyGenesis.getSlotLength()).isEqualTo(1);
        assertThat(shelleyGenesis.getSystemStart()).isEqualTo("2017-09-23T21:44:51Z");
        assertThat(shelleyGenesis.getActiveSlotsCoeff()).isEqualTo(0.05);
        assertThat(shelleyGenesis.getMaxLovelaceSupply()).isEqualTo(new BigInteger("45000000000000000"));
        assertThat(shelleyGenesis.getEpochLength()).isEqualTo(432000);
        assertThat(shelleyGenesis.getNetworkMagic()).isEqualTo(764824073L);

        //protocol params
        assertThat(protocolParams.getProtocolMajorVer()).isEqualTo(2);
        assertThat(protocolParams.getProtocolMinorVer()).isEqualTo(0);
        assertThat(safeRatio(protocolParams.getDecentralisationParam())).isEqualTo(BigDecimal.valueOf(1));
        assertThat(protocolParams.getMaxEpoch()).isEqualTo(18);
        assertThat(protocolParams.getMaxTxSize()).isEqualTo(16384);
        assertThat(protocolParams.getMaxBlockSize()).isEqualTo(65536);
        assertThat(protocolParams.getMaxBlockHeaderSize()).isEqualTo(1100);
        assertThat(protocolParams.getMinFeeA()).isEqualTo(44);
        assertThat(protocolParams.getMinFeeB()).isEqualTo(155381);
        assertThat(protocolParams.getMinUtxo()).isEqualTo(1000000);
        assertThat(protocolParams.getPoolDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getMinPoolCost()).isEqualTo(340000000);
        assertThat(protocolParams.getKeyDeposit()).isEqualTo(2000000);
        assertThat(protocolParams.getNOpt()).isEqualTo(150);
        assertThat(safeRatio(protocolParams.getExpansionRate())).isEqualByComparingTo(BigDecimal.valueOf(0.003));
        assertThat(safeRatio(protocolParams.getTreasuryGrowthRate())).isEqualByComparingTo(BigDecimal.valueOf(0.20));
        assertThat(safeRatio(protocolParams.getPoolPledgeInfluence())).isEqualByComparingTo(BigDecimal.valueOf(0.3));
    }

    @Test
    void parseShelleyGenesis_sanchonet_bundled() {
        ShelleyGenesis shelleyGenesis = new ShelleyGenesis(NetworkType.SANCHONET.getProtocolMagic());
        ProtocolParams protocolParams = shelleyGenesis.getProtocolParams();

        assertThat(shelleyGenesis.getSlotLength()).isEqualTo(1);
        assertThat(shelleyGenesis.getSystemStart()).isEqualTo("2023-06-15T00:30:00Z");
        assertThat(shelleyGenesis.getActiveSlotsCoeff()).isEqualTo(0.05);
        assertThat(shelleyGenesis.getMaxLovelaceSupply()).isEqualTo(new BigInteger("45000000000000000"));
        assertThat(shelleyGenesis.getEpochLength()).isEqualTo(86400L);
        assertThat(shelleyGenesis.getNetworkMagic()).isEqualTo(4);

        //protocol params
        assertThat(protocolParams.getProtocolMajorVer()).isEqualTo(6);
        assertThat(protocolParams.getProtocolMinorVer()).isEqualTo(0);
        assertThat(safeRatio(protocolParams.getDecentralisationParam())).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        assertThat(protocolParams.getMaxEpoch()).isEqualTo(18);
        assertThat(protocolParams.getMaxTxSize()).isEqualTo(16384);
        assertThat(protocolParams.getMaxBlockSize()).isEqualTo(65536);
        assertThat(protocolParams.getMaxBlockHeaderSize()).isEqualTo(1100);
        assertThat(protocolParams.getMinFeeA()).isEqualTo(44);
        assertThat(protocolParams.getMinFeeB()).isEqualTo(155381);
        assertThat(protocolParams.getMinUtxo()).isEqualTo(1000000);
        assertThat(protocolParams.getPoolDeposit()).isEqualTo(500000000);
        assertThat(protocolParams.getMinPoolCost()).isEqualTo(340000000);
        assertThat(protocolParams.getKeyDeposit()).isEqualTo(2000000);
        assertThat(protocolParams.getNOpt()).isEqualTo(150);
        assertThat(safeRatio(protocolParams.getExpansionRate())).isEqualByComparingTo(BigDecimal.valueOf(0.003));
        assertThat(safeRatio(protocolParams.getTreasuryGrowthRate())).isEqualByComparingTo(BigDecimal.valueOf(0.20));
        assertThat(safeRatio(protocolParams.getPoolPledgeInfluence())).isEqualByComparingTo(BigDecimal.valueOf(0.3));
    }

}
