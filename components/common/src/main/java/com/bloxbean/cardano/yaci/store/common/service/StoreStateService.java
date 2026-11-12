package com.bloxbean.cardano.yaci.store.common.service;

import com.bloxbean.cardano.yaci.store.common.domain.StoreState;

import java.util.Optional;

/**
 * Reads and writes store_state rows. Calls join the caller's transaction, so a module can move its
 * state in the same transaction as the writes it describes.
 */
public interface StoreStateService {
    Optional<StoreState> getState(String stateId);
    void saveState(StoreState state);
    void deleteState(String stateId);
}
