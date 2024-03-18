package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.JpaBlockEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class JpaBlockMapper {
    public abstract Block toBlock(JpaBlockEntity jpaBlockEntity);
    public abstract JpaBlockEntity toBlockEntity(Block blockDetails);

    public BlockSummary toBlockSummary(JpaBlockEntity jpaBlockEntity) {
        return BlockSummary.builder()
                .time(jpaBlockEntity.getBlockTime())
                .number(jpaBlockEntity.getNumber())
                .slot(jpaBlockEntity.getSlot())
                .epoch(jpaBlockEntity.getEpochNumber())
                .era(jpaBlockEntity.getEra())
                .output(jpaBlockEntity.getTotalOutput())
                .fees(jpaBlockEntity.getTotalFees())
                .slotLeader(jpaBlockEntity.getSlotLeader())
                .size(jpaBlockEntity.getBlockBodySize())
                .txCount(jpaBlockEntity.getNoOfTxs())
                .issuerVkey(jpaBlockEntity.getIssuerVkey())
                .build();
    }

    private String getEra(Integer era) {
        return switch (era) {
            case 1 -> "Byron";
            case 2 -> "Shelley";
            case 3 -> "Allegra";
            case 4 -> "Mary";
            case 5 -> "Alonzo";
            case 6 -> "Babbage";
            default -> String.valueOf(era);
        };
    }
}
