package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.service.StoreStateService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** store_state for tests that run without the core module's tables. */
class InMemoryStoreStateService implements StoreStateService {
    private final Map<String, StoreState> states = new ConcurrentHashMap<>();

    @Override
    public Optional<StoreState> getState(String stateId) {
        return Optional.ofNullable(states.get(stateId));
    }

    @Override
    public void saveState(StoreState state) {
        states.put(state.getStateId(), state);
    }

    @Override
    public void deleteState(String stateId) {
        states.remove(stateId);
    }
}
