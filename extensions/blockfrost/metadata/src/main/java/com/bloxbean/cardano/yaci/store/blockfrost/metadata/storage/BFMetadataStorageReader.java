package com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;

import java.util.List;

public interface BFMetadataStorageReader {

    List<BFMetadataLabelDto> findLabelsWithCount(int page, int count, Order order);
    List<TxMetadataLabel> findMetadataByLabel(String label, int page, int count, Order order);
}
