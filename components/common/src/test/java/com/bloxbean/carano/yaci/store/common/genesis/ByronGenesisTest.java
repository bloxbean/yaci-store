package com.bloxbean.carano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.genesis.ByronGenesis;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ByronGenesisTest {
    public static final String EXPECTED_MAINNET_ADDRESS_BALANCE_JSON = "/genesis/expected/mainnet-address-balance.json";
    public static final String EXPECTED_MAINNET_TX_BALANCE_JSON = "/genesis/expected/mainnet-txs-balance.json";
    public static final String EXPECTED_PREPROD_ADDRESS_BALANCE_JSON = "/genesis/expected/preprod-address-balance.json";
    public static final String EXPECTED_PREPROD_TX_BALANCE_JSON = "/genesis/expected/preprod-txs-balance.json";


    public static final String GENESIS_MAINNET_BYRON_GENESIS_JSON = "/genesis/mainnet-byron-genesis.json";
    public static final String GENESIS_PREPROD_BYRON_GENESIS_JSON = "/genesis/preprod-byron-genesis.json";

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getByronAddressBalances_mainnet() {
        InputStream is = this.getClass().getResourceAsStream(GENESIS_MAINNET_BYRON_GENESIS_JSON);
        ByronGenesis byronGenesis = new ByronGenesis(is);
        List<GenesisBalance> avvmGenesisBalances = byronGenesis.getAvvmGenesisBalances();
        List<GenesisBalance> nonAvvmGenesisBalances = byronGenesis.getNonAvvmGenesisBalances();

        Map<String, BigInteger> actualAddrBalance = avvmGenesisBalances.stream()
                .collect(Collectors.toMap(GenesisBalance::getAddress, GenesisBalance::getBalance));
        Map<String, BigInteger> expectedAddrBalance = loadMap(EXPECTED_MAINNET_ADDRESS_BALANCE_JSON);

        Map<String, BigInteger> actualTxsBalance = avvmGenesisBalances.stream()
                .collect(Collectors.toMap(GenesisBalance::getTxnHash, GenesisBalance::getBalance));
        Map<String, BigInteger> expectedTxsBalance = loadMap(EXPECTED_MAINNET_TX_BALANCE_JSON);

        assertThat(avvmGenesisBalances.size()).isEqualTo(expectedAddrBalance.size());
        assertThat(nonAvvmGenesisBalances).isEmpty();

        assertThat(actualAddrBalance).isEqualTo(expectedAddrBalance);
        assertThat(actualTxsBalance).isEqualTo(expectedTxsBalance);
    }

    @Test
    void getByronAddressBalances_preprod() {
        InputStream is = this.getClass().getResourceAsStream(GENESIS_PREPROD_BYRON_GENESIS_JSON);
        ByronGenesis byronGenesis = new ByronGenesis(is);
        List<GenesisBalance> avvmGenesisBalances = byronGenesis.getAvvmGenesisBalances();
        List<GenesisBalance> nonAvvmGenesisBalances = byronGenesis.getNonAvvmGenesisBalances();

        Map<String, BigInteger> actualAddrBalance = nonAvvmGenesisBalances.stream()
                .collect(Collectors.toMap(GenesisBalance::getAddress, GenesisBalance::getBalance));
        Map<String, BigInteger> expectedAddrBalance = loadMap(EXPECTED_PREPROD_ADDRESS_BALANCE_JSON);

        Map<String, BigInteger> actualTxsBalance = nonAvvmGenesisBalances.stream()
                .collect(Collectors.toMap(GenesisBalance::getTxnHash, GenesisBalance::getBalance));
        Map<String, BigInteger> expectedTxsBalance = loadMap(EXPECTED_PREPROD_TX_BALANCE_JSON);

        assertThat(nonAvvmGenesisBalances.size()).isEqualTo(expectedAddrBalance.size());
        assertThat(avvmGenesisBalances).isEmpty();

        assertThat(actualAddrBalance).isEqualTo(expectedAddrBalance);
        assertThat(actualTxsBalance).isEqualTo(expectedTxsBalance);
    }

    @Test
    void getByronAddressBalances_custom() {
        InputStream is = this.getClass().getResourceAsStream(GENESIS_PREPROD_BYRON_GENESIS_JSON);
        ByronGenesis byronGenesis = new ByronGenesis(is);
        List<GenesisBalance> avvmGenesisBalances = byronGenesis.getAvvmGenesisBalances();
        List<GenesisBalance> nonAvvmGenesisBalances = byronGenesis.getNonAvvmGenesisBalances();
        System.out.println(avvmGenesisBalances);
    }

    private Map<String, BigInteger> loadMap(String file) {
        InputStream is = this.getClass().getResourceAsStream(file);
        try {
            return objectMapper.readValue(is, new TypeReference<Map<String, BigInteger>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
