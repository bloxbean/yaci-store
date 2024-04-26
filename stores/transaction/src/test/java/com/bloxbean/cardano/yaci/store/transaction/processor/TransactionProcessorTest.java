package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.client.utxo.DummyUtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxWitnessType;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {
    @Mock
    private TransactionStorage transactionStorage;
    @Mock
    private TransactionWitnessStorage transactionWitnessStorage;

    private FeeResolver feeResolver;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private InvalidTransactionStorage invalidTransactionStorage;

    private TransactionProcessor transactionProcessor;
    @Captor
    private ArgumentCaptor<List<Txn>> txnListCaptor;
    @Captor
    private ArgumentCaptor<List<TxnWitness>> txnWitnessesCaptor;
    @Captor
    private ArgumentCaptor<InvalidTransaction> invalidTxCaptor;

    @BeforeEach
    public void setup() {
        feeResolver = new FeeResolver(new DummyUtxoClient());
        transactionProcessor = new TransactionProcessor(transactionStorage, transactionWitnessStorage, invalidTransactionStorage, new ObjectMapper(), feeResolver, publisher);
    }

    @Test
    void givenTransactionEvent_shouldSaveTxnList() {
        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(transactions())
                .metadata(eventMetadata())
                .build();

        transactionProcessor.handleTransactionEvent(transactionEvent);

        verify(transactionStorage, Mockito.times(1)).saveAll(txnListCaptor.capture());
        verify(invalidTransactionStorage, Mockito.never()).save(invalidTxCaptor.capture());
        List<Txn> txnList = txnListCaptor.getValue();

        assertThat(txnList).hasSize(1);

        assertThat(txnList.get(0).getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(txnList.get(0).getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(txnList.get(0).getSlot()).isEqualTo(eventMetadata().getSlot());

        assertThat(txnList.get(0).getTxHash()).isEqualTo("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8");

        assertThat(txnList.get(0).getInputs()).hasSize(3);
        assertThat(txnList.get(0).getInputs()).map(UtxoKey::getTxHash).contains(
                "aaaae529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8",
                "bbbbe529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8",
                "cccce529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8");

        assertThat(txnList.get(0).getInputs()).map(UtxoKey::getOutputIndex).contains(0, 1, 2);
        assertThat(txnList.get(0).getOutputs()).hasSize(2);
        assertThat(txnList.get(0).getOutputs())
                .filteredOn(utxoKey -> utxoKey.getTxHash().equals("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8"))
                .hasSize(2);

        assertThat(txnList.get(0).getFee()).isEqualTo(new BigInteger("300000"));
        assertThat(txnList.get(0).getTtl()).isEqualTo(1);
        assertThat(txnList.get(0).getValidityIntervalStart()).isEqualTo(1);
        assertThat(txnList.get(0).getAuxiliaryDataHash()).isEqualTo("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e");
        assertThat(txnList.get(0).getScriptDataHash()).isEqualTo("aaaab33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e");
        assertThat(txnList.get(0).getTotalCollateral()).isEqualTo(BigInteger.ONE);

        assertThat(txnList.get(0).getCollateralInputs()).hasSize(2);
        assertThat(txnList.get(0).getCollateralInputs()).map(UtxoKey::getTxHash).contains(
                "dddd529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8",
                "eeee529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8");
        assertThat(txnList.get(0).getCollateralInputs()).map(UtxoKey::getOutputIndex).contains(0, 4);

        assertThat(txnList.get(0).getReferenceInputs()).hasSize(2);
        assertThat(txnList.get(0).getReferenceInputs()).map(UtxoKey::getTxHash).contains(
                "c3e9dcc6c638b78c2591f74cfca27fba728e56c18e0b5a61a2a0cc900592c59f",
                "5fb81bcabefefdb725a26fc9825d517ba79a05f86f845fda42b3f0f54b7ab90b");
        assertThat(txnList.get(0).getReferenceInputs()).map(UtxoKey::getOutputIndex).contains(0, 1);

        assertThat(txnList.get(0).getCollateralReturnJson().getAddress()).isEqualTo("addr_test1vpfwv0ezc5g8a4mkku8hhy3y3vp92t7s3ul8g778g5yegsgalc6gc");
        assertThat(txnList.get(0).getCollateralReturnJson().getAmounts()).hasSize(1);
        assertThat(txnList.get(0).getCollateralReturnJson().getAmounts().get(0).getQuantity()).isEqualTo(BigInteger.valueOf(500));
        assertThat(txnList.get(0).getCollateralReturnJson().getAmounts().get(0).getAssetName()).isEqualTo(LOVELACE);
        assertThat(txnList.get(0).getCollateralReturnJson().getAmounts().get(0).getUnit()).isEqualTo(LOVELACE);
    }

    @Test
    void givenTransactionEvent_shouldSaveTxnWitnessList() {
        List<VkeyWitness> vkeyWitnesses = List.of(VkeyWitness.builder()
                .signature("5ce8776d3b749e7b096f5dbd388029db57a0c9fc87662b93cf31da4ad1748b3d5d92a7d65454043ae10dbfa4ac34929311526323a7ab3a7436f02e16abdbdb08")
                .key("9691ed9d98a5b79d5bc46c4496a6dba7e103f668f525c8349b6b92676cb3eae4")
                .build());

        List<BootstrapWitness> bootstrapWitnesses = List.of(BootstrapWitness.builder()
                .signature("a6ace79954aeec7630c4c9376110a1c710d11af30eb056394aee03968251845d6fa1406c23f753b359068082d47bcf985fd14bf21feb8067bcf2234726578d0f")
                .attributes("a1024101")
                .chainCode("cd37d942b4eb28d5435df4d4e18bc7ed11388fe2ea43615f544f7c09c7b29f25")
                .publicKey("7229838c0d5c5baaf0f09138853a52c7e7088bda6aa497af5606e5de5120ef99")
                .build());

        List<Transaction> transactions = List.of(Transaction.builder()
                .witnesses(Witnesses.builder()
                        .vkeyWitnesses(vkeyWitnesses)
                        .bootstrapWitnesses(bootstrapWitnesses)
                        .build())
                .txHash("a3d6f2627a56fe7921eeda546abfe164321881d41549b7f2fbf09ea0b718d758")
                .build());

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(transactions)
                .metadata(EventMetadata.builder()
                        .slot(10608917)
                        .build())
                .build();

        transactionProcessor.handleTransactionWitnesses(transactionEvent);

        verify(transactionWitnessStorage, Mockito.times(1)).saveAll(txnWitnessesCaptor.capture());

        List<TxnWitness> txnWitnessList = txnWitnessesCaptor.getValue();
        assertThat(txnWitnessList).hasSize(2);
        assertThat(txnWitnessList).map(TxnWitness::getType).contains(TxWitnessType.VKEY_WITNESS, TxWitnessType.BOOTSTRAP_WITNESS);
        for (var txWitness : txnWitnessList) {
            assertThat(txWitness.getSlot()).isEqualTo(10608917);
            assertThat(txWitness.getTxHash()).isEqualTo("a3d6f2627a56fe7921eeda546abfe164321881d41549b7f2fbf09ea0b718d758");

            if (txWitness.getType().equals(TxWitnessType.VKEY_WITNESS)) {
                assertThat(txWitness.getPubKey()).isEqualTo("9691ed9d98a5b79d5bc46c4496a6dba7e103f668f525c8349b6b92676cb3eae4");
                assertThat(txWitness.getSignature()).isEqualTo("5ce8776d3b749e7b096f5dbd388029db57a0c9fc87662b93cf31da4ad1748b3d5d92a7d65454043ae10dbfa4ac34929311526323a7ab3a7436f02e16abdbdb08");
            } else if (txWitness.getType().equals(TxWitnessType.BOOTSTRAP_WITNESS)) {
                assertThat(txWitness.getPubKey()).isEqualTo("7229838c0d5c5baaf0f09138853a52c7e7088bda6aa497af5606e5de5120ef99");
                assertThat(txWitness.getSignature()).isEqualTo("a6ace79954aeec7630c4c9376110a1c710d11af30eb056394aee03968251845d6fa1406c23f753b359068082d47bcf985fd14bf21feb8067bcf2234726578d0f");
                assertThat(txWitness.getAdditionalData().get("chaincode").asText()).isEqualTo("cd37d942b4eb28d5435df4d4e18bc7ed11388fe2ea43615f544f7c09c7b29f25");
                assertThat(txWitness.getAdditionalData().get("attributes").asText()).isEqualTo("a1024101");
            }
        }
    }

    @Test
    void givenTransactionEvent_withInvalidTransaction_shouldSaveInvalidTransaction() {
        var transactions = transactions();
        transactions.get(0).setInvalid(true);

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(transactions)
                .metadata(eventMetadata())
                .build();

        transactionProcessor.handleTransactionEvent(transactionEvent);

        verify(transactionStorage, Mockito.times(1)).saveAll(txnListCaptor.capture());
        verify(invalidTransactionStorage, Mockito.times(1)).save(invalidTxCaptor.capture());
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .era(Era.Babbage)
                .slotLeader("12946a3fe080dd99af599bfff10a05cd3de19bd38ed85b25dee35dd5")
                .protocolMagic(1)
                .epochNumber(29)
                .block(190253)
                .blockHash("82af2e80f94c60678b32b5b17da46b12e4095745a6c2a5ae841e0c89434a8b35")
                .blockTime(1666609974)
                .prevBlockHash("0080bd77e570d254a6c6bd34917c96a9e99d4e1eee227bb77d276b6483071bcb")
                .slot(10926774)
                .epochSlot(40374)
                .noOfTxs(1)
                .parallelMode(true)
                .build();
    }

    private List<Transaction> transactions() {
        String txHash = "f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8";
        Transaction transaction = Transaction.builder()
                .txHash(txHash)
                .slot(eventMetadata().getSlot())
                .blockNumber(eventMetadata().getBlock())
                .body(TransactionBody.builder()
                        .inputs(Set.of(
                                new TransactionInput("aaaae529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8", 1),
                                new TransactionInput("bbbbe529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8", 2),
                                new TransactionInput("cccce529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8", 0)
                        ))
                        .outputs(transactionOutputs())
                        .collateralInputs(Set.of(
                                new TransactionInput("dddd529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8", 4),
                                new TransactionInput("eeee529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8", 0)
                        ))
                        .collateralReturn(TransactionOutput.builder()
                                .address("addr_test1vpfwv0ezc5g8a4mkku8hhy3y3vp92t7s3ul8g778g5yegsgalc6gc")
                                .amounts(List.of(Amount.builder()
                                        .unit(LOVELACE)
                                        .assetName(LOVELACE)
                                        .quantity(BigInteger.valueOf(500))
                                        .build()))
                                .build())
                        .fee(BigInteger.valueOf(300000))
                        .ttl(1)
                        .netowrkId(1)
                        .validityIntervalStart(1)
                        .auxiliaryDataHash("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e")
                        .scriptDataHash("aaaab33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e")
                        .totalCollateral(BigInteger.ONE)
                        .referenceInputs(
                                Set.of(
                                        new TransactionInput("c3e9dcc6c638b78c2591f74cfca27fba728e56c18e0b5a61a2a0cc900592c59f", 0),
                                        new TransactionInput("5fb81bcabefefdb725a26fc9825d517ba79a05f86f845fda42b3f0f54b7ab90b", 1)
                                ))
                        .build()
                )
                .utxos(utxos(txHash, transactionOutputs()))
                .invalid(false)
                .build();

        return List.of(transaction);
    }

    private List<TransactionOutput> transactionOutputs() {
        TransactionOutput output1 = TransactionOutput.builder()
                .address("addr_test1vpfwv0ezc5g8a4mkku8hhy3y3vp92t7s3ul8g778g5yegsgalc6gc")
                .amounts(List.of(Amount.builder()
                        .unit(LOVELACE)
                        .assetName(LOVELACE)
                        .quantity(BigInteger.valueOf(20000))
                        .build()))
                .build();

        TransactionOutput output2 = TransactionOutput.builder()
                .address("addr_test1abcwv0ezc5g8a4mkku8hhy3y3vp92t7s3ul8g778g5yegsgalc6gc")
                .amounts(List.of(Amount.builder()
                        .unit(LOVELACE)
                        .assetName(LOVELACE)
                        .quantity(BigInteger.valueOf(20000))
                        .build()))
                .build();
        return List.of(output1, output2);
    }

    private List<Utxo> utxos(String txHash, List<TransactionOutput> txOutputs) {
        AtomicInteger index = new AtomicInteger();
        return txOutputs.stream()
                .map(txOut -> Utxo.builder()
                        .address(txOut.getAddress())
                        .txHash(txHash)
                        .index(index.getAndIncrement())
                        .amounts(txOut.getAmounts())
                        .build()).collect(Collectors.toList());
    }
}
