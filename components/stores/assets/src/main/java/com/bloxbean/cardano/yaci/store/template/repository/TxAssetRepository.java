package com.bloxbean.cardano.yaci.store.template.repository;

import com.bloxbean.cardano.yaci.store.template.model.TxAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TxAssetRepository extends PagingAndSortingRepository<TxAsset, Long> {
    List<TxAsset> findByTxHash(String txHash);

    List<TxAsset> findByPolicy(String policy);

    Page<TxAsset> findByPolicy(String policy, Pageable page);

    List<TxAsset> findByPolicyAndAssetName(String policy, String assetName);
//    Page<TxAsset> findByPolicyAndAssetName(String policy, String assetName, Pageable page);

    int deleteBySlotGreaterThan(Long slot);
}
