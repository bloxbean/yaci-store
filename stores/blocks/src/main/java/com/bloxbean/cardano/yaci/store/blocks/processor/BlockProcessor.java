package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.blocks.util.BlockUtil;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.blocks.BlocksStoreConfiguration.STORE_BLOCKS_ENABLED;

@Component
@EnableIf(STORE_BLOCKS_ENABLED)
@Slf4j
public class BlockProcessor {
    private BlockStorage blockStorage;

    public BlockProcessor(BlockStorage blockStorage) {
        this.blockStorage = blockStorage;
    }

    @EventListener
    @Order(1)
    @Transactional
    public void handleBlockHeaderEvent(@NonNull BlockEvent blockEvent) {
        BlockHeader blockHeader = blockEvent.getBlock().getHeader();
        long blockNumber = blockHeader.getHeaderBody().getBlockNumber();
        long slot = blockHeader.getHeaderBody().getSlot();

        Block block = Block.builder()
                .hash(blockHeader.getHeaderBody().getBlockHash())
                .number(blockNumber)
                .slot(slot)
                .totalOutput(BigInteger.ZERO)
                .totalFees(BigInteger.ZERO)
                .epochNumber(blockEvent.getMetadata().getEpochNumber())
                .epochSlot((int) blockEvent.getMetadata().getEpochSlot())
                .blockTime(blockEvent.getMetadata().getBlockTime())
                .era(blockEvent.getMetadata().getEra().getValue())
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
                .noOfTxs(blockEvent.getMetadata().getNoOfTxs())
                .slotLeader(blockEvent.getMetadata().getSlotLeader())
                .build();

        if (blockHeader.getHeaderBody().getOperationalCert() != null) {
            block.setOpCertHotVKey(blockHeader.getHeaderBody().getOperationalCert().getHotVKey());
            block.setOpCertSeqNumber(blockHeader.getHeaderBody().getOperationalCert().getSequenceNumber());
            block.setOpcertKesPeriod(blockHeader.getHeaderBody().getOperationalCert().getKesPeriod());
            block.setOpCertSigma(blockHeader.getHeaderBody().getOperationalCert().getSigma());
        }

        handleTransaction(block, blockEvent.getBlock().getTransactionBodies());

        blockStorage.save(block);
    }

    public void handleTransaction(Block block, List<TransactionBody> transactionBodies) {
        if (transactionBodies == null || transactionBodies.size() == 0)
            return;

        BigInteger transactionOutputInLovelace = transactionBodies
                .stream()
                .flatMap(transactionBody -> transactionBody.getOutputs().stream())
                .flatMap(transactionOutput -> transactionOutput.getAmounts().stream())
                .filter(amount -> BlockUtil.amountIsInADA(amount))
                .map(amount -> amount.getQuantity())
                .reduce((a, b) -> a.add(b)).orElse(BigInteger.ZERO);

        BigInteger totalFees = transactionBodies
                .stream()
                .map(transactionBody -> transactionBody.getFee())
                .reduce((a, b) -> a.add(b)).orElse(BigInteger.ZERO);

        block.setTotalOutput(transactionOutputInLovelace == null ? BigInteger.ZERO : transactionOutputInLovelace);
        block.setTotalFees(totalFees == null ? BigInteger.ZERO : totalFees);
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
