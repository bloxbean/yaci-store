package com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.model.v2.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.AssetType;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MetadataV2QueryService")
class MetadataV2QueryServiceTest {

    private static final String KNOWN_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544848";
    private static final String UNKNOWN_SUBJECT = "025146866af908340247fe4e9672d5ac7059f1e8534696b5f920c9e66362544843";
    private static final String FLDT_SUBJECT = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454";
    private static final String FLDT_POLICY_ID = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e";

    private static final List<QueryPriority> DEFAULT_PRIORITY = List.of(QueryPriority.CIP_68, QueryPriority.CIP_26);

    @Mock
    private Cip26StorageReader cip26StorageReader;

    @Mock
    private Cip68StorageReader cip68StorageReader;

    @Mock
    private Cip113StorageReader cip113StorageReader;

    @InjectMocks
    private MetadataV2QueryService service;

    @BeforeEach
    void setUp() {
        // Unknown subject: no CIP-26, no CIP-68
        AssetType unknownAssetType = AssetType.fromUnit(UNKNOWN_SUBJECT);
        when(cip26StorageReader.findBySubject(UNKNOWN_SUBJECT)).thenReturn(Optional.empty());
        when(cip68StorageReader.findBySubject(eq(UNKNOWN_SUBJECT), any())).thenReturn(Optional.empty());

        // Known subject: CIP-26 + CIP-68
        TokenMetadata knownCip26 = new TokenMetadata();
        knownCip26.setSubject(KNOWN_SUBJECT);
        knownCip26.setName("nutcoin");
        knownCip26.setDescription("The legendary Nutcoin, the first native asset minted on Cardano.");
        knownCip26.setUrl("https://fivebinaries.com/nutcoin");
        when(cip26StorageReader.findBySubject(KNOWN_SUBJECT)).thenReturn(Optional.of(knownCip26));
        when(cip26StorageReader.findLogoBySubject(KNOWN_SUBJECT)).thenReturn(Optional.empty());

        // CIP-68 for known subject (name and url overridden)
        when(cip68StorageReader.findBySubject(eq(KNOWN_SUBJECT), any()))
                .thenReturn(Optional.of(new FungibleTokenMetadata(null, null, null, "NUTCOIN", null, "https://cip68-url.com/nutcoin", null)));

        // FLDT: CIP-26 data
        TokenMetadata fldtCip26 = new TokenMetadata();
        fldtCip26.setSubject(FLDT_SUBJECT);
        fldtCip26.setName("FLDT");
        fldtCip26.setTicker("FLDT");
        fldtCip26.setUrl("https://fluidtokens.com");
        when(cip26StorageReader.findBySubject(FLDT_SUBJECT)).thenReturn(Optional.of(fldtCip26));
        when(cip26StorageReader.findLogoBySubject(FLDT_SUBJECT)).thenReturn(Optional.empty());

        // FLDT: CIP-68 data
        when(cip68StorageReader.findBySubject(eq(FLDT_SUBJECT), any()))
                .thenReturn(Optional.of(new FungibleTokenMetadata(null,
                        "The official token of FluidTokens, a leading DeFi ecosystem fueled by innovation and community backing.",
                        null, "FLDT", "FLDT", null, null)));

        // CIP-113: FLDT is programmable
        when(cip113StorageReader.findByPolicyId(FLDT_POLICY_ID))
                .thenReturn(Optional.of(new ProgrammableTokenCip113(
                        "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd",
                        "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344",
                        "eeff0011eeff0011eeff0011eeff0011eeff0011eeff0011eeff0011")));

        // CIP-113: non-programmable tokens return empty
        AssetType knownAssetType = AssetType.fromUnit(KNOWN_SUBJECT);
        when(cip113StorageReader.findByPolicyId(knownAssetType.policyId())).thenReturn(Optional.empty());
        when(cip113StorageReader.findByPolicyId(unknownAssetType.policyId())).thenReturn(Optional.empty());

        // CIP-113 batch
        when(cip113StorageReader.findByPolicyIds(anyCollection()))
                .thenReturn(Map.of(FLDT_POLICY_ID, new ProgrammableTokenCip113(
                        "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd",
                        "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344",
                        "eeff0011eeff0011eeff0011eeff0011eeff0011eeff0011eeff0011")));
    }

    @Nested
    @DisplayName("querySubject - unknown subject")
    class UnknownSubject {

        @Test
        void returnsNullForNonExistingSubject() {
            Subject result = service.querySubject(UNKNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false);
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("querySubject - CIP-26 + CIP-68 token")
    class Cip26AndCip68Token {

        @Test
        void returnsMetadataWithCorrectSubject() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false);
            assertThat(result).isNotNull();
            assertThat(result.subject()).isEqualTo(KNOWN_SUBJECT);
        }

        @Test
        void cip68TakesPriorityForNameAndUrl() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false);

            assertThat(result).isNotNull();
            assertThat(result.metadata().name().value()).isEqualTo("NUTCOIN");
            assertThat(result.metadata().name().source()).isEqualTo("CIP_68");
            assertThat(result.metadata().url().value()).isEqualTo("https://cip68-url.com/nutcoin");
            assertThat(result.metadata().url().source()).isEqualTo("CIP_68");
            assertThat(result.metadata().description().value()).isEqualTo("The legendary Nutcoin, the first native asset minted on Cardano.");
            assertThat(result.metadata().description().source()).isEqualTo("CIP_26");
        }

        @Test
        void nonProgrammableTokenShouldNotHaveExtensions() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false);
            assertThat(result).isNotNull();
            assertThat(result.extensions()).isNull();
        }
    }

    @Nested
    @DisplayName("querySubject - FLDT (CIP-26 + CIP-68)")
    class FldtToken {

        @Test
        void mergesMetadataWithCip68Priority() {
            Subject result = service.querySubject(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), false);

            assertThat(result).isNotNull();
            assertThat(result.metadata().name().value()).isEqualTo("FLDT");
            assertThat(result.metadata().name().source()).isEqualTo("CIP_68");
            assertThat(result.metadata().url().value()).isEqualTo("https://fluidtokens.com");
            assertThat(result.metadata().url().source()).isEqualTo("CIP_26");
        }

        @Test
        void showCipsDetailsReturnsStandards() {
            Subject result = service.querySubject(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), true);

            assertThat(result).isNotNull();
            assertThat(result.standards()).isNotNull();
            assertThat(result.standards().cip26()).isNotNull();
            assertThat(result.standards().cip26().getName()).isEqualTo("FLDT");
            assertThat(result.standards().cip68()).isNotNull();
            assertThat(result.standards().cip68().name()).isEqualTo("FLDT");
        }

        @Test
        void cip26PriorityOverrideChangesMergeOrder() {
            List<QueryPriority> cip26First = List.of(QueryPriority.CIP_26, QueryPriority.CIP_68);
            Subject result = service.querySubject(FLDT_SUBJECT, cip26First, List.of(), false);

            assertThat(result).isNotNull();
            assertThat(result.metadata().name().value()).isEqualTo("FLDT");
            assertThat(result.metadata().name().source()).isEqualTo("CIP_26");
            assertThat(result.metadata().url().value()).isEqualTo("https://fluidtokens.com");
            assertThat(result.metadata().url().source()).isEqualTo("CIP_26");
        }
    }

    @Nested
    @DisplayName("querySubject - CIP-113 extensions")
    class Cip113Extensions {

        @Test
        void cip113ExtensionShouldAppearForProgrammableToken() {
            Subject result = service.querySubject(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), false);

            assertThat(result).isNotNull();
            assertThat(result.extensions()).isNotNull();
            assertThat(result.extensions()).containsKey("cip113");

            ProgrammableTokenCip113 cip113 = (ProgrammableTokenCip113) result.extensions().get("cip113");
            assertThat(cip113.transferLogicScript())
                    .isEqualTo("aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd");
        }

        @Test
        void nonProgrammableTokenShouldNotHaveExtensions() {
            Subject result = service.querySubject(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), false);
            assertThat(result).isNotNull();
            assertThat(result.extensions()).isNull();
        }
    }

    @Nested
    @DisplayName("querySubjectBatch")
    class BatchQuery {

        @Test
        void returnsMergedMetadataForMultipleSubjects() {
            Map<String, ProgrammableTokenCip113> cip113Map = service.prefetchCip113(
                    List.of(KNOWN_SUBJECT, FLDT_SUBJECT));

            Subject known = service.querySubjectBatch(KNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), cip113Map, false);
            Subject fldt = service.querySubjectBatch(FLDT_SUBJECT, DEFAULT_PRIORITY, List.of(), cip113Map, false);

            assertThat(known.subject()).isEqualTo(KNOWN_SUBJECT);
            assertThat(known.metadata().url().value()).isEqualTo("https://cip68-url.com/nutcoin");

            assertThat(fldt.subject()).isEqualTo(FLDT_SUBJECT);
            assertThat(fldt.metadata().name().source()).isEqualTo("CIP_68");
        }

        @Test
        void unknownSubjectReturnsEmptyMetadata() {
            Map<String, ProgrammableTokenCip113> cip113Map = service.prefetchCip113(
                    List.of(UNKNOWN_SUBJECT));

            Subject unknown = service.querySubjectBatch(UNKNOWN_SUBJECT, DEFAULT_PRIORITY, List.of(), cip113Map, false);
            assertThat(unknown.metadata().isEmpty() || !unknown.metadata().isValid()).isTrue();
        }
    }
}
