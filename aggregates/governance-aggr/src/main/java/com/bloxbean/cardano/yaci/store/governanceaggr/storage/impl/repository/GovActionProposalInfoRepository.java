package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalInfoEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalInfoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GovActionProposalInfoRepository extends JpaRepository<GovActionProposalInfoEntity, GovActionProposalInfoId> {

}
