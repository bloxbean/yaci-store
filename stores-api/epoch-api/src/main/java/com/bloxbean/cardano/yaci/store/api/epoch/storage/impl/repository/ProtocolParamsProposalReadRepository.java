package com.bloxbean.cardano.yaci.store.api.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model.ProtocolParamsProposalEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository.ProtocolParamsProposalRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtocolParamsProposalReadRepository extends ProtocolParamsProposalRepository {
    List<ProtocolParamsProposalEntity> findByEpoch(int epoch);
}
