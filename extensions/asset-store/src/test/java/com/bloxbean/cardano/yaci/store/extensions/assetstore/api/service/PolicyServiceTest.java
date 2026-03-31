package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.PolicyResponse;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PolicyService")
class PolicyServiceTest {

    private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String UNKNOWN_POLICY = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    @Mock
    private Cip26StorageReader cip26StorageReader;

    @Mock
    private Cip68StorageReader cip68StorageReader;

    @Mock
    private Cip113StorageReader cip113StorageReader;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(cip26StorageReader, cip68StorageReader, cip113StorageReader);

        // CIP-26 tokens for POLICY_ID
        TokenMetadata cip26Token = new TokenMetadata();
        cip26Token.setSubject(POLICY_ID + "4e5554");
        cip26Token.setPolicy(POLICY_ID);
        cip26Token.setName("NUT");
        cip26Token.setTicker("NUT");
        cip26Token.setDecimals(6L);
        when(cip26StorageReader.findByPolicy(POLICY_ID)).thenReturn(List.of(cip26Token));
        when(cip26StorageReader.findByPolicies(any())).thenReturn(List.of(cip26Token));

        // CIP-68 tokens for POLICY_ID
        MetadataReferenceNft cip68Token = MetadataReferenceNft.builder()
                .policyId(POLICY_ID)
                .assetName("000643b0464c4454")
                .slot(100L)
                .name("FLDT")
                .ticker("FLDT")
                .decimals(0L)
                .description("A CIP-68 token")
                .version(1L)
                .datum("deadbeef")
                .build();
        when(cip68StorageReader.findLatestByPolicyIds(any())).thenReturn(List.of(cip68Token));

        // CIP-113: POLICY_ID is programmable
        when(cip113StorageReader.findByPolicyId(POLICY_ID))
                .thenReturn(Optional.of(new ProgrammableTokenCip113("script1", "script2", null)));
        when(cip113StorageReader.findByPolicyIds(anyCollection()))
                .thenReturn(Map.of(POLICY_ID, new ProgrammableTokenCip113("script1", "script2", null)));

        // Unknown policy
        when(cip26StorageReader.findByPolicy(UNKNOWN_POLICY)).thenReturn(List.of());
        when(cip113StorageReader.findByPolicyId(UNKNOWN_POLICY)).thenReturn(Optional.empty());
    }

    @Nested
    @DisplayName("findByPolicyId")
    class FindByPolicyId {

        @Test
        void returnsTokensAndExtensions() {
            Optional<PolicyResponse> result = policyService.findByPolicyId(POLICY_ID);

            assertThat(result).isPresent();
            PolicyResponse policy = result.get();
            assertThat(policy.policyId()).isEqualTo(POLICY_ID);
            assertThat(policy.tokens()).hasSize(2);
            assertThat(policy.extensions()).containsKey("cip113");
        }

        @Test
        void cip68OverridesCip26ForSameSubject() {
            // If CIP-26 and CIP-68 have same subject, CIP-68 wins
            TokenMetadata cip26Dup = new TokenMetadata();
            String dupSubject = POLICY_ID + "000643b0464c4454";
            cip26Dup.setSubject(dupSubject);
            cip26Dup.setPolicy(POLICY_ID);
            cip26Dup.setName("OLD_NAME");
            when(cip26StorageReader.findByPolicy(POLICY_ID)).thenReturn(List.of(cip26Dup));

            Optional<PolicyResponse> result = policyService.findByPolicyId(POLICY_ID);

            assertThat(result).isPresent();
            // The CIP-68 token should override the CIP-26 one
            assertThat(result.get().tokens()).anyMatch(t -> t.source().equals("CIP_68") && t.name().equals("FLDT"));
        }

        @Test
        void returnsEmptyForUnknownPolicy() {
            when(cip68StorageReader.findLatestByPolicyIds(List.of(UNKNOWN_POLICY))).thenReturn(List.of());

            Optional<PolicyResponse> result = policyService.findByPolicyId(UNKNOWN_POLICY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByPolicyIds (batch)")
    class FindByPolicyIds {

        @Test
        void returnsBatchResults() {
            List<PolicyResponse> results = policyService.findByPolicyIds(List.of(POLICY_ID));

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().policyId()).isEqualTo(POLICY_ID);
            assertThat(results.getFirst().tokens()).isNotEmpty();
        }

        @Test
        void returnsEmptyForEmptyInput() {
            assertThat(policyService.findByPolicyIds(List.of())).isEmpty();
        }
    }
}
