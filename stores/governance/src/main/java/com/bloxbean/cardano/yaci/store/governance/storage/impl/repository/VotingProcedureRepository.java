package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingProcedureRepository extends JpaRepository<VotingProcedureEntity, VotingProcedureId> {
}
