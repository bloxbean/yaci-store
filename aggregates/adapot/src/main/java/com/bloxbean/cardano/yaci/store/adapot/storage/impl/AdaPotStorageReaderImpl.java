package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.AdaPotStorageReader;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.AdaPotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

@RequiredArgsConstructor
public class AdaPotStorageReaderImpl implements AdaPotStorageReader {
    private final AdaPotRepository adaPotRepository;
    private final AdaPotMapper adaPotMapper;

    @Override
    public List<AdaPot> getAdaPots(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        return adaPotRepository.findAdaPots(sortedBySlot)
                .stream().map(adaPotMapper::toAdaPot)
                .toList();
    }
}
