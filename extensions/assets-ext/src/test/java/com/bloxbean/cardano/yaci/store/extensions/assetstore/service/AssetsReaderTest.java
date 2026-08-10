package com.bloxbean.cardano.yaci.store.extensions.assetstore.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.QueryPriority;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.StringProperty;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.Subject;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.dto.TokenType;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.api.service.TokenQueryService.BatchPrefetchData;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.Cip26StorageReader;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.Cip68StorageReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Coverage for {@link AssetsReader} — the top-level facade that library consumers (no REST
 * controller) use to query token metadata. Each public method is a thin delegate, so the tests
 * verify (a) happy-path delegation reaches the right downstream and (b) not-found from the
 * downstream is propagated through {@link Optional}/empty-collection contract.
 *
 * <p>Mock-based; no Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetsReader")
class AssetsReaderTest {

    private static final String SUBJECT = "0011fbab202151eca9e9ef7680569d9419d12e51e693cb05a2edd2ed4341524b";
    private static final String POLICY = "0011fbab202151eca9e9ef7680569d9419d12e51e693cb05a2edd2ed";

    @Mock private TokenQueryService tokenQueryService;
    @Mock private Cip26StorageReader cip26StorageReader;
    @Mock private Cip68StorageReader cip68StorageReader;
    @Mock private Cip113StorageReader cip113StorageReader;

    @InjectMocks private AssetsReader assetsReader;

    @Nested
    @DisplayName("getSubject (merged)")
    class GetSubject {

        @Test
        void defaultPriorityIsCip68ThenCip26() {
            // The single-arg overload supplies the canonical priority order. If a future change
            // accidentally swaps it, every consumer that didn't pass an explicit priority would
            // silently start returning CIP-26-prioritised data.
            ArgumentCaptor<List<QueryPriority>> priorityCaptor =
                    ArgumentCaptor.forClass(List.class);
            when(tokenQueryService.querySubject(eq(SUBJECT), priorityCaptor.capture(), eq(List.of()), eq(false)))
                    .thenReturn(Optional.of(subject(SUBJECT)));

            assetsReader.getSubject(SUBJECT);

            assertThat(priorityCaptor.getValue())
                    .containsExactly(QueryPriority.CIP_68, QueryPriority.CIP_26);
        }

        @Test
        void explicitPriorityIsForwardedVerbatim() {
            List<QueryPriority> priority = List.of(QueryPriority.CIP_26, QueryPriority.CIP_68);
            when(tokenQueryService.querySubject(SUBJECT, priority, List.of(), false))
                    .thenReturn(Optional.of(subject(SUBJECT)));

            Optional<Subject> result = assetsReader.getSubject(SUBJECT, priority);

            assertThat(result).isPresent();
            verify(tokenQueryService).querySubject(SUBJECT, priority, List.of(), false);
        }

        @Test
        void notFoundReturnsEmpty() {
            when(tokenQueryService.querySubject(any(), any(), any(), anyBoolean()))
                    .thenReturn(Optional.empty());

            assertThat(assetsReader.getSubject(SUBJECT)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getSubjects (batch)")
    class GetSubjects {

        @Test
        void invalidMetadataSubjectsAreFilteredOut() {
            // The batch facade silently drops subjects whose metadata is incomplete (no name +
            // description), so callers don't have to defend against half-populated rows.
            List<String> subjects = List.of("a-valid-subject", "an-invalid-subject");
            BatchPrefetchData prefetch = new BatchPrefetchData(Map.of(), Map.of(), Map.of(), Map.of());

            when(tokenQueryService.prefetchBatch(subjects, List.of())).thenReturn(prefetch);
            when(tokenQueryService.querySubjectBatch(eq("a-valid-subject"), any(), any(), eq(prefetch), eq(false)))
                    .thenReturn(subject("a-valid-subject"));
            when(tokenQueryService.querySubjectBatch(eq("an-invalid-subject"), any(), any(), eq(prefetch), eq(false)))
                    .thenReturn(subjectWithoutMetadata("an-invalid-subject"));

            List<Subject> result = assetsReader.getSubjects(subjects, List.of(QueryPriority.CIP_68));

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().subject()).isEqualTo("a-valid-subject");
        }

        @Test
        void emptyInputReturnsEmptyResult() {
            when(tokenQueryService.prefetchBatch(List.of(), List.of()))
                    .thenReturn(new BatchPrefetchData(Map.of(), Map.of(), Map.of(), Map.of()));

            assertThat(assetsReader.getSubjects(List.of(), List.of(QueryPriority.CIP_68))).isEmpty();
        }
    }

    @Nested
    @DisplayName("per-CIP queries")
    class PerCip {

        @Test
        void getCip26MetadataDelegatesToReader() {
            Cip26Metadata m = new Cip26Metadata();
            when(cip26StorageReader.findBySubject(SUBJECT)).thenReturn(Optional.of(m));

            assertThat(assetsReader.getCip26Metadata(SUBJECT)).containsSame(m);
        }

        @Test
        void getCip26MetadataReturnsEmptyWhenNotFound() {
            when(cip26StorageReader.findBySubject(SUBJECT)).thenReturn(Optional.empty());

            assertThat(assetsReader.getCip26Metadata(SUBJECT)).isEmpty();
        }

        @Test
        void getCip26LogoDelegatesToReader() {
            when(cip26StorageReader.findLogoBySubject(SUBJECT)).thenReturn(Optional.of("base64"));

            assertThat(assetsReader.getCip26Logo(SUBJECT)).contains("base64");
        }

        @Test
        void getCip68MetadataDelegatesToReader() {
            FungibleTokenMetadata m = new FungibleTokenMetadata(6L, "desc", null, "Token", "TKN", "https://x", 1L);
            when(cip68StorageReader.findBySubject(SUBJECT)).thenReturn(Optional.of(m));

            assertThat(assetsReader.getCip68Metadata(SUBJECT)).contains(m);
        }

        @Test
        void getCip68MetadataReturnsEmptyWhenNotFound() {
            when(cip68StorageReader.findBySubject(SUBJECT)).thenReturn(Optional.empty());

            assertThat(assetsReader.getCip68Metadata(SUBJECT)).isEmpty();
        }

        @Test
        void getCip113RegistryNodeDelegatesToReader() {
            ProgrammableTokenCip113 token = new ProgrammableTokenCip113("script", null, null, null, null);
            when(cip113StorageReader.findByPolicyId(POLICY)).thenReturn(Optional.of(token));

            assertThat(assetsReader.getCip113RegistryNode(POLICY)).contains(token);
        }

        @Test
        void getCip113RegistryNodeReturnsEmptyWhenNotFound() {
            // Either CIP-113 disabled or the policy is not in the registry — both surface as empty.
            when(cip113StorageReader.findByPolicyId(POLICY)).thenReturn(Optional.empty());

            assertThat(assetsReader.getCip113RegistryNode(POLICY)).isEmpty();
        }

        @Test
        void getCip113RegistryNodesBatchDelegatesToReader() {
            List<String> policies = List.of(POLICY, "another-policy-id");
            ProgrammableTokenCip113 token = new ProgrammableTokenCip113("script", null, null, null, null);
            when(cip113StorageReader.findByPolicyIds(policies)).thenReturn(Map.of(POLICY, token));

            Map<String, ProgrammableTokenCip113> result = assetsReader.getCip113RegistryNodes(policies);

            assertThat(result).containsOnlyKeys(POLICY);
            // `another-policy-id` legitimately absent — facade does not fabricate empty entries.
            verify(cip113StorageReader, never()).findByPolicyId(any());
        }

        @Test
        void isProgrammableTokenDelegatesToReader() {
            when(cip113StorageReader.isProgrammableToken(POLICY)).thenReturn(true);
            assertThat(assetsReader.isProgrammableToken(POLICY)).isTrue();

            when(cip113StorageReader.isProgrammableToken(POLICY)).thenReturn(false);
            assertThat(assetsReader.isProgrammableToken(POLICY)).isFalse();
        }
    }

    private static Subject subject(String subjectId) {
        // valid metadata: name + description both set so Metadata.isValid() returns true
        Metadata metadata = Metadata.builder()
                .name(new StringProperty("a name", QueryPriority.CIP_26.name()))
                .description(new StringProperty("a description", QueryPriority.CIP_26.name()))
                .build();
        return new Subject(subjectId, TokenType.NATIVE, metadata, null, null);
    }

    private static Subject subjectWithoutMetadata(String subjectId) {
        // no name → Metadata.isValid() = false → batch facade filters this out
        return new Subject(subjectId, TokenType.NATIVE, Metadata.empty(), null, null);
    }
}
