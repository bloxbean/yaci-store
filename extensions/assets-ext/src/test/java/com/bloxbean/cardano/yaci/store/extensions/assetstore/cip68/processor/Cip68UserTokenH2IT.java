package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68UserTokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68UserTokenRepository;
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
import java.time.Clock;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** The user token table on a real H2 database: what the processor records, that a replay adds nothing, the rollback, and the lookup. */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql,classpath:db/store/h2/V0_1700_2__validation_errors.sql,"
                + "classpath:db/store/h2/V0_1700_3__invalid_token.sql,classpath:db/store/h2/V0_1700_4__cip68_user_token.sql"
})
@DisplayName("cip68_user_token on H2")
class Cip68UserTokenH2IT {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    private static final String FT = POLICY + "0014df10" + "6162";
    private static final String RFT = POLICY + "001bc280" + "6162";
    private static final String DATUM = "d8799fa3446e616d65" + "4178" + "4b6465736372697074696f6e" + "4164" + "43666f6f" + "00" + "01" + "ff";

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = {Cip68Metadata.class, Cip68UserToken.class})
    @EnableJpaRepositories(basePackageClasses = {Cip68MetadataRepository.class, Cip68UserTokenRepository.class})
    static class Config {
        @org.springframework.context.annotation.Bean
        Cip68UserTokenWriter cip68UserTokenWriter() {
            return new Cip68UserTokenWriter();
        }
    }

    @Autowired
    private Cip68MetadataRepository metadataRepository;
    @Autowired
    private Cip68UserTokenRepository repository;
    @Autowired
    private Cip68UserTokenWriter writer;
    @Autowired
    private EntityManager entityManager;

    private Cip68Processor processor;

    @BeforeEach
    void setUp() {
        processor = new Cip68Processor(new Cip68TokenService(metadataRepository), new Cip68DatumParser(), metadataRepository,
                com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics.noop(),
                com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder.noop(), Clock.systemDefaultZone(),
                new Cip68UserTokenRecorder(repository, writer));
    }

    @Test
    @DisplayName("only the 333 and 444 user tokens with a name are recorded, with the slot and the transaction")
    void recordsFungibleAndRichFungibleTokensOnly() {
        processor.processTransaction(event(100L, FT, RFT,
                POLICY + "000de140" + "6162",      // an NFT user token (222)
                POLICY + "000643b0" + "6162",      // a reference NFT
                POLICY + "0014df10"));             // a 333 label with no name
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).extracting(Cip68UserToken::getUnit).containsExactlyInAnyOrder(FT, RFT);
        assertThat(repository.findAll()).allSatisfy(t -> {
            assertThat(t.getSlot()).isEqualTo(100L);
            assertThat(t.getTxHash()).isEqualTo(TX_HASH);
        });
    }

    @Test
    @DisplayName("replaying a block adds nothing")
    void replayIsIdempotent() {
        processor.processTransaction(event(100L, FT));
        entityManager.flush();
        processor.processTransaction(event(100L, FT));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("a token keeps the slot where it was first seen")
    void keepsTheFirstSlot() {
        processor.processTransaction(event(100L, FT));
        entityManager.flush();
        processor.processTransaction(event(200L, FT));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(FT)).get().extracting(Cip68UserToken::getSlot).isEqualTo(100L);
    }

    @Test
    @DisplayName("the upsert keeps the smallest slot: a later block that committed first is lowered by an earlier one, never raised")
    void upsertKeepsTheSmallestSlot() {
        writer.upsertFirstSeen(FT, "cc".repeat(32), 200L);
        entityManager.flush();
        writer.upsertFirstSeen(FT, TX_HASH, 100L);
        entityManager.flush();
        writer.upsertFirstSeen(FT, "dd".repeat(32), 300L);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).singleElement().satisfies(t -> {
            assertThat(t.getUnit()).isEqualTo(FT);
            assertThat(t.getSlot()).isEqualTo(100L);
            assertThat(t.getTxHash()).isEqualTo(TX_HASH);
        });
    }

    @Test
    @DisplayName("two writers of the same new token do not fail with a duplicate key: the database resolves the conflict")
    void writingTheSameTokenTwiceDoesNotFail() {
        // what two blocks processed in parallel do: both saw the token as new, and both write it
        writer.upsertFirstSeen(FT, TX_HASH, 100L);
        writer.upsertFirstSeen(FT, TX_HASH, 100L);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("a rollback removes the tokens first seen after the rollback slot and keeps the earlier ones")
    void rollbackRemovesLaterTokens() {
        processor.processTransaction(event(100L, FT));
        processor.processTransaction(event(200L, RFT));
        entityManager.flush();

        new Cip68UserTokenRollbackProcessor(repository).handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(150L, "ab".repeat(32))).build());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).extracting(Cip68UserToken::getUnit).containsExactly(FT);
    }

    @Test
    @DisplayName("the service answers from the table, in one lookup, whatever the case of the unit")
    void serviceLooksTheTokensUp() {
        processor.processTransaction(event(100L, FT));
        entityManager.flush();
        entityManager.clear();
        Cip68UserTokenService service = new Cip68UserTokenService(repository);

        assertThat(service.exists(FT.toUpperCase())).isTrue();
        assertThat(service.exists(RFT)).isFalse();
        assertThat(service.existing(List.of(FT, RFT))).isEqualTo(Set.of(FT));
        assertThat(service.existsAsFungibleOrRichFungible(POLICY, "6162")).isTrue();
        assertThat(service.existsAsFungibleOrRichFungible(POLICY, "6163")).isFalse();
    }

    private static AddressUtxoEvent event(long slot, String... units) {
        java.util.List<Amt> amounts = new java.util.ArrayList<>();
        for (String unit : units) {
            amounts.add(Amt.builder().unit(unit).quantity(BigInteger.ONE).build());
        }
        AddressUtxo utxo = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).outputIndex(0).inlineDatum(DATUM).amounts(amounts).build();
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }
}
