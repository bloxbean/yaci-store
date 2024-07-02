package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepInfoEntity;

import java.util.Collection;
import java.util.List;

public interface DRepInfoRepository {
    List<DRepInfoEntity> findAllByDrepHashIn(Collection<String> drepHashes);
}
