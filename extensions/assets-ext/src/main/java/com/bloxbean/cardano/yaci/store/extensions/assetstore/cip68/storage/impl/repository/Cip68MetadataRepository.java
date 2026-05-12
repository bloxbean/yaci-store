package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68MetadataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface Cip68MetadataRepository extends JpaRepository<Cip68Metadata, Cip68MetadataId> {

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
    Optional<Cip68Metadata> findFirstByPolicyIdAndAssetNameOrderBySlotDesc(
            String policyId, String assetName);

    /**
     * Returns the latest reference NFT row per {@code (policy_id, asset_name)} pair, regardless of label.
     * Replaces the N+1 per-subject query pattern with a single round-trip.
     * <p>
     * Callers pass a list of {@code policy_id || asset_name} concatenated keys. Since policy IDs are
     * always exactly 56 hex characters, the concatenation is unambiguous — the DB can split the key
     * back if needed, though this method matches directly against the concatenated form.
     * <p>
     * Uses {@code ROW_NUMBER() OVER} window function for per-pair dedup. Portable across
     * PostgreSQL, H2, and MySQL 8+.
     * <p>
     * No label filter — see {@link #findFirstByPolicyIdAndAssetNameOrderBySlotDesc} for why.
     *
     * @param concatenatedKeys list of {@code policy_id || asset_name} keys (reference-NFT asset names)
     * @return one entity per matching concatenated key; pairs with no data are omitted
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name " +
            "ORDER BY slot DESC) AS rn FROM cip68_metadata " +
            "WHERE CONCAT(policy_id, asset_name) IN (:concatenatedKeys)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<Cip68Metadata> findLatestByConcatenatedKeys(
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
