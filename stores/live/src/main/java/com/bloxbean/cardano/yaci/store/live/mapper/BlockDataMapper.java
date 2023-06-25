package com.bloxbean.cardano.yaci.store.live.mapper;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.store.live.Constants;
import com.bloxbean.cardano.yaci.store.live.dto.BlockData;
import com.bloxbean.cardano.yaci.store.live.dto.ResType;
import org.springframework.stereotype.Component;

@Component
public class BlockDataMapper {
    public BlockData blockHeaderToBlockData(BlockHeader blockHeader) {
//        long totalFees = block.getTransactionBodies().stream()
//                .map(transactionBody -> transactionBody.getFee().longValue())
//                .reduce(0L, Long::sum);

        long size = blockHeader.getHeaderBody().getBlockBodySize();

        int sizePerct = size != 0 ? (int) Math.round((size / (double) Constants.MAX_BLOCK_BODY_SIZE) * 100) : 0;

        return BlockData.builder()
                .resType(ResType.BLOCK_DATA)
                .time(System.currentTimeMillis())
                .number(blockHeader.getHeaderBody().getBlockNumber())
                .slot(blockHeader.getHeaderBody().getSlot())
                .slotLeader(blockHeader.getHeaderBody().getVrfVkey())
                .hash(blockHeader.getHeaderBody().getBlockHash())
                .size(blockHeader.getHeaderBody().getBlockBodySize())
                .sizePerct(sizePerct)
                .build();
    }
}
