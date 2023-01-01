package com.bloxbean.cardano.yaci.store.repository;

import com.bloxbean.cardano.yaci.store.model.CursorEntity;
import com.bloxbean.cardano.yaci.store.model.CursorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CursorRepository extends JpaRepository<CursorEntity, CursorId> {
    List<CursorEntity> findTop50ByIdOrderByBlockDesc(Long id);
}
