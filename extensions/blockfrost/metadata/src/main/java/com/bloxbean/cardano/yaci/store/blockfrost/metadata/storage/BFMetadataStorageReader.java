package com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;

public interface BFMetadataStorageReader {


    List<BFMetadataLabelDto> findLabelsWithCount(int page, int count, Order order);
}
