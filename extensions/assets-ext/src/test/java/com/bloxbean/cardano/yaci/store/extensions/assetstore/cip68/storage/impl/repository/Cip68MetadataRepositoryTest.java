package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link Cip68MetadataRepository#findLatestByConcatenatedKeys} with the query answering as MySQL does with its default
 * case-insensitive collation: a key in another case matches the same stored row. H2 cannot show this, as its
 * {@code CONCAT} compares case-sensitively even on case-insensitive columns; the queries here stand in for MySQL.
 */
@DisplayName("Cip68MetadataRepository.findLatestByConcatenatedKeys")
class Cip68MetadataRepositoryTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String REF = "000643b0414141";

    private final Cip68MetadataRepository repository = mock(Cip68MetadataRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("returns a stored pair once when keys in two cases match it from different queries")
    void returnsPairOnceWhenMatchedFromTwoQueries() {
        Cip68Metadata stored = Cip68Metadata.builder().policyId(POLICY).assetName(REF).name("stored").build();
        when(repository.findLatestByPolicyIdsAndAssetNames(anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(List.of(stored));
        List<String> keys = new ArrayList<>();
        keys.add(POLICY + REF);
        for (int i = 1; i < Cip68MetadataRepository.MAX_KEYS_PER_QUERY; i++) {
            keys.add(POLICY + "000643b0" + String.format("%06x", i));
        }
        keys.add(POLICY.toUpperCase() + REF.toUpperCase());   // the same pair, the first key of the second query

        assertThat(repository.findLatestByConcatenatedKeys(keys)).containsExactly(stored);
        verify(repository, times(2)).findLatestByPolicyIdsAndAssetNames(anyCollection(), anyCollection(), anyCollection());
    }
}
