package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor.Cip113Processor;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor.Cip113RollbackProcessor;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.Cip113StorageReaderImpl;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
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
import java.util.Map;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113MainnetNodes.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CIP-113 table created by the module's own H2 migration, written by {@link Cip113Processor} from mainnet registry
 * node outputs and read back by {@link Cip113StorageReaderImpl}: the reserved words {@code key} and {@code next}, the
 * choice of the current node among its versions, the registry each node belongs to, and rollback.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_4__cip113_registry_node.sql"
})
@DisplayName("CIP-113 registry nodes on H2")
class Cip113RegistryNodeH2IT {

    private static final String TX_1 = "11".repeat(32);
    private static final String TX_2 = "22".repeat(32);
    private static final String TX_3 = "33".repeat(32);
    /** A second registry, as when a new registry is deployed next to the official one. */
    private static final String REGISTRY_B = "bb".repeat(28);
    private static final List<String> MAINNET = List.of(REGISTRY_POLICY_ID);

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = Cip113RegistryNode.class)
    @EnableJpaRepositories(basePackageClasses = Cip113RegistryNodeRepository.class)
    static class Config {
    }

    @Autowired
    private Cip113RegistryNodeRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Cip113Processor processor;
    private Cip113RollbackProcessor rollbackProcessor;
    private Cip113StorageReaderImpl reader;

    @BeforeEach
    void setUp() {
        configure();
    }

    /** The bundled mainnet registry, or the registries given, as store.assets.ext.cip113.registry-nft-policy-ids does. */
    private void configure(String... registryPolicyIds) {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setProtocolMagic(764824073L);
        AssetsExtStoreProperties properties = new AssetsExtStoreProperties();
        properties.getCip113().setRegistryNftPolicyIds(List.of(registryPolicyIds));
        Cip113Configuration configuration = new Cip113Configuration(storeProperties, properties);
        configuration.resolve();
        processor = new Cip113Processor(configuration, new Cip113RegistryNodeParser(), repository,
                InvalidTokenRecorder.noop(), Clock.systemDefaultZone());
        rollbackProcessor = new Cip113RollbackProcessor(repository);
        reader = new Cip113StorageReaderImpl(repository, configuration);
    }

    @Test
    @DisplayName("stores the mainnet nodes and serves the extension of a registered policy")
    void storesAndServes() {
        index(199_716_706L, nodeOutput(TX_1, 0, 1, "", ORIGIN_DATUM), nodeOutput(TX_1, 0, 2, POLICY_01C2, POLICY_01C2_DATUM));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(reader.findByPolicyId(POLICY_01C2)).contains(new ProgrammableTokenCip113(
                "4e9ba4785eebbeb77fe694c8451f1abec411f7774c09bbacfa3ba1ac",
                "ddf0363f0d73f54778e792e16bad802df5fa50061319b1f01d280a37",
                "446fc9584df8316cfab440c60d140e2007f236cdb65180917a374fb8",
                null,
                "218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2"));
        assertThat(reader.findByPolicyId(POLICY_01C2.toUpperCase())).isPresent();
        assertThat(reader.findByPolicyId(POLICY_B6F4)).isEmpty();
        assertThat(reader.findByPolicyId("")).as("the origin node is no policy").isEmpty();
    }

    @Test
    @DisplayName("the current node is the latest by slot, then by transaction index within the slot")
    void currentNodeIsTheLatest() {
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, datumWithNext(POLICY_01C2, POLICY_B6F4)));
        index(200L, nodeOutput(TX_2, 1, 0, POLICY_01C2, datumWithNext(POLICY_01C2, "ff".repeat(30))),
                nodeOutput(TX_3, 4, 0, POLICY_01C2, POLICY_01C2_DATUM));

        assertThat(repository.findFirstByKeyAndRegistryPolicyIdInOrderBySlotDescTxIndexDescOutputIndexDesc(POLICY_01C2, MAINNET))
                .get().extracting(Cip113RegistryNode::getTxHash).isEqualTo(TX_3);
        Map<String, ProgrammableTokenCip113> latest = reader.findByPolicyIds(List.of(POLICY_01C2, POLICY_B6F4));
        assertThat(latest).containsOnlyKeys(POLICY_01C2);
        assertThat(latest.get(POLICY_01C2).globalStatePolicyId()).isEqualTo("218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2");
        assertThat(repository.findLatestByKeys(List.of(POLICY_01C2), MAINNET)).extracting(Cip113RegistryNode::getNext)
                .containsExactly("b025efe5b44b43ed154419c66b0efc9cf148fd98f464e261018c89e8");
    }

    @Test
    @DisplayName("a rollback deletes the versions above the rollback slot, and the previous version is current again")
    void rollback() {
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, datumWithNext(POLICY_01C2, POLICY_B6F4)));
        index(200L, nodeOutput(TX_2, 0, 0, POLICY_01C2, POLICY_01C2_DATUM),
                nodeOutput(TX_2, 0, 1, POLICY_B6F4, POLICY_B6F4_DATUM));

        rollbackProcessor.handleRollback(RollbackEvent.builder()
                .rollbackTo(new Point(100L, "aa".repeat(32)))
                .currentPoint(new Point(200L, "bb".repeat(32)))
                .build());
        entityManager.clear();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(reader.findByPolicyId(POLICY_B6F4)).isEmpty();
        assertThat(repository.findFirstByKeyAndRegistryPolicyIdInOrderBySlotDescTxIndexDescOutputIndexDesc(POLICY_01C2, MAINNET))
                .get().extracting(Cip113RegistryNode::getNext).isEqualTo(POLICY_B6F4);
    }

    @Test
    @DisplayName("a replayed block writes the same rows again")
    void replay() {
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, POLICY_01C2_DATUM));
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, POLICY_01C2_DATUM));

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("each node records the registry whose NFT it holds")
    void storesTheRegistry() {
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, POLICY_01C2_DATUM));

        assertThat(repository.findAll()).extracting(Cip113RegistryNode::getRegistryPolicyId)
                .containsExactly(REGISTRY_POLICY_ID);
    }

    @Test
    @DisplayName("a registry added to the configuration keeps the registrations of the old one")
    void registryAdded() {
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, POLICY_01C2_DATUM));
        configure(REGISTRY_POLICY_ID, REGISTRY_B);
        index(200L, registryBNode(TX_2, 0, 0, POLICY_B6F4, POLICY_B6F4_DATUM));

        assertThat(reader.findByPolicyId(POLICY_01C2)).isPresent();
        assertThat(reader.findByPolicyId(POLICY_B6F4)).isPresent();
        assertThat(reader.findByPolicyIds(List.of(POLICY_01C2, POLICY_B6F4))).containsOnlyKeys(POLICY_01C2, POLICY_B6F4);
    }

    @Test
    @DisplayName("the nodes of a registry that is no longer configured are not served")
    void registryReplaced() {
        configure(REGISTRY_POLICY_ID, REGISTRY_B);
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, POLICY_01C2_DATUM),
                registryBNode(TX_1, 0, 1, POLICY_B6F4, POLICY_B6F4_DATUM));

        configure(REGISTRY_B);

        assertThat(repository.count()).as("the rows stay, only the reads change").isEqualTo(2);
        assertThat(reader.findByPolicyId(POLICY_01C2)).isEmpty();
        assertThat(reader.findByPolicyId(POLICY_B6F4)).isPresent();
        assertThat(reader.findByPolicyIds(List.of(POLICY_01C2, POLICY_B6F4))).containsOnlyKeys(POLICY_B6F4);
    }

    @Test
    @DisplayName("a policy registered in two configured registries is served from its most recent node")
    void policyInTwoRegistries() {
        configure(REGISTRY_POLICY_ID, REGISTRY_B);
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, POLICY_01C2_DATUM));
        index(200L, registryBNode(TX_2, 0, 0, POLICY_01C2, datumWithNext(POLICY_01C2, POLICY_B6F4)));

        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.findFirstByKeyAndRegistryPolicyIdInOrderBySlotDescTxIndexDescOutputIndexDesc(
                POLICY_01C2, List.of(REGISTRY_POLICY_ID, REGISTRY_B)))
                .get().extracting(Cip113RegistryNode::getRegistryPolicyId).isEqualTo(REGISTRY_B);
        assertThat(reader.findByPolicyIds(List.of(POLICY_01C2))).containsOnlyKeys(POLICY_01C2);

        configure(REGISTRY_POLICY_ID);
        assertThat(repository.findLatestByKeys(List.of(POLICY_01C2), MAINNET))
                .extracting(Cip113RegistryNode::getRegistryPolicyId).containsExactly(REGISTRY_POLICY_ID);
    }

    @Test
    @DisplayName("two registries holding a node of the same policy in one transaction are two rows; the later output wins")
    void twoRegistriesInOneTransaction() {
        configure(REGISTRY_POLICY_ID, REGISTRY_B);
        index(100L, nodeOutput(TX_1, 0, 0, POLICY_01C2, datumWithGlobalState("cc".repeat(28))),
                registryBNode(TX_1, 0, 1, POLICY_01C2, POLICY_01C2_DATUM));

        assertThat(repository.count()).as("no primary key collision").isEqualTo(2);
        String latestGlobalState = "218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2";
        assertThat(reader.findByPolicyId(POLICY_01C2)).get()
                .extracting(ProgrammableTokenCip113::globalStatePolicyId).isEqualTo(latestGlobalState);
        assertThat(reader.findByPolicyIds(List.of(POLICY_01C2)).get(POLICY_01C2).globalStatePolicyId())
                .as("the batch picks the same node as the single lookup").isEqualTo(latestGlobalState);
        assertThat(repository.findFirstByKeyAndRegistryPolicyIdInOrderBySlotDescTxIndexDescOutputIndexDesc(
                POLICY_01C2, List.of(REGISTRY_POLICY_ID, REGISTRY_B)))
                .get().extracting(Cip113RegistryNode::getOutputIndex).isEqualTo(1);
    }

    /** A node output of {@link #REGISTRY_B}: at its script address, holding its NFT. */
    private static AddressUtxo registryBNode(String txHash, int txIndex, int outputIndex, String key, String datum) {
        return output(txHash, txIndex, outputIndex, REGISTRY_B, REGISTRY_B + key, BigInteger.ONE, datum);
    }

    private void index(long slot, AddressUtxo... outputs) {
        processor.processTransaction(AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(outputs)).build()))
                .build());
        entityManager.flush();
        entityManager.clear();
    }

    /** The datum of policy 01c24df7… with another global state policy. */
    private static String datumWithGlobalState(String globalStatePolicyId) {
        return POLICY_01C2_DATUM.replace("581c218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2", "581c" + globalStatePolicyId);
    }

    /** The datum of policy 01c24df7… with another {@code next}, as after an insert behind it. */
    private static String datumWithNext(String key, String next) {
        String original = "581cb025efe5b44b43ed154419c66b0efc9cf148fd98f464e261018c89e8";
        String replacement = (next.length() == 56 ? "581c" : "581e") + next;
        return POLICY_01C2_DATUM.replace(original, replacement).replace("581c" + POLICY_01C2, "581c" + key);
    }
}
