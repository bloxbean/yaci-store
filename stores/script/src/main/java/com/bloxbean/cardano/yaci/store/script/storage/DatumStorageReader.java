package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.script.domain.Datum;

import java.util.Optional;

public interface DatumStorageReader {
    Optional<Datum> getDatum(String datumHash);
}
