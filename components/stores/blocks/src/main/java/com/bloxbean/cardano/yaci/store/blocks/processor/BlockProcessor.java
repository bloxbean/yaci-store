package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.BlockHeader;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.blocks.configuration.BlockConfig;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Epoch;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.persistence.BlockPersistence;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@Component
@Slf4j
public class BlockProcessor {

    private BlockPersistence blockPersistence;

    @Autowired
    private BlockConfig blockConfig;

    @Value("${store.cardano.protocol-magic}")
    private long protocolMagic;

    public BlockProcessor(BlockPersistence blockPersistence) {
        this.blockPersistence = blockPersistence;
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
                .totalOutput(BigInteger.valueOf(0))
                .epochNumber(blockHeaderEvent.getMetadata().getEpochNumber())
                .blockTime(calculateBlockTime(blockNumber, slot))
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
        blockPersistence.save(block);
    }

    public long calculateBlockTime(long blockNumber, long slot) {
        long time = blockConfig.getStartTime(protocolMagic);
        long lastByronBlock = blockConfig.getLastByronBlock(protocolMagic);

        final long actualSlot = slot - 1;
        if (blockNumber > lastByronBlock) {
            final long otherBlocks = actualSlot - lastByronBlock;
            time += time + (lastByronBlock * blockConfig.getByronProcessTime());
            time += time + (otherBlocks * blockConfig.getShellyProcessTime());
        } else {
            time = time + (actualSlot * blockConfig.getByronProcessTime());
        }
        return time;
    }

    private boolean amountIsInADA(Amount amount) {
        return amount.getPolicyId() == "" && amount.getAssetName() == "";
    }

    @EventListener
    @Async
    public void handleTransactionEvent(TransactionEvent transactionEvent) {
        if (!transactionEvent.getMetadata().isSyncMode() || transactionEvent.getTransactions().size() == 0)
            return;

        BigInteger transactionOutputInLovelace = BigInteger.valueOf(0);
        for (Transaction transaction : transactionEvent.getTransactions()) {

            List<TransactionOutput> outputs = transaction.getBody().getOutputs();

            for (TransactionOutput output : outputs) {
                List<Amount> amounts = output.getAmounts();
                for (Amount amount : amounts) {
                    if (amountIsInADA((amount))) {
                        transactionOutputInLovelace.add(amount.getQuantity());
                    }
                }
            }
        }

        Optional<Block> block = blockPersistence.findByBlockHash(transactionEvent.getMetadata().getBlockHash());
        if (block.isPresent()) {
            Block transactionBlock = block.get();
            transactionBlock.setTotalOutput(transactionBlock.getTotalOutput().add(transactionOutputInLovelace));
            blockPersistence.save(transactionBlock);
        } else {
            log.warn(String.format("Block {} is not present and a calculated output will be ignored in aggregation which will result in incorrect output values in the future.", transactionEvent.getMetadata().getBlock()));
        }
    }

    @EventListener
    @Order(1)
    @Transactional
    //TODO -- add test
    public void handleRollbackEvent(@NotNull RollbackEvent rollbackEvent) {
        int count = blockPersistence.deleteAllBeforeSlot(rollbackEvent.getRollbackTo().getSlot());

        log.info("Rollback -- {} block records", count);
    }
}
