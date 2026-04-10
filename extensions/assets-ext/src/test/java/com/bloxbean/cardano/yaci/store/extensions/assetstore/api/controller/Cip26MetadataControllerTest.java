package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip26MetadataController")
class Cip26MetadataControllerTest {

    private static final String VALID_POLICY_ID = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e6";
    private static final String VALID_ASSET_NAME = "6362544848";
    private static final String VALID_SUBJECT = VALID_POLICY_ID + VALID_ASSET_NAME;

    @Mock
    private Cip26StorageReader cip26StorageReader;

    @InjectMocks
    private Cip26MetadataController controller;

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
    @DisplayName("GET /tokens/cip26/{subject}")
    class GetBySubject {

        @Test
        void returns200WithMetadataWhenFound() throws Exception {
            TokenMetadata metadata = new TokenMetadata();
            metadata.setSubject(VALID_SUBJECT);
            metadata.setName("Nutcoin");
            metadata.setDescription("A test token");
            when(cip26StorageReader.findBySubject(VALID_SUBJECT)).thenReturn(Optional.of(metadata));

            mockMvc.perform(get("/api/v1/tokens/cip26/{subject}", VALID_SUBJECT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subject").value(VALID_SUBJECT))
                    .andExpect(jsonPath("$.name").value("Nutcoin"));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(cip26StorageReader.findBySubject(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tokens/cip26/{subject}", VALID_SUBJECT))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400WhenSubjectTooShort() throws Exception {
            String tooShort = "ab";

            mockMvc.perform(get("/api/v1/tokens/cip26/{subject}", tooShort))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenSubjectTooLong() throws Exception {
            String tooLong = VALID_POLICY_ID + "a".repeat(65); // 56 + 65 = 121 > max 120

            mockMvc.perform(get("/api/v1/tokens/cip26/{subject}", tooLong))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenSubjectNotHex() throws Exception {
            String nonHex = "zz" + VALID_POLICY_ID.substring(2);

            mockMvc.perform(get("/api/v1/tokens/cip26/{subject}", nonHex))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /tokens/cip26/{policyId}/{assetName}")
    class GetByPolicyAndAssetName {

        @Test
        void returns200WithMetadataWhenFound() throws Exception {
            TokenMetadata metadata = new TokenMetadata();
            metadata.setSubject(VALID_SUBJECT);
            metadata.setName("Nutcoin");
            metadata.setDescription("A test token");
            when(cip26StorageReader.findBySubject(VALID_SUBJECT)).thenReturn(Optional.of(metadata));

            mockMvc.perform(get("/api/v1/tokens/cip26/{policyId}/{assetName}", VALID_POLICY_ID, VALID_ASSET_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Nutcoin"));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            when(cip26StorageReader.findBySubject(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tokens/cip26/{policyId}/{assetName}", VALID_POLICY_ID, VALID_ASSET_NAME))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400WhenPolicyIdWrongLength() throws Exception {
            String shortPolicyId = VALID_POLICY_ID.substring(0, 55);

            mockMvc.perform(get("/api/v1/tokens/cip26/{policyId}/{assetName}", shortPolicyId, VALID_ASSET_NAME))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenPolicyIdNotHex() throws Exception {
            String nonHexPolicyId = "zz" + VALID_POLICY_ID.substring(2);

            mockMvc.perform(get("/api/v1/tokens/cip26/{policyId}/{assetName}", nonHexPolicyId, VALID_ASSET_NAME))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenAssetNameTooLong() throws Exception {
            String tooLong = "a".repeat(65);

            mockMvc.perform(get("/api/v1/tokens/cip26/{policyId}/{assetName}", VALID_POLICY_ID, tooLong))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenAssetNameNotHex() throws Exception {
            mockMvc.perform(get("/api/v1/tokens/cip26/{policyId}/{assetName}", VALID_POLICY_ID, "xyz"))
                    .andExpect(status().isBadRequest());
        }
    }
}
