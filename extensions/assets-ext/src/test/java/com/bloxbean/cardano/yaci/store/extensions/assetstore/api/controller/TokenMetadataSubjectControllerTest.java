package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.controller;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.StringProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.TokenType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService.BatchPrefetchData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenMetadataSubjectController")
class TokenMetadataSubjectControllerTest {

    private static final String VALID_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848";
    private static final String VALID_SUBJECT_2 = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TokenQueryService tokenQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AssetsExtStoreProperties properties = new AssetsExtStoreProperties();
        TokenMetadataSubjectController controller = new TokenMetadataSubjectController(tokenQueryService, properties);
        controller.init(); // @PostConstruct needs to be called manually outside Spring context

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .addPlaceholderValue("apiPrefix", "/api/v1")
                .build();
    }

    private static Subject subjectWithName(String subject, String name) {
        Metadata metadata = Metadata.builder()
                .name(new StringProperty(name, "CIP_26"))
                .description(new StringProperty("a token", "CIP_26"))
                .build();
        return new Subject(subject, TokenType.NATIVE, metadata, null, null);
    }

    @Nested
    @DisplayName("GET /tokens/subject/{subject}")
    class GetSubject {

        @Test
        void returns200WithMergedMetadataWhenFound() throws Exception {
            when(tokenQueryService.querySubject(eq(VALID_SUBJECT), anyList(), anyList(), anyBoolean()))
                    .thenReturn(Optional.of(subjectWithName(VALID_SUBJECT, "Nutcoin")));

            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", VALID_SUBJECT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subject.subject").value(VALID_SUBJECT))
                    .andExpect(jsonPath("$.subject.metadata.name.value").value("Nutcoin"))
                    .andExpect(jsonPath("$.queryPriority[0]").value("CIP_68"))
                    .andExpect(jsonPath("$.queryPriority[1]").value("CIP_26"));
        }

        @Test
        void returns404WhenSubjectHasNoValidMetadata() throws Exception {
            when(tokenQueryService.querySubject(anyString(), anyList(), anyList(), anyBoolean()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", VALID_SUBJECT))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400WhenSubjectTooShort() throws Exception {
            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", "ab"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenSubjectNotHex() throws Exception {
            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", "zz" + VALID_SUBJECT.substring(2)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenPropertyFilterMissesRequiredFields() throws Exception {
            // When filtering properties, 'name' and 'description' are required.
            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", VALID_SUBJECT)
                            .param("property", "ticker"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void acceptsPropertyFilterWithRequiredFields() throws Exception {
            when(tokenQueryService.querySubject(anyString(), anyList(), anyList(), anyBoolean()))
                    .thenReturn(Optional.of(subjectWithName(VALID_SUBJECT, "Nutcoin")));

            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", VALID_SUBJECT)
                            .param("property", "name")
                            .param("property", "description"))
                    .andExpect(status().isOk());
        }

        @Test
        void usesRequestedQueryPriority() throws Exception {
            when(tokenQueryService.querySubject(anyString(), anyList(), anyList(), anyBoolean()))
                    .thenReturn(Optional.of(subjectWithName(VALID_SUBJECT, "Nutcoin")));

            mockMvc.perform(get("/api/v1/tokens/subject/{subject}", VALID_SUBJECT)
                            .param("query_priority", "CIP_26", "CIP_68"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.queryPriority[0]").value("CIP_26"))
                    .andExpect(jsonPath("$.queryPriority[1]").value("CIP_68"));
        }
    }

    @Nested
    @DisplayName("POST /tokens/subject/query")
    class BatchQuery {

        @Test
        void returns200WithBatchResultsWhenValid() throws Exception {
            when(tokenQueryService.prefetchBatch(anyList(), anyList()))
                    .thenReturn(new BatchPrefetchData(Map.of(), Map.of(), Map.of(), Map.of()));
            when(tokenQueryService.querySubjectBatch(eq(VALID_SUBJECT), anyList(), anyList(), any(), anyBoolean()))
                    .thenReturn(subjectWithName(VALID_SUBJECT, "Nutcoin"));
            when(tokenQueryService.querySubjectBatch(eq(VALID_SUBJECT_2), anyList(), anyList(), any(), anyBoolean()))
                    .thenReturn(subjectWithName(VALID_SUBJECT_2, "FLDT"));

            String body = objectMapper.writeValueAsString(Map.of(
                    "subjects", List.of(VALID_SUBJECT, VALID_SUBJECT_2)));

            mockMvc.perform(post("/api/v1/tokens/subject/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subjects.length()").value(2))
                    .andExpect(jsonPath("$.subjects[0].metadata.name.value").value("Nutcoin"))
                    .andExpect(jsonPath("$.subjects[1].metadata.name.value").value("FLDT"));
        }

        @Test
        void filtersOutSubjectsWithEmptyMetadata() throws Exception {
            when(tokenQueryService.prefetchBatch(anyList(), anyList()))
                    .thenReturn(new BatchPrefetchData(Map.of(), Map.of(), Map.of(), Map.of()));
            when(tokenQueryService.querySubjectBatch(eq(VALID_SUBJECT), anyList(), anyList(), any(), anyBoolean()))
                    .thenReturn(new Subject(VALID_SUBJECT, TokenType.NATIVE, Metadata.empty(), null, null));

            String body = objectMapper.writeValueAsString(Map.of("subjects", List.of(VALID_SUBJECT)));

            mockMvc.perform(post("/api/v1/tokens/subject/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subjects.length()").value(0));
        }

        @Test
        void returns400WhenSubjectsListEmpty() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("subjects", List.of()));

            mockMvc.perform(post("/api/v1/tokens/subject/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenSubjectsListExceedsMaxSize() throws Exception {
            List<String> tooMany = new java.util.ArrayList<>();
            for (int i = 0; i < 101; i++) {
                tooMany.add(VALID_SUBJECT);
            }
            String body = objectMapper.writeValueAsString(Map.of("subjects", tooMany));

            mockMvc.perform(post("/api/v1/tokens/subject/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenAnySubjectInBatchIsInvalid() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "subjects", List.of(VALID_SUBJECT, "invalid-not-hex")));

            mockMvc.perform(post("/api/v1/tokens/subject/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400WhenPropertyFilterMissesRequiredFields() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "subjects", List.of(VALID_SUBJECT),
                    "properties", List.of("ticker")));

            mockMvc.perform(post("/api/v1/tokens/subject/query")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
