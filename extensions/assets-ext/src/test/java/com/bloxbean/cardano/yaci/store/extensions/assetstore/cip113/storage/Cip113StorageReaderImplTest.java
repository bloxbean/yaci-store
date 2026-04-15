package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryServiceTest.buildConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip113StorageReaderImpl")
class Cip113StorageReaderImplTest {

    private static final String MONITORED_POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";

    @Mock
    private Cip113RegistryNodeRepository repository;

    private Cip113Configuration config;
    private Cip113StorageReaderImpl reader;

    @BeforeEach
    void setUp() {
        config = buildConfig(MONITORED_POLICY);
        reader = new Cip113StorageReaderImpl(repository, config);
    }

    @Nested
    @DisplayName("findByPolicyId")
    class FindByPolicyId {

        @Test
        void returnsDto() {
            Cip113RegistryNode entity = Cip113RegistryNode.builder()
                    .key("deadbeef")
                    .transferLogicScript("script1")
                    .thirdPartyTransferLogicScript("script2")
                    .globalStatePolicyId("globalState")
                    .build();

            when(repository.findFirstByKeyOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(entity));

            Optional<ProgrammableTokenCip113> result = reader.findByPolicyId("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isEqualTo("script1");
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("script2");
            assertThat(result.get().globalStatePolicyId()).isEqualTo("globalState");
        }

        @Test
        void normalizesNullTransferLogicScript() {
            Cip113RegistryNode entity = Cip113RegistryNode.builder()
                    .key("deadbeef")
                    .transferLogicScript(null)
                    .thirdPartyTransferLogicScript("script2")
                    .globalStatePolicyId(null)
                    .build();

            when(repository.findFirstByKeyOrderBySlotDesc("deadbeef"))
                    .thenReturn(Optional.of(entity));

            Optional<ProgrammableTokenCip113> result = reader.findByPolicyId("deadbeef");

            assertThat(result).isPresent();
            assertThat(result.get().transferLogicScript()).isNull();
            assertThat(result.get().thirdPartyTransferLogicScript()).isEqualTo("script2");
        }

        @Test
        void returnsEmptyWhenNotFound() {
            when(repository.findFirstByKeyOrderBySlotDesc("unknown"))
                    .thenReturn(Optional.empty());

            assertThat(reader.findByPolicyId("unknown")).isEmpty();
        }

        @Test
        void returnsEmptyWhenDisabled() {
            config = buildConfig();
            reader = new Cip113StorageReaderImpl(repository, config);
            assertThat(reader.findByPolicyId("deadbeef")).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPolicyIds (batch)")
    class FindByPolicyIds {

        @Test
        void returnsMappedDtos() {
            Cip113RegistryNode entity1 = Cip113RegistryNode.builder()
                    .key("policy1").transferLogicScript("s1")
                    .thirdPartyTransferLogicScript("s2").globalStatePolicyId("").build();
            Cip113RegistryNode entity2 = Cip113RegistryNode.builder()
                    .key("policy2").transferLogicScript("s3")
                    .thirdPartyTransferLogicScript("s4").globalStatePolicyId("gs").build();

            when(repository.findLatestByKeys(List.of("policy1", "policy2")))
                    .thenReturn(List.of(entity1, entity2));

            Map<String, ProgrammableTokenCip113> result = reader.findByPolicyIds(List.of("policy1", "policy2"));

            assertThat(result).hasSize(2);
            assertThat(result.get("policy1").transferLogicScript()).isEqualTo("s1");
            assertThat(result.get("policy2").transferLogicScript()).isEqualTo("s3");
        }

        @Test
        void returnsEmptyMapWhenDisabled() {
            config = buildConfig();
            reader = new Cip113StorageReaderImpl(repository, config);
            assertThat(reader.findByPolicyIds(List.of("policy1"))).isEmpty();
        }

        @Test
        void returnsEmptyMapForEmptyInput() {
            assertThat(reader.findByPolicyIds(List.of())).isEmpty();
        }
    }
}
