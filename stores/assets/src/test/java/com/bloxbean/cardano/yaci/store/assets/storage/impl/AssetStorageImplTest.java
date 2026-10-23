package com.bloxbean.cardano.yaci.store.assets.storage.impl;

import com.bloxbean.cardano.yaci.store.assets.domain.MintType;
import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.assets.storage.AssetStorage;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.mapper.AssetMapperImpl;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.model.TxAssetEntity;
import com.bloxbean.cardano.yaci.store.assets.storage.impl.repository.TxAssetRepository;
import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.test.autoconfigure.jooq.AutoConfigureJooq;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureJooq
@Import(AssetStorageImplTest.JooqTestConfig.class)
class AssetStorageImplTest {
    private static final String TX_HASH = "428eae79467035033f8365afcd2b0995afddb972042962e86640ba1f0395bb4e";
    private static final String POLICY = "d92e8da9e0439de648d6c44514e7021f04cb8fb57ecbd1018ad85de8";

    @Autowired
    private TxAssetRepository repository;

    @Autowired
    private DSLContext dsl;

    private AssetStorage assetStorage;

    @BeforeEach
    void setUp() {
        assetStorage = new AssetStorageImpl(repository, new AssetMapperImpl(), dsl);
    }

    @Test
    void saveAll_sameRowsTwice_keepsOneRowPerUnit() {
        //"a\u0000" and "a " are different units with the same sanitized asset name
        assetStorage.saveAll(List.of(txAsset("a ", POLICY + "6100", 1), txAsset("a ", POLICY + "6120", 2)));
        assetStorage.saveAll(List.of(txAsset("a ", POLICY + "6100", 10), txAsset("a ", POLICY + "6120", 20)));

        List<TxAssetEntity> rows = repository.findAll();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(TxAssetEntity::getId)
                .containsExactlyInAnyOrder(UUIDUtil.nameUUID(TX_HASH, POLICY + "6100"), UUIDUtil.nameUUID(TX_HASH, POLICY + "6120"));
        assertThat(rows).filteredOn(row -> row.getUnit().equals(POLICY + "6100"))
                .singleElement().satisfies(row -> assertThat(row.getQuantity()).isEqualTo(BigInteger.valueOf(10)));
        assertThat(rows).filteredOn(row -> row.getUnit().equals(POLICY + "6120"))
                .singleElement().satisfies(row -> assertThat(row.getQuantity()).isEqualTo(BigInteger.valueOf(20)));
    }

    private TxAsset txAsset(String assetName, String unit, long quantity) {
        return TxAsset.builder()
                .slot(10707576L)
                .txHash(TX_HASH)
                .policy(POLICY)
                .assetName(assetName)
                .unit(unit)
                .quantity(BigInteger.valueOf(quantity))
                .mintType(MintType.MINT)
                .blockNumber(179428L)
                .blockTime(1666390776L)
                .build();
    }

    @TestConfiguration
    static class JooqTestConfig {
        @Bean
        DefaultConfigurationCustomizer jooqSettings() {
            return configuration -> configuration.settings()
                    .withRenderQuotedNames(RenderQuotedNames.NEVER);
        }
    }
}
