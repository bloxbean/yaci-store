package com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.model.MIREntity;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public interface MIRRepository
        extends JpaRepository<MIREntity, Long> {
    int deleteBySlotGreaterThan(Long slot);
}
