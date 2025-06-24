package com.bloxbean.cardano.yaci.store.adapot.job.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJob;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:27+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class AdaPotJobMapperImpl extends AdaPotJobMapper {

    @Override
    public AdaPotJob toDomain(AdaPotJobEntity rewardCalcJobEntity) {
        if ( rewardCalcJobEntity == null ) {
            return null;
        }

        AdaPotJob.AdaPotJobBuilder<?, ?> adaPotJob = AdaPotJob.builder();

        adaPotJob.block( rewardCalcJobEntity.getBlock() );
        adaPotJob.drepDistrSnapshotTime( rewardCalcJobEntity.getDrepDistrSnapshotTime() );
        adaPotJob.epoch( (int) rewardCalcJobEntity.getEpoch() );
        adaPotJob.errorMessage( rewardCalcJobEntity.getErrorMessage() );
        adaPotJob.extraInfo( rewardCalcJobEntity.getExtraInfo() );
        adaPotJob.rewardCalcTime( rewardCalcJobEntity.getRewardCalcTime() );
        adaPotJob.slot( rewardCalcJobEntity.getSlot() );
        adaPotJob.stakeSnapshotTime( rewardCalcJobEntity.getStakeSnapshotTime() );
        adaPotJob.status( rewardCalcJobEntity.getStatus() );
        adaPotJob.totalTime( rewardCalcJobEntity.getTotalTime() );
        adaPotJob.type( rewardCalcJobEntity.getType() );
        adaPotJob.updateRewardTime( rewardCalcJobEntity.getUpdateRewardTime() );

        return adaPotJob.build();
    }

    @Override
    public AdaPotJobEntity toEntity(AdaPotJob rewardCalcJob) {
        if ( rewardCalcJob == null ) {
            return null;
        }

        AdaPotJobEntity adaPotJobEntity = new AdaPotJobEntity();

        adaPotJobEntity.setEpoch( rewardCalcJob.getEpoch() );
        adaPotJobEntity.setSlot( rewardCalcJob.getSlot() );
        adaPotJobEntity.setBlock( rewardCalcJob.getBlock() );
        adaPotJobEntity.setType( rewardCalcJob.getType() );
        adaPotJobEntity.setStatus( rewardCalcJob.getStatus() );
        adaPotJobEntity.setTotalTime( rewardCalcJob.getTotalTime() );
        adaPotJobEntity.setRewardCalcTime( rewardCalcJob.getRewardCalcTime() );
        adaPotJobEntity.setUpdateRewardTime( rewardCalcJob.getUpdateRewardTime() );
        adaPotJobEntity.setStakeSnapshotTime( rewardCalcJob.getStakeSnapshotTime() );
        adaPotJobEntity.setDrepDistrSnapshotTime( rewardCalcJob.getDrepDistrSnapshotTime() );
        adaPotJobEntity.setExtraInfo( rewardCalcJob.getExtraInfo() );
        adaPotJobEntity.setErrorMessage( rewardCalcJob.getErrorMessage() );

        return adaPotJobEntity;
    }
}
