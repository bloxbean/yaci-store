package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.core.model.RedeemerTag;
import com.bloxbean.cardano.yaci.store.common.util.UUIDUtil;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapperImpl;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.TxScriptRepository;
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
import static org.assertj.core.groups.Tuple.tuple;

@DataJpaTest
@AutoConfigureJooq
@Import(TxScriptStorageImplTest.JooqTestConfig.class)
class TxScriptStorageImplTest {
    private static final String TX_HASH = "428eae79467035033f8365afcd2b0995afddb972042962e86640ba1f0395bb4e";
    private static final String SCRIPT_HASH = "d92e8da9e0439de648d6c44514e7021f04cb8fb57ecbd1018ad85de8";

    @Autowired
    private TxScriptRepository repository;

    @Autowired
    private DSLContext dsl;

    private TxScriptStorage txScriptStorage;

    @BeforeEach
    void setUp() {
        txScriptStorage = new TxScriptStorageImpl(repository, new ScriptMapperImpl(), dsl);
    }

    @Test
    void saveAll_sameRowsTwice_keepsOneRowPerRedeemer() {
        //Same script for two spend redeemers and a mint redeemer with the same index
        txScriptStorage.saveAll(List.of(txScript(RedeemerTag.Spend, 0, 100), txScript(RedeemerTag.Spend, 1, 100),
                txScript(RedeemerTag.Mint, 0, 100)));
        txScriptStorage.saveAll(List.of(txScript(RedeemerTag.Spend, 0, 200), txScript(RedeemerTag.Spend, 1, 200),
                txScript(RedeemerTag.Mint, 0, 200)));

        List<TxScriptEntity> rows = repository.findAll();
        assertThat(rows).hasSize(3)
                .allSatisfy(row -> {
                    assertThat(row.getUnitMem()).isEqualTo(BigInteger.valueOf(200));
                    assertThat(row.getType()).isEqualTo(ScriptType.PLUTUS_V2);
                });
        assertThat(rows).extracting(TxScriptEntity::getPurpose, TxScriptEntity::getRedeemerIndex)
                .containsExactlyInAnyOrder(
                        tuple(RedeemerTag.Spend, 0),
                        tuple(RedeemerTag.Spend, 1),
                        tuple(RedeemerTag.Mint, 0));
        assertThat(rows).extracting(TxScriptEntity::getId)
                .containsExactlyInAnyOrder(UUIDUtil.nameUUID(TX_HASH, "Spend", "0"), UUIDUtil.nameUUID(TX_HASH, "Spend", "1"),
                        UUIDUtil.nameUUID(TX_HASH, "Mint", "0"));
    }

    private TxScript txScript(RedeemerTag purpose, int redeemerIndex, long unitMem) {
        return TxScript.builder()
                .txHash(TX_HASH)
                .scriptHash(SCRIPT_HASH)
                .slot(10707576L)
                .blockHash("5068285d5eccc63bbfe0caa446fde9c28d77e45b6f34d37c29bbae8d47c30b9b")
                .type(ScriptType.PLUTUS_V2)
                .redeemerCbor("d87980")
                .unitMem(BigInteger.valueOf(unitMem))
                .unitSteps(BigInteger.valueOf(unitMem * 1000))
                .purpose(purpose)
                .redeemerIndex(redeemerIndex)
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
