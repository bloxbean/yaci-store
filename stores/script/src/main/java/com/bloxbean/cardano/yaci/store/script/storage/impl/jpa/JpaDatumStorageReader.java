package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorageReader;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaDatumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class JpaDatumStorageReader implements DatumStorageReader {

    private final JpaDatumRepository jpaDatumRepository;

    @Override
    public Optional<Datum> getDatum(String datumHash) {
        return jpaDatumRepository.findByHash(datumHash)
                .map(datumEntity -> new Datum(datumEntity.getHash(), datumEntity.getDatum(), datumEntity.getCreatedAtTx()));
    }
}
