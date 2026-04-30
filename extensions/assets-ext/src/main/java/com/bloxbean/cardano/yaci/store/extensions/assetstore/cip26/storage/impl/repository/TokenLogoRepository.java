package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenLogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenLogoRepository extends JpaRepository<TokenLogo, String> {

}
