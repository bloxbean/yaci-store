package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovEpochActivityEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovEpochActivityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GovEpochActivityService {
    private final GovEpochActivityRepository govEpochActivityRepository;

    @Transactional
    public void saveGovEpochActivity(
            Integer epoch,
            Boolean dormant,
            Integer dormantEpochCount) {
        GovEpochActivityEntity entity = new GovEpochActivityEntity();

        entity.setEpoch(epoch);
        entity.setDormant(dormant);
        entity.setDormantEpochCount(dormantEpochCount);

        govEpochActivityRepository.save(entity);
    }

    public Optional<GovEpochActivityEntity> getGovEpochActivity(Integer epoch) {
        return govEpochActivityRepository.findById(epoch);
    }
}
