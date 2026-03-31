package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, String> {

    List<TokenMetadata> findByPolicy(String policy);

    List<TokenMetadata> findByPolicyIn(Collection<String> policies);

    Slice<TokenMetadata> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Slice<TokenMetadata> findByTicker(String ticker, Pageable pageable);

    Slice<TokenMetadata> findByTickerIgnoreCase(String ticker, Pageable pageable);

    long countByPolicyNotNull();
}
