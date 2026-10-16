package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
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

/**
 * Runs {@link Cip68RollbackProcessor} against a real H2 {@code cip68_metadata} table created by the
 * module's own migration. The table keeps one row per datum version, so a rollback is a delete of
 * the rows above the rollback slot and the previous datum becomes the latest again.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql"
})
@DisplayName("Cip68RollbackProcessor on H2")
class Cip68RollbackProcessorH2IT {

    private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String OTHER_POLICY_ID = "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344";
    private static final String REF_NFT_ASSET_NAME = "000643b0464c4454";
    private static final String TX_1 = "11".repeat(32);
    private static final String TX_2 = "22".repeat(32);
    private static final String TX_3 = "33".repeat(32);

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
    private Cip68RollbackProcessor rollbackProcessor;

    @BeforeEach
    void setUp() {
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(), repository);
        rollbackProcessor = new Cip68RollbackProcessor(repository);
    }

    @Test
    @DisplayName("keeps the rows at the rollback slot and deletes only those above it")
    void keepsRowAtRollbackSlot() {
        save(POLICY_ID, 100, TX_1, 0, "v1");
        save(POLICY_ID, 200, TX_2, 0, "v2");
        save(POLICY_ID, 300, TX_3, 0, "v3");

        rollbackTo(200);

        assertThat(slots()).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("the previous datum becomes the latest again")
    void previousDatumBecomesLatest() {
        save(POLICY_ID, 100, TX_1, 0, "Old name");
        save(POLICY_ID, 300, TX_2, 0, "New name");
        assertThat(latest(POLICY_ID)).get().extracting(Cip68Metadata::getName).isEqualTo("New name");

        rollbackTo(200);

        assertThat(latest(POLICY_ID)).get().extracting(Cip68Metadata::getName).isEqualTo("Old name");
    }

    @Test
    @DisplayName("a token created after the rollback point is gone")
    void tokenCreatedAfterRollbackPointDisappears() {
        save(POLICY_ID, 300, TX_1, 0, "Born too late");

        rollbackTo(200);

        assertThat(latest(POLICY_ID)).isEmpty();
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("a rollback above the newest row changes nothing")
    void rollbackAboveNewestRowChangesNothing() {
        save(POLICY_ID, 100, TX_1, 0, "v1");
        save(POLICY_ID, 200, TX_2, 0, "v2");

        rollbackTo(500);

        assertThat(slots()).containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    @DisplayName("every row of a slot goes when the rollback is before it, whatever the tx index")
    void allRowsOfARolledBackSlotGo() {
        save(POLICY_ID, 100, TX_1, 0, "kept");
        save(POLICY_ID, 300, TX_2, 0, "first in slot");
        save(POLICY_ID, 300, TX_3, 1, "second in slot");
        assertThat(latest(POLICY_ID)).get().extracting(Cip68Metadata::getName).isEqualTo("second in slot");

        rollbackTo(299);

        assertThat(repository.findAll()).singleElement().extracting(Cip68Metadata::getName).isEqualTo("kept");
    }

    @Test
    @DisplayName("rolls back every token, not only one")
    void rollsBackEveryToken() {
        save(POLICY_ID, 100, TX_1, 0, "A");
        save(POLICY_ID, 300, TX_2, 0, "A2");
        save(OTHER_POLICY_ID, 300, TX_3, 0, "B");

        rollbackTo(200);

        assertThat(latest(POLICY_ID)).get().extracting(Cip68Metadata::getName).isEqualTo("A");
        assertThat(latest(OTHER_POLICY_ID)).isEmpty();
    }

    @Test
    @DisplayName("replaying the rolled-back block stores the row again, without a duplicate")
    void replayAfterRollbackStoresTheRowOnce() {
        processor.processTransaction(eventWithDatum(300, TX_2, "Replayed"));
        entityManager.flush();
        assertThat(repository.count()).isEqualTo(1);

        rollbackTo(200);
        assertThat(repository.count()).isZero();

        // the fork replays the same block, then it is delivered again
        processor.processTransaction(eventWithDatum(300, TX_2, "Replayed"));
        processor.processTransaction(eventWithDatum(300, TX_2, "Replayed"));
        entityManager.flush();

        assertThat(repository.findAll()).singleElement().satisfies(m -> {
            assertThat(m.getSlot()).isEqualTo(300L);
            assertThat(m.getName()).isEqualTo("Replayed");
        });
    }

    @Test
    @DisplayName("a replayed block on the new fork can carry a different datum")
    void newForkCanCarryADifferentDatum() {
        save(POLICY_ID, 100, TX_1, 0, "Before fork");
        processor.processTransaction(eventWithDatum(300, TX_2, "Old fork"));
        entityManager.flush();

        rollbackTo(200);
        processor.processTransaction(eventWithDatum(300, TX_3, "New fork"));
        entityManager.flush();

        assertThat(latest(POLICY_ID)).get().extracting(Cip68Metadata::getName).isEqualTo("New fork");
        assertThat(repository.findAll()).extracting(Cip68Metadata::getName)
                .containsExactlyInAnyOrder("Before fork", "New fork");
    }

    private void rollbackTo(long slot) {
        rollbackProcessor.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(slot, "ab".repeat(32)))
                .build());
        entityManager.flush();
        entityManager.clear();
    }

    private void save(String policyId, long slot, String txHash, int txIndex, String name) {
        repository.save(Cip68Metadata.builder()
                .policyId(policyId).assetName(REF_NFT_ASSET_NAME).slot(slot).txHash(txHash).txIndex(txIndex)
                .label(333).version(1L).datum("00").name(name).lastSyncedAt(LocalDateTime.now())
                .build());
        entityManager.flush();
        entityManager.clear();
    }

    private java.util.Optional<Cip68Metadata> latest(String policyId) {
        return repository.findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(policyId, REF_NFT_ASSET_NAME);
    }

    private List<Long> slots() {
        return repository.findAll().stream().map(Cip68Metadata::getSlot).toList();
    }

    private static AddressUtxoEvent eventWithDatum(long slot, String txHash, String name) {
        Amt refNft = Amt.builder()
                .unit(POLICY_ID + REF_NFT_ASSET_NAME)
                .quantity(BigInteger.ONE)
                .build();

        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(txHash)
                .txIndex(0)
                .inlineDatum(datum(name))
                .amounts(List.of(refNft))
                .build();

        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
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
