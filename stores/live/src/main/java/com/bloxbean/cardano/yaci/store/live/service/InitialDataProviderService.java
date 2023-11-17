package com.bloxbean.cardano.yaci.store.live.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.api.blocks.service.BlockService;
import com.bloxbean.cardano.yaci.store.live.dto.BlockData;
import com.bloxbean.cardano.yaci.store.live.dto.RecentTx;
import com.bloxbean.cardano.yaci.store.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is used to provide initial data to the client when they connect to the websocket and initial
 * sync is not completed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InitialDataProviderService {
    //TODO -- Replace the direct service calls with client implementations
    private final BlockService blockService;
    private final TransactionService transactionService;
    private final EraService eraService;

    public List<BlockData> getRecentBlocks() {
        return blockService.getBlocks(0, 10)
                .getBlocks()
                .stream()
                .map(blockSummary -> {
                    long epochSlot = 0;
                    long slotsPerEpoch = 0;
                    if (blockSummary.getEra() != Era.Byron.getValue()) {
                        epochSlot = eraService.getShelleyEpochSlot(blockSummary.getSlot());
                        slotsPerEpoch = eraService.slotsPerEpoch(Era.Shelley);
                    }

                    BlockData blockData = new BlockData();
                    blockData.setTime(System.currentTimeMillis());
                    blockData.setNumber(blockSummary.getNumber());
                    blockData.setEpoch(blockSummary.getEpoch());
                    blockData.setEra(blockSummary.getEra());
                    blockData.setSlot(blockSummary.getSlot());
                    blockData.setEpochSlot(epochSlot);
                    blockData.setNTx(blockSummary.getTxCount());
                    blockData.setBlockTime(blockSummary.getTime());
                    blockData.setSlotLeader(blockSummary.getSlotLeader());
                    blockData.setSlotsPerEpoch(slotsPerEpoch);
                    blockData.setFee(blockSummary.getFees());
                    return blockData;
                }).collect(Collectors.toList());
    }

    public List<RecentTx> getRecentTransactions() {
        return transactionService.getTransactions(0, 10)
                .getTransactionSummaries()
                .stream()
                .map(transactionSummary -> RecentTx
                        .builder()
                        .hash(transactionSummary.getTxHash())
                        .block(transactionSummary.getBlockNumber())
                        .slot(transactionSummary.getSlot())
                        .outputAddresses(Set.copyOf(transactionSummary.getOutputAddresses()))
                        .output(transactionSummary.getTotalOutput())
                        .build()).collect(Collectors.toList());
    }
}
