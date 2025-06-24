package com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalTreasuryWithdrawal;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.LocalTreasuryWithdrawalEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:18+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class LocalTreasuryWithdrawalMapperImpl extends LocalTreasuryWithdrawalMapper {

    @Override
    public LocalTreasuryWithdrawalEntity toLocalTreasuryWithdrawalEntity(LocalTreasuryWithdrawal localTreasuryWithdrawal) {
        if ( localTreasuryWithdrawal == null ) {
            return null;
        }

        LocalTreasuryWithdrawalEntity.LocalTreasuryWithdrawalEntityBuilder localTreasuryWithdrawalEntity = LocalTreasuryWithdrawalEntity.builder();

        localTreasuryWithdrawalEntity.address( localTreasuryWithdrawal.getAddress() );
        localTreasuryWithdrawalEntity.amount( localTreasuryWithdrawal.getAmount() );
        localTreasuryWithdrawalEntity.epoch( localTreasuryWithdrawal.getEpoch() );
        localTreasuryWithdrawalEntity.govActionIndex( localTreasuryWithdrawal.getGovActionIndex() );
        localTreasuryWithdrawalEntity.govActionTxHash( localTreasuryWithdrawal.getGovActionTxHash() );
        localTreasuryWithdrawalEntity.slot( localTreasuryWithdrawal.getSlot() );

        return localTreasuryWithdrawalEntity.build();
    }

    @Override
    public LocalTreasuryWithdrawal toLocalTreasuryWithdrawal(LocalTreasuryWithdrawalEntity localTreasuryWithdrawalEntity) {
        if ( localTreasuryWithdrawalEntity == null ) {
            return null;
        }

        LocalTreasuryWithdrawal.LocalTreasuryWithdrawalBuilder localTreasuryWithdrawal = LocalTreasuryWithdrawal.builder();

        localTreasuryWithdrawal.address( localTreasuryWithdrawalEntity.getAddress() );
        localTreasuryWithdrawal.amount( localTreasuryWithdrawalEntity.getAmount() );
        if ( localTreasuryWithdrawalEntity.getEpoch() != null ) {
            localTreasuryWithdrawal.epoch( localTreasuryWithdrawalEntity.getEpoch() );
        }
        localTreasuryWithdrawal.govActionIndex( localTreasuryWithdrawalEntity.getGovActionIndex() );
        localTreasuryWithdrawal.govActionTxHash( localTreasuryWithdrawalEntity.getGovActionTxHash() );
        if ( localTreasuryWithdrawalEntity.getSlot() != null ) {
            localTreasuryWithdrawal.slot( localTreasuryWithdrawalEntity.getSlot() );
        }

        return localTreasuryWithdrawal.build();
    }
}
