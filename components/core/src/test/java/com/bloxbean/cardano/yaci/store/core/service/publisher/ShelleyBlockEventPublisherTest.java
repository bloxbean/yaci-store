package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.core.model.governance.ProposalProcedure;
import com.bloxbean.cardano.yaci.core.model.governance.VotingProcedures;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.stubbing.Answer;
import com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelleyBlockEventPublisherTest {

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private CursorService cursorService;

    @Mock
    private ExecutorService blockExecutor;

    @Mock
    private ExecutorService eventExecutor;

    private ShelleyBlockEventPublisher shelleyBlockEventPublisher;

    @Captor
    private ArgumentCaptor<Object> eventCaptor;

    @BeforeEach
    void setup() {
        StoreProperties storeProperties = StoreProperties.builder()
                .blocksBatchSize(100)
                .blocksPartitionSize(15)
                .processingThreadsTimeout(5)
                .build();

        shelleyBlockEventPublisher = new ShelleyBlockEventPublisher(
                blockExecutor, eventExecutor, publisher, cursorService, storeProperties);
    }

    @Test
    void shouldNotPublishMintBurnDataForInvalidTransactions() {
        List<Transaction> transactions = List.of(
                validTransactionWithMintData("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d"),
                invalidTransactionWithMintData("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var mintBurnEvent = capturePublishedEvent(MintBurnEvent.class);
        assertThat(mintBurnEvent).isNotNull();
        assertThat(mintBurnEvent.getTxMintBurns()).hasSize(1);
        assertThat(mintBurnEvent.getTxMintBurns().get(0).getTxHash()).isEqualTo("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d");
    }

    @Test
    void shouldSetMintBurnTxIndexCountingInvalidTransactions() {
        List<Transaction> transactions = List.of(
                invalidTransactionWithMintData("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d"),
                validTransactionWithMintData("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var mintBurnEvent = capturePublishedEvent(MintBurnEvent.class);
        assertThat(mintBurnEvent).isNotNull();
        assertThat(mintBurnEvent.getTxMintBurns()).hasSize(1);
        assertThat(mintBurnEvent.getTxMintBurns().get(0).getTxIndex()).isEqualTo(1);
    }

    @Test
    void shouldNotPublishCertificateDataForInvalidTransactions() {
        List<Transaction> transactions = List.of(
                validTransactionWithCertificateData("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d"),
                invalidTransactionWithCertificateData("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var certEvent = capturePublishedEvent(CertificateEvent.class);
        assertThat(certEvent).isNotNull();
        assertThat(certEvent.getTxCertificatesList()).hasSize(1);
        assertThat(certEvent.getTxCertificatesList().get(0).getTxHash()).isEqualTo("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d");
    }

    @Test
    void shouldNotPublishGovernanceDataForInvalidTransactions() {
        List<Transaction> transactions = List.of(
                validTransactionWithGovernanceData("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d"),
                invalidTransactionWithGovernanceData("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var govEvent = capturePublishedEvent(GovernanceEvent.class);
        assertThat(govEvent).isNotNull();
        assertThat(govEvent.getTxGovernanceList()).hasSize(1);
        assertThat(govEvent.getTxGovernanceList().get(0).getTxHash()).isEqualTo("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d");
    }

    @Test
    void shouldNotPublishAuxDataForInvalidTransactions() {
        AuxData auxData = new AuxData();

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .txHash("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .auxData(auxData)
                        .invalid(false)
                        .build(),
                Transaction.builder()
                        .txHash("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .auxData(auxData)
                        .invalid(true)
                        .build()
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var auxDataEvent = capturePublishedEvent(AuxDataEvent.class);
        assertThat(auxDataEvent).isNotNull();
        assertThat(auxDataEvent.getTxAuxDataList()).hasSize(1);
        assertThat(auxDataEvent.getTxAuxDataList().get(0).getTxHash()).isEqualTo("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d");
    }

    @Test
    void shouldSetAuxDataTxIndexCountingInvalidTransactions() {
        AuxData auxData = new AuxData();

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .txHash("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .auxData(auxData)
                        .invalid(true)
                        .build(),
                Transaction.builder()
                        .txHash("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .auxData(auxData)
                        .invalid(false)
                        .build()
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var auxDataEvent = capturePublishedEvent(AuxDataEvent.class);
        assertThat(auxDataEvent).isNotNull();
        assertThat(auxDataEvent.getTxAuxDataList()).hasSize(1);
        assertThat(auxDataEvent.getTxAuxDataList().get(0).getTxIndex()).isEqualTo(1);
    }

    @Test
    void shouldNotPublishUpdateDataForInvalidTransactions() {
        Update update = mock(Update.class);

        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .txHash("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(TransactionBody.builder()
                                .update(update)
                                .build())
                        .witnesses(emptyWitnesses())
                        .invalid(false)
                        .build(),
                Transaction.builder()
                        .txHash("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(TransactionBody.builder()
                                .update(update)
                                .build())
                        .witnesses(emptyWitnesses())
                        .invalid(true)
                        .build()
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var updateEvent = capturePublishedEvent(UpdateEvent.class);
        assertThat(updateEvent).isNotNull();
        assertThat(updateEvent.getUpdates()).hasSize(1);
        assertThat(updateEvent.getUpdates().get(0).getTxHash()).isEqualTo("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d");
    }

    @Test
    void shouldStillPublishTransactionEventWithInvalidTransactions() {
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .txHash("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .invalid(false)
                        .build(),
                Transaction.builder()
                        .txHash("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .invalid(true)
                        .build()
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var txEvent = capturePublishedEvent(TransactionEvent.class);
        assertThat(txEvent).isNotNull();
        assertThat(txEvent.getTransactions()).hasSize(2);
    }

    @Test
    void shouldNotPublishScriptDataForInvalidTransaction() {
        List<Transaction> transactions = List.of(
                Transaction.builder()
                        .txHash("aaaaaaaaaa9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .invalid(false)
                        .build(),
                Transaction.builder()
                        .txHash("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
                        .body(minimalBody())
                        .witnesses(emptyWitnesses())
                        .invalid(true)
                        .build()
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var scriptEvent = capturePublishedEvent(ScriptEvent.class);
        assertThat(scriptEvent).isNotNull();
        assertThat(scriptEvent.getTxScriptsList()).hasSize(1);
    }

    @Test
    void shouldPublishMintBurnEventWithEmptyDataWhenAllTransactionsInvalid() {
        List<Transaction> transactions = List.of(
                invalidTransactionWithMintData("bbbbbbbbbb9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d"),
                invalidTransactionWithMintData("cccccccccc9cecf2602f1110e53331a3b305ec668ad57081df98416b30473f5d")
        );

        shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), transactions);

        var mintBurnEvent = capturePublishedEvent(MintBurnEvent.class);
        assertThat(mintBurnEvent).isNotNull();
        assertThat(mintBurnEvent.getTxMintBurns()).isEmpty();
    }

    @Test
    void blockFromAnOldSyncSessionIsNotCommitted() {
        var guard = new com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long oldSession = guard.newSession();
        guard.newSession(); //a restart started a new session while this thread was still writing

        guard.enter(oldSession);
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                            shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), List.of()))
                    .isInstanceOf(com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        verify(cursorService, never()).setCursor(any());
        verify(publisher, never()).publishEvent(any(com.bloxbean.cardano.yaci.store.events.internal.CommitEvent.class));
    }

    @Test
    void flushFromAnOldSyncSessionLeavesTheCurrentBatchAlone() {
        var guard = new com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long oldSession = guard.newSession();
        long newSession = guard.newSession();

        guard.enter(newSession);
        try {
            shelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata(), mock(Block.class), List.of());
        } finally {
            guard.exit();
        }

        guard.enter(oldSession);
        try {
            org.assertj.core.api.Assertions.assertThatThrownBy(shelleyBlockEventPublisher::processBlocksInParallel)
                    .isInstanceOf(com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        List<?> batch = (List<?>) org.springframework.test.util.ReflectionTestUtils.getField(shelleyBlockEventPublisher, "batchBlockList");
        assertThat(batch).hasSize(1);
        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void sessionReplacedDuringCommitEventsDoesNotMoveTheCursor() {
        //#1344 finding 1: a commit listener hangs while a restart starts a new session
        var guard = new SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long session = guard.newSession();
        lenient().doAnswer(inv -> guard.newSession()).when(publisher).publishEvent(any(PreCommitEvent.class));

        guard.enter(session);
        try {
            assertThatThrownBy(() -> shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), List.of()))
                    .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void sessionReplacedDuringTheCursorWriteFailsTheBlockTransaction() {
        //The check after the write throws inside the block's transaction, so the cursor is rolled back
        var guard = new SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long session = guard.newSession();
        doAnswer(inv -> guard.newSession()).when(cursorService).setCursor(any());

        guard.enter(session);
        try {
            assertThatThrownBy(() -> shelleyBlockEventPublisher.publishBlockEvents(eventMetadata(), mockBlock(), List.of()))
                    .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }
    }

    @Test
    void sessionReplacedDuringCommitEventsOfABatchDoesNotMoveTheCursor() {
        runExecutorsInline();
        var guard = new SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long session = guard.newSession();
        lenient().doAnswer(inv -> guard.newSession()).when(publisher).publishEvent(any(PreCommitEvent.class));

        guard.enter(session);
        try {
            shelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata(), mockBlock(), List.of());
            assertThatThrownBy(shelleyBlockEventPublisher::processBlocksInParallel)
                    .isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        } finally {
            guard.exit();
        }

        verify(publisher).publishEvent(any(PreCommitEvent.class));
        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void oldFlushWaitingForTheBatchLockLeavesTheNewBatchAlone() throws Exception {
        //#1344 finding 2: the old flush passes the session check, then waits for the lock while a new session starts
        //and queues a block
        runExecutorsInline();
        var guard = new SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long oldSession = guard.newSession();
        List<?> batch = (List<?>) ReflectionTestUtils.getField(shelleyBlockEventPublisher, "batchBlockList");

        BlockedCall oldFlush;
        synchronized (batch) {
            oldFlush = startBlockedOn(guard, oldSession, shelleyBlockEventPublisher::processBlocksInParallel);

            long newSession = guard.newSession();
            guard.enter(newSession);
            try {
                shelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata(), mock(Block.class), List.of());
            } finally {
                guard.exit();
            }
        }
        Throwable oldError = oldFlush.await();

        assertThat(oldError).isNotNull();
        assertThat(batch).hasSize(1);
        verify(cursorService, never()).setCursor(any());
    }

    @Test
    void oldEnqueueWaitingForTheBatchLockDoesNotAddToTheNewBatch() throws Exception {
        //A block of the old session must not be committed by the session that replaced it
        var guard = new SyncSessionGuard();
        shelleyBlockEventPublisher.setSessionGuard(guard);
        long oldSession = guard.newSession();
        List<?> batch = (List<?>) ReflectionTestUtils.getField(shelleyBlockEventPublisher, "batchBlockList");

        BlockedCall oldEnqueue;
        synchronized (batch) {
            oldEnqueue = startBlockedOn(guard, oldSession, () ->
                    shelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata(), mock(Block.class), List.of()));

            long newSession = guard.newSession();
            guard.enter(newSession);
            try {
                shelleyBlockEventPublisher.publishBlockEventsInParallel(eventMetadata(), mock(Block.class), List.of());
            } finally {
                guard.exit();
            }
        }
        Throwable oldError = oldEnqueue.await();

        assertThat(oldError).isInstanceOf(SyncSessionGuard.StaleSyncSessionException.class);
        assertThat(batch).hasSize(1);
    }

    private void runExecutorsInline() {
        Answer<Void> runInline = inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        };
        lenient().doAnswer(runInline).when(blockExecutor).execute(any());
        lenient().doAnswer(runInline).when(eventExecutor).execute(any());
    }

    /**
     * Starts the call on a thread of the given session and waits until it is blocked on a lock the test holds.
     */
    static BlockedCall startBlockedOn(SyncSessionGuard guard, long session, Runnable call) throws InterruptedException {
        var error = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
            guard.enter(session);
            try {
                call.run();
            } catch (Throwable e) {
                error.set(e);
            } finally {
                guard.exit();
            }
        });
        thread.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (thread.getState() != Thread.State.BLOCKED) {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("Call did not block on the lock, state: " + thread.getState());
            Thread.sleep(5);
        }
        return new BlockedCall(thread, error);
    }

    record BlockedCall(Thread thread, AtomicReference<Throwable> error) {
        /**
         * Call after releasing the lock: waits for the call to end and returns what it threw (null: nothing).
         */
        Throwable await() throws InterruptedException {
            thread.join(10_000);
            assertThat(thread.isAlive()).isFalse();
            return error.get();
        }
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .era(Era.Babbage)
                .protocolMagic(1)
                .epochNumber(29)
                .block(190253)
                .blockHash("82af2e80f94c60678b32b5b17da46b12e4095745a6c2a5ae841e0c89434a8b35")
                .blockTime(1666609974)
                .prevBlockHash("0080bd77e570d254a6c6bd34917c96a9e99d4e1eee227bb77d276b6483071bcb")
                .slot(10926774)
                .build();
    }

    private Block mockBlock() {
        Block block = mock(Block.class);
        BlockHeader header = mock(BlockHeader.class);
        when(block.getHeader()).thenReturn(header);
        return block;
    }

    private TransactionBody minimalBody() {
        return TransactionBody.builder().build();
    }

    private Witnesses emptyWitnesses() {
        return Witnesses.builder().build();
    }

    private Transaction validTransactionWithMintData(String txHash) {
        return Transaction.builder()
                .txHash(txHash)
                .body(TransactionBody.builder()
                        .mint(List.of(Amount.builder()
                                .unit("eeeeeee2939a1b471771fd28d89bda0d224b4f9cb69ff67e9c48397d4e46543238")
                                .policyId("eeeeeee2939a1b471771fd28d89bda0d224b4f9cb69ff67e9c48397d")
                                .assetName("NFTxx")
                                .assetNameBytes(new byte[0])
                                .quantity(BigInteger.TEN)
                                .build()))
                        .build())
                .witnesses(emptyWitnesses())
                .invalid(false)
                .build();
    }

    private Transaction invalidTransactionWithMintData(String txHash) {
        return Transaction.builder()
                .txHash(txHash)
                .body(TransactionBody.builder()
                        .mint(List.of(Amount.builder()
                                .unit("ddddddd2939a1b471771fd28d89bda0d224b4f9cb69ff67e9c48397d4e46543238")
                                .policyId("ddddddd2939a1b471771fd28d89bda0d224b4f9cb69ff67e9c48397d")
                                .assetName("NFTyy")
                                .assetNameBytes(new byte[0])
                                .quantity(BigInteger.TEN)
                                .build()))
                        .build())
                .witnesses(emptyWitnesses())
                .invalid(true)
                .build();
    }

    private Transaction validTransactionWithCertificateData(String txHash) {
        return Transaction.builder()
                .txHash(txHash)
                .body(TransactionBody.builder()
                        .certificates(List.of())
                        .build())
                .witnesses(emptyWitnesses())
                .invalid(false)
                .build();
    }

    private Transaction invalidTransactionWithCertificateData(String txHash) {
        return Transaction.builder()
                .txHash(txHash)
                .body(TransactionBody.builder()
                        .certificates(List.of())
                        .build())
                .witnesses(emptyWitnesses())
                .invalid(true)
                .build();
    }

    private Transaction validTransactionWithGovernanceData(String txHash) {
        return Transaction.builder()
                .txHash(txHash)
                .body(TransactionBody.builder()
                        .proposalProcedures(List.of(mock(ProposalProcedure.class)))
                        .votingProcedures(mock(VotingProcedures.class))
                        .build())
                .witnesses(emptyWitnesses())
                .invalid(false)
                .build();
    }

    private Transaction invalidTransactionWithGovernanceData(String txHash) {
        return Transaction.builder()
                .txHash(txHash)
                .body(TransactionBody.builder()
                        .proposalProcedures(List.of(mock(ProposalProcedure.class)))
                        .votingProcedures(mock(VotingProcedures.class))
                        .build())
                .witnesses(emptyWitnesses())
                .invalid(true)
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T capturePublishedEvent(Class<T> eventType) {
        verify(publisher, atLeastOnce()).publishEvent(eventCaptor.capture());

        return eventCaptor.getAllValues().stream()
                .filter(eventType::isInstance)
                .map(eventType::cast)
                .findFirst()
                .orElse(null);
    }
}
