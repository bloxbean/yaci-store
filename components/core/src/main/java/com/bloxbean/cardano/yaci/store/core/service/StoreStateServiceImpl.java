package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.common.domain.StoreState;
import com.bloxbean.cardano.yaci.store.common.service.StoreStateService;
import com.bloxbean.cardano.yaci.store.core.storage.impl.StoreStateRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.StoreStateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StoreStateServiceImpl implements StoreStateService {
    private final StoreStateRepository storeStateRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<StoreState> getState(String stateId) {
        return storeStateRepository.findById(stateId)
                .map(entity -> StoreState.builder()
                        .stateId(entity.getStateId())
                        .status(entity.getStatus())
                        .slot(entity.getSlot())
                        .block(entity.getBlock())
                        .blockHash(entity.getBlockHash())
                        .data(entity.getData())
                        .updateDateTime(entity.getUpdateDateTime())
                        .build());
    }

    @Override
    @Transactional
    public void saveState(StoreState state) {
        storeStateRepository.save(StoreStateEntity.builder()
                .stateId(state.getStateId())
                .status(state.getStatus())
                .slot(state.getSlot())
                .block(state.getBlock())
                .blockHash(state.getBlockHash())
                .data(state.getData())
                .build());
    }

    @Override
    @Transactional
    public void deleteState(String stateId) {
        storeStateRepository.deleteById(stateId);
    }
}
