package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.AdaPotRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class AdaPotStorageImpl implements AdaPotStorage {

    private final AdaPotRepository adaPotRepository;
    private final AdaPotMapper adaPotMapper;

    @Override
    public void save(@NonNull AdaPot adaPot) {
        adaPotRepository.save(adaPotMapper.toAdaPotEntity(adaPot));
    }

    @Override
    public Optional<AdaPot> findRecentByEpoch(long epoch) {
        return adaPotRepository.findRecentByEpoch(epoch).map(adaPotMapper::toAdaPot);
    }

    @Override
    public Optional<AdaPot> findByEpoch(long epoch) {
        return adaPotRepository.findByEpoch(epoch).map(adaPotMapper::toAdaPot);
    }

    @Override
    public Optional<AdaPot> findByEpochAtEpochBoundary(long epoch) {
        return adaPotRepository.findByEpochAtEpochBoundary(epoch).map(adaPotMapper::toAdaPot);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return adaPotRepository.deleteBySlotGreaterThan(slot);
    }
}
