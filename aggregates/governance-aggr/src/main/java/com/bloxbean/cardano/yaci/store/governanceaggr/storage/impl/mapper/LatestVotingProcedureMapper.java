package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.LatestVotingProcedureEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LatestVotingProcedureMapper {
    LatestVotingProcedureEntity toLatestVotingProcedureEntity(LatestVotingProcedure latestVotingProcedure);

    LatestVotingProcedure toLatestVotingProcedure(LatestVotingProcedureEntity latestVotingProcedureEntity);

    LatestVotingProcedure fromVotingProcedure(VotingProcedure latestVotingProcedure);

    void updateByVotingProcedure(
            @MappingTarget LatestVotingProcedure latestVotingProcedure, VotingProcedure votingProcedure);
}
