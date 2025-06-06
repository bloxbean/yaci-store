package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;

import java.util.Collection;

public interface DatumStorage {
    void saveAll(Collection<Datum> datumList);

    void handleCommit(CommitEvent commitEvent);

}
