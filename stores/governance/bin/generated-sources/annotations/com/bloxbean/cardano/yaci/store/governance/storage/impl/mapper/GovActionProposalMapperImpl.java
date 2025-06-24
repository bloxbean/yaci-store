package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.GovActionProposalEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class GovActionProposalMapperImpl extends GovActionProposalMapper {

    @Override
    public GovActionProposalEntity toGovActionProposalEntity(GovActionProposal govActionProposal) {
        if ( govActionProposal == null ) {
            return null;
        }

        GovActionProposalEntity.GovActionProposalEntityBuilder<?, ?> govActionProposalEntity = GovActionProposalEntity.builder();

        govActionProposalEntity.blockNumber( govActionProposal.getBlockNumber() );
        govActionProposalEntity.blockTime( govActionProposal.getBlockTime() );
        govActionProposalEntity.anchorHash( govActionProposal.getAnchorHash() );
        govActionProposalEntity.anchorUrl( govActionProposal.getAnchorUrl() );
        govActionProposalEntity.deposit( govActionProposal.getDeposit() );
        govActionProposalEntity.details( govActionProposal.getDetails() );
        govActionProposalEntity.epoch( govActionProposal.getEpoch() );
        govActionProposalEntity.index( govActionProposal.getIndex() );
        govActionProposalEntity.returnAddress( govActionProposal.getReturnAddress() );
        govActionProposalEntity.slot( govActionProposal.getSlot() );
        govActionProposalEntity.txHash( govActionProposal.getTxHash() );
        govActionProposalEntity.type( govActionProposal.getType() );

        return govActionProposalEntity.build();
    }

    @Override
    public GovActionProposal toGovActionProposal(GovActionProposalEntity govActionProposalEntity) {
        if ( govActionProposalEntity == null ) {
            return null;
        }

        GovActionProposal.GovActionProposalBuilder<?, ?> govActionProposal = GovActionProposal.builder();

        govActionProposal.blockNumber( govActionProposalEntity.getBlockNumber() );
        govActionProposal.blockTime( govActionProposalEntity.getBlockTime() );
        govActionProposal.anchorHash( govActionProposalEntity.getAnchorHash() );
        govActionProposal.anchorUrl( govActionProposalEntity.getAnchorUrl() );
        govActionProposal.deposit( govActionProposalEntity.getDeposit() );
        govActionProposal.details( govActionProposalEntity.getDetails() );
        govActionProposal.epoch( govActionProposalEntity.getEpoch() );
        govActionProposal.index( govActionProposalEntity.getIndex() );
        govActionProposal.returnAddress( govActionProposalEntity.getReturnAddress() );
        govActionProposal.slot( govActionProposalEntity.getSlot() );
        govActionProposal.txHash( govActionProposalEntity.getTxHash() );
        govActionProposal.type( govActionProposalEntity.getType() );

        return govActionProposal.build();
    }
}
