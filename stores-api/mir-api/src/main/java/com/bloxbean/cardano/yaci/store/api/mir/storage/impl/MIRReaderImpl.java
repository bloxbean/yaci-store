package com.bloxbean.cardano.yaci.store.api.mir.storage.impl;

import com.bloxbean.cardano.yaci.store.api.mir.storage.MIRReader;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.jpa.mapper.MIRMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class MIRReaderImpl implements MIRReader {
    private final MIRReadRepository repository;
    private final MIRReaderMapper mirReaderMapper;
    private final MIRMapper mirMapper;

    @Override
    public List<MoveInstataneousRewardSummary> findMIRSummaries(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return repository.findRecentMIRSummaries(sortedBySlot)
                .stream()
                .map(mirReaderMapper::toMoveInstataneousRewardSummary)
                .toList();
    }

    @Override
    public List<MoveInstataneousReward> findMIRsByTxHash(String txHash) {
        return repository.findByTxHash(txHash)
                .stream()
                .map(mirMapper::toMoveInstataneousReward)
                .toList();
    }


}
