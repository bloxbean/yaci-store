package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.DatumEntity;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.DatumRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatumStorageImpl implements DatumStorage {
    private final DatumRepository datumRepository;

    @Override
    @Transactional
    public void saveAll(Collection<Datum> datumList) {
        if (datumList == null || datumList.isEmpty()) return;

        List<DatumEntity> datumEntities = datumList.stream()
                .map(datum -> new DatumEntity(datum.getHash(), datum.getDatum(), datum.getCreatedAtTx()))
                .toList();

        datumRepository.saveAll(datumEntities);
    }

    @Override
    public Optional<Datum> getDatum(String datumHash) {
        return datumRepository.findByHash(datumHash)
                .map(datumEntity -> new Datum(datumEntity.getHash(), datumEntity.getDatum(), datumEntity.getCreatedAtTx()));
    }
}
