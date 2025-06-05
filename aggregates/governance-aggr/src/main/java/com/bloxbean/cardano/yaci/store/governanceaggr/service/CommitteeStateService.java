package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeStateEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.CommitteeStateRepository;
import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommitteeStateService {
    private final CommitteeStateRepository committeeStateRepository;

    public ConstitutionCommitteeState getCurrentCommitteeState() {
        return committeeStateRepository.findByMaxEpoch()
                .map(CommitteeStateEntity::getState)
                .orElse(ConstitutionCommitteeState.NORMAL);
    }

    public void saveCommitteeState(ConstitutionCommitteeState committeeState, int epoch) {
        CommitteeStateEntity entity = new CommitteeStateEntity();

        entity.setEpoch(epoch);
        entity.setState(committeeState);

        committeeStateRepository.save(entity);
    }
}
