package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor.Cip68Processor;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.model.InvalidToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.repository.InvalidTokenRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

/** The {@code invalid_token} table on a real H2 database: the recorder, the CIP-68 processor writing to it, and the rollback. */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql,classpath:db/store/h2/V0_1700_2__validation_errors.sql,"
                + "classpath:db/store/h2/V0_1700_3__invalid_token.sql"
})
@DisplayName("invalid_token on H2")
class InvalidTokenH2IT {

    private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String REF_NFT_ASSET_NAME = "000643b0464c4454";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    private static final String UNDECODABLE = "zz";

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = {Cip68Metadata.class, InvalidToken.class})
    @EnableJpaRepositories(basePackageClasses = {Cip68MetadataRepository.class, InvalidTokenRepository.class})
    static class Config {
    }

    @Autowired
    private Cip68MetadataRepository cip68Repository;
    @Autowired
    private InvalidTokenRepository repository;
    @Autowired
    private EntityManager entityManager;

    private InvalidTokenRecorder recorder;
    private Cip68Processor processor;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        recorder = new InvalidTokenRecorder(repository, meters);
        processor = new Cip68Processor(new Cip68TokenService(cip68Repository), new Cip68DatumParser(), cip68Repository, com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics.noop(), recorder);
    }

    @Test
    @DisplayName("a datum that cannot be decoded is recorded with its token, transaction and output, and replaying the block adds nothing")
    void undecodableDatumIsRecordedOnce() {
        processor.processTransaction(event(UNDECODABLE, 100L));
        processor.processTransaction(event(UNDECODABLE, 100L));
        entityManager.flush();
        entityManager.clear();

        assertThat(cip68Repository.findAll()).isEmpty();
        assertThat(repository.findAll()).singleElement().satisfies(row -> {
            assertThat(row.getStandard()).isEqualTo(InvalidTokenReason.CIP_68);
            assertThat(row.getReasonCode()).isEqualTo(InvalidTokenReason.DATUM_UNDECODABLE);
            assertThat(row.getPolicyId()).isEqualTo(POLICY_ID);
            assertThat(row.getAssetName()).isEqualTo(REF_NFT_ASSET_NAME);
            assertThat(row.getTxHash()).isEqualTo(TX_HASH);
            assertThat(row.getSlot()).isEqualTo(100L);
        });
    }

    @Test
    @DisplayName("a datum that is not CIP-68 at all is not recorded")
    void aDatumWithoutCip68ShapeIsNotRecorded() {
        processor.processTransaction(event("00", 100L));
        entityManager.flush();

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("a rollback deletes the rows after the slot and keeps the ones from the CIP-26 registry")
    void rollbackKeepsOffChainRows() {
        recorder.record(InvalidTokenReport.builder().standard(InvalidTokenReason.CIP_68).reasonCode(InvalidTokenReason.DATUM_UNDECODABLE)
                .reason("old").txHash(TX_HASH).outputIndex(0).slot(50L).build());
        recorder.record(InvalidTokenReport.builder().standard(InvalidTokenReason.CIP_68).reasonCode(InvalidTokenReason.DATUM_UNDECODABLE)
                .reason("new").txHash(TX_HASH).outputIndex(1).slot(200L).build());
        recorder.record(InvalidTokenReport.builder().standard(InvalidTokenReason.CIP_26)
                .reasonCode(InvalidTokenReason.FILENAME_SUBJECT_MISMATCH).reason("registry").origin("x.json")
                .details(Map.of("inner_subject", "y")).build());
        entityManager.flush();

        new InvalidTokenRollbackProcessor(repository).handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(100L, "ab".repeat(32))).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).extracting(InvalidToken::getReason).containsExactlyInAnyOrder("old", "registry");
    }

    @Test
    @DisplayName("forgetting a file removes only that file's rows")
    void forgetOrigin() {
        for (String file : List.of("a.json", "b.json")) {
            recorder.record(InvalidTokenReport.builder().standard(InvalidTokenReason.CIP_26)
                    .reasonCode(InvalidTokenReason.UNPARSEABLE_MAPPING).reason("bad").origin(file).build());
        }
        entityManager.flush();

        recorder.forgetOrigin(InvalidTokenReason.CIP_26, "a.json");
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).extracting(InvalidToken::getOrigin).containsExactly("b.json");
    }

    private static AddressUtxoEvent event(String datum, long slot) {
        Amt refNft = Amt.builder().unit(POLICY_ID + REF_NFT_ASSET_NAME).quantity(BigInteger.ONE).build();
        AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0).inlineDatum(datum).amounts(List.of(refNft)).build();
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }
}
