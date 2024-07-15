package com.bloxbean.cardano.yaci.store.governance.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalGovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalGovActionProposalStatusId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalGovActionProposalStatusRepository extends JpaRepository<LocalGovActionProposalStatusEntity, LocalGovActionProposalStatusId> {

}
