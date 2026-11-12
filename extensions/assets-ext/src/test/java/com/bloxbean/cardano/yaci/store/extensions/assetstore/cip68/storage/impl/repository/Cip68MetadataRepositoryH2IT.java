package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * {@link Cip68MetadataRepository#findLatestByConcatenatedKeys} against a real H2 {@code cip68_metadata}
 * table created by the module's own migrations: exactly the requested {@code (policy_id, asset_name)}
 * pairs, latest row per pair, also when the request is split into several queries.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.schema-locations=classpath:db/store/h2/V0_1700_1__init.sql,classpath:db/store/h2/V0_1700_2__validation_errors.sql"
})
@DisplayName("Cip68MetadataRepository.findLatestByConcatenatedKeys on H2")
class Cip68MetadataRepositoryH2IT {

    private static final String POLICY_1 = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String POLICY_2 = "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344";
    private static final String REF_A = "000643b0414141";
    private static final String REF_B = "000643b0424242";

    @SpringBootConfiguration
    @EntityScan(basePackageClasses = Cip68Metadata.class)
    @EnableJpaRepositories(basePackageClasses = Cip68MetadataRepository.class)
    static class Config {
    }

    @Autowired
    private Cip68MetadataRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("returns the latest row per requested pair, by slot and then tx_index")
    void returnsLatestRowPerPair() {
        save(POLICY_1, REF_A, 100, "11", 0, "a-v1");
        save(POLICY_1, REF_A, 200, "22", 0, "a-v2");
        save(POLICY_1, REF_A, 200, "33", 1, "a-v3");   // same slot, later in the block
        save(POLICY_2, REF_B, 150, "44", 0, "b-v1");

        List<Cip68Metadata> rows = repository.findLatestByConcatenatedKeys(
                List.of(POLICY_1 + REF_A, POLICY_2 + REF_B));

        assertThat(rows).extracting(Cip68Metadata::getPolicyId, Cip68Metadata::getAssetName, Cip68Metadata::getName)
                .containsExactlyInAnyOrder(tuple(POLICY_1, REF_A, "a-v3"), tuple(POLICY_2, REF_B, "b-v1"));
    }

    @Test
    @DisplayName("does not return a pair that was not requested, even if it matches both IN lists")
    void excludesUnrequestedCrossProductPair() {
        save(POLICY_1, REF_A, 100, "11", 0, "p1-a");
        save(POLICY_2, REF_B, 100, "22", 0, "p2-b");
        save(POLICY_1, REF_B, 100, "33", 0, "p1-b");   // policy 1 and asset B are both requested, the pair is not

        List<Cip68Metadata> rows = repository.findLatestByConcatenatedKeys(
                List.of(POLICY_1 + REF_A, POLICY_2 + REF_B));

        assertThat(rows).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("p1-a", "p2-b");
    }

    @Test
    @DisplayName("the query itself drops an unrequested cross-product pair, before ranking")
    void queryExcludesUnrequestedCrossProductPair() {
        save(POLICY_1, REF_A, 100, "11", 0, "p1-a");
        save(POLICY_2, REF_B, 100, "22", 0, "p2-b");
        save(POLICY_1, REF_B, 100, "33", 0, "p1-b");   // matches both IN lists, but is not a requested key

        List<Cip68Metadata> rows = repository.findLatestByPolicyIdsAndAssetNames(
                Set.of(POLICY_1, POLICY_2), Set.of(REF_A, REF_B), List.of(POLICY_1 + REF_A, POLICY_2 + REF_B));

        assertThat(rows).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("p1-a", "p2-b");
    }

    @Test
    @DisplayName("splits a request larger than MAX_KEYS_PER_QUERY into several queries and returns every match once")
    void splitsLargeRequests() {
        int keyCount = 2 * Cip68MetadataRepository.MAX_KEYS_PER_QUERY + 1;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < keyCount; i++) {
            keys.add(POLICY_1 + "000643b0" + String.format("%06x", i));
        }
        // one stored pair in each of the three queries: first key, first key of the second query, last key
        int[] stored = {0, Cip68MetadataRepository.MAX_KEYS_PER_QUERY, keyCount - 1};
        for (int i : stored) {
            save(POLICY_1, keys.get(i).substring(Cip68MetadataRepository.POLICY_ID_HEX_LENGTH), 100,
                    String.format("%02x", i % 256), 0, "key-" + i);
        }
        keys.add(keys.get(0));   // a duplicate key is looked up once

        assertThat(repository.findLatestByConcatenatedKeys(keys)).extracting(Cip68Metadata::getName)
                .containsExactlyInAnyOrder("key-0", "key-" + Cip68MetadataRepository.MAX_KEYS_PER_QUERY,
                        "key-" + (keyCount - 1));
    }

    @Test
    @DisplayName("omits requested pairs without rows and returns nothing for an empty request")
    void omitsMissingPairsAndHandlesEmptyRequest() {
        save(POLICY_1, REF_A, 100, "11", 0, "p1-a");

        assertThat(repository.findLatestByConcatenatedKeys(List.of(POLICY_1 + REF_A, POLICY_2 + REF_A)))
                .extracting(Cip68Metadata::getName).containsExactly("p1-a");
        assertThat(repository.findLatestByConcatenatedKeys(List.of())).isEmpty();
    }

    private void save(String policyId, String assetName, long slot, String txByte, int txIndex, String name) {
        repository.save(Cip68Metadata.builder()
                .policyId(policyId).assetName(assetName).slot(slot).txHash(txByte.repeat(32)).txIndex(txIndex)
                .label(333).version(1L).datum("00").name(name).lastSyncedAt(LocalDateTime.now())
                .build());
        entityManager.flush();
        entityManager.clear();
    }
}
