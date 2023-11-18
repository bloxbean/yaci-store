package com.bloxbean.cardano.yaci.store.api.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.storage.EpochStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpochReadService {
    private final EpochStorageReader epochReader;

    public Optional<Epoch> getEpochByNumber(int epochNumber) {
        return epochReader.findByNumber(epochNumber);
    }

    public EpochsPage getEpochs(int page, int count) {
        return epochReader.findEpochs(page, count);
    }

}
