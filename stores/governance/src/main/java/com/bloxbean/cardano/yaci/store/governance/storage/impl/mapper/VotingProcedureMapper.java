package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class VotingProcedureMapper {
    public abstract VotingProcedureEntity toVotingProcedureEntity(VotingProcedure votingProcedure);

    public abstract VotingProcedure toVotingProcedure(VotingProcedureEntity votingProcedureEntity);
}
