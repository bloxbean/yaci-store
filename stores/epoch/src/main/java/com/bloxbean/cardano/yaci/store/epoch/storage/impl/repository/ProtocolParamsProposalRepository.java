package com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.JpaProtocolParamsProposalEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.model.ProtocolParamsProposalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtocolParamsProposalRepository extends JpaRepository<JpaProtocolParamsProposalEntity, ProtocolParamsProposalId> {

    List<JpaProtocolParamsProposalEntity> findByTargetEpoch(int targetEpoch);

    int deleteBySlotGreaterThan(Long slot);

    //Optional read queries
    List<JpaProtocolParamsProposalEntity> findByEpoch(int epoch);
}
