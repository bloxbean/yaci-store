package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * An additional property of a CIP-68 datum can be nested as deep as the transaction size allows, and Jackson refuses to
 * write a JSON document nested deeper than 1000 levels. The failure came out of the {@code properties} column on flush,
 * inside the block transaction, and stopped the sync (#1240). Runs the processor against a real H2 table.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql"
})
@DisplayName("Cip68Processor with a deeply nested property on H2")
class Cip68DeepNestingH2IT {

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
    @DisplayName("a property nested 1100 levels deep does not fail the insert; the token is stored without it")
    void aPropertyNestedBeyondJacksonsLimitDoesNotStopTheSync() {
        assertThatCode(() -> processAndFlush(datumWithNestedProperty(1100))).doesNotThrowAnyException();

        entityManager.clear();
        assertThat(repository.findAll()).singleElement().satisfies(row -> {
            assertThat(row.getName()).isEqualTo("x");
            assertThat(row.getDescription()).isEqualTo("d");
            assertThat(row.getProperties()).isNull();
        });
    }

    @Test
    @DisplayName("a property nested at the limit is saved and read back")
    void aPropertyNestedAtTheLimitRoundTrips() {
        processAndFlush(datumWithNestedProperty(Cip68DatumParser.MAX_PROPERTY_DEPTH));

        entityManager.clear();
        Cip68Metadata row = repository.findAll().getFirst();
        assertThat(row.getProperties()).containsKey("additional_properties");
        assertThat(depth(((Map<?, ?>) row.getProperties().get("additional_properties")).get("foo")))
                .isEqualTo(Cip68DatumParser.MAX_PROPERTY_DEPTH);
    }

    private static int depth(Object value) {
        int levels = 0;
        while (value instanceof List<?> list) {
            levels++;
            value = list.getFirst();
        }
        return levels;
    }

    private void processAndFlush(String datum) {
        processor.processTransaction(event(datum));
        entityManager.flush();
    }

    private static AddressUtxoEvent event(String datum) {
        Amt refNft = Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build();
        AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).inlineDatum(datum).amounts(List.of(refNft)).build();
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }

    /** Constr 0 [{"name": "x", "description": "d", "foo": [[[ ... 0 ... ]]]}, 1], the list nested {@code depth} levels. */
    private static String datumWithNestedProperty(int depth) {
        return "d8799f" + "a3"
                + "446e616d65" + "4178"
                + "4b6465736372697074696f6e" + "4164"
                + "43666f6f" + "81".repeat(depth) + "00"
                + "01" + "ff";
    }
}
