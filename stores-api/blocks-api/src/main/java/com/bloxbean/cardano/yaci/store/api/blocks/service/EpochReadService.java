package com.bloxbean.cardano.yaci.store.api.blocks.service;

import com.bloxbean.cardano.yaci.store.api.blocks.storage.EpochReader;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpochReadService {
    private final EpochReader epochReader;

    public Optional<Epoch> getEpochByNumber(int epochNumber) {
        return epochReader.findByNumber(epochNumber);
    }

    public EpochsPage getEpochs(int page, int count) {
        return epochReader.findEpochs(page, count);
    }

}
