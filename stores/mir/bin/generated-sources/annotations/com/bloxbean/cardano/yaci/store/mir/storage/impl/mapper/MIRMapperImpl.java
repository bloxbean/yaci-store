package com.bloxbean.cardano.yaci.store.mir.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousRewardSummary;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.model.MIREntity;
import com.bloxbean.cardano.yaci.store.mir.storage.impl.projection.MIRSummary;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:17+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class MIRMapperImpl extends MIRMapper {

    @Override
    public MIREntity toMIREntity(MoveInstataneousReward moveInstataneousReward) {
        if ( moveInstataneousReward == null ) {
            return null;
        }

        MIREntity.MIREntityBuilder<?, ?> mIREntity = MIREntity.builder();

        mIREntity.blockNumber( moveInstataneousReward.getBlockNumber() );
        mIREntity.blockTime( moveInstataneousReward.getBlockTime() );
        mIREntity.address( moveInstataneousReward.getAddress() );
        mIREntity.amount( moveInstataneousReward.getAmount() );
        mIREntity.blockHash( moveInstataneousReward.getBlockHash() );
        mIREntity.certIndex( moveInstataneousReward.getCertIndex() );
        mIREntity.credential( moveInstataneousReward.getCredential() );
        mIREntity.epoch( moveInstataneousReward.getEpoch() );
        mIREntity.pot( moveInstataneousReward.getPot() );
        mIREntity.slot( moveInstataneousReward.getSlot() );
        mIREntity.txHash( moveInstataneousReward.getTxHash() );

        return mIREntity.build();
    }

    @Override
    public MoveInstataneousReward toMoveInstataneousReward(MIREntity mirEntity) {
        if ( mirEntity == null ) {
            return null;
        }

        MoveInstataneousReward.MoveInstataneousRewardBuilder<?, ?> moveInstataneousReward = MoveInstataneousReward.builder();

        moveInstataneousReward.blockNumber( mirEntity.getBlockNumber() );
        moveInstataneousReward.blockTime( mirEntity.getBlockTime() );
        moveInstataneousReward.address( mirEntity.getAddress() );
        moveInstataneousReward.amount( mirEntity.getAmount() );
        moveInstataneousReward.blockHash( mirEntity.getBlockHash() );
        moveInstataneousReward.certIndex( mirEntity.getCertIndex() );
        moveInstataneousReward.credential( mirEntity.getCredential() );
        moveInstataneousReward.epoch( mirEntity.getEpoch() );
        moveInstataneousReward.pot( mirEntity.getPot() );
        moveInstataneousReward.slot( mirEntity.getSlot() );
        moveInstataneousReward.txHash( mirEntity.getTxHash() );

        return moveInstataneousReward.build();
    }

    @Override
    public MoveInstataneousRewardSummary toMoveInstataneousRewardSummary(MIRSummary mirSummary) {
        if ( mirSummary == null ) {
            return null;
        }

        MoveInstataneousRewardSummary.MoveInstataneousRewardSummaryBuilder<?, ?> moveInstataneousRewardSummary = MoveInstataneousRewardSummary.builder();

        moveInstataneousRewardSummary.blockNumber( mirSummary.getBlockNumber() );
        moveInstataneousRewardSummary.blockTime( mirSummary.getBlockTime() );
        if ( mirSummary.getCertIndex() != null ) {
            moveInstataneousRewardSummary.certIndex( mirSummary.getCertIndex() );
        }
        if ( mirSummary.getPot() != null ) {
            moveInstataneousRewardSummary.pot( Enum.valueOf( MirPot.class, mirSummary.getPot() ) );
        }
        moveInstataneousRewardSummary.slot( mirSummary.getSlot() );
        moveInstataneousRewardSummary.totalRewards( mirSummary.getTotalRewards() );
        if ( mirSummary.getTotalStakeKeys() != null ) {
            moveInstataneousRewardSummary.totalStakeKeys( mirSummary.getTotalStakeKeys().intValue() );
        }
        moveInstataneousRewardSummary.txHash( mirSummary.getTxHash() );

        return moveInstataneousRewardSummary.build();
    }
}
