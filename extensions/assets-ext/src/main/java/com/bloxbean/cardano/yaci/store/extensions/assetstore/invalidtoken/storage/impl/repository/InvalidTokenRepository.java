package com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.storage.impl.model.InvalidToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface InvalidTokenRepository extends JpaRepository<InvalidToken, String> {

    /** A chain rollback: what was found after the rollback slot is gone with the blocks. Rows with no slot (from Git) are not touched. */
    @Transactional
    @Modifying
    @Query("DELETE FROM InvalidToken t WHERE t.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

    /** What an off-chain file had wrong is forgotten when the file is processed again or removed, so the table says what is wrong now. */
    @Transactional
    @Modifying
    @Query("DELETE FROM InvalidToken t WHERE t.standard = :standard AND t.origin = :origin")
    int deleteByStandardAndOrigin(@Param("standard") String standard, @Param("origin") String origin);
}
