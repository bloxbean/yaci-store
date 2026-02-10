package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorage;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.util.HexUtil.encodeHexString;
import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WithdrawalProcessorTest {

    @Mock
    private WithdrawalStorage withdrawalStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    private WithdrawalProcessor withdrawalProcessor;

    @Captor
    private ArgumentCaptor<List<Txn>> txnListCaptor;
    @Captor
    private ArgumentCaptor<List<Withdrawal>> withdrawalListCaptor;

    @BeforeEach
    public void setup() {
        withdrawalProcessor = new WithdrawalProcessor(withdrawalStorage, publisher);
    }

    @Test
    void givenTransactionEvent_shouldSaveWithdrawals() {
        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(transactions())
                .metadata(eventMetadata())
                .build();

        withdrawalProcessor.processWithdrawal(transactionEvent);

        verify(withdrawalStorage, Mockito.times(1)).save(withdrawalListCaptor.capture());
        List<Withdrawal> withdrawals = withdrawalListCaptor.getValue();

        withdrawals.sort(Comparator.comparing(Withdrawal::getAmount));

        assertThat(withdrawals).hasSize(2);

        assertThat(withdrawals.get(0).getAddress()).isEqualTo("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq90uxgd");
        assertThat(withdrawals.get(0).getAmount()).isEqualTo(BigInteger.valueOf(1000));
        assertThat(withdrawals.get(0).getTxHash()).isEqualTo("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8");
        assertThat(withdrawals.get(0).getEpoch()).isEqualTo(29);
        assertThat(withdrawals.get(0).getSlot()).isEqualTo(10926774);

        assertThat(withdrawals.get(1).getAddress()).isEqualTo("stake1u9q0cgjgpulrnqmqpkef2gpkj4d0svwxv2u2fps30xda2wswap4kk");
        assertThat(withdrawals.get(1).getAmount()).isEqualTo(BigInteger.valueOf(2000));
        assertThat(withdrawals.get(1).getSlot()).isEqualTo(10926774);
        assertThat(withdrawals.get(1).getTxHash()).isEqualTo("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8");
        assertThat(withdrawals.get(1).getEpoch()).isEqualTo(29);
        assertThat(withdrawals.get(1).getSlot()).isEqualTo(10926774);

    }

    @Test
    void givenInvalidTransactionEvent_shouldNotSaveWithdrawals() {
        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(invalidTransactions())
                .metadata(eventMetadata())
                .build();

        withdrawalProcessor.processWithdrawal(transactionEvent);

        verify(withdrawalStorage, Mockito.never()).save(withdrawalListCaptor.capture());
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
                        .withdrawals(Map.of(
                                encodeHexString(new Address("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq90uxgd").getBytes()), BigInteger.valueOf(1000),
                                encodeHexString(new Address("stake1u9q0cgjgpulrnqmqpkef2gpkj4d0svwxv2u2fps30xda2wswap4kk").getBytes()), BigInteger.valueOf(2000)
                        ))
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

    private List<Transaction> invalidTransactions() {
        String txHash = "f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8";
        Transaction transaction = Transaction.builder()
                .txHash(txHash)
                .slot(eventMetadata().getSlot())
                .blockNumber(eventMetadata().getBlock())
                .body(TransactionBody.builder()
                        .inputs(Set.of(
                                new TransactionInput("aaaae529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8", 1)
                        ))
                        .outputs(transactionOutputs())
                        .fee(BigInteger.valueOf(300000))
                        .withdrawals(Map.of(
                                encodeHexString(new Address("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq90uxgd").getBytes()), BigInteger.valueOf(1000)
                        ))
                        .build()
                )
                .utxos(utxos(txHash, transactionOutputs()))
                .invalid(true)
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
