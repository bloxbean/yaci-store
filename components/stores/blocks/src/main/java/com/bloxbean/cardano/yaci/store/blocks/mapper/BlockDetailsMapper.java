package com.bloxbean.cardano.yaci.store.blocks.mapper;

import com.bloxbean.cardano.yaci.store.blocks.dto.BlockDetails;
import com.bloxbean.cardano.yaci.store.blocks.dto.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.model.BlockEntity;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class BlockDetailsMapper {
    public abstract BlockDetails blockEntityToBlockDetailsMapper(BlockEntity blockEntity);

    public abstract BlockSummary blockEntityToBlockSummaryMapper(BlockEntity blockEntity);

    @BeforeMapping
    protected void enrichBlockDetails(BlockEntity blockEntity, @MappingTarget BlockDetails blockDetails) {
        if (blockEntity.getEra() != null) {
            blockDetails.setEra(getEra(blockEntity.getEra()));
        }
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
