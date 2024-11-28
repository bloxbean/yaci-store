package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GovActionProposalStatusMapper {
    GovActionProposalStatusEntity toGovActionProposalStatusEntity(GovActionProposalStatus govActionProposalStatus);
    GovActionProposalStatus toGovActionProposalStatus(GovActionProposalStatusEntity govActionProposalStatusEntity);
}
