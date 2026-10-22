package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface Cip68UserTokenRepository extends JpaRepository<Cip68UserToken, String> {

    /** A chain rollback: the tokens first seen after the rollback slot are gone with the blocks. */
    @Transactional
    @Modifying
    @Query("DELETE FROM Cip68UserToken t WHERE t.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);
}
