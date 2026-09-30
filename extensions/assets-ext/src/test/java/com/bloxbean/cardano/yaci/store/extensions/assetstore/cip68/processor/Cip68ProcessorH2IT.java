package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs {@link Cip68Processor} against a real H2 {@code cip68_metadata} table created by the module's
 * own migration, so a value the parser lets through but the column rejects shows up as a flush
 * failure here instead of as a stopped sync in production.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql"
})
@DisplayName("Cip68Processor on H2")
class Cip68ProcessorH2IT {

    private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String REF_NFT_ASSET_NAME = "000643b0464c4454";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = Cip68Metadata.class)
    @EnableJpaRepositories(basePackageClasses = Cip68MetadataRepository.class)
    static class Config {
    }

    @Autowired
    private Cip68MetadataRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Cip68Processor processor;

    @BeforeEach
    void setUp() {
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(), repository);
    }

    @Test
    void storesNameAtColumnWidth() {
        processAndFlush("N".repeat(Cip68Metadata.NAME_MAX_LENGTH));

        assertThat(repository.findAll())
                .singleElement()
                .satisfies(m -> assertThat(m.getName()).hasSize(Cip68Metadata.NAME_MAX_LENGTH));
    }

    @Test
    void skipsDatumWithOversizedNameWithoutFailingTheInsert() {
        assertThatCode(() -> processAndFlush("N".repeat(Cip68Metadata.NAME_MAX_LENGTH + 1)))
                .doesNotThrowAnyException();

        assertThat(repository.count()).isZero();
    }

    @Test
    void skipsDatumWhoseNameOverflowsH2InUtf16Units() {
        // 128 emoji = 128 code points but 256 UTF-16 units: fits a Postgres VARCHAR(255), not H2's
        assertThatCode(() -> processAndFlush("🚀".repeat(128)))
                .doesNotThrowAnyException();

        assertThat(repository.count()).isZero();
    }

    @Test
    void h2CountsVarcharWidthInUtf16Units() {
        // Guards the premise of the parser's length check: if H2 ever counted code points, this
        // would pass and the check could be relaxed.
        repository.save(Cip68Metadata.builder()
                .policyId(POLICY_ID).assetName(REF_NFT_ASSET_NAME).slot(1L).txHash(TX_HASH).txIndex(0)
                .label(333).version(1L).datum("00").lastSyncedAt(LocalDateTime.now())
                .name("🚀".repeat(128))
                .build());

        assertThatThrownBy(entityManager::flush).hasMessageContaining("Value too long");
    }

    private void processAndFlush(String name) {
        processor.processTransaction(eventWithDatum(name));
        entityManager.flush();
    }

    private static AddressUtxoEvent eventWithDatum(String name) {
        Amt refNft = Amt.builder()
                .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                .quantity(BigInteger.ONE)
                .build();

        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(TX_HASH)
                .txIndex(0)
                .inlineDatum(datum(name))
                .amounts(List.of(refNft))
                .build();

        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }

    private static String datum(String name) {
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
        properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));

        ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1));
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
