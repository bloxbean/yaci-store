package com.bloxbean.cardano.yaci.store.api.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.SpecialDRepDto;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DRepApiService {
    private final DRepStorageReader dRepStorageReader;
    private final BlockStorage blockStorage;

    public List<DRepDetailsDto> getDReps(int page,  int count, Order order) {
        Integer maxEpoch = blockStorage.findRecentBlock().map(Block::getEpochNumber).orElse(null);

        if (maxEpoch == null) {
            return List.of();
        }

        return dRepStorageReader.getDReps(maxEpoch, page, count, order);
    }

    public Optional<DRepDetailsDto> getDRepDetailsByDRepId(String drepId) {
        Integer maxEpoch = blockStorage.findRecentBlock().map(Block::getEpochNumber).orElse(null);

        if (maxEpoch == null) {
            return Optional.empty();
        }

        return dRepStorageReader.getDRepDetailsByDRepId(drepId, maxEpoch);
    }

    public List<SpecialDRepDto> getAutoAbstainAndNoConfidenceDRepDetail() {
        Integer maxEpoch = blockStorage.findRecentBlock().map(Block::getEpochNumber).orElse(null);

        if (maxEpoch == null) {
            return List.of();
        }

        return dRepStorageReader.getSpecialDRepDetail(maxEpoch);
    }
}
