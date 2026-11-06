package com.bloxbean.cardano.yaci.store.mir.storage.impl;

import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.mapper.MIRMapperImpl;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.model.MIREntity;
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
@Import(MIRStorageImplTest.JooqTestConfig.class)
class MIRStorageImplTest {
    private static final String TX_HASH = "a6bb2e2a7ea34b1dcd7c8aa6a1e2abeb1ef6b5a6d0a5c1e5f1b2d4a4ef6f1a21";
    private static final String CREDENTIAL_1 = "f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539";
    private static final String CREDENTIAL_2 = "0e1b5a8f1d2c3b4a59687766554433221100ffeeddccbbaa99887766";

    @Autowired
    private MIRRepository repository;

    @Autowired
    private DSLContext dsl;

    private MIRStorage mirStorage;

    @BeforeEach
    void setUp() {
        mirStorage = new MIRStorageImpl(repository, new MIRMapperImpl(), dsl);
    }

    @Test
    void save_sameRowsTwice_keepsOneRowPerCertAndCredential() {
        //Cert 0 moves to two stake credentials, cert 1 moves to the other pot (no credential)
        mirStorage.save(List.of(mir(0, CREDENTIAL_1, 100), mir(0, CREDENTIAL_2, 200), mir(1, null, 300)));
        mirStorage.save(List.of(mir(0, CREDENTIAL_1, 101), mir(0, CREDENTIAL_2, 201), mir(1, null, 301)));

        List<MIREntity> rows = repository.findAll();
        assertThat(rows).hasSize(3);
        assertThat(rows).extracting(MIREntity::getId)
                .containsExactlyInAnyOrder(UUIDUtil.nameUUID(TX_HASH, "0", CREDENTIAL_1), UUIDUtil.nameUUID(TX_HASH, "0", CREDENTIAL_2),
                        UUIDUtil.nameUUID(TX_HASH, "1", null));
        assertThat(rows).extracting(MIREntity::getAmount)
                .containsExactlyInAnyOrder(BigInteger.valueOf(101), BigInteger.valueOf(201), BigInteger.valueOf(301));
    }

    private MoveInstataneousReward mir(int certIndex, String credential, long amount) {
        return MoveInstataneousReward.builder()
                .pot(MirPot.reserves)
                .txHash(TX_HASH)
                .certIndex(certIndex)
                .credential(credential)
                .amount(BigInteger.valueOf(amount))
                .epoch(236)
                .slot(13220525L)
                .blockHash("c46dba8ba6246f6f3b5b7cca258271406c9a2d119baf3ebc383ad6eed63f33e1")
                .blockNumber(4800000L)
                .blockTime(1604000000L)
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
