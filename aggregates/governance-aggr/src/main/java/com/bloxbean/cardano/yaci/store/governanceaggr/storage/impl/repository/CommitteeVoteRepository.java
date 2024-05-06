package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;


import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitteeVoteRepository extends JpaRepository<CommitteeVoteEntity, CommitteeVoteId> {

}
