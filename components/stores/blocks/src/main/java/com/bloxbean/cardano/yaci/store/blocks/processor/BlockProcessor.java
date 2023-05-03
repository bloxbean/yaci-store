package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.store.blocks.configuration.BlockConfig;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.util.BlockUtil;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Component
@Slf4j
public class BlockProcessor {

    private BlockStorage blockStorage;

    @Autowired
    private BlockConfig blockConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;

    public BlockProcessor(BlockStorage blockStorage) {
        this.blockStorage = blockStorage;
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleBlockHeaderEvent(@NonNull BlockHeaderEvent blockHeaderEvent) {
        BlockHeader blockHeader = blockHeaderEvent.getBlockHeader();
        long blockNumber = blockHeader.getHeaderBody().getBlockNumber();
        long slot = blockHeader.getHeaderBody().getSlot();

        long time = blockConfig.getStartTime(protocolMagic);
        long lastByronBlock = blockConfig.getLastByronBlock(protocolMagic);
        long byronProcessingTime = blockConfig.getByronProcessTime();
        long shellyProcessingTime = blockConfig.getShellyProcessTime();

        Block block = Block.builder()
                .hash(blockHeader.getHeaderBody().getBlockHash())
                .number(blockNumber)
                .slot(slot)
                .totalOutput(BigInteger.valueOf(0))
                .epochNumber(blockHeaderEvent.getMetadata().getEpochNumber())
                .blockTime(BlockUtil.calculateBlockTime(blockNumber, slot, time,
                        lastByronBlock, byronProcessingTime, shellyProcessingTime))
                .era(blockHeaderEvent.getMetadata().getEra().getValue())
                .prevHash(blockHeader.getHeaderBody().getPrevHash())
                .issuerVkey(blockHeader.getHeaderBody().getIssuerVkey())
                .vrfVkey(blockHeader.getHeaderBody().getVrfVkey())
                .nonceVrf(Vrf.from(blockHeader.getHeaderBody().getNonceVrf()))
                .leaderVrf(Vrf.from(blockHeader.getHeaderBody().getLeaderVrf()))
                .vrfResult(Vrf.from(blockHeader.getHeaderBody().getVrfResult()))
                .blockBodySize(blockHeader.getHeaderBody().getBlockBodySize())
                .blockBodyHash(blockHeader.getHeaderBody().getBlockBodyHash())
                .protocolVersion(blockHeader.getHeaderBody().getProtocolVersion().get_1()
                        + "." + blockHeader.getHeaderBody().getProtocolVersion().get_2())
                .noOfTxs(blockHeaderEvent.getMetadata().getNoOfTxs())
                .build();
        blockStorage.save(block);
    }

    @EventListener
    public void handleTransactionEvent(TransactionEvent transactionEvent) {
        if (transactionEvent.getTransactions().size() == 0)
            return;

        BigInteger transactionOutputInLovelace = transactionEvent.getTransactions()
                .parallelStream()
                .flatMap(transaction -> transaction.getBody().getOutputs().stream())
                .flatMap(transactionOutput -> transactionOutput.getAmounts().stream())
                .filter(amount -> BlockUtil.amountIsInADA(amount))
                .map(amount -> amount.getQuantity())
                .reduce((a, b) -> a.add(b)).orElse(BigInteger.ZERO);

       blockStorage.findByBlockHash(transactionEvent.getMetadata().getBlockHash())
                .ifPresentOrElse(block -> {
                    Block transactionBlock = block;
                    transactionBlock.setTotalOutput(transactionBlock.getTotalOutput().add(transactionOutputInLovelace));
                    blockStorage.save(transactionBlock);
                }, () -> {
                    log.warn(String.format("Block {} is not present and a calculated output will be ignored in aggregation" +
                            " which will result in incorrect output values in the future.", transactionEvent.getMetadata().getBlock()));
                });
    }

    @EventListener
    @Order(1)
    @Transactional
    //TODO -- add test
    public void handleRollbackEvent(@NotNull RollbackEvent rollbackEvent) {
        int count = blockStorage.deleteAllBeforeSlot(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} block records", count);
    }
}
