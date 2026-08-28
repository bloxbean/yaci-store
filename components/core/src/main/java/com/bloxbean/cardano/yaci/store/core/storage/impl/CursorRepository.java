package com.bloxbean.cardano.yaci.store.core.storage.impl;

import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.CursorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CursorRepository extends JpaRepository<CursorEntity, CursorId> {
    Optional<CursorEntity> findTopByIdOrderBySlotDesc(Long id);

    Optional<CursorEntity> findTopByIdAndSlotBeforeOrderBySlotDesc(Long id, Long slot);

    Optional<CursorEntity> findByIdAndBlockHash(Long id, String blockHash);

    // Set-based (bulk) deletes. These were Spring Data DERIVED deletes, which Spring implements by
    // SELECTing every matching row into the persistence context and removing them one entity at a
    // time -- so memory scales with the number of rows deleted. When cursor_ is large (e.g. the
    // cleanup below has never completed since genesis) a single run loads millions of CursorEntity
    // objects and throws OutOfMemoryError; the cursor then never prunes, so the failure is permanent
    // and self-reinforcing. A @Modifying @Query issues one SQL DELETE with memory independent of the
    // number of rows removed.
    @Modifying
    @Query("delete from CursorEntity c where c.id = :id and c.slot > :slot")
    int deleteByIdAndSlotGreaterThan(@Param("id") Long id, @Param("slot") Long slot);

    //Required for history cleanup
    @Modifying
    @Query("delete from CursorEntity c where c.id = :id and c.block < :block")
    int deleteByIdAndBlockLessThan(@Param("id") Long id, @Param("block") Long block);
}
