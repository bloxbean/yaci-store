package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip113RegistryService")
class Cip113RegistryServiceTest {

    private static final String MONITORED_POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String OTHER_POLICY = "9999999999999999999999999999999999999999999999999999999999";

    private Cip113Configuration config;
    private Cip113RegistryService service;

    @BeforeEach
    void setUp() {
        config = buildConfig(MONITORED_POLICY);
        service = new Cip113RegistryService(config);
    }

    static Cip113Configuration buildConfig(String... policyIds) {
        Cip113Configuration config = org.mockito.Mockito.mock(Cip113Configuration.class,
                org.mockito.Mockito.withSettings().lenient());
        java.util.Set<String> policyIdSet = java.util.Set.of(policyIds);
        when(config.isEnabled()).thenReturn(!policyIdSet.isEmpty());
        when(config.getRegistryNftPolicyIdSet()).thenReturn(policyIdSet);
        for (String id : policyIds) {
            when(config.isMonitoredPolicyId(id)).thenReturn(true);
        }
        return config;
    }

    @Nested
    @DisplayName("containsRegistryNode")
    class ContainsRegistryNode {

        @Test
        void returnsTrueForMatchingNft() {
            AddressUtxo utxo = utxoWithAmount(MONITORED_POLICY, "deadbeef", BigInteger.ONE);
            assertThat(service.containsRegistryNode(utxo)).isTrue();
        }

        @Test
        void returnsFalseForNonMatchingPolicy() {
            AddressUtxo utxo = utxoWithAmount(OTHER_POLICY, "deadbeef", BigInteger.ONE);
            assertThat(service.containsRegistryNode(utxo)).isFalse();
        }

        @Test
        void returnsFalseWhenQuantityNotOne() {
            AddressUtxo utxo = utxoWithAmount(MONITORED_POLICY, "deadbeef", BigInteger.TEN);
            assertThat(service.containsRegistryNode(utxo)).isFalse();
        }

        @Test
        void returnsFalseWhenDisabled() {
            config = buildConfig();
            service = new Cip113RegistryService(config);
            AddressUtxo utxo = utxoWithAmount(MONITORED_POLICY, "deadbeef", BigInteger.ONE);
            assertThat(service.containsRegistryNode(utxo)).isFalse();
        }

        private AddressUtxo utxoWithAmount(String policyId, String assetName, BigInteger quantity) {
            return AddressUtxo.builder()
                    .amounts(List.of(Amt.builder()
                            .policyId(policyId)
                            .assetName(assetName)
                            .unit(policyId + assetName)
                            .quantity(quantity).build()))
                    .build();
        }
    }
}
