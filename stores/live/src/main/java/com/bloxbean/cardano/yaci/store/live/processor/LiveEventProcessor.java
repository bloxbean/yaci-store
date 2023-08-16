package com.bloxbean.cardano.yaci.store.live.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.live.BlocksWebSocketHandler;
import com.bloxbean.cardano.yaci.store.live.cache.BlockCache;
import com.bloxbean.cardano.yaci.store.live.cache.RecentTxCache;
import com.bloxbean.cardano.yaci.store.live.dto.BlockData;
import com.bloxbean.cardano.yaci.store.live.dto.RecentTx;
import com.bloxbean.cardano.yaci.store.live.dto.RecentTxs;
import com.bloxbean.cardano.yaci.store.live.mapper.BlockDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveEventProcessor {
    private DecimalFormat df = new DecimalFormat("0.00");

    private final BlockDataMapper blockDataMapper;
    private final BlocksWebSocketHandler socketHandler;
    private final EraService eraService;
    private final BlockCache blockCache;
    private final RecentTxCache recentTxCache;

    private long slotsPerEpoch;

    @EventListener
    @Async
    public void handleBlockHeaderEvent(BlockHeaderEvent blockHeaderEvent) {
        //If sync mode ignore
        //As this live mode is supported only for fully sync mode
        if (log.isDebugEnabled())
            log.debug("Inside handleBlockEvent >> " + blockHeaderEvent.getMetadata().isSyncMode());
        if (!blockHeaderEvent.getMetadata().isSyncMode())
            return;

        BlockHeader blockHeader = blockHeaderEvent.getBlockHeader();
        if (log.isDebugEnabled()) {
            log.debug("Live Blk # : " + blockHeader.getHeaderBody().getBlockNumber() + ",  Size : " + df.format(blockHeader.getHeaderBody().getBlockBodySize() / 1024.0) + " KB");
        }

        if (slotsPerEpoch == 0)
            slotsPerEpoch = eraService.slotsPerEpoch(blockHeaderEvent.getMetadata().getEra());

        BlockData blockData = blockDataMapper.blockHeaderToBlockData(blockHeader);
        blockData.setNTx(blockHeaderEvent.getMetadata().getNoOfTxs());
        blockData.setBlockTime(blockHeaderEvent.getMetadata().getBlockTime());
        blockData.setSlotLeader(blockHeaderEvent.getMetadata().getSlotLeader());
        blockData.setEpoch(blockHeaderEvent.getMetadata().getEpochNumber());
        blockData.setEra(blockHeaderEvent.getMetadata().getEra() != null ? blockHeaderEvent.getMetadata().getEra().getValue() : null);
        blockData.setSlot(blockHeaderEvent.getMetadata().getSlot());
        blockData.setEpochSlot(blockHeaderEvent.getMetadata().getEpochSlot());
        blockData.setSlotsPerEpoch(slotsPerEpoch);
//        blockData.setFee(); //TODO -- Get total fee from somewhere
        blockCache.addBlock(blockData);
        try {
            socketHandler.broadcastBlockData(blockData);
        } catch (IOException e) {
            log.error("Error in broadcast : ", e);
        }
    }

    @EventListener
    @Async
    public void handleTransactionEvent(TransactionEvent transactionEvent) {
        if (!transactionEvent.getMetadata().isSyncMode() || transactionEvent.getTransactions().size() == 0)
            return;

        List<RecentTx> recentTxs = new ArrayList<>();
        int counter = 0;
        for (Transaction transaction : transactionEvent.getTransactions()) {
            if (counter >= 10)
                break;
            counter++;

            RecentTx recentTx = new RecentTx();
            recentTx.setHash(transaction.getTxHash());
            recentTx.setBlock(transactionEvent.getMetadata().getBlock());
            recentTx.setSlot(transactionEvent.getMetadata().getSlot());

            Set<String> outputAddress = transaction.getBody().getOutputs()
                    .stream()
                    .map(transactionOutput -> transactionOutput.getAddress())
                    .collect(Collectors.toSet());

            BigInteger output = transaction.getBody().getOutputs()
                    .stream()
                    .flatMap(transactionOutput -> transactionOutput.getAmounts().stream())
                    .filter(amount -> LOVELACE.equals(amount.getAssetName()) && !StringUtils.hasLength(amount.getPolicyId()))
                    .map(amount -> amount.getQuantity())
                    .reduce(BigInteger.ZERO, BigInteger::add);

            recentTx.setOutputAddresses(outputAddress);
            recentTx.setOutput(output);

            recentTxs.add(recentTx);
            recentTxCache.addTx(recentTx);
        }

        try {
            socketHandler.broadcastRecentTxs(RecentTxs.builder()
                    .recentTxs(recentTxs)
                    .build());
        } catch (IOException e) {
            log.error("Error in broadcast : ", e);
        }

    }
}
