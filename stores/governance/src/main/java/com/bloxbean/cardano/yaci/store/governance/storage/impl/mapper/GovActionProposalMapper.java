package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class GovActionProposalMapper {
    public abstract GovActionProposalEntity toGovActionProposalEntity(GovActionProposal govActionProposal);

    public abstract GovActionProposal toGovActionProposal(GovActionProposalEntity govActionProposalEntity);
}
