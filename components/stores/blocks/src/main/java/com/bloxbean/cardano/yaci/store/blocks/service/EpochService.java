package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
import com.bloxbean.cardano.yaci.store.blocks.persistence.EpochPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpochService {
    private final EpochPersistence epochPersistence;
    private final BlockPersistence blockPersistence;

    public Optional<Epoch> getEpochByNumber(int epochNumber) {
        return epochPersistence.findByNumber(epochNumber);
    }

    public EpochsPage getEpochs(int page, int count) {
        return epochPersistence.findEpochs(page, count);
    }

    public void aggregateData() {
        Optional<Block> recentBlock = blockPersistence.findRecentBlock();

        if(recentBlock.isPresent()) {
            Block latestBlock = recentBlock.get();
            int epochNumber = latestBlock.getEpochNumber();

            Optional<Epoch> recentEpoch = epochPersistence.findByNumber(epochNumber);

            if (recentEpoch.isPresent()) {
                aggregateForEpoch(epochNumber);
            } else {
                if (epochNumber > 0) {
                    aggregateForEpoch(epochNumber - 1);
                }
                aggregateForEpoch(epochNumber);
            }
        }
    }

    private void aggregateForEpoch(int epochNumber) {
        List<Block> blocks = blockPersistence.findBlocksByEpoch(epochNumber);

        if (blocks.size() > 0) {
            Epoch epoch = Epoch.builder()
                    .number(epochNumber)
                    .startTime(blocks.get(0).getBlockTime())
                    .endTime(blocks.get(blocks.size() - 1).getBlockTime())
                    .blockCount(blocks.size())
                    .maxSlot(blocks.get(blocks.size() - 1).getSlot())
                    .build();
            for (Block block : blocks) {
                epoch.setTransactionCount(epoch.getTransactionCount() + block.getNoOfTxs());
                epoch.setTotalOutput(epoch.getTotalOutput());
            }
        }
    }
}
