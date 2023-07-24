package com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.mapper.MIRMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    public List<MoveInstataneousRewardSummary> findMIRSummaries(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return repository.findRecentMIRSummaries(sortedBySlot)
                .stream()
                .map(mirMapper::toMoveInstataneousRewardSummary)
                .toList();
    }

    @Override
    public List<MoveInstataneousReward> findMIRsByTxHash(String txHash) {
        return repository.findByTxHash(txHash)
                .stream()
                .map(mirMapper::toMoveInstataneousReward)
                .toList();
    }


    @Override
    public int rollbackMIRs(Long slot) {
        return repository.deleteBySlotGreaterThan(slot);
    }
}
