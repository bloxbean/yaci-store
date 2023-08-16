package com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.mapper;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.jpa.model.BlockEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class BlockMapper {
    public abstract Block toBlock(BlockEntity blockEntity);
    public abstract BlockEntity toBlockEntity(Block blockDetails);

    public BlockSummary toBlockSummary(BlockEntity blockEntity) {
        return BlockSummary.builder()
                .time(blockEntity.getBlockTime())
                .number(blockEntity.getNumber())
                .slot(blockEntity.getSlot())
                .epoch(blockEntity.getEpochNumber())
                .era(blockEntity.getEra())
                .output(blockEntity.getTotalOutput())
                .fees(blockEntity.getTotalFees())
                .slotLeader(blockEntity.getSlotLeader())
                .size(blockEntity.getBlockBodySize())
                .txCount(blockEntity.getNoOfTxs())
                .issuerVkey(blockEntity.getIssuerVkey())
                .build();
    }

    private String getEra(Integer era) {
        switch (era) {
            case 1:
                return "Byron";
            case 2:
                return "Shelley";
            case 3:
                return "Allegra";
            case 4:
                return "Mary";
            case 5:
                return "Alonzo";
            case 6:
                return "Babbage";
            default:
                return String.valueOf(era);
        }
    }
}
