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

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
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
