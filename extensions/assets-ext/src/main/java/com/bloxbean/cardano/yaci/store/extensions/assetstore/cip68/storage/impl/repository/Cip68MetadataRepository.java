package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68MetadataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface Cip68MetadataRepository extends JpaRepository<Cip68Metadata, Cip68MetadataId> {

    /** A policy ID is a 28-byte hash, 56 hex characters. */
    int POLICY_ID_HEX_LENGTH = 56;

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
     * Replaces the N+1 per-subject query pattern with a single round-trip.
     * <p>
     * Callers pass a list of {@code policy_id || asset_name} concatenated keys. Since policy IDs are
     * always exactly 56 hex characters, the concatenation is unambiguous: each key is split back into
     * its {@code policy_id} and {@code asset_name}, so the lookup can use the primary key
     * {@code (policy_id, asset_name, slot, tx_hash)} instead of scanning the table (a filter on
     * {@code CONCAT(policy_id, asset_name)} cannot use any index). The two {@code IN} lists can match
     * a few pairs that were not asked for; those are dropped, so the result is exactly the rows of the
     * requested pairs.
     * <p>
     * No label filter — see {@link #findFirstByPolicyIdAndAssetNameOrderBySlotDescTxIndexDesc} for why.
     *
     * @param concatenatedKeys list of {@code policy_id || asset_name} keys (reference-NFT asset names)
     * @return one entity per matching concatenated key; pairs with no data are omitted
     */
    default List<Cip68Metadata> findLatestByConcatenatedKeys(Collection<String> concatenatedKeys) {
        Set<String> keys = concatenatedKeys.stream()
                .filter(key -> key.length() >= POLICY_ID_HEX_LENGTH)
                .collect(Collectors.toSet());
        if (keys.isEmpty()) {
            return List.of();
        }

        Set<String> policyIds = new HashSet<>();
        Set<String> assetNames = new HashSet<>();
        for (String key : keys) {
            policyIds.add(key.substring(0, POLICY_ID_HEX_LENGTH));
            assetNames.add(key.substring(POLICY_ID_HEX_LENGTH));
        }

        return findLatestByPolicyIdsAndAssetNames(policyIds, assetNames).stream()
                .filter(row -> keys.contains(row.getPolicyId() + row.getAssetName()))
                .toList();
    }

    /**
     * Latest row per {@code (policy_id, asset_name)} pair for the pairs in the cross product of
     * {@code policyIds} and {@code assetNames}. Filters on the leading primary-key columns, so it is an
     * index lookup. Use {@link #findLatestByConcatenatedKeys} to get exactly the requested pairs.
     * <p>
     * Uses {@code ROW_NUMBER() OVER} window function for per-pair dedup. Portable across
     * PostgreSQL, H2, and MySQL 8+.
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name " +
            "ORDER BY slot DESC, tx_index DESC) AS rn FROM cip68_metadata " +
            "WHERE policy_id IN (:policyIds) AND asset_name IN (:assetNames)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<Cip68Metadata> findLatestByPolicyIdsAndAssetNames(
            @Param("policyIds") Collection<String> policyIds,
            @Param("assetNames") Collection<String> assetNames);

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
