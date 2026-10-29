package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
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
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql,classpath:db/store/h2/V0_1700_2__validation_errors.sql"
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
    void storesAnythingTheChainHoldsWithoutALengthCap() {
        // the columns are TEXT: a long name, and one whose characters take two UTF-16 units each, are stored as written
        String longName = "N".repeat(5_000) + "🚀".repeat(300);
        processAndFlush(longName);

        assertThat(repository.findAll())
                .singleElement()
                .satisfies(m -> assertThat(m.getName()).isEqualTo(longName));
    }

    @Test
    void storesTheValidationErrorsAsJsonAndReadsThemBack() {
        // a version that is not an integer: indexed, with the error
        processor.processTransaction(eventWithDatum(datum("A name", true, BytesPlutusData.of("one"))));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).singleElement().satisfies(m ->
                assertThat(m.getValidationErrors()).singleElement().satisfies(e -> {
                    assertThat(e.cip()).isEqualTo("CIP-68");
                    assertThat(e.subStandard()).isNull();
                    assertThat(e.fieldName()).isEqualTo("version");
                    assertThat(e.fieldCategory().name()).isEqualTo("MANDATORY");
                    assertThat(e.code()).isEqualTo("invalid_version");
                }));
    }

    @Test
    void countsOnlyADatumFromEarlierInTheChainAsAlreadyIndexed() {
        // during a parallel sync a later block can be stored first: it must not make an earlier datum look like an update
        repository.saveAll(List.of(row(100L, 2), row(300L, 0)));
        entityManager.flush();

        assertThat(repository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 50L, 0)).as("only later rows").isFalse();
        assertThat(repository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 100L, 1)).as("a later transaction of the block").isFalse();
        assertThat(repository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 100L, 2)).as("itself").isFalse();
        assertThat(repository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 100L, 3)).as("an earlier transaction of the block").isTrue();
        assertThat(repository.existsEarlierThan(POLICY_ID, REF_NFT_ASSET_NAME, 200L, 0)).as("an earlier block").isTrue();
        assertThat(repository.existsEarlierThan(POLICY_ID, "000643b0ffff", 400L, 0)).as("another reference NFT").isFalse();
    }

    private static Cip68Metadata row(long slot, int txIndex) {
        return Cip68Metadata.builder()
                .policyId(POLICY_ID).assetName(REF_NFT_ASSET_NAME).slot(slot).txHash(TX_HASH).txIndex(txIndex)
                .label(333).name("Token").datum("d8799fa0ff").build();
    }

    private void processAndFlush(String name) {
        processor.processTransaction(eventWithDatum(datum(name, true)));
        entityManager.flush();
    }

    private static AddressUtxoEvent eventWithDatum(String inlineDatum) {
        Amt refNft = Amt.builder()
                .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                .quantity(BigInteger.ONE)
                .build();

        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(TX_HASH)
                .txIndex(0)
                .inlineDatum(inlineDatum)
                .amounts(List.of(refNft))
                .build();

        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }

    private static String datum(String name, boolean withDescription) {
        return datum(name, withDescription, BigIntPlutusData.of(1));
    }

    private static String datum(String name, boolean withDescription, PlutusData version) {
        MapPlutusData properties = new MapPlutusData();
        properties.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
        if (withDescription) {
            properties.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
        }

        ConstrPlutusData datum = ConstrPlutusData.of(0, properties, version);
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
