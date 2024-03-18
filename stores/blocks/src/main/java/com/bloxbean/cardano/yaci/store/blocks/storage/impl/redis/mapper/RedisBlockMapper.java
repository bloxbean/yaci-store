package com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.mapper;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlockSummary;
import com.bloxbean.cardano.yaci.store.blocks.storage.impl.redis.model.RedisBlockEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class RedisBlockMapper {

    public abstract Block toBlock(RedisBlockEntity redisBlockEntity);
    public abstract RedisBlockEntity toBlockEntity(Block blockDetails);

    public BlockSummary toBlockSummary(RedisBlockEntity redisBlockEntity) {
        return BlockSummary.builder()
                .time(redisBlockEntity.getBlockTime())
                .number(redisBlockEntity.getNumber())
                .slot(redisBlockEntity.getSlot())
                .epoch(redisBlockEntity.getEpochNumber())
                .era(redisBlockEntity.getEra())
                .output(redisBlockEntity.getTotalOutput())
                .fees(redisBlockEntity.getTotalFees())
                .slotLeader(redisBlockEntity.getSlotLeader())
                .size(redisBlockEntity.getBlockBodySize())
                .txCount(redisBlockEntity.getNoOfTxs())
                .issuerVkey(redisBlockEntity.getIssuerVkey())
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
