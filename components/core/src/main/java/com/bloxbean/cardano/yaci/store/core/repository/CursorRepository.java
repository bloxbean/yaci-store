package com.bloxbean.cardano.yaci.store.core.repository;

import com.bloxbean.cardano.yaci.store.core.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.core.model.CursorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursorRepository extends JpaRepository<CursorEntity, CursorId> {
    List<CursorEntity> findTop50ByIdOrderBySlotDesc(Long id);
    Optional<CursorEntity> findTopByIdOrderBySlotDesc(Long id);

    Optional<CursorEntity> findByBlockHash(String blockHash);
    int deleteBySlotGreaterThan(Long slot);
}
