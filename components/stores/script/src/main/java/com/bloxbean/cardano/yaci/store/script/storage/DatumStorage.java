package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.script.domain.Datum;

import java.util.Collection;
import java.util.Optional;

public interface DatumStorage {
    void saveAll(Collection<Datum> datumList);

    Optional<Datum> getDatum(String datumHash);
}
