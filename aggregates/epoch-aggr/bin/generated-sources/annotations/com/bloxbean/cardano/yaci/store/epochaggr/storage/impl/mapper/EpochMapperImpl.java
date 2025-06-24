package com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.impl.model.EpochEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-20T15:09:16+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.42.0.v20250514-1000, environment: Java 21.0.7 (Eclipse Adoptium)"
)
@Component
public class EpochMapperImpl extends EpochMapper {

    @Override
    public Epoch toEpoch(EpochEntity blockEntity) {
        if ( blockEntity == null ) {
            return null;
        }

        Epoch.EpochBuilder epoch = Epoch.builder();

        epoch.blockCount( blockEntity.getBlockCount() );
        epoch.endTime( blockEntity.getEndTime() );
        epoch.maxSlot( blockEntity.getMaxSlot() );
        epoch.number( blockEntity.getNumber() );
        epoch.startTime( blockEntity.getStartTime() );
        epoch.totalFees( blockEntity.getTotalFees() );
        epoch.totalOutput( blockEntity.getTotalOutput() );
        epoch.transactionCount( blockEntity.getTransactionCount() );

        return epoch.build();
    }

    @Override
    public EpochEntity toEpochEntity(Epoch blockEntity) {
        if ( blockEntity == null ) {
            return null;
        }

        EpochEntity.EpochEntityBuilder epochEntity = EpochEntity.builder();

        epochEntity.blockCount( blockEntity.getBlockCount() );
        epochEntity.endTime( blockEntity.getEndTime() );
        epochEntity.maxSlot( blockEntity.getMaxSlot() );
        epochEntity.number( blockEntity.getNumber() );
        epochEntity.startTime( blockEntity.getStartTime() );
        epochEntity.totalFees( blockEntity.getTotalFees() );
        epochEntity.totalOutput( blockEntity.getTotalOutput() );
        epochEntity.transactionCount( blockEntity.getTransactionCount() );

        return epochEntity.build();
    }
}
