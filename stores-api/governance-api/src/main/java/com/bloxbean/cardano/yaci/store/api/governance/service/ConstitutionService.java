package com.bloxbean.cardano.yaci.store.api.governance.service;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;
import com.bloxbean.cardano.yaci.store.governance.storage.ConstitutionStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConstitutionService {
    private final ConstitutionStorageReader constitutionStorageReader;

    public Optional<Constitution> findCurrentConstitution() {
        return constitutionStorageReader.findCurrentConstitution();
    }
}
