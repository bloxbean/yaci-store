package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.core.model.byron.*;
import com.bloxbean.cardano.yaci.core.model.byron.payload.ByronTxPayload;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxWitnessType;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ByronTransactionProcessorTest {
    @Mock
    private TransactionStorage transactionStorage;

    @Mock
    private TransactionWitnessStorage transactionWitnessStorage;

    @InjectMocks
    private ByronTransactionProcessor byronTransactionProcessor;

    @Captor
    private ArgumentCaptor<List<Txn>> txnTransactionArgCaptor;

    @Captor
    private ArgumentCaptor<List<TxnWitness>> txnWitnessesArgCaptor;

    @Test
    void givenByronMainBlockEvent_WhenTxNotExists_ShouldNotSaveAnything() {
        ByronMainBlockEvent event = ByronMainBlockEvent.builder()
                .byronMainBlock(ByronMainBlock.builder()
                        .body(ByronBlockBody.builder()
                                .txPayload(List.of())
                                .build())
                        .build())
                .metadata(eventMetadata())
                .build();

        byronTransactionProcessor.handleByronTransactionEvent(event);

        Mockito.verify(transactionStorage, Mockito.never()).saveAll(any());
    }

    @Test
    void givenByronMainBlockEvent_shouldSaveTxnList() {
        List<ByronTxIn> inputs = byronTxInputs();

        List<ByronTxOut> outputs
                = byronTxOutputs();

        List<ByronTxPayload> txPayloads = List.of(ByronTxPayload.builder()
                .transaction(ByronTx.builder()
                        .inputs(inputs)
                        .outputs(outputs)
                        .txHash("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e")
                        .build())
                .build());

        ByronMainBlockEvent event = ByronMainBlockEvent.builder()
                .byronMainBlock(ByronMainBlock.builder()
                        .body(ByronBlockBody.builder()
                                .txPayload(txPayloads)
                                .build())
                        .build())
                .metadata(eventMetadata())
                .build();

        byronTransactionProcessor.handleByronTransactionEvent(event);

        Mockito.verify(transactionStorage, Mockito.times(1)).saveAll(txnTransactionArgCaptor.capture());

        List<Txn> txnList = txnTransactionArgCaptor.getValue();
        assertThat(txnList).hasSize(1);

        assertThat(txnList.get(0).getTxHash()).isEqualTo("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e");

        assertThat(txnList.get(0).getInputs()).hasSize(1);
        assertThat(txnList.get(0).getInputs().get(0).getTxHash()).isEqualTo(byronTxInputs().get(0).getTxId());
        assertThat(txnList.get(0).getInputs().get(0).getOutputIndex()).isEqualTo(byronTxInputs().get(0).getIndex());

        assertThat(txnList.get(0).getOutputs()).hasSize(2);
        assertThat(txnList.get(0).getOutputs())
                .filteredOn(utxoKey -> utxoKey.getTxHash().equals("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e"))
                .hasSize(2);

        assertThat(txnList.get(0).getBlockHash()).isEqualTo(eventMetadata().getBlockHash());
        assertThat(txnList.get(0).getSlot()).isEqualTo(eventMetadata().getSlot());
        assertThat(txnList.get(0).getBlockNumber()).isEqualTo(eventMetadata().getBlock());
        assertThat(txnList.get(0).getBlockTime()).isEqualTo(eventMetadata().getBlockTime());
    }

    @Test
    void givenByronMainBlockEvent_shouldSaveTxnWitnessList() {
        List<ByronTxIn> inputs = byronTxInputs();

        List<ByronTxOut> outputs
                = byronTxOutputs();

        List<ByronTxWitnesses> witnesses = List.of(
                byronTxWitness(ByronPkWitness.TYPE),
                byronTxWitness(ByronRedeemWitness.TYPE),
                byronTxWitness(ByronScriptWitness.TYPE),
                byronTxWitness(ByronUnknownWitness.TYPE));

        List<ByronTxPayload> txPayloads = List.of(ByronTxPayload.builder()
                .transaction(ByronTx.builder()
                        .inputs(inputs)
                        .outputs(outputs)
                        .txHash("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e")
                        .build())
                .witnesses(witnesses)
                .build());

        ByronMainBlockEvent event = ByronMainBlockEvent.builder()
                .byronMainBlock(ByronMainBlock.builder()
                        .body(ByronBlockBody.builder()
                                .txPayload(txPayloads)
                                .build())
                        .build())
                .metadata(eventMetadata())
                .build();

        byronTransactionProcessor.handleByronTransactionWitnesses(event);

        Mockito.verify(transactionWitnessStorage).saveAll(txnWitnessesArgCaptor.capture());

        List<TxnWitness> txnWitnesses = txnWitnessesArgCaptor.getValue();

        assertThat(txnWitnesses).hasSize(4);

        assertThat(txnWitnesses.stream().map(TxnWitness::getType)).contains(
                TxWitnessType.BYRON_PK_WITNESS,
                TxWitnessType.BYRON_SCRIPT_WITNESS,
                TxWitnessType.BYRON_REDEEM_WITNESS,
                TxWitnessType.BYRON_UNKNOWN_WITNESS
        );

        for (var txnWitness : txnWitnesses) {
            assertThat(txnWitness.getTxHash()).isEqualTo("6497b33b10fa2619c6efbd9f874ecd1c91badb10bf70850732aab45b90524d9e");
            assertThat(txnWitness.getSlot()).isEqualTo(eventMetadata().getSlot());

            if (txnWitness.getType().equals(TxWitnessType.BYRON_PK_WITNESS)) {
                assertThat(txnWitness.getPubKey()).isEqualTo("58bddacf1154e7e58164ff1e72268283771299b65b4a92ffdf64b94aafe8dd9e");
                assertThat(txnWitness.getSignature()).isEqualTo("47d4697affdb6774ac96ae6e86e8b0a2db10c4a4eb24f326e2ddf990923e54f1d43faa3dfc6fbe1247d1d9b85b3b622307a211357ed6c7b0b59cff5286b1b30a");
            }

            if (txnWitness.getType().equals(TxWitnessType.BYRON_REDEEM_WITNESS)) {
                assertThat(txnWitness.getPubKey()).isEqualTo("8c0bdedfbbab26a1308300512ffb1b220f068ee13f7612afb076c22de3fb7641");
                assertThat(txnWitness.getSignature()).isEqualTo("6cc41635a9794234966629ccfa2a5b089a20ae392f0e92154ff97eda30ff7a082a65fc4b362c24cf58c27f30103b1f1345e15479cf4b80cd4134c0f9dca83109");
            }
        }
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .blockHash("cf80534e520fa8f4bde1ed2f623553b8a6a9fd616d73bf9d4f7d6d1687685248")
                .block(3314)
                .blockTime(1506269351)
                .slot(3313)
                .build();
    }

    private List<ByronTxIn> byronTxInputs() {
        return List.of(ByronTxIn.builder()
                .txId("a12a839c25a01fa5d118167db5acdbd9e38172ae8f00e5ac0a4997ef792a2007")
                .index(0)
                .build());
    }

    private List<ByronTxOut> byronTxOutputs() {
        return List.of(ByronTxOut.builder()
                        .address(ByronAddress.builder()
                                .base58Raw("DdzFFzCqrhsszHTvbjTmYje5hehGbadkT6WgWbaqCy5XNxNttsPNF13eAjjBHYT7JaLJz2XVxiucam1EvwBRPSTiCrT4TNCBas4hfzic")
                                .addressId("6c9982e7f2b6dcc5eaa880e8014568913c8868d9f0f86eb687b2633c")
                                .addressAttr(ByronAddressAttr.builder()
                                        .pkDerivationPath("581e581c010d876783fb2b4d0d17c86df29af8d35356ed3d1827bf4744f06700")
                                        .stakeDistribution(null)
                                        .build())
                                .addressType("PubKey")
                                .build())
                        .amount(BigInteger.valueOf(1000000))
                        .build(),
                ByronTxOut.builder()
                        .address(ByronAddress.builder()
                                .base58Raw("DdzFFzCqrhsqz23SkTxevzJ3Dn4ee14BpQVe5T9LX2yWJpcjHToP2qxnzaEiy5qiHwNVtX5ANXtLJyBwKz8PvjJZYq2n8fyy7Dp9RqXa")
                                .addressId("5d4704fc22524e98ea5b9580ab2a29396b8ad2a92764d08ce23ea1e5")
                                .addressAttr(ByronAddressAttr.builder()
                                        .pkDerivationPath("581e581cd2c9d85d9e2ce454557363216e45b9f015e9b5c2617f0294ac5bc2d0")
                                        .stakeDistribution(null)
                                        .build())
                                .addressType("PubKey")
                                .build())
                        .amount(BigInteger.valueOf(1000000))
                        .build()
        );
    }

    private ByronTxWitnesses byronTxWitness(String type) {
        return switch (type) {
            case ByronPkWitness.TYPE -> ByronPkWitness.builder()
                    .publicKey("58bddacf1154e7e58164ff1e72268283771299b65b4a92ffdf64b94aafe8dd9e")
                    .signature("47d4697affdb6774ac96ae6e86e8b0a2db10c4a4eb24f326e2ddf990923e54f1d43faa3dfc6fbe1247d1d9b85b3b622307a211357ed6c7b0b59cff5286b1b30a")
                    .build();
            case ByronRedeemWitness.TYPE -> ByronRedeemWitness.builder()
                    .redeemPublicKey("8c0bdedfbbab26a1308300512ffb1b220f068ee13f7612afb076c22de3fb7641")
                    .redeemSignature("6cc41635a9794234966629ccfa2a5b089a20ae392f0e92154ff97eda30ff7a082a65fc4b362c24cf58c27f30103b1f1345e15479cf4b80cd4134c0f9dca83109")
                    .build();
            case ByronScriptWitness.TYPE -> ByronScriptWitness.builder()
                    .redeemer(ByronScript.builder().build())
                    .validator(ByronScript.builder().build())
                    .build();
            case ByronUnknownWitness.TYPE -> ByronUnknownWitness.builder()
                    .data("data")
                    .build();
            default -> throw new IllegalArgumentException("Invalid type");
        };
    }
}
