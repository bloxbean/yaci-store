package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.VotingProcedureEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class VotingProcedureMapper {
    public abstract VotingProcedureEntityJpa toVotingProcedureEntity(VotingProcedure votingProcedure);

    public abstract VotingProcedure toVotingProcedure(VotingProcedureEntityJpa votingProcedureEntity);
}
