package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.projection.LatestEpochVotingProcedureProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface LatestVotingProcedureRepository extends JpaRepository<LatestVotingProcedureEntity, LatestVotingProcedureId> {
    @Query(value = "SELECT lvp.slot FROM LatestVotingProcedureEntity lvp ORDER BY lvp.slot DESC LIMIT 1")
    Optional<Long> findLatestSlotOfVotingProcedure();

    @Query(
            value =
                    "SELECT lvp FROM LatestVotingProcedureEntity lvp "
                            + "WHERE lvp.latestVotingProcedureId IN :votingProcedureIds")
    List<LatestVotingProcedureEntity> getAllByIdIn(
            @Param("votingProcedureIds") Collection<LatestVotingProcedureId> votingProcedureIds);

    int deleteBySlotGreaterThan(long slot);

    List<LatestVotingProcedureEntity> findBySlotGreaterThan(Long slot);

    @Query(
            value =
                    "select max(lvp.epoch) as epoch, lvp.voterHash as voterHash from LatestVotingProcedureEntity lvp "
                            + " where lvp.voterType = 'DREP_KEY_HASH' and lvp.epoch >= :fromEpoch"
                            + " and lvp.voterHash in :dRepHashes"
                            + " group by lvp.voterHash")
    List<LatestEpochVotingProcedureProjection> findAllByVoterHashAndEpochNo(
            @Param("fromEpoch") Long fromEpoch, @Param("dRepHashes") Set<String> dRepHashes);

    List<LatestVotingProcedureEntity> findByVoterTypeAndEpochIsGreaterThanEqual(VoterType voterType, int epoch);
}
