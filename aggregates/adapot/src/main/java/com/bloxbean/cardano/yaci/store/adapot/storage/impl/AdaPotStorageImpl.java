package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.AdaPotRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class AdaPotStorageImpl implements AdaPotStorage {

    private final AdaPotRepository adaPotRepository;
    private final AdaPotMapper adaPotMapper;

    @Override
    public void save(AdaPot adaPot) {

    }

    @Override
    public Optional<AdaPot> findByEpoch(int epoch) {
        return Optional.empty();
    }
}
