package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

public interface DRepStorageReader {
    List<DRepDetailsDto> getDReps(int epoch, int page, int count, Order order);
    Optional<DRepDetailsDto> getDRepDetailsByDRepId(String drepId, int epoch);
}
