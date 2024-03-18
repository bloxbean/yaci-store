package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DelegationVoteEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class DelegationVoteMapper {
    public abstract DelegationVoteEntityJpa toDelegationVoteEntity(DelegationVote delegationVote);

    public abstract DelegationVote toDelegationVote(DelegationVoteEntityJpa delegationVoteEntity);
}
