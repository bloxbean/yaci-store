package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip113RegistryService")
class Cip113RegistryServiceTest {

    private static final String MONITORED_POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String OTHER_POLICY = "9999999999999999999999999999999999999999999999999999999999";

    @Mock
    private Cip113RegistryNodeRepository repository;

    private Cip113Configuration config;
    private Cip113RegistryService service;

    @BeforeEach
    void setUp() {
        config = buildConfig(MONITORED_POLICY);
        service = new Cip113RegistryService(repository, config);
    }

    private static Cip113Configuration buildConfig(String... policyIds) {
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
    @DisplayName("findByPolicyId")
    class FindByPolicyId {

        @Test
        void returnsDto() {
            Cip113RegistryNode entity = Cip113RegistryNode.builder()
                    .policyId("deadbeef")
                    .transferLogicScript("script1")
                    .thirdPartyTransferLogicScript("script2")
                    .globalStatePolicyId("globalState")
                    .build();

            when(repository.findFirstByPolicyIdOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(entity));

            Optional<ProgrammableTokenCip113> result = service.findByPolicyId("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isEqualTo("script1");
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("script2");
            assertThat(result.get().globalStatePolicyId()).isEqualTo("globalState");
        }

        @Test
        void normalizesNullTransferLogicScript() {
            Cip113RegistryNode entity = Cip113RegistryNode.builder()
                    .policyId("deadbeef")
                    .transferLogicScript(null)
                    .thirdPartyTransferLogicScript("script2")
                    .globalStatePolicyId(null)
                    .build();

            when(repository.findFirstByPolicyIdOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(entity));

            Optional<ProgrammableTokenCip113> result = service.findByPolicyId("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isNull();
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("script2");
        }

        @Test
        void returnsEmptyWhenNotFound() {
            when(repository.findFirstByPolicyIdOrderBySlotDesc("unknown"))
                    .thenReturn(Optional.empty());

            assertThat(service.findByPolicyId("unknown")).isEmpty();
        }

        @Test
        void returnsEmptyWhenDisabled() {
            config = buildConfig();
            service = new Cip113RegistryService(repository, config);
            assertThat(service.findByPolicyId("deadbeef")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPolicyIds (batch)")
    class FindByPolicyIds {

        @Test
        void returnsMappedDtos() {
            Cip113RegistryNode entity1 = Cip113RegistryNode.builder()
                    .policyId("policy1").transferLogicScript("s1")
                    .thirdPartyTransferLogicScript("s2").globalStatePolicyId("").build();
            Cip113RegistryNode entity2 = Cip113RegistryNode.builder()
                    .policyId("policy2").transferLogicScript("s3")
                    .thirdPartyTransferLogicScript("s4").globalStatePolicyId("gs").build();

            when(repository.findLatestByPolicyIds(List.of("policy1", "policy2")))
                    .thenReturn(List.of(entity1, entity2));

            Map<String, ProgrammableTokenCip113> result = service.findByPolicyIds(List.of("policy1", "policy2"));

            assertThat(result).hasSize(2);
            assertThat(result.get("policy1").transferLogicScript()).isEqualTo("s1");
            assertThat(result.get("policy2").transferLogicScript()).isEqualTo("s3");
        }

        @Test
        void returnsEmptyMapWhenDisabled() {
            config = buildConfig();
            service = new Cip113RegistryService(repository, config);
            assertThat(service.findByPolicyIds(List.of("policy1"))).isEmpty();
        }

        @Test
        void returnsEmptyMapForEmptyInput() {
            assertThat(service.findByPolicyIds(List.of())).isEmpty();
        }
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
            service = new Cip113RegistryService(repository, config);
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
