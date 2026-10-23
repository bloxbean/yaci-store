package com.bloxbean.cardano.yaci.store.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.mapper.MetadataMapperImpl;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.model.TxMetadataLabelEntity;
import com.bloxbean.cardano.yaci.store.metadata.storage.impl.repository.TxMetadataLabelRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureJooq
@Import(TxMetadataStorageImplTest.JooqTestConfig.class)
class TxMetadataStorageImplTest {
    private static final String TX_HASH = "9163a0795ca1dee7d51f0601f65ec2d90408a653ed89cd45e36c73e04ccdaa8e";

    @Autowired
    private TxMetadataLabelRepository repository;

    @Autowired
    private DSLContext dsl;

    private TxMetadataStorage txMetadataStorage;

    @BeforeEach
    void setUp() {
        txMetadataStorage = new TxMetadataStorageImpl(repository, new MetadataMapperImpl(), dsl);
    }

    @Test
    void saveAll_sameRowsTwice_keepsOneRowPerLabel() {
        txMetadataStorage.saveAll(List.of(label("721", "{\"v\":1}"), label("674", "{\"msg\":\"a\"}")));
        txMetadataStorage.saveAll(List.of(label("721", "{\"v\":2}"), label("674", "{\"msg\":\"b\"}")));

        List<TxMetadataLabelEntity> rows = repository.findAll();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(TxMetadataLabelEntity::getId)
                .containsExactlyInAnyOrder(UUIDUtil.nameUUID(TX_HASH, "721"), UUIDUtil.nameUUID(TX_HASH, "674"));
        assertThat(rows).filteredOn(row -> row.getLabel().equals("721"))
                .singleElement().satisfies(row -> assertThat(row.getBody()).isEqualTo("{\"v\":2}"));
        assertThat(rows).filteredOn(row -> row.getLabel().equals("674"))
                .singleElement().satisfies(row -> assertThat(row.getBody()).isEqualTo("{\"msg\":\"b\"}"));
    }

    private TxMetadataLabel label(String label, String body) {
        return TxMetadataLabel.builder()
                .txHash(TX_HASH)
                .txIndex(0)
                .slot(10700060L)
                .label(label)
                .body(body)
                .blockNumber(179014L)
                .blockTime(1666383260L)
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
