package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68MetadataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface Cip68MetadataRepository extends JpaRepository<Cip68Metadata, Cip68MetadataId> {

    /** A policy ID is a 28-byte hash, 56 hex characters. */
    int POLICY_ID_HEX_LENGTH = 56;

    /** Most keys {@link #findLatestByConcatenatedKeys} sends in one query; larger requests are split. */
    int MAX_KEYS_PER_QUERY = 1000;

    /**
     * Returns the latest row for a given {@code (policy_id, asset_name)} reference NFT,
     * regardless of label.
     * <p>
     * The cross-output classifier in {@code Cip68Processor} tags each reference-NFT update row
     * with the co-minted user-token label observed in that transaction. A single reference NFT
     * therefore accumulates rows under multiple labels over its lifetime (e.g. an FT whose
     * later updates happened to be co-minted alongside a 222-prefixed NFT will have its newest
     * row stored under label 222). Filtering by label here would silently return stale metadata.
     */
    Optional<Cip68Metadata> findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc(
            String policyId, String assetName);

    /** Whether any datum of this reference NFT is stored, whatever its label. */
    boolean existsByPolicyIdAndAssetName(String policyId, String assetName);

    /**
     * Returns the latest reference NFT row per {@code (policy_id, asset_name)} pair, regardless of label.
     * Replaces the N+1 per-subject query pattern with one query per {@value #MAX_KEYS_PER_QUERY} keys.
     * <p>
     * Callers pass {@code policy_id || asset_name} concatenated keys. Since policy IDs are always exactly 56 hex
     * characters, each key is split back into its {@code policy_id} and {@code asset_name}, and the query filters on
     * both:
     * <ul>
     *   <li>{@code policy_id IN (...) AND asset_name IN (...)} lets the database use the primary key
     *       {@code (policy_id, asset_name, slot, tx_hash)}. A filter on {@code CONCAT(policy_id, asset_name)} alone
     *       cannot use any index and scans the whole table.</li>
     *   <li>The two lists also match pairs that were not asked for (a requested policy combined with an asset name
     *       requested for another policy). {@code CONCAT(policy_id, asset_name) IN (...)} drops those rows in the
     *       database, before they are ranked and returned, so only the requested pairs are ranked and loaded.</li>
     * </ul>
     * Large requests are split into queries of at most {@value #MAX_KEYS_PER_QUERY} keys, so callers can pass any
     * number of keys without batching on their side:
     * <ul>
     *   <li>each query binds at most three parameters per key, far below the 65,535 bind parameters a PostgreSQL
     *       statement accepts;</li>
     *   <li>the pairs the two {@code IN} lists can match grow with the number of policies times the number of asset
     *       names, so a bounded batch also bounds the index lookups of one query.</li>
     * </ul>
     * The result stays exact: every key is in exactly one query, and the latest row is chosen per pair.
     * <p>
     * No label filter — see {@link #findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc} for why.
     *
     * @param concatenatedKeys {@code policy_id || asset_name} keys (reference-NFT asset names); keys shorter than a
     *                         policy ID are skipped
     * @return one entity per requested key that has data; keys with no data are omitted
     */
    default List<Cip68Metadata> findLatestByConcatenatedKeys(Collection<String> concatenatedKeys) {
        List<String> keys = concatenatedKeys.stream()
                .filter(key -> key.length() >= POLICY_ID_HEX_LENGTH)
                .distinct()
                .toList();

        List<Cip68Metadata> rows = new ArrayList<>();
        for (int from = 0; from < keys.size(); from += MAX_KEYS_PER_QUERY) {
            List<String> chunk = keys.subList(from, Math.min(from + MAX_KEYS_PER_QUERY, keys.size()));
            Set<String> policyIds = new HashSet<>();
            Set<String> assetNames = new HashSet<>();
            for (String key : chunk) {
                policyIds.add(key.substring(0, POLICY_ID_HEX_LENGTH));
                assetNames.add(key.substring(POLICY_ID_HEX_LENGTH));
            }
            rows.addAll(findLatestByPolicyIdsAndAssetNames(policyIds, assetNames, chunk));
        }
        return rows;
    }

    /**
     * Latest row per requested {@code (policy_id, asset_name)} pair. Use {@link #findLatestByConcatenatedKeys}, which
     * derives the three arguments from the keys and splits large requests; see there for why the query filters on
     * both the {@code IN} lists and the concatenated keys. {@code policyIds} and {@code assetNames} must be the
     * policy IDs and asset names of {@code concatenatedKeys}: the {@code IN} lists select the rows through the primary
     * key, and the concatenated keys keep only the requested pairs.
     * <p>
     * Uses {@code ROW_NUMBER() OVER} window function for per-pair dedup. Portable across
     * PostgreSQL, H2, and MySQL 8+.
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name " +
            "ORDER BY slot DESC, tx_index DESC) AS rn FROM cip68_metadata " +
            "WHERE policy_id IN (:policyIds) AND asset_name IN (:assetNames) " +
            "AND CONCAT(policy_id, asset_name) IN (:concatenatedKeys)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<Cip68Metadata> findLatestByPolicyIdsAndAssetNames(
            @Param("policyIds") Collection<String> policyIds,
            @Param("assetNames") Collection<String> assetNames,
            @Param("concatenatedKeys") Collection<String> concatenatedKeys);

    /**
     * Bulk delete on rollback. {@code @Modifying @Query} with JPQL issues a single SQL DELETE;
     * without these annotations Spring Data's derived deleteBy* loads each entity then deletes
     * per-row (one round-trip per matching row), which is pathological for deep rollbacks.
     * Caller ({@code Cip68RollbackProcessor.handleRollback}) is already {@code @Transactional}.
     */
    @Modifying
    @Query("DELETE FROM Cip68Metadata e WHERE e.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

}
