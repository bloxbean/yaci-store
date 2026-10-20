package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReaderImpl;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReaderImpl;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API as a client sees it, with the real controllers, {@link TokenQueryService}, the storage readers and
 * {@link Cip68TokenService}; only the two repositories are mocked. It pins what the unit tests of each layer cannot: that a
 * token that breaks its CIP is returned with its errors, that rich fungible tokens are served with the image as the logo, that
 * NFTs are not served, and the JSON a client reads: compliance is the list of {@code validation_errors}, there is no separate valid flag.
 */
class TokenApiEndToEndTest {

    private static final String POLICY = "aa".repeat(28);
    private static final String BASE = "4e4654"; // "NFT"
    private static final String REF = "000643b0" + BASE;

    private static final String FT_SUBJECT = POLICY + "0014df10" + BASE;
    private static final String RFT_SUBJECT = POLICY + "001bc280" + BASE;
    private static final String NFT_SUBJECT = POLICY + "000de140" + BASE;

    private Cip68MetadataRepository cip68Repository;
    private Cip26MetadataRepository cip26Repository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cip68Repository = mock(Cip68MetadataRepository.class);
        cip26Repository = mock(Cip26MetadataRepository.class);
        Cip68TokenService cip68TokenService = new Cip68TokenService(cip68Repository);
        TokenQueryService queryService = new TokenQueryService(
                new Cip26StorageReaderImpl(cip26Repository), new Cip68StorageReaderImpl(cip68TokenService));

        AssetsExtStoreProperties properties = new AssetsExtStoreProperties();
        TokenMetadataSubjectController subjectController = new TokenMetadataSubjectController(queryService, properties);
        subjectController.init();
        Cip68MetadataController cip68Controller = new Cip68MetadataController(new Cip68StorageReaderImpl(cip68TokenService));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(subjectController, cip68Controller)
                .setValidator(validator)
                .addPlaceholderValue("apiPrefix", "/api/v1")
                .build();
    }

    private void store(Cip68Metadata row) {
        when(cip68Repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(row.getPolicyId(), row.getAssetName()))
                .thenReturn(Optional.of(row));
        when(cip68Repository.findLatestByConcatenatedKeys(anyList())).thenReturn(List.of(row));
    }

    private static Cip68Metadata.Cip68MetadataBuilder row(int label) {
        return Cip68Metadata.builder().policyId(POLICY).assetName(REF).slot(100L).txHash("b".repeat(64)).txIndex(0)
                .label(label).version(1L);
    }

    @Test
    void aTokenThatFollowsItsCipIsReturnedWithNoErrors() throws Exception {
        store(row(333).name("Coin").description("A coin").ticker("COIN").decimals(6L).logo("ipfs://QmLogo")
                .validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.name.value").value("Coin"))
                .andExpect(jsonPath("$.subject.metadata.logo.value").value("ipfs://QmLogo"))
                .andExpect(jsonPath("$.subject.metadata.valid").doesNotExist())
                .andExpect(jsonPath("$.subject.validation_errors").isArray())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(0));
    }

    @Test
    void aTokenThatBreaksItsCipIsStillReturnedWithItsErrors() throws Exception {
        ValidationError error = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
                "description field is missing a mandatory value");
        store(row(333).name("Coin").validationErrors(List.of(error)).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.name.value").value("Coin"))
                .andExpect(jsonPath("$.subject.metadata.description").doesNotExist())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(1))
                .andExpect(jsonPath("$.subject.validation_errors[0].sub_standard").value("333"))
                .andExpect(jsonPath("$.subject.validation_errors[0].field_category").value("MANDATORY"))
                .andExpect(jsonPath("$.subject.validation_errors[0].code").value("missing_value"));
    }

    @Test
    void aRowIndexedBeforeTheColumnExistedHasAnEmptyList() throws Exception {
        store(row(333).name("Old").description("Indexed before validation_errors").build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.validation_errors").isArray())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(0));
    }

    @Test
    void aRichFungibleTokenIsServedByItsOwnSubjectWithTheImageAsTheLogo() throws Exception {
        store(row(444).name("Edition").image("ipfs://QmImage").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", RFT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.name.value").value("Edition"))
                .andExpect(jsonPath("$.subject.metadata.logo.value").value("ipfs://QmImage"))
                .andExpect(jsonPath("$.subject.metadata.logo.source").value("CIP_68"))
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(0));
    }

    @Test
    void aRichFungibleTokenWithAnImageSchemeTheCipDoesNotAllowIsServedAsWrittenWithTheError() throws Exception {
        ValidationError error = ValidationError.mandatory("CIP-68", "444", "image", "invalid_uri_scheme",
                "image 'iagon://x' is not a URI with one of the schemes CIP-68 allows (https, ipfs, ar, data)");
        store(row(444).name("Edition").image("iagon://x").validationErrors(List.of(error)).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", RFT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.logo.value").value("iagon://x"))
                .andExpect(jsonPath("$.subject.validation_errors[0].code").value("invalid_uri_scheme"))
                .andExpect(jsonPath("$.subject.validation_errors[0].sub_standard").value("444"));
    }

    @Test
    void aRichFungibleTokenWithoutADescriptionHasNoError() throws Exception {
        // description is optional for 444 (CIP-68 requires only name and image)
        store(row(444).name("Edition").image("ipfs://QmImage").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", RFT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.description").doesNotExist())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(0));
    }

    @Test
    void anNftSubjectIsNotServed() throws Exception {
        store(row(222).name("NFT").image("ipfs://QmImage").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", NFT_SUBJECT)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", NFT_SUBJECT)).andExpect(status().isNotFound());
    }

    @Test
    void aSubjectNothingKnowsIsA404() throws Exception {
        when(cip68Repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(POLICY, REF)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT)).andExpect(status().isNotFound());
    }

    @Test
    void theCip68EndpointReturnsTheErrorsToo() throws Exception {
        ValidationError error = ValidationError.mandatory("CIP-68", "444", "name", "missing_value", "name field is missing a mandatory value");
        store(row(444).image("ipfs://QmImage").validationErrors(List.of(error)).build());

        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", RFT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logo").value("ipfs://QmImage"))
                .andExpect(jsonPath("$.validation_errors[0].field_name").value("name"));
    }

    @Test
    void aPropertyFilterWithoutNameAndDescriptionStillReturnsTheToken() throws Exception {
        store(row(333).name("Coin").ticker("COIN").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "ticker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.ticker.value").value("COIN"))
                .andExpect(jsonPath("$.subject.metadata.name").doesNotExist());
    }

    @Test
    void theBatchReturnsFungibleAndRichFungibleTokensWithErrorsAndLeavesOutWhatNothingKnows() throws Exception {
        ValidationError error = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
                "description field is missing a mandatory value");
        Cip68Metadata ft = Cip68Metadata.builder().policyId(POLICY).assetName(REF).slot(100L).txHash("b".repeat(64)).txIndex(0)
                .label(333).version(1L).name("Coin").validationErrors(List.of(error)).build();
        String otherBase = "4f5448";
        Cip68Metadata rft = Cip68Metadata.builder().policyId(POLICY).assetName("000643b0" + otherBase).slot(100L)
                .txHash("c".repeat(64)).txIndex(0).label(444).version(1L).name("Edition").image("ipfs://QmImage")
                .validationErrors(List.of()).build();
        when(cip68Repository.findLatestByConcatenatedKeys(anyList())).thenReturn(List.of(ft, rft));
        String unknown = "bb".repeat(28) + "0014df10" + BASE;

        mockMvc.perform(post("/api/v1/tokens/subject/query").contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of(
                                "subjects", List.of(FT_SUBJECT, POLICY + "001bc280" + otherBase, unknown)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.subjects[0].metadata.name.value").value("Coin"))
                .andExpect(jsonPath("$.subjects[0].validation_errors.length()").value(1))
                .andExpect(jsonPath("$.subjects[1].metadata.logo.value").value("ipfs://QmImage"))
                .andExpect(jsonPath("$.subjects[1].validation_errors.length()").value(0));
    }
}
