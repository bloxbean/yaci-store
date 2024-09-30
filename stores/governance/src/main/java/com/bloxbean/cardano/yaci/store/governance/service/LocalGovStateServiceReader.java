package com.bloxbean.cardano.yaci.store.governance.service;

import com.bloxbean.cardano.yaci.store.core.annotation.LocalSupport;
import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;
import com.bloxbean.cardano.yaci.store.governance.storage.local.LocalConstitutionStorageReader;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@LocalSupport
public class LocalGovStateServiceReader {
    private final LocalConstitutionStorageReader localConstitutionStorageReader;

    public LocalGovStateServiceReader(LocalConstitutionStorageReader localConstitutionStorageReader) {
        this.localConstitutionStorageReader = localConstitutionStorageReader;
    }

    public Optional<LocalConstitution> getCurrentConstitution() {
        return localConstitutionStorageReader.findByMaxSlot();
    }
}
