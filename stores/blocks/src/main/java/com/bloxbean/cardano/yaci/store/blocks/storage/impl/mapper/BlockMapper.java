package com.bloxbean.cardano.yaci.store.blocks.storage.impl.mapper;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.model.BlockEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", implementationName = "BlockMapperJpa")
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
