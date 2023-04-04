package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.persistence.EpochPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpochService {
    private final EpochPersistence epochPersistence;

    public Optional<Epoch> getEpochByNumber(int epochNumber) {
        return epochPersistence.findByNumber(epochNumber);
    }

    public EpochsPage getEpochs(int page, int count) {
        return epochPersistence.findEpochs(page, count);
    }
}
