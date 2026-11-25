package com.bloxbean.cardano.yaci.store.assets.storage.impl;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper.AssetMapperImpl;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
class AssetStorageReaderImplTest {

    @Autowired
    private TxAssetRepository repository;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void historiesReturnEveryRowInOrderAcrossPages(boolean legacy) {
        var fixtures = new ArrayList<TxAssetEntity>();
        for (int i = 0; i < 600; i++) {
            fixtures.add(TxAssetEntity.builder()
                    .slot(100L)
                    .txIndex(legacy ? null : i / 20)
                    .txHash("%064x".formatted(600 - i / 20))
                    .policy("policy")
                    .unit("unit%02d".formatted((i / 2) % 10))
                    .fingerprint("fingerprint%02d".formatted((i / 2) % 10))
                    // Distinguish even rows with the same transaction and unit in the API response.
                    .quantity(BigInteger.valueOf(i))
                    .build());
        }
        repository.saveAllAndFlush(fixtures);
        Comparator<TxAssetEntity> ordering = Comparator
                .comparing(TxAssetEntity::getTxIndex, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TxAssetEntity::getTxHash, Comparator.reverseOrder())
                .thenComparing(TxAssetEntity::getUnit, Comparator.reverseOrder())
                .thenComparing(row -> row.getId().toString(), Comparator.reverseOrder());
        fixtures.sort(ordering);

        var reader = new AssetStorageReaderImpl(repository, new AssetMapperImpl());
        assertPages(page -> reader.findByPolicy("policy", page, 100), fixtures, 100);
        var unitRows = fixtures.stream().filter(row -> row.getUnit().equals("unit00")).toList();
        assertPages(page -> reader.findByUnit("unit00", page, 7), unitRows, 7);
        assertPages(page -> reader.findByFingerprint("fingerprint00", page, 7), unitRows, 7);
    }

    private void assertPages(IntFunction<List<TxAsset>> fetch, List<TxAssetEntity> expected, int pageSize) {
        var actual = new ArrayList<BigInteger>();
        int pages = (expected.size() + pageSize - 1) / pageSize;
        for (int page = 0; page < pages; page++) {
            var rows = fetch.apply(page);
            assertThat(rows).hasSize(Math.min(pageSize, expected.size() - page * pageSize));
            actual.addAll(rows.stream().map(TxAsset::getQuantity).toList());
        }
        assertThat(actual).doesNotHaveDuplicates()
                .containsExactlyElementsOf(expected.stream().map(TxAssetEntity::getQuantity).toList());
        assertThat(fetch.apply(pages)).isEmpty();
    }
}
