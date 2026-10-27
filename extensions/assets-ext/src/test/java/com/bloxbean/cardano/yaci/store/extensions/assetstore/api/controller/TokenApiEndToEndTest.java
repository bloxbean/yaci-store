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

import static org.assertj.core.api.Assertions.assertThat;
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
 * NFTs are not served, and the JSON a client reads, including the {@code metadata.valid} flag it always had.
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
    void aTokenThatFollowsItsCipIsReturnedWithNoErrorsAndIsValid() throws Exception {
        store(row(333).name("Coin").description("A coin").ticker("COIN").decimals(6L).logo("ipfs://QmLogo")
                .validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.name.value").value("Coin"))
                .andExpect(jsonPath("$.subject.metadata.logo.value").value("ipfs://QmLogo"))
                .andExpect(jsonPath("$.subject.metadata.valid").value(true))
                .andExpect(jsonPath("$.subject.validation_errors").isArray())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(0));
    }

    @Test
    void aTokenThatBreaksItsCipIsStillReturnedWithItsErrors() throws Exception {
        // nothing is wrong with the datum itself; a 333 token needs a description, reported when it is served
        store(row(333).name("Coin").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.name.value").value("Coin"))
                .andExpect(jsonPath("$.subject.metadata.description").doesNotExist())
                .andExpect(jsonPath("$.subject.metadata.valid").value(false))
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(1))
                .andExpect(jsonPath("$.subject.validation_errors[0].sub_standard").value("333"))
                .andExpect(jsonPath("$.subject.validation_errors[0].field_category").value("MANDATORY"))
                .andExpect(jsonPath("$.subject.validation_errors[0].code").value("missing_value"));
    }

    @Test
    void validMeansNoErrorsAtAllAnOptionalErrorMakesTheTokenInvalidToo() throws Exception {
        ValidationError optional = ValidationError.optional("CIP-68", null, "logo", "invalid_uri_scheme",
                "logo 'Qm…' is not a URI with one of the schemes CIP-68 allows (https, ipfs, ar, data)");
        store(row(333).name("Coin").description("A coin").logo("QmBareHash").validationErrors(List.of(optional)).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.name.value").value("Coin"))
                .andExpect(jsonPath("$.subject.metadata.description.value").value("A coin"))
                .andExpect(jsonPath("$.subject.metadata.valid").value(false))
                .andExpect(jsonPath("$.subject.validation_errors[0].field_category").value("OPTIONAL"));
    }

    @Test
    void aRowIndexedBeforeTheColumnExistedHasNoErrorsFieldAndKeepsTheOldRuleForValid() throws Exception {
        store(row(333).name("Old").description("Indexed before validation_errors").build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.validation_errors").doesNotExist())
                .andExpect(jsonPath("$.subject.metadata.valid").value(true));
    }

    @Test
    void anOldRowWithoutADescriptionIsNotValidByTheOldRule() throws Exception {
        store(row(333).name("Old").build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.validation_errors").doesNotExist())
                .andExpect(jsonPath("$.subject.metadata.valid").value(false));
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
        store(row(444).name("Edition").image("iagon://x").validationErrors(List.of()).build());

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
                .andExpect(jsonPath("$.subject.metadata.valid").value(true))
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
        store(row(444).image("ipfs://QmImage").validationErrors(List.of()).build());

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
        Cip68Metadata ft = Cip68Metadata.builder().policyId(POLICY).assetName(REF).slot(100L).txHash("b".repeat(64)).txIndex(0)
                .label(333).version(1L).name("Coin").validationErrors(List.of()).build();
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

    // the label of the subject asked for decides what is required and whether the image is the logo, not the label the
    // row was stored with, which is guessed for a metadata update made without the user token

    @Test
    void aFungibleTokenStoredAsAnNftByAGuessIsCheckedAsAFungibleToken() throws Exception {
        // name and image, no description, ticker, logo or decimals: an update without the user token is stored as 222
        store(row(222).name("Coin").image("ipfs://QmCoin").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", FT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logo").doesNotExist())
                .andExpect(jsonPath("$.validation_errors.length()").value(1))
                .andExpect(jsonPath("$.validation_errors[0].sub_standard").value("333"))
                .andExpect(jsonPath("$.validation_errors[0].field_name").value("description"))
                .andExpect(jsonPath("$.validation_errors[0].field_category").value("MANDATORY"));
        assertThat(statusOf("/api/v1/tokens/cip68/ft/{s}", "MANDATORY_ONLY")).isEqualTo(404);
    }

    @Test
    void aRichFungibleTokenStoredAsAnNftByAGuessKeepsItsImageAsTheLogo() throws Exception {
        // no decimals: an update without the user token is stored as 222
        store(row(222).name("Rich").description("A rich token").image("ipfs://QmRich").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", RFT_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logo").value("ipfs://QmRich"))
                .andExpect(jsonPath("$.validation_errors.length()").value(0));
    }

    @Test
    void theFungibleAndRichFungibleTokenOfOneReferenceNftAreEachCheckedForTheirLabel() throws Exception {
        // minted together: stored as 333, no description, an image
        store(row(333).name("Both").image("ipfs://QmBoth").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", FT_SUBJECT))
                .andExpect(jsonPath("$.logo").doesNotExist())
                .andExpect(jsonPath("$.validation_errors[0].sub_standard").value("333"));
        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", RFT_SUBJECT))
                .andExpect(jsonPath("$.logo").value("ipfs://QmBoth"))
                .andExpect(jsonPath("$.validation_errors.length()").value(0));
        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", RFT_SUBJECT).param("validation", "MANDATORY_ONLY"))
                .andExpect(status().isOk());
    }

    @Test
    void theBatchReturnsBothTokensOfOneReferenceNft() throws Exception {
        store(row(333).name("Both").image("ipfs://QmBoth").validationErrors(List.of()).build());

        mockMvc.perform(post("/api/v1/tokens/subject/query").contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(Map.of("subjects", List.of(FT_SUBJECT, RFT_SUBJECT)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.subjects[?(@.subject == '" + FT_SUBJECT + "')].validation_errors[0].sub_standard").value("333"))
                .andExpect(jsonPath("$.subjects[?(@.subject == '" + RFT_SUBJECT + "')].metadata.logo.value").value("ipfs://QmBoth"))
                .andExpect(jsonPath("$.subjects[?(@.subject == '" + RFT_SUBJECT + "')].validation_errors.length()").value(0));
    }

    // ?validation=

    private static final ValidationError OPTIONAL_ERROR = ValidationError.optional("CIP-68", null, "logo", "invalid_uri_scheme", "bad logo");

    // a 333 "Coin" stored without a description has a mandatory error when it is served

    private int statusOf(String path, String validation) throws Exception {
        return mockMvc.perform(get(path, FT_SUBJECT).param("validation", validation)).andReturn().getResponse().getStatus();
    }

    @Test
    void validationAllIsTheDefaultAndReturnsATokenWithAMandatoryError() throws Exception {
        store(row(333).name("Coin").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("validation", "ALL")).andExpect(status().isOk());
    }

    @Test
    void validationMandatoryOnlyIs404ForAMandatoryErrorAndKeepsOptionalOnes() throws Exception {
        store(row(333).name("Coin").validationErrors(List.of()).build());
        assertThat(statusOf("/api/v1/tokens/subject/{s}", "MANDATORY_ONLY")).isEqualTo(404);

        store(row(333).name("Coin").description("d").logo("QmBare").validationErrors(List.of(OPTIONAL_ERROR)).build());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("validation", "MANDATORY_ONLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(1));
    }

    @Test
    void validationStrictIs404ForAnyErrorAndKeepsACleanToken() throws Exception {
        store(row(333).name("Coin").description("d").logo("QmBare").validationErrors(List.of(OPTIONAL_ERROR)).build());
        assertThat(statusOf("/api/v1/tokens/subject/{s}", "STRICT")).isEqualTo(404);

        store(row(333).name("Coin").description("d").validationErrors(List.of()).build());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("validation", "STRICT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.valid").value(true));
    }

    @Test
    void anOldRowIsJudgedByNameAndDescriptionWhenAScopeIsAsked() throws Exception {
        store(row(333).name("Old").description("d").build());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("validation", "STRICT")).andExpect(status().isOk());

        store(row(333).name("Old").build());
        assertThat(statusOf("/api/v1/tokens/subject/{s}", "STRICT")).isEqualTo(404);
        assertThat(statusOf("/api/v1/tokens/subject/{s}", "ALL")).isEqualTo(200);
    }

    @Test
    void anOldRowIsJudgedOnlyByTheNameAndDescriptionAskedFor() throws Exception {
        // review of #1220: the old rule ran on the filtered response, so ?property=ticker always lost the name and description
        store(row(333).name("Old").description("d").ticker("OLD").build());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "ticker").param("validation", "STRICT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.metadata.valid").value(true));

        // asked for, a missing description still counts
        store(row(333).name("Old").ticker("OLD").build());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "description").param("validation", "STRICT"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "ticker").param("validation", "STRICT"))
                .andExpect(status().isOk());
    }

    @Test
    void anUnknownValidationValueIsABadRequest() throws Exception {
        store(row(333).name("Coin").description("d").validationErrors(List.of()).build());

        assertThat(statusOf("/api/v1/tokens/subject/{s}", "LOOSE")).isEqualTo(400);
    }

    @Test
    void theBatchLeavesOutATokenThatIsNotAccepted() throws Exception {
        store(row(333).name("Coin").validationErrors(List.of()).build());
        String body = new ObjectMapper().writeValueAsString(Map.of("subjects", List.of(FT_SUBJECT)));

        mockMvc.perform(post("/api/v1/tokens/subject/query").param("validation", "MANDATORY_ONLY")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(0));
        mockMvc.perform(post("/api/v1/tokens/subject/query").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(1));
    }

    @Test
    void theCip68FungibleTokenEndpointTakesTheSameParameter() throws Exception {
        store(row(333).name("Coin").validationErrors(List.of()).build());

        assertThat(statusOf("/api/v1/tokens/cip68/ft/{s}", "MANDATORY_ONLY")).isEqualTo(404);
        assertThat(statusOf("/api/v1/tokens/cip68/ft/{s}", "ALL")).isEqualTo(200);
        mockMvc.perform(get("/api/v1/tokens/cip68/ft/{s}", FT_SUBJECT)).andExpect(status().isOk());
    }

    // ?property= limits the errors to the requested properties

    @Test
    void aPropertyFilterReportsOnlyTheErrorsOfTheRequestedProperties() throws Exception {
        store(row(333).name("Coin").ticker("COIN").validationErrors(List.of()).build());

        // the broken field is the description, the client reads only the ticker
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "ticker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(0))
                .andExpect(jsonPath("$.subject.metadata.valid").value(true));
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(1))
                .andExpect(jsonPath("$.subject.metadata.valid").value(false));
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT))
                .andExpect(jsonPath("$.subject.validation_errors.length()").value(1));
    }

    @Test
    void strictAndAPropertyFilterTogetherJudgeOnlyTheRequestedProperties() throws Exception {
        store(row(333).name("Coin").ticker("COIN").validationErrors(List.of()).build());

        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("property", "ticker").param("validation", "STRICT"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tokens/subject/{s}", FT_SUBJECT).param("validation", "STRICT"))
                .andExpect(status().isNotFound());
    }
}
