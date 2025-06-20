package com.bloxbean.cardano.yaci.store.core.storage.api;

import com.bloxbean.cardano.yaci.store.events.ErrorEvent;

import java.util.List;

public interface ErrorStorage {
    void save(ErrorEvent error);

    ErrorEvent findById(Integer id);
    List<ErrorEvent> findAll();
}
