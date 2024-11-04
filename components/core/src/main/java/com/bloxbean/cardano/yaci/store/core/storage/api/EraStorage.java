package com.bloxbean.cardano.yaci.store.core.storage.api;

import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;

import java.util.List;
import java.util.Optional;

public interface EraStorage {
    void saveEra(CardanoEra era);
    Optional<CardanoEra> findEra(int era);
    Optional<CardanoEra> findFirstNonByronEra();
    Optional<CardanoEra> findCurrentEra();

    List<CardanoEra> findAllEras();
}
