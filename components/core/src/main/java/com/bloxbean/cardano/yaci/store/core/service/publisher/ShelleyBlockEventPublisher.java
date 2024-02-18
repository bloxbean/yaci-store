package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.*;
import com.bloxbean.cardano.yaci.store.events.domain.*;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;

@Component
@Slf4j
public class ShelleyBlockEventPublisher implements BlockEventPublisher<Block> {
    private final ApplicationEventPublisher publisher;
    private final CursorService cursorService;
    private final ExecutorService blockExecutor;
    private final ExecutorService eventExecutor;
    private final StoreProperties storeProperties;

    public ShelleyBlockEventPublisher(@Qualifier("blockExecutor") ExecutorService blockExecutor,
                                      @Qualifier("blockEventExecutor") ExecutorService eventExecutor,
                                      ApplicationEventPublisher publisher,
                                      CursorService cursorService,
                                      StoreProperties storeProperties) {
        this.blockExecutor = blockExecutor;
        this.eventExecutor = eventExecutor;
        this.publisher = publisher;
        this.cursorService = cursorService;
        this.storeProperties = storeProperties;
    }

    private List<BatchBlock> batchBlockList = new ArrayList<>();

    @Transactional
    public void publishBlockEvents(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        processBlockSingleThread(eventMetadata, block, transactions);
        publisher.publishEvent(new PreCommitEvent(eventMetadata));
        publisher.publishEvent(new CommitEvent(eventMetadata, List.of(new BatchBlock(eventMetadata, block, transactions))));

        cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(), eventMetadata.getBlock(),
                eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
    }

    //Don't add transactional annotation here
    public void publishBlockEventsInParallel(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        handleBlockBatchInParallel(eventMetadata, block, transactions);
    }

    private void handleBlockBatchInParallel(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        batchBlockList.add(new BatchBlock(eventMetadata, block, transactions));
        if (batchBlockList.size() != storeProperties.getBlocksBatchSize())
            return;

        processBlocksInParallel();

    }

    public void processBlocksInParallel() {
        if (batchBlockList.size() == 0)
            return;

        List<List<BatchBlock>> partitions = partition(batchBlockList, storeProperties.getBlocksPartitionSize());
        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchBlock blockCache : partition) {
                    processBlockInParallel(blockCache.getMetadata(), blockCache.getBlock(), blockCache.getTransactions());
                }

                return true;
            }, blockExecutor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        BatchBlock lastBatchBlock = batchBlockList.getLast();

        //Publish BatchProcessedEvent. This may be useful for some scenarios where we need to do some processing before CommitEvent
        publisher.publishEvent(new BatchBlocksProcessedEvent(lastBatchBlock.getMetadata(), batchBlockList));

        publisher.publishEvent(new PreCommitEvent(lastBatchBlock.getMetadata()));
        publisher.publishEvent(new CommitEvent(lastBatchBlock.getMetadata(), batchBlockList));

        /**
         var postProcessingFuture = CompletableFuture.supplyAsync(() -> {
         publisher.publishEvent(new PreCommitEvent(lastBatchBlock.getMetadata()));
         return true;
         }, eventExecutor);

         var commitFuture = CompletableFuture.supplyAsync(() -> {
         publisher.publishEvent(new CommitEvent(lastBatchBlock.getMetadata(), batchBlockList));
         return true;
         }, eventExecutor);
         CompletableFuture.allOf(postProcessingFuture, commitFuture).join();
         **/

        //Finally Set the cursor
        cursorService.setCursor(new Cursor(lastBatchBlock.getMetadata().getSlot(), lastBatchBlock.getMetadata().getBlockHash(), lastBatchBlock.getMetadata().getBlock(),
                lastBatchBlock.getMetadata().getPrevBlockHash(), lastBatchBlock.getMetadata().getEra()));
        batchBlockList.clear();
    }

    private void processBlockInParallel(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        var blockHeader = block.getHeader();

        var eraEventCf = publishEventAsync(eventMetadata.getEra());
        var blockEventCf = publishEventAsync(new BlockEvent(eventMetadata, block));
        var blockHeaderEventCf = publishEventAsync(new BlockHeaderEvent(eventMetadata, blockHeader));
        var txnEventCf = publishEventAsync(new TransactionEvent(eventMetadata, transactions));

        //Script Event
        var txScriptEvent = CompletableFuture.supplyAsync(() -> {
            List<TxScripts> txScriptsList = getTxScripts(transactions);
            publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));
            return true;
        }, eventExecutor);

        //AuxData event
        var txAuxDataEvent = CompletableFuture.supplyAsync(() -> {
            List<TxAuxData> txAuxDataList = transactions.stream()
                    .filter(transaction -> transaction.getAuxData() != null)
                    .map(transaction -> TxAuxData.builder()
                            .txHash(transaction.getTxHash())
                            .auxData(transaction.getAuxData())
                            .build()
                    ).collect(Collectors.toList());
            publisher.publishEvent(new AuxDataEvent(eventMetadata, txAuxDataList));
            return true;
        }, eventExecutor);

        //Certificate event
        var txCertificateEvent = CompletableFuture.supplyAsync(() -> {
            List<TxCertificates> txCertificatesList =
                    IntStream.range(0, transactions.size())
                            .mapToObj(i -> {
                                        var transaction = transactions.get(i);
                                        return TxCertificates.builder()
                                                .txHash(transaction.getTxHash())
                                                .blockIndex(i)
                                                .certificates(transaction.getBody().getCertificates())
                                                .build();
                                    }
                            ).collect(Collectors.toList());
            publisher.publishEvent(new CertificateEvent(eventMetadata, txCertificatesList));
            return true;
        }, eventExecutor);

        //Mints
        var txMintBurnEvent = CompletableFuture.supplyAsync(() -> {
            List<TxMintBurn> txMintBurnEvents = transactions.stream().filter(transaction ->
                            transaction.getBody().getMint() != null && transaction.getBody().getMint().size() > 0)
                    .map(transaction -> new TxMintBurn(transaction.getTxHash(), sanitizeAmounts(transaction.getBody().getMint())))
                    .collect(Collectors.toList());
            publisher.publishEvent(new MintBurnEvent(eventMetadata, txMintBurnEvents));
            return true;
        }, eventExecutor);

        //Updates
        var txUpdateEvent = CompletableFuture.supplyAsync(() -> {
            List<TxUpdate> txUpdates = transactions.stream().filter(transaction -> transaction.getBody().getUpdate() != null)
                    .map(transaction -> new TxUpdate(transaction.getTxHash(), transaction.getBody().getUpdate()))
                    .toList();
            if (txUpdates.size() > 0)
                publisher.publishEvent(new UpdateEvent(eventMetadata, txUpdates));
            return true;
        });

        //Governance
        var governanceEvent = CompletableFuture.supplyAsync(() -> {
            List<TxGovernance> txGovernanceList = transactions.stream().filter(transaction ->
                            transaction.getBody().getProposalProcedures() != null || transaction.getBody().getVotingProcedures() != null)
                    .map(transaction -> new TxGovernance(transaction.getTxHash(), transaction.getBody().getVotingProcedures(),
                            transaction.getBody().getProposalProcedures()))
                    .collect(Collectors.toList());

            if (txGovernanceList.size() > 0)
                publisher.publishEvent(new GovernanceEvent(eventMetadata, txGovernanceList));
            return true;
        });

        CompletableFuture.allOf(eraEventCf, blockEventCf, blockHeaderEventCf, txnEventCf, txScriptEvent, txAuxDataEvent,
                txCertificateEvent, txMintBurnEvent, txUpdateEvent, governanceEvent).join();
    }

    private void processBlockSingleThread(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        var blockHeader = block.getHeader();

        publisher.publishEvent(eventMetadata.getEra());
        publisher.publishEvent(new BlockEvent(eventMetadata, block));
        publisher.publishEvent(new BlockHeaderEvent(eventMetadata, blockHeader));
        publisher.publishEvent(new TransactionEvent(eventMetadata, transactions));

        //Addtional events
        //TxScript Event
        List<TxScripts> txScriptsList = getTxScripts(transactions);
        publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));

        //AuxData event
        List<TxAuxData> txAuxDataList = transactions.stream()
                .filter(transaction -> transaction.getAuxData() != null)
                .map(transaction -> TxAuxData.builder()
                        .txHash(transaction.getTxHash())
                        .auxData(transaction.getAuxData())
                        .build()
                ).collect(Collectors.toList());
        publisher.publishEvent(new AuxDataEvent(eventMetadata, txAuxDataList));

        //Certificate event
        List<TxCertificates> txCertificatesList = IntStream.range(0, transactions.size())
                .mapToObj(i -> {
                            var transaction = transactions.get(i);
                            return TxCertificates.builder()
                                    .txHash(transaction.getTxHash())
                                    .blockIndex(i)
                                    .certificates(transaction.getBody().getCertificates())
                                    .build();
                        }
                ).collect(Collectors.toList());
        publisher.publishEvent(new CertificateEvent(eventMetadata, txCertificatesList));

        //Mints
        List<TxMintBurn> txMintBurnEvents = transactions.stream().filter(transaction ->
                        transaction.getBody().getMint() != null && transaction.getBody().getMint().size() > 0)
                .map(transaction -> new TxMintBurn(transaction.getTxHash(), sanitizeAmounts(transaction.getBody().getMint())))
                .collect(Collectors.toList());
        publisher.publishEvent(new MintBurnEvent(eventMetadata, txMintBurnEvents));

        //Updates
        List<TxUpdate> txUpdates = transactions.stream().filter(transaction -> transaction.getBody().getUpdate() != null)
                .map(transaction -> new TxUpdate(transaction.getTxHash(), transaction.getBody().getUpdate()))
                .toList();
        if (txUpdates.size() > 0)
            publisher.publishEvent(new UpdateEvent(eventMetadata, txUpdates));

        //Governance
        List<TxGovernance> txGovernanceList = transactions.stream().filter(transaction ->
                        transaction.getBody().getProposalProcedures() != null || transaction.getBody().getVotingProcedures() != null)
                .map(transaction -> new TxGovernance(transaction.getTxHash(), transaction.getBody().getVotingProcedures(),
                        transaction.getBody().getProposalProcedures()))
                .collect(Collectors.toList());

        if (txGovernanceList.size() > 0)
            publisher.publishEvent(new GovernanceEvent(eventMetadata, txGovernanceList));
    }

    private CompletableFuture<Boolean> publishEventAsync(Object event) {
        return CompletableFuture.supplyAsync(() -> {
            publisher.publishEvent(event);
            return true;
        }, eventExecutor);
    }

    //Replace asset name contains \u0000 -- postgres can't convert this to text. so replace
    private List<Amount> sanitizeAmounts(List<Amount> amounts) {
        if (amounts == null) return Collections.EMPTY_LIST;
        //Fix -- some asset name contains \u0000 -- postgres can't convert this to text. so replace
        return amounts.stream().map(amount ->
                Amount.builder()
                        .unit(amount.getUnit() != null ? amount.getUnit().replace(".", "") : null)
                        .policyId(amount.getPolicyId())
                        .assetName(amount.getAssetName().replace('\u0000', ' '))
                        .assetNameBytes(amount.getAssetNameBytes())
                        .quantity(amount.getQuantity())
                        .build()).collect(Collectors.toList());
    }

    private List<TxScripts> getTxScripts(List<Transaction> transactions) {
        List<TxScripts> txScriptsList = transactions.stream().map(transaction -> TxScripts.builder()
                .txHash(transaction.getTxHash())
                .plutusV1Scripts(transaction.getWitnesses().getPlutusV1Scripts())
                .plutusV2Scripts(transaction.getWitnesses().getPlutusV2Scripts())
                .nativeScripts(transaction.getWitnesses().getNativeScripts())
                .datums(transaction.getWitnesses().getDatums())
                .redeemers(transaction.getWitnesses().getRedeemers())
                .build()
        ).collect(Collectors.toList());
        return txScriptsList;
    }

}
