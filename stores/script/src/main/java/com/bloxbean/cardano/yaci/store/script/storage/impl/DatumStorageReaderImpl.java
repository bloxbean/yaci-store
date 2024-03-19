package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorageReader;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.DatumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class DatumStorageReaderImpl implements DatumStorageReader {
    private final DatumRepository datumRepository;

    @Override
    public Optional<Datum> getDatum(String datumHash) {
        return datumRepository.findByHash(datumHash)
                .map(datumEntity -> new Datum(datumEntity.getHash(), datumEntity.getDatum(), datumEntity.getCreatedAtTx()));
    }
}
