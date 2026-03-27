package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, String> {

    List<TokenMetadata> findByPolicy(String policy);

    Slice<TokenMetadata> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Slice<TokenMetadata> findByTicker(String ticker, Pageable pageable);

    Slice<TokenMetadata> findByTickerIgnoreCase(String ticker, Pageable pageable);

    long countByPolicyNotNull();
}
