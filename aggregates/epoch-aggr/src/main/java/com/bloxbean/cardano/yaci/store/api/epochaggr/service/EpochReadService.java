package com.bloxbean.cardano.yaci.store.api.epochaggr.service;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorageReader;
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

    public Optional<Epoch> getLatestEpoch() {
        return epochReader.findRecentEpoch();
    }

}
