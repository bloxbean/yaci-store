package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TokenQueryService")
class TokenQueryServiceTest {

    private static final String KNOWN_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848";
    private static final String UNKNOWN_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843";
    private static final String FLDT_SUBJECT = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454";

    private static final List<QueryPriority> DEFAULT_PRIORITY = List.of(QueryPriority.CIP_68, QueryPriority.CIP_26);

    @Mock
    private Cip26StorageReader cip26StorageReader;

    @Mock
    private Cip68StorageReader cip68StorageReader;

    private TokenQueryService service;

    @BeforeEach
    void setUp() {
        service = new TokenQueryService(cip26StorageReader, cip68StorageReader);

        // Unknown subject: no CIP-26, no CIP-68
        when(cip26StorageReader.findBySubject(UNKNOWN_SUBJECT)).thenReturn(Optional.empty());
        when(cip68StorageReader.findBySubject(eq(UNKNOWN_SUBJECT), any())).thenReturn(Optional.empty());

        // Known subject: CIP-26 + CIP-68
        Cip26Metadata knownCip26 = new Cip26Metadata();
        knownCip26.setSubject(KNOWN_SUBJECT);
        knownCip26.setName("nutcoin");
        knownCip26.setDescription("The legendary Nutcoin, the first native asset minted on Cardano.");
        knownCip26.setUrl("https://fivebinaries.com/nutcoin");
        knownCip26.setProperties(new Mapping(
                KNOWN_SUBJECT,
                new Item(0, "https://fivebinaries.com/nutcoin", List.of()),
                new Item(0, "nutcoin", List.of()),
                null,
                null,
                null,
                null,
                new Item(0, "The legendary Nutcoin, the first native asset minted on Cardano.", List.of())));
        when(cip26StorageReader.findBySubject(KNOWN_SUBJECT)).thenReturn(Optional.of(knownCip26));
        when(cip26StorageReader.findLogoBySubject(KNOWN_SUBJECT)).thenReturn(Optional.empty());

        // CIP-68 for known subject (name and url overridden)
        when(cip68StorageReader.findBySubject(eq(KNOWN_SUBJECT), any()))
                .thenReturn(Optional.of(new FungibleTokenMetadata(null, null, null, "NUTCOIN", null, "https://cip68-url.com/nutcoin", null)));

        // FLDT: CIP-26 data. The Mapping JSON is what populates the show_cips_details=true
        // standards.cip26 envelope (signatures + sequenceNumber + value), so set it here
        // alongside the entity's flat columns.
        Cip26Metadata fldtCip26 = new Cip26Metadata();
        fldtCip26.setSubject(FLDT_SUBJECT);
        fldtCip26.setName("FLDT");
        fldtCip26.setTicker("FLDT");
        fldtCip26.setUrl("https://fluidtokens.com");
        fldtCip26.setProperties(new Mapping(
                FLDT_SUBJECT,
                new Item(0, "https://fluidtokens.com", List.of()),
                new Item(0, "FLDT", List.of()),
                new Item(0, "FLDT", List.of()),
                null,
                null,
                null,
                null));
        when(cip26StorageReader.findBySubject(FLDT_SUBJECT)).thenReturn(Optional.of(fldtCip26));
        when(cip26StorageReader.findLogoBySubject(FLDT_SUBJECT)).thenReturn(Optional.empty());

        // FLDT: CIP-68 data
        when(cip68StorageReader.findBySubject(eq(FLDT_SUBJECT), any()))
                .thenReturn(Optional.of(new FungibleTokenMetadata(null,
                        "The official token of FluidTokens, a leading DeFi ecosystem fueled by innovation and community backing.",
                        null, "FLDT", "FLDT", null, null)));

        // CIP-26 batch: findBySubjects returns all known metadata
        when(cip26StorageReader.findBySubjects(any())).thenAnswer(invocation -> {
            List<String> subjects = invocation.getArgument(0);
            return subjects.stream()
                    .map(s -> cip26StorageReader.findBySubject(s).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        });

        // CIP-26 batch: findLogosBySubjects returns empty (no logos mocked)
        when(cip26StorageReader.findLogosBySubjects(any())).thenReturn(Map.of());

        // CIP-68 batch: findBySubjects falls back to the per-subject mocks above
        when(cip68StorageReader.findBySubjects(any(), any())).thenAnswer(invocation -> {
            List<String> subjects = invocation.getArgument(0);
            List<String> props = invocation.getArgument(1);
            java.util.Map<String, FungibleTokenMetadata> result = new java.util.HashMap<>();
            for (String s : subjects) {
                cip68StorageReader.findBySubject(s, props).ifPresent(md -> result.put(s, md));
            }
            return result;
        });
    }

    @Nested
    @DisplayName("querySubject - unknown subject")
    class UnknownSubject {

        @Test
        void returnsEmptyForNonExistingSubject() {
            assertThat(service.querySubject(UNKNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false)).isEmpty();
        }
    }

    @Nested
    @DisplayName("querySubject - CIP-26 + CIP-68 token")
    class Cip26AndCip68Token {

        @Test
        void returnsMetadataWithCorrectSubject() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false).orElseThrow();
            assertThat(result.subject()).isEqualTo(KNOWN_SUBJECT);
        }

        @Test
        void cip68TakesPriorityForNameAndUrl() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false).orElseThrow();

            assertThat(result.metadata().name().value()).isEqualTo("NUTCOIN");
            assertThat(result.metadata().name().source()).isEqualTo("CIP_68");
            assertThat(result.metadata().url().value()).isEqualTo("https://cip68-url.com/nutcoin");
            assertThat(result.metadata().url().source()).isEqualTo("CIP_68");
            assertThat(result.metadata().description().value()).isEqualTo("The legendary Nutcoin, the first native asset minted on Cardano.");
            assertThat(result.metadata().description().source()).isEqualTo("CIP_26");
        }
    }

    @Nested
    @DisplayName("querySubject - FLDT (CIP-26 + CIP-68)")
    class FldtToken {

        @Test
        void mergesMetadataWithCip68Priority() {
            Subject result = service.querySubject(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), false).orElseThrow();
            assertThat(result.metadata().name().value()).isEqualTo("FLDT");
            assertThat(result.metadata().name().source()).isEqualTo("CIP_68");
            assertThat(result.metadata().url().value()).isEqualTo("https://fluidtokens.com");
            assertThat(result.metadata().url().source()).isEqualTo("CIP_26");
        }

        @Test
        void showCipsDetailsReturnsStandards() {
            Subject result = service.querySubject(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), true).orElseThrow();
            assertThat(result.standards()).isNotNull();
            assertThat(result.standards().cip26()).isNotNull();
            // standards.cip26 now uses CF-compatible envelopes — value lives under .getName().getValue()
            assertThat(result.standards().cip26().getName()).isNotNull();
            assertThat(result.standards().cip26().getName().getValue()).isEqualTo("FLDT");
            // Mock signatures = List.of() — empty list passes through as []. CF preprod
            // is inconsistent (some subjects emit null for missing JSON field, others emit
            // [] for "signatures": []), so yaci must preserve the source distinction
            // verbatim rather than normalising. The null-source path is not exercised here.
            assertThat(result.standards().cip26().getName().getSignatures()).isEmpty();
            // Mock sequenceNumber = 0 — a real value passes through unchanged. The null-
            // source path (sequenceNumber absent in registry) is covered by sequenceNumberOf
            // returning null; not exercised here because every FLDT mock Item sets it.
            assertThat(result.standards().cip26().getName().getSequenceNumber())
                    .isEqualByComparingTo(java.math.BigDecimal.ZERO);
            assertThat(result.standards().cip26().getTicker().getValue()).isEqualTo("FLDT");
            assertThat(result.standards().cip26().getUrl().getValue()).isEqualTo("https://fluidtokens.com");
            // additionalProperties is always {} on CF for tokens with no extra fields — match that.
            assertThat(result.standards().cip26().getAdditionalProperties()).isEmpty();
            assertThat(result.standards().cip68()).isNotNull();
            assertThat(result.standards().cip68().name()).isEqualTo("FLDT");
        }

        @Test
        void cip26PriorityOverrideChangesMergeOrder() {
            List<QueryPriority> cip26First = List.of(QueryPriority.CIP_26, QueryPriority.CIP_68);
            Subject result = service.querySubject(FLDT_SUBJECT, cip26First, List.of(), false).orElseThrow();
            assertThat(result.metadata().name().value()).isEqualTo("FLDT");
            assertThat(result.metadata().name().source()).isEqualTo("CIP_26");
            assertThat(result.metadata().url().value()).isEqualTo("https://fluidtokens.com");
            assertThat(result.metadata().url().source()).isEqualTo("CIP_26");
        }
    }

    @Nested
    @DisplayName("validation errors and tokens that break their CIP")
    class ValidationErrors {

        private static final String BROKEN_SUBJECT = "aa".repeat(28) + "0014df10" + "4e4654";

        private final ValidationError cip68Error = ValidationError.mandatory("CIP-68", "333", "description", "missing_value",
                "description field is missing a mandatory value");
        private final ValidationError cip26Error = ValidationError.mandatory("CIP-26", null, "name", "missing_value",
                "name field is missing a mandatory value");

        private void cip68Only(FungibleTokenMetadata metadata) {
            when(cip26StorageReader.findBySubject(BROKEN_SUBJECT)).thenReturn(Optional.empty());
            when(cip68StorageReader.findBySubject(eq(BROKEN_SUBJECT), any())).thenReturn(Optional.of(metadata));
        }

        @Test
        void aTokenWithoutNameAndDescriptionIsReturnedWithItsErrors() {
            // it used to be a 404: a result needed both name and description
            cip68Only(new FungibleTokenMetadata(null, null, null, null, "NFT", null, 1L, List.of(cip68Error)));

            Subject result = service.querySubject(BROKEN_SUBJECT, DEFAULT_PRIORITY, List.of(), false).orElseThrow();

            assertThat(result.metadata().name()).isNull();
            assertThat(result.metadata().ticker().value()).isEqualTo("NFT");
            assertThat(result.validationErrors()).containsExactly(cip68Error);
        }

        @Test
        void aTokenWithErrorsAndNoValuesAtAllIsStillReturned() {
            cip68Only(new FungibleTokenMetadata(null, null, null, null, null, null, null, List.of(cip68Error)));

            Subject result = service.querySubject(BROKEN_SUBJECT, DEFAULT_PRIORITY, List.of(), true).orElseThrow();

            assertThat(result.metadata().isEmpty()).isTrue();
            assertThat(result.validationErrors()).containsExactly(cip68Error);
            assertThat(result.standards().cip68().validationErrors()).containsExactly(cip68Error);
        }

        @Test
        void theErrorsOfBothStandardsAreMergedInPriorityOrder() {
            Cip26Metadata cip26 = new Cip26Metadata();
            cip26.setSubject(BROKEN_SUBJECT);
            cip26.setValidationErrors(List.of(cip26Error));
            cip26.setProperties(new Mapping(BROKEN_SUBJECT, null, null, null, null, null, null, null));
            when(cip26StorageReader.findBySubject(BROKEN_SUBJECT)).thenReturn(Optional.of(cip26));
            when(cip26StorageReader.findLogoBySubject(BROKEN_SUBJECT)).thenReturn(Optional.empty());
            when(cip68StorageReader.findBySubject(eq(BROKEN_SUBJECT), any()))
                    .thenReturn(Optional.of(new FungibleTokenMetadata(null, null, null, "N", null, null, 1L, List.of(cip68Error))));

            assertThat(service.querySubject(BROKEN_SUBJECT, List.of(QueryPriority.CIP_68, QueryPriority.CIP_26), List.of(), false)
                    .orElseThrow().validationErrors()).containsExactly(cip68Error, cip26Error);
            assertThat(service.querySubject(BROKEN_SUBJECT, List.of(QueryPriority.CIP_26, QueryPriority.CIP_68), List.of(), false)
                    .orElseThrow().validationErrors()).containsExactly(cip26Error, cip68Error);
        }

        @Test
        void aTokenThatFollowsItsCipsHasAnEmptyList() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false).orElseThrow();

            assertThat(result.validationErrors()).isEmpty();
        }

        @Test
        void aPropertyFilterWithoutNameAndDescriptionStillReturnsTheToken() {
            cip68Only(new FungibleTokenMetadata(null, null, null, "N", "TKR", null, 1L, List.of()));

            Subject result = service.querySubject(BROKEN_SUBJECT, DEFAULT_PRIORITY, List.of("ticker"), false).orElseThrow();

            assertThat(result.metadata().ticker().value()).isEqualTo("TKR");
        }

        @Test
        void theBatchReturnsATokenWithErrorsToo() {
            cip68Only(new FungibleTokenMetadata(null, null, null, null, null, null, null, List.of(cip68Error)));
            TokenQueryService.BatchPrefetchData prefetch = service.prefetchBatch(List.of(BROKEN_SUBJECT), List.of());

            assertThat(service.querySubjectBatch(BROKEN_SUBJECT, DEFAULT_PRIORITY, List.of(), prefetch, false))
                    .hasValueSatisfying(subject -> assertThat(subject.validationErrors()).containsExactly(cip68Error));
        }
    }

    @Nested
    @DisplayName("querySubjectBatch")
    class BatchQuery {

        @Test
        void returnsMergedMetadataForMultipleSubjects() {
            TokenQueryService.BatchPrefetchData prefetchData = service.prefetchBatch(
                    List.of(KNOWN_SUBJECT, FLDT_SUBJECT), List.of());

            Subject known = service.querySubjectBatch(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), prefetchData, false).orElseThrow();
            Subject fldt = service.querySubjectBatch(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), prefetchData, false).orElseThrow();

            assertThat(known.subject()).isEqualTo(KNOWN_SUBJECT);
            assertThat(known.metadata().url().value()).isEqualTo("https://cip68-url.com/nutcoin");

            assertThat(fldt.subject()).isEqualTo(FLDT_SUBJECT);
            assertThat(fldt.metadata().name().source()).isEqualTo("CIP_68");
        }

        @Test
        void unknownSubjectIsNotReturned() {
            TokenQueryService.BatchPrefetchData prefetchData = service.prefetchBatch(
                    List.of(UNKNOWN_SUBJECT), List.of());

            assertThat(service.querySubjectBatch(UNKNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), prefetchData, false)).isEmpty();
        }
    }
}
