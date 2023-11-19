package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.DatumEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.DatumRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class DatumStorageImpl implements DatumStorage {
    private final DatumRepository datumRepository;
    private Set<Datum> datumCache = Collections.synchronizedSet(new HashSet<>());

    @Override
    @Transactional
    public void saveAll(Collection<Datum> datumList) {
        if (datumList == null || datumList.isEmpty()) return;

        datumCache.addAll(datumList);
    }

    @EventListener
    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        try {
            List<DatumEntity> datumEntities = datumCache.stream()
                    .filter(datum -> !datumRepository.existsById(datum.getHash()))
                    .map(datum -> new DatumEntity(datum.getHash(), datum.getDatum(), datum.getCreatedAtTx()))
                    .toList();

            datumRepository.saveAll(datumEntities);
        } finally {
            datumCache.clear();
        }
    }
}