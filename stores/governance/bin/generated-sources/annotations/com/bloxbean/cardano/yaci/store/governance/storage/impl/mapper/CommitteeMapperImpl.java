package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.CommitteeEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:19+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class CommitteeMapperImpl extends CommitteeMapper {

    @Override
    public CommitteeEntity toCommitteeEntity(Committee committee) {
        if ( committee == null ) {
            return null;
        }

        CommitteeEntity committeeEntity = new CommitteeEntity();

        committeeEntity.setEpoch( committee.getEpoch() );
        committeeEntity.setGovActionIndex( committee.getGovActionIndex() );
        committeeEntity.setGovActionTxHash( committee.getGovActionTxHash() );
        committeeEntity.setSlot( committee.getSlot() );
        committeeEntity.setThreshold( committee.getThreshold() );
        committeeEntity.setThresholdDenominator( committee.getThresholdDenominator() );
        committeeEntity.setThresholdNumerator( committee.getThresholdNumerator() );

        return committeeEntity;
    }

    @Override
    public Committee toCommittee(CommitteeEntity committeeEntity) {
        if ( committeeEntity == null ) {
            return null;
        }

        Committee.CommitteeBuilder committee = Committee.builder();

        committee.epoch( committeeEntity.getEpoch() );
        committee.govActionIndex( committeeEntity.getGovActionIndex() );
        committee.govActionTxHash( committeeEntity.getGovActionTxHash() );
        committee.slot( committeeEntity.getSlot() );
        committee.threshold( committeeEntity.getThreshold() );
        committee.thresholdDenominator( committeeEntity.getThresholdDenominator() );
        committee.thresholdNumerator( committeeEntity.getThresholdNumerator() );

        return committee.build();
    }
}
