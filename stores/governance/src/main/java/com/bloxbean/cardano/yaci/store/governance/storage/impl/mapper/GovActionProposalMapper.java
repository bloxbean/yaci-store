package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntityJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class GovActionProposalMapper {
    public abstract GovActionProposalEntityJpa toGovActionProposalEntity(GovActionProposal govActionProposal);

    public abstract GovActionProposal toGovActionProposal(GovActionProposalEntityJpa govActionProposalEntity);
}
