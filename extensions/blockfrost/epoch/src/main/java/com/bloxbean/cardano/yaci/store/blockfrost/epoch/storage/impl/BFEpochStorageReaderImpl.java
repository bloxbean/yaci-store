package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.BFEpochStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository.BFBlockRepository;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository.BFEpochRepository;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.repository.BFEpochStakeRepository;
import com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl.model.BFEpochSumProjection;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper.EpochMapper;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BFEpochStorageReaderImpl implements BFEpochStorageReader {
    private final BFEpochRepository epochRepository;
    private final BFBlockRepository blockRepository;
    private final ObjectProvider<BFEpochStakeRepository> epochStakeRepositoryProvider;
    private final EpochMapper epochMapper;

    @Override
    public List<Epoch> findNextEpochs(int epoch, int page, int count) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), count);
        List<EpochEntity> epochs = epochRepository.findNextEpochs(epoch, pageable);
        return epochs.stream()
                .map(epochMapper::toEpoch)
                .collect(Collectors.toList());
    }

    @Override
    public List<Epoch> findPreviousEpochs(int epoch, int page, int count) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), count);
        List<EpochEntity> epochs = epochRepository.findPreviousEpochs(epoch, pageable);
        return epochs.stream()
                .map(epochMapper::toEpoch)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> findBlockHashesByEpoch(int epoch, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), count, toSort(order));
        return blockRepository.findBlockHashesByEpoch(epoch, pageable);
    }

    @Override
    public List<String> findBlockHashesByEpochAndPool(int epoch, String poolId, int page, int count, Order order) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), count, toSort(order));
        return blockRepository.findBlockHashesByEpochAndPool(epoch, poolId, pageable);
    }

    @Override
    public Map<Integer, BigInteger> getActiveStakesByEpochs(List<Integer> epochs) {
        if (epochs == null || epochs.isEmpty()) {
            return Collections.emptyMap();
        }

        BFEpochStakeRepository epochStakeRepository = epochStakeRepositoryProvider.getIfAvailable();
        if (epochStakeRepository == null) {
            return Collections.emptyMap();
        }

        List<BFEpochSumProjection> rows = epochStakeRepository.getActiveStakesByEpochs(epochs);
        return rows.stream()
                .filter(row -> row != null && row.getEpoch() != null && row.getTotal() != null)
                .collect(Collectors.toMap(
                        BFEpochSumProjection::getEpoch,
                        BFEpochSumProjection::getTotal
                ));
    }

    private Sort toSort(Order order) {
        Sort.Direction direction = order == Order.desc ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, "number");
    }


}
