package com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.adapot.domain.AdaPot;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.model.AdaPotEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:27+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class AdaPotMapperImpl extends AdaPotMapper {

    @Override
    public AdaPot toAdaPot(AdaPotEntity adaPotEntity) {
        if ( adaPotEntity == null ) {
            return null;
        }

        AdaPot.AdaPotBuilder<?, ?> adaPot = AdaPot.builder();

        adaPot.circulation( adaPotEntity.getCirculation() );
        adaPot.depositsStake( adaPotEntity.getDepositsStake() );
        adaPot.distributedRewards( adaPotEntity.getDistributedRewards() );
        adaPot.epoch( adaPotEntity.getEpoch() );
        adaPot.fees( adaPotEntity.getFees() );
        adaPot.poolRewardsPot( adaPotEntity.getPoolRewardsPot() );
        adaPot.reserves( adaPotEntity.getReserves() );
        adaPot.rewardsPot( adaPotEntity.getRewardsPot() );
        adaPot.slot( adaPotEntity.getSlot() );
        adaPot.treasury( adaPotEntity.getTreasury() );
        adaPot.undistributedRewards( adaPotEntity.getUndistributedRewards() );
        adaPot.utxo( adaPotEntity.getUtxo() );

        return adaPot.build();
    }

    @Override
    public AdaPotEntity toAdaPotEntity(AdaPot adaPot) {
        if ( adaPot == null ) {
            return null;
        }

        AdaPotEntity.AdaPotEntityBuilder<?, ?> adaPotEntity = AdaPotEntity.builder();

        adaPotEntity.circulation( adaPot.getCirculation() );
        adaPotEntity.depositsStake( adaPot.getDepositsStake() );
        adaPotEntity.distributedRewards( adaPot.getDistributedRewards() );
        adaPotEntity.epoch( adaPot.getEpoch() );
        adaPotEntity.fees( adaPot.getFees() );
        adaPotEntity.poolRewardsPot( adaPot.getPoolRewardsPot() );
        adaPotEntity.reserves( adaPot.getReserves() );
        adaPotEntity.rewardsPot( adaPot.getRewardsPot() );
        adaPotEntity.slot( adaPot.getSlot() );
        adaPotEntity.treasury( adaPot.getTreasury() );
        adaPotEntity.undistributedRewards( adaPot.getUndistributedRewards() );
        adaPotEntity.utxo( adaPot.getUtxo() );

        return adaPotEntity.build();
    }
}
