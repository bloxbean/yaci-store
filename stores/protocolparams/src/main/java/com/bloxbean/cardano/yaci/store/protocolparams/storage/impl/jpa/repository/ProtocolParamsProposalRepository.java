package com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.ProtocolParamsProposalEntity;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.model.ProtocolParamsProposalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtocolParamsProposalRepository extends JpaRepository<ProtocolParamsProposalEntity, ProtocolParamsProposalId> {

    List<ProtocolParamsProposalEntity> findByTargetEpoch(int targetEpoch);

    List<ProtocolParamsProposalEntity> findByEpoch(int epoch);

    int deleteBySlotGreaterThan(Long slot);
}
