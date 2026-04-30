package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip68MetadataController")
class Cip68MetadataControllerTest {

    private static final String VALID_POLICY_ID = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e";
    private static final String RAW_ASSET_NAME = "464c4454"; // "FLDT" in hex
    private static final String REF_NFT_ASSET_NAME = Cip68Constants.REFERENCE_TOKEN_PREFIX + RAW_ASSET_NAME;
    private static final String VALID_SUBJECT = VALID_POLICY_ID + "0014df10" + RAW_ASSET_NAME;

    @Mock
    private Cip68StorageReader cip68StorageReader;

    @InjectMocks
    private Cip68MetadataController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .addPlaceholderValue("apiPrefix", "/api/v1")
                .build();
    }

    @Nested
    @DisplayName("GET /tokens/cip68/ft/{subject}")
    class GetBySubject {

        @Test
        void returns200WithMetadataWhenFound() throws Exception {
            FungibleTokenMetadata metadata = new FungibleTokenMetadata(
                    6L, "FLDT fungible token", "logo", "FLDT", "FLDT", "https://fluidtokens.com", 1L);
            when(cip68StorageReader.findBySubject(VALID_SUBJECT)).thenReturn(Optional.of(metadata));

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{subject}", VALID_SUBJECT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("FLDT"))
                    .andExpect(jsonPath("$.decimals").value(6));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(cip68StorageReader.findBySubject(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{subject}", VALID_SUBJECT))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400WhenSubjectTooShort() throws Exception {
            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{subject}", "ab"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenSubjectNotHex() throws Exception {
            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{subject}", "zz" + VALID_POLICY_ID.substring(2)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /tokens/cip68/ft/{policyId}/{rawAssetName}")
    class GetByPolicyAndRawAssetName {

        @Test
        void returns200WithMetadataWhenFound() throws Exception {
            FungibleTokenMetadata metadata = new FungibleTokenMetadata(
                    6L, "FLDT fungible token", "logo", "FLDT", "FLDT", "https://fluidtokens.com", 1L);
            when(cip68StorageReader.findByPolicyIdAndAssetName(eq(VALID_POLICY_ID), eq(REF_NFT_ASSET_NAME)))
                    .thenReturn(Optional.of(metadata));

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{policyId}/{rawAssetName}",
                            VALID_POLICY_ID, RAW_ASSET_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("FLDT"));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(cip68StorageReader.findByPolicyIdAndAssetName(any(), any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{policyId}/{rawAssetName}",
                            VALID_POLICY_ID, RAW_ASSET_NAME))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400WhenPolicyIdWrongLength() throws Exception {
            String shortPolicyId = VALID_POLICY_ID.substring(0, 55);

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{policyId}/{rawAssetName}",
                            shortPolicyId, RAW_ASSET_NAME))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenRawAssetNameExceeds56Chars() throws Exception {
            // CIP-68 specific: raw asset name is capped at 56 hex chars because the controller
            // prepends the 8-char label automatically (total must stay within 64 hex chars).
            String tooLong = "a".repeat(57);

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{policyId}/{rawAssetName}",
                            VALID_POLICY_ID, tooLong))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void acceptsRawAssetNameUpTo56Chars() throws Exception {
            String max = "a".repeat(56);
            when(cip68StorageReader.findByPolicyIdAndAssetName(any(), any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{policyId}/{rawAssetName}",
                            VALID_POLICY_ID, max))
                    .andExpect(status().isNotFound()); // passes validation; storage returns empty
        }

        @Test
        void returns400WhenRawAssetNameNotHex() throws Exception {
            mockMvc.perform(get("/api/v1/tokens/cip68/ft/{policyId}/{rawAssetName}",
                            VALID_POLICY_ID, "xyz"))
                    .andExpect(status().isBadRequest());
        }
    }
}
