package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;


import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitteeVoteRepository extends JpaRepository<CommitteeVoteEntity, CommitteeVoteId> {

    @Query("SELECT cv FROM CommitteeVoteEntity cv JOIN " +
            "(SELECT c.govActionTxHash, c.govActionIndex, MAX(c.slot) as maxSlot" +
            " FROM CommitteeVoteEntity c GROUP BY c.govActionTxHash, c.govActionIndex) maxCV" +
            " ON cv.govActionTxHash = maxCV.govActionTxHash" +
            " AND cv.govActionIndex = maxCV.govActionIndex" +
            " AND cv.slot = maxCV.maxSlot" +
            " WHERE (cv.govActionTxHash, cv.govActionIndex) IN :govActionIds")
    List<CommitteeVoteEntity> findByGovActionTxHashAndGovActionIndexPairsWithMaxSlot(@Param("govActionIds") List<GovActionId> govActionIds);

}
