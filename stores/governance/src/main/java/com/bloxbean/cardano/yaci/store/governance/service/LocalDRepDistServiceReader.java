package com.bloxbean.cardano.yaci.store.governance.service;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalDRepDistr;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalDRepDistrStorageReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
public class LocalDRepDistServiceReader {
    private final LocalDRepDistrStorageReader localDRepDistrStorageReader;

    public LocalDRepDistServiceReader(LocalDRepDistrStorageReader localDRepDistrStorageReader) {
        this.localDRepDistrStorageReader = localDRepDistrStorageReader;
    }

    public Optional<LocalDRepDistr> getLatestDRepDistrByDRepHashAndEpoch(String dRepHash) {
        return localDRepDistrStorageReader.findLatestLocalDRepDistrByDRepHash(dRepHash);
    }
}
