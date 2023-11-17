package com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository;

import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model.ProtocolParamsProposalEntity;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model.ProtocolParamsProposalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProtocolParamsProposalRepository extends JpaRepository<ProtocolParamsProposalEntity, ProtocolParamsProposalId> {

    List<ProtocolParamsProposalEntity> findByTargetEpoch(int targetEpoch);

    int deleteBySlotGreaterThan(Long slot);
}
