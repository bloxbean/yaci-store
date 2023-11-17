package com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.mapper.MIRMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class MIRStorageImpl implements MIRStorage {
    private final MIRRepository repository;
    private final MIRMapper mirMapper;

    @Override
    public void save(List<MoveInstataneousReward> moveInstataneousRewards) {
        repository.saveAll(moveInstataneousRewards.stream()
                .map(mirMapper::toMIREntity)
                .toList());
    }

    @Override
    public int rollbackMIRs(Long slot) {
        return repository.deleteBySlotGreaterThan(slot);
    }
}
