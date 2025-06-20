package com.bloxbean.cardano.yaci.store.core.storage;

import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.impl.ErrorRepository;
import com.bloxbean.cardano.yaci.store.core.storage.impl.model.ErrorEntity;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ErrorStorageImpl implements ErrorStorage {
    private final ErrorRepository errorRepository;

    @Override
    public void save(ErrorEvent error) {
        if (error == null) return;

        ErrorEntity errorEntity = ErrorEntity.builder()
                .block(error.getBlock())
                .errorCode(error.getErrorCode())
                .reason(error.getReason())
                .details(error.getDetails())
                .build();

        errorRepository.save(errorEntity);
    }

    @Override
    public ErrorEvent findById(Integer id) {
        return errorRepository.findById(id)
                .map(errorEntity -> ErrorEvent.builder()
                        .id(errorEntity.getId())
                        .block(errorEntity.getBlock())
                        .errorCode(errorEntity.getErrorCode())
                        .reason(errorEntity.getReason())
                        .details(errorEntity.getDetails())
                        .build())
                .orElse(null);
    }

    @Override
    public List<ErrorEvent> findAll() {
        return errorRepository.findAllOrderById()
                .stream()
                .map(errorEntity -> ErrorEvent.builder()
                        .id(errorEntity.getId())
                        .block(errorEntity.getBlock())
                        .errorCode(errorEntity.getErrorCode())
                        .reason(errorEntity.getReason())
                        .details(errorEntity.getDetails())
                        .build())
                .toList();
    }
}
