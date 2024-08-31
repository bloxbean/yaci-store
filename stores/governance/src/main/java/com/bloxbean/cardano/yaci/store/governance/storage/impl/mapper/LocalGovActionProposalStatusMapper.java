package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalGovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalGovActionProposalStatusEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class LocalGovActionProposalStatusMapper {
    public abstract LocalGovActionProposalStatusEntity toLocalGovActionProposalStatusEntity(LocalGovActionProposalStatus localGovActionProposalStatus);

    public abstract LocalGovActionProposalStatus toLocalGovActionProposalStatus(LocalGovActionProposalStatusEntity localGovActionProposalStatusEntity);
}
