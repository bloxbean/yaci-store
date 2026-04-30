package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNftId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataReferenceNftRepository extends JpaRepository<MetadataReferenceNft, MetadataReferenceNftId> {

    Optional<MetadataReferenceNft> findFirstByPolicyIdAndAssetNameAndLabelOrderBySlotDesc(
            String policyId, String assetName, int label);

    /**
     * Returns the latest reference NFT row per {@code (policy_id, asset_name)} pair, filtered by label.
     * Replaces the N+1 per-subject query pattern with a single round-trip.
     * <p>
     * Callers pass a list of {@code policy_id || asset_name} concatenated keys. Since policy IDs are
     * always exactly 56 hex characters, the concatenation is unambiguous — the DB can split the key
     * back if needed, though this method matches directly against the concatenated form.
     * <p>
     * Uses {@code ROW_NUMBER() OVER} window function with a label filter for per-pair dedup.
     * Portable across PostgreSQL, H2, and MySQL 8+.
     * <p>
     * For very large {@code metadata_reference_nft} tables, add the optional index
     * {@code idx_metadata_reference_nft_policy_label} from {@code optional-indexes.sql}
     * so the label-filtered scan becomes an index-only scan.
     *
     * @param concatenatedKeys list of {@code policy_id || asset_name} keys (reference-NFT asset names)
     * @param label            CIP-68 label to filter by (e.g. 333 for fungible tokens)
     * @return one entity per matching concatenated key; pairs with no data are omitted
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY policy_id, asset_name " +
            "ORDER BY slot DESC) AS rn FROM metadata_reference_nft WHERE label = :label " +
            "AND CONCAT(policy_id, asset_name) IN (:concatenatedKeys)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<MetadataReferenceNft> findLatestByConcatenatedKeys(
            @Param("concatenatedKeys") Collection<String> concatenatedKeys,
            @Param("label") int label);

    int deleteBySlotGreaterThan(Long slot);
}
