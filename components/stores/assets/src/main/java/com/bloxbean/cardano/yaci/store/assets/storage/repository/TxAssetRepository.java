package com.bloxbean.cardano.yaci.store.assets.storage.repository;

import com.bloxbean.cardano.yaci.store.assets.storage.model.TxAssetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxAssetRepository extends JpaRepository<TxAssetEntity, Long> {
    List<TxAssetEntity> findByTxHash(String txHash);

    List<TxAssetEntity> findByPolicy(String policy);

    Page<TxAssetEntity> findByPolicy(String policy, Pageable page);

    List<TxAssetEntity> findByPolicyAndAssetName(String policy, String assetName);
//    Page<TxAsset> findByPolicyAndAssetName(String policy, String assetName, Pageable page);

    int deleteBySlotGreaterThan(Long slot);
}
