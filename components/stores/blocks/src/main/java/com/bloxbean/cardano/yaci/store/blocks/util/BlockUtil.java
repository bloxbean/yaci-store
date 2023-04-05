package com.bloxbean.cardano.yaci.store.blocks.util;

import com.bloxbean.cardano.yaci.core.model.Amount;

public class BlockUtil {
    public static long calculateBlockTime(long blockNumber, long slot, long time, long lastByronBlock, long byronProcessingTime, long shellyProcessingTime) {
        final long actualSlot = slot - 1;
        if (blockNumber > lastByronBlock) {
            final long otherBlocks = actualSlot - lastByronBlock;
            time += time + (lastByronBlock * byronProcessingTime);
            time += time + (otherBlocks * shellyProcessingTime);
        } else {
            time = time + (actualSlot * byronProcessingTime);
        }
        return time;
    }

    public static boolean amountIsInADA(Amount amount) {
        return amount.getPolicyId() == "" && amount.getAssetName() == "";
    }

}
