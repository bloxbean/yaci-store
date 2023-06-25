package com.bloxbean.cardano.yaci.store.blocks.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.EpochsPage;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.EpochStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EpochService {
    private final EpochStorage epochStorage;
    private final BlockStorage blockStorage;

    public Optional<Epoch> getEpochByNumber(int epochNumber) {
        return epochStorage.findByNumber(epochNumber);
    }

    public EpochsPage getEpochs(int page, int count) {
        return epochStorage.findEpochs(page, count);
    }

    @Transactional
    public void aggregateData() {
        Optional<Block> recentBlock = blockStorage.findRecentBlock();

        if(recentBlock.isPresent()) {
            Block latestBlock = recentBlock.get();
            int epochNumber = latestBlock.getEpochNumber();

            Optional<Epoch> recentEpoch = epochStorage.findByNumber(epochNumber);

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
        List<Block> blocks = blockStorage.findBlocksByEpoch(epochNumber);

        if (blocks.size() > 0) {
            Epoch epoch = Epoch.builder()
                    .number(epochNumber)
                    .startTime(blocks.get(0).getBlockTime())
                    .endTime(blocks.get(blocks.size() - 1).getBlockTime())
                    .blockCount(blocks.size())
                    .maxSlot(blocks.get(blocks.size() - 1).getSlot())
                    .totalOutput(BigInteger.ZERO)
                    .totalFees(BigInteger.ZERO)
                    .build();
            for (Block block : blocks) {
                epoch.setTransactionCount(epoch.getTransactionCount() + block.getNoOfTxs());

                if (block.getTotalOutput() != null)
                    epoch.setTotalOutput(epoch.getTotalOutput().add(block.getTotalOutput()));

                if (block.getTotalFees() != null)
                    epoch.setTotalFees(epoch.getTotalFees().add(block.getTotalFees()));
            }

            epochStorage.save(epoch);
        }
    }
}
