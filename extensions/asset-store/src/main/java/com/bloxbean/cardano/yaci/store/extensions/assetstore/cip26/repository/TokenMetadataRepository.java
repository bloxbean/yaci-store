package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenMetadataRepository extends JpaRepository<TokenMetadata, String> {

}
