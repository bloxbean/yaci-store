package com.bloxbean.cardano.yaci.store.core.storage;

import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;

import java.util.Optional;

public interface EraStorage {
    void saveEra(CardanoEra era);
    Optional<CardanoEra> findEra(int era);
    Optional<CardanoEra> findFirstNonByronEra();
}
