package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
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
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;

@Component
@Slf4j
public class BlockProcessor {

    private final BlockStorage blockStorage;

    @Autowired
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

        Block block = Block.builder()
                .hash(blockHeader.getHeaderBody().getBlockHash())
                .number(blockNumber)
                .slot(slot)
                .totalOutput(BigInteger.ZERO)
                .totalFees(BigInteger.ZERO)
                .epochNumber(blockHeaderEvent.getMetadata().getEpochNumber())
                .epochSlot((int) blockHeaderEvent.getMetadata().getEpochSlot())
                .blockTime(blockHeaderEvent.getMetadata().getBlockTime())
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
                .slotLeader(blockHeaderEvent.getMetadata().getSlotLeader())
                .build();

        if (blockHeader.getHeaderBody().getOperationalCert() != null) {
            block.setOpCertHotVKey(blockHeader.getHeaderBody().getOperationalCert().getHotVKey());
            block.setOpCertSeqNumber(blockHeader.getHeaderBody().getOperationalCert().getSequenceNumber());
            block.setOpcertKesPeriod(blockHeader.getHeaderBody().getOperationalCert().getKesPeriod());
            block.setOpCertSigma(blockHeader.getHeaderBody().getOperationalCert().getSigma());
        }

        blockStorage.save(block);
    }

    @EventListener
    @Order(2)
    @Transactional
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

        BigInteger totalFees = transactionEvent.getTransactions()
                .parallelStream()
                .map(transaction -> transaction.getBody().getFee())
                .reduce((a, b) -> a.add(b)).orElse(BigInteger.ZERO);

        blockStorage.findByBlockHash(transactionEvent.getMetadata().getBlockHash())
                .ifPresentOrElse(block -> {
                    block.setTotalOutput(transactionOutputInLovelace == null ? BigInteger.ZERO : transactionOutputInLovelace);
                    block.setTotalFees(totalFees == null ? BigInteger.ZERO : totalFees);
                    blockStorage.save(block);
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
        int count = blockStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} block records", count);
    }
}
