package com.bloxbean.cardano.yaci.store.api.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DRepApiService {
    private final DRepStorageReader dRepStorageReader;
    private final EpochParamStorage epochParamStorage;

    public List<DRepDetailsDto> getDReps(int page,  int count, Order order) {
        Integer maxEpoch = epochParamStorage.getMaxEpoch();

        return dRepStorageReader.getDReps(maxEpoch, page, count, order);
    }

    public Optional<DRepDetailsDto> getDRepDetailsByDRepId(String drepId) {
        Integer maxEpoch = epochParamStorage.getMaxEpoch();

        return dRepStorageReader.getDRepDetailsByDRepId(drepId, maxEpoch);
    }
}
