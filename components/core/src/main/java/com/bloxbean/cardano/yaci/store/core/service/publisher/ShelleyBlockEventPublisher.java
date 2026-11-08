package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;

@Component
@ReadOnly(false)
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
    //The sync session the blocks in batchBlockList belong to. Guarded by batchBlockList.
    private Long batchSession;

    //Shared with BlockFetchService. A default instance keeps hand-built instances (tests) working.
    private SyncSessionGuard sessionGuard = new SyncSessionGuard();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSessionGuard(SyncSessionGuard sessionGuard) {
        this.sessionGuard = sessionGuard;
    }

    public void reset() {
        synchronized (batchBlockList) {
            batchBlockList.clear();
            batchSession = null;
        }
    }

    /**
     * Call with the batchBlockList lock held. A thread of an old sync session must not add to, or take, the batch of
     * the session that replaced it, so the session is checked under the same lock. Blocks left by an older session are
     * dropped: the current session fetches them again from the cursor.
     */
    private void ownBatch() {
        sessionGuard.checkCaller();
        Long session = sessionGuard.callerSession();
        if (session != null && !session.equals(batchSession)) {
            if (!batchBlockList.isEmpty())
                log.warn("Dropping {} batched blocks of old sync session {}", batchBlockList.size(), batchSession);
            batchBlockList.clear();
            batchSession = session;
        }
    }

    @Transactional
    public void publishBlockEvents(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        processBlockSingleThread(eventMetadata, block, transactions);
        //A thread from an old sync session (e.g. a listener that hung past a restart) must not commit this block
        sessionGuard.checkCaller();
        publisher.publishEvent(new PreCommitEvent(eventMetadata));
        publisher.publishEvent(new CommitEvent(eventMetadata, List.of(new BatchBlock(eventMetadata, block, transactions))));

        //Checked again: a commit listener can hang while a restart starts a new session. In this block's transaction,
        //so a stale block is rolled back with its cursor.
        sessionGuard.writeIfCurrent(() -> cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra())));
    }

    //Don't add transactional annotation here
    public void publishBlockEventsInParallel(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        handleBlockBatchInParallel(eventMetadata, block, transactions);
    }

    private void handleBlockBatchInParallel(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        int size;
        synchronized (batchBlockList) {
            ownBatch();
            batchBlockList.add(new BatchBlock(eventMetadata, block, transactions));
            size = batchBlockList.size();
        }
        if (size < storeProperties.getBlocksBatchSize())
            return;

        processBlocksInParallel();

    }

    public void processBlocksInParallel() {
        //Work on a copy: the list may be reset or refilled by another sync session while this batch is written
        List<BatchBlock> batch;
        synchronized (batchBlockList) {
            ownBatch();
            if (batchBlockList.isEmpty())
                return;
            batch = new ArrayList<>(batchBlockList);
            batchBlockList.clear();
        }

        //The workers check the caller's session before each block, so an old session stops writing early
        Long session = sessionGuard.callerSession();
        List<List<BatchBlock>> partitions = partition(batch, storeProperties.getBlocksPartitionSize());
        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchBlock blockCache : partition) {
                    sessionGuard.check(session);
                    processBlockInParallel(blockCache.getMetadata(), blockCache.getBlock(), blockCache.getTransactions());
                }

                return true;
            }, blockExecutor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(storeProperties.getProcessingThreadsTimeout(), TimeUnit.MINUTES)
                .join();

        //A batch of an old sync session is not committed and the cursor is not moved. Its blocks are fetched again
        //by the current session.
        sessionGuard.checkCaller();

        BatchBlock lastBatchBlock = batch.getLast();

        //Publish BatchProcessedEvent. This may be useful for some scenarios where we need to do some processing before CommitEvent
        publisher.publishEvent(new BatchBlocksProcessedEvent(lastBatchBlock.getMetadata(), batch));

        publisher.publishEvent(new PreCommitEvent(lastBatchBlock.getMetadata()));
        publisher.publishEvent(new CommitEvent(lastBatchBlock.getMetadata(), batch));

        //Finally Set the cursor. Checked again (a commit listener can hang while a restart starts a new session), and
        //once more after the write in the cursor's transaction.
        sessionGuard.writeIfCurrent(() -> cursorService.setCursor(new Cursor(lastBatchBlock.getMetadata().getSlot(),
                lastBatchBlock.getMetadata().getBlockHash(), lastBatchBlock.getMetadata().getBlock(),
                lastBatchBlock.getMetadata().getPrevBlockHash(), lastBatchBlock.getMetadata().getEra())));
    }

    private void processBlockInParallel(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        var blockHeader = block.getHeader();

        var eraEventCf = publishEventAsync(eventMetadata.getEra());
        var blockEventCf = publishEventAsync(new BlockEvent(eventMetadata, block));
        var blockHeaderEventCf = publishEventAsync(new BlockHeaderEvent(eventMetadata, blockHeader));
        var txnEventCf = publishEventAsync(new TransactionEvent(eventMetadata, transactions));

        //Script Event
        var txScriptEvent = CompletableFuture.supplyAsync(() -> {
            List<Transaction> validTransactions = transactions.stream()
                    .filter(transaction -> !transaction.isInvalid())
                    .collect(Collectors.toList());
            List<TxScripts> txScriptsList = getTxScripts(validTransactions);
            publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));
            return true;
        }, eventExecutor);

        //AuxData event
        var txAuxDataEvent = CompletableFuture.supplyAsync(() -> {
            //Index over the full list so tx_index counts invalid transactions, then filter
            List<TxAuxData> txAuxDataList = IntStream.range(0, transactions.size())
                    .filter(i -> !transactions.get(i).isInvalid())
                    .filter(i -> transactions.get(i).getAuxData() != null)
                    .mapToObj(i -> TxAuxData.builder()
                            .txHash(transactions.get(i).getTxHash())
                            .txIndex(i)
                            .auxData(transactions.get(i).getAuxData())
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
                                        if (transaction.isInvalid())
                                            return null;
                                        return TxCertificates.builder()
                                                .txHash(transaction.getTxHash())
                                                .txIndex(i)
                                                .certificates(transaction.getBody().getCertificates())
                                                .build();
                                    }
                            )
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
            publisher.publishEvent(new CertificateEvent(eventMetadata, txCertificatesList));
            return true;
        }, eventExecutor);

        //Mints
        var txMintBurnEvent = CompletableFuture.supplyAsync(() -> {
            //Index over the full list so tx_index counts invalid transactions, then filter
            List<TxMintBurn> txMintBurnEvents = IntStream.range(0, transactions.size())
                    .filter(i -> !transactions.get(i).isInvalid())
                    .filter(i -> transactions.get(i).getBody().getMint() != null
                            && transactions.get(i).getBody().getMint().size() > 0)
                    .mapToObj(i -> TxMintBurn.builder()
                            .txHash(transactions.get(i).getTxHash())
                            .txIndex(i)
                            .amounts(sanitizeAmounts(transactions.get(i).getBody().getMint()))
                            .build()
                    ).collect(Collectors.toList());
            publisher.publishEvent(new MintBurnEvent(eventMetadata, txMintBurnEvents));
            return true;
        }, eventExecutor);

        //Updates
        var txUpdateEvent = CompletableFuture.supplyAsync(() -> {
            List<TxUpdate> txUpdates = transactions.stream()
                    .filter(transaction -> !transaction.isInvalid())
                    .filter(transaction -> transaction.getBody().getUpdate() != null)
                    .map(transaction -> new TxUpdate(transaction.getTxHash(), transaction.getBody().getUpdate()))
                    .toList();
            if (txUpdates.size() > 0)
                publisher.publishEvent(new UpdateEvent(eventMetadata, txUpdates));
            return true;
        }, eventExecutor);

        //Governance
        var governanceEvent = CompletableFuture.supplyAsync(() -> {
            List<TxGovernance> txGovernanceList = IntStream.range(0, transactions.size())
                    .mapToObj(i -> {
                        var transaction = transactions.get(i);
                        if (transaction.isInvalid())
                            return null;
                        if (transaction.getBody().getProposalProcedures() != null || transaction.getBody().getVotingProcedures() != null) {
                            return TxGovernance.builder()
                                    .txHash(transaction.getTxHash())
                                    .txIndex(i)
                                    .proposalProcedures(transaction.getBody().getProposalProcedures())
                                    .votingProcedures(transaction.getBody().getVotingProcedures())
                                    .build();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (txGovernanceList.size() > 0)
                publisher.publishEvent(new GovernanceEvent(eventMetadata, txGovernanceList));
            return true;
        }, eventExecutor);

        CompletableFuture.allOf(eraEventCf, blockEventCf, blockHeaderEventCf, txnEventCf, txScriptEvent, txAuxDataEvent,
                txCertificateEvent, txMintBurnEvent, txUpdateEvent, governanceEvent)
                .orTimeout(storeProperties.getProcessingThreadsTimeout(), TimeUnit.MINUTES)
                .join();
    }

    private void processBlockSingleThread(EventMetadata eventMetadata, Block block, List<Transaction> transactions) {
        var blockHeader = block.getHeader();

        publisher.publishEvent(eventMetadata.getEra());
        publisher.publishEvent(new BlockEvent(eventMetadata, block));
        publisher.publishEvent(new BlockHeaderEvent(eventMetadata, blockHeader));
        publisher.publishEvent(new TransactionEvent(eventMetadata, transactions));

        //Addtional events
        //TxScript Event
        List<Transaction> validTransactions = transactions.stream()
                .filter(transaction -> !transaction.isInvalid())
                .collect(Collectors.toList());
        List<TxScripts> txScriptsList = getTxScripts(validTransactions);

        publisher.publishEvent(new ScriptEvent(eventMetadata, txScriptsList));

        //AuxData event
        //Index over the full list so tx_index counts invalid transactions, then filter
        List<TxAuxData> txAuxDataList = IntStream.range(0, transactions.size())
                .filter(i -> !transactions.get(i).isInvalid())
                .filter(i -> transactions.get(i).getAuxData() != null)
                .mapToObj(i -> TxAuxData.builder()
                        .txHash(transactions.get(i).getTxHash())
                        .txIndex(i)
                        .auxData(transactions.get(i).getAuxData())
                        .build()
                ).collect(Collectors.toList());
        publisher.publishEvent(new AuxDataEvent(eventMetadata, txAuxDataList));

        //Certificate event
        List<TxCertificates> txCertificatesList = IntStream.range(0, transactions.size())
                .mapToObj(i -> {
                            var transaction = transactions.get(i);
                            if (transaction.isInvalid())
                                return null;
                            return TxCertificates.builder()
                                    .txHash(transaction.getTxHash())
                                    .txIndex(i)
                                    .certificates(transaction.getBody().getCertificates())
                                    .build();
                        }
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        publisher.publishEvent(new CertificateEvent(eventMetadata, txCertificatesList));

        //Mints
        //Index over the full list so tx_index counts invalid transactions, then filter
        List<TxMintBurn> txMintBurnEvents = IntStream.range(0, transactions.size())
                .filter(i -> !transactions.get(i).isInvalid())
                .filter(i -> transactions.get(i).getBody().getMint() != null
                        && transactions.get(i).getBody().getMint().size() > 0)
                .mapToObj(i -> TxMintBurn.builder()
                        .txHash(transactions.get(i).getTxHash())
                        .txIndex(i)
                        .amounts(sanitizeAmounts(transactions.get(i).getBody().getMint()))
                        .build()
                ).collect(Collectors.toList());
        publisher.publishEvent(new MintBurnEvent(eventMetadata, txMintBurnEvents));

        //Updates
        List<TxUpdate> txUpdates = transactions.stream()
                .filter(transaction -> !transaction.isInvalid())
                .filter(transaction -> transaction.getBody().getUpdate() != null)
                .map(transaction -> new TxUpdate(transaction.getTxHash(), transaction.getBody().getUpdate()))
                .toList();
        if (txUpdates.size() > 0)
            publisher.publishEvent(new UpdateEvent(eventMetadata, txUpdates));

        //Governance
        List<TxGovernance> txGovernanceList = IntStream.range(0, transactions.size())
                .mapToObj(i -> {
                    var transaction = transactions.get(i);
                    if (transaction.isInvalid())
                        return null;
                    if (transaction.getBody().getProposalProcedures() != null || transaction.getBody().getVotingProcedures() != null) {
                        return TxGovernance.builder()
                                .txHash(transaction.getTxHash())
                                .txIndex(i)
                                .proposalProcedures(transaction.getBody().getProposalProcedures())
                                .votingProcedures(transaction.getBody().getVotingProcedures())
                                .build();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
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
                .plutusV3Scripts(transaction.getWitnesses().getPlutusV3Scripts())
                .nativeScripts(transaction.getWitnesses().getNativeScripts())
                .datums(transaction.getWitnesses().getDatums())
                .redeemers(transaction.getWitnesses().getRedeemers())
                .build()
        ).collect(Collectors.toList());
        return txScriptsList;
    }

}
