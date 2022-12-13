package com.bloxbean.cardano.yaci.store.repository;

import com.bloxbean.cardano.yaci.store.model.CursorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CursorRepository extends JpaRepository<CursorEntity, Long> {

}
