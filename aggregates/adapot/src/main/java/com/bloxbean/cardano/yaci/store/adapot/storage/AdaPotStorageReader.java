package com.bloxbean.cardano.yaci.store.adapot.storage;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;

import java.util.List;

public interface AdaPotStorageReader {
    List<AdaPot> getAdaPots(int page, int count);
}
