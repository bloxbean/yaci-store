package com.bloxbean.cardano.yaci.store.script.processor;

//import com.bloxbean.cardano.client.transaction.spec.Transaction;

import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.helper.model.Utxo;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.script.domain.DatumEvent;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;

import java.math.BigInteger;
import java.util.*;


import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class OutputDatumProcessorTest {
    @Mock
    private DatumStorage datumStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private OutputDatumProcessor outputDatumProcessor;

    @Captor
    private ArgumentCaptor<Collection<Datum>> outputDatumArgCaptor;

    @Captor
    private ArgumentCaptor<DatumEvent> datumEventArgCaptor;

    @Test
    void givenTransactionEvent_whenTransactionIsInvalidTrue_shouldReturn() {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(Transaction.builder()
                .invalid(true)
                .build());

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(transactions)
                .build();

        outputDatumProcessor.handleOutputDatumInTransaction(transactionEvent);
        Mockito.verify(datumStorage, Mockito.never()).saveAll(Mockito.any());
    }

    @Test
    void givenTransactionEvent_whenTransactionIsInvalidFalse_shouldSaveDatumList() {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(Transaction.builder()
                        .auxData(null)
                        .txHash("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625")
                        .blockNumber(47)
                        .body(mockTransactionBody())
                        .invalid(false)
                        .witnesses(mockWitnesses())
                        .slot(86420)
                        .utxos(mockUtxos())
                .build());

        TransactionEvent transactionEvent = TransactionEvent.builder()
                .transactions(transactions)
                .metadata(EventMetadata.builder()
                        .mainnet(false)
                        .protocolMagic(1)
                        .era(Era.Shelley)
                        .slotLeader("d15422b2e8b60e500a82a8f4ceaa98b04e55a0171d1125f6c58f8758")
                        .epochNumber(4)
                        .block(47)
                        .blockHash("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665")
                        .blockTime(1655769620)
                        .prevBlockHash("c971bfb21d2732457f9febf79d9b02b20b9a3bef12c561a78b818bcb8b35a574")
                        .slot(86420)
                        .epochSlot(20)
                        .noOfTxs(1)
                        .syncMode(false)
                        .parallelMode(true)
                        .remotePublish(false)
                        .build())
                .build();

        outputDatumProcessor.handleOutputDatumInTransaction(transactionEvent);

        Mockito.verify(datumStorage, Mockito.times(1)).saveAll(outputDatumArgCaptor.capture());
        Mockito.verify(publisher).publishEvent(datumEventArgCaptor.capture());

        Collection<Datum> datumCollection = outputDatumArgCaptor.getValue();
        List<Datum> datumList = new ArrayList<>(datumCollection);

        assertThat(datumList.get(0).getDatum()).isEqualTo("679a55b523ff8d61942b2583b76e5d49498468164802ef1ebe513c685d6fb5c2");
        assertThat(datumList.get(0).getHash()).isEqualTo("15bf4a5efaf35d00d116f911226b2fb26217c456afce0d346062e325870bfe3c");
        assertThat(datumList.get(0).getCreatedAtTx()).isEqualTo("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625");

        DatumEvent datumEvent = datumEventArgCaptor.getValue();

        assertThat(datumEvent.getOutputDatums().get(0).getDatum().getDatum()).isEqualTo("679a55b523ff8d61942b2583b76e5d49498468164802ef1ebe513c685d6fb5c2");
        assertThat(datumEvent.getOutputDatums().get(0).getDatum().getHash()).isEqualTo("15bf4a5efaf35d00d116f911226b2fb26217c456afce0d346062e325870bfe3c");
        assertThat(datumEvent.getOutputDatums().get(0).getDatum().getCreatedAtTx()).isEqualTo("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625");
        assertThat(datumEvent.getOutputDatums().get(0).getOutputAddress()).isEqualTo("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        assertThat(datumEvent.getOutputDatums().get(0).getTxHash()).isEqualTo("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625");
        assertThat(datumEvent.getOutputDatums().get(0).getOutputIndex()).isEqualTo(0);
        assertThat(datumEvent.getEventMetadata().isMainnet()).isEqualTo(false);
        assertThat(datumEvent.getEventMetadata().getProtocolMagic()).isEqualTo(1);
        assertThat(datumEvent.getEventMetadata().getEra()).isEqualTo(Era.Shelley);
        assertThat(datumEvent.getEventMetadata().getSlotLeader()).isEqualTo("d15422b2e8b60e500a82a8f4ceaa98b04e55a0171d1125f6c58f8758");
        assertThat(datumEvent.getEventMetadata().getEpochNumber()).isEqualTo(4);
        assertThat(datumEvent.getEventMetadata().getBlock()).isEqualTo(47);
        assertThat(datumEvent.getEventMetadata().getBlockHash()).isEqualTo("664b6ec8a708b9cf90b87c904e688477887b55cbf4ee6c36877166a2ef216665");
        assertThat(datumEvent.getEventMetadata().getBlockTime()).isEqualTo(1655769620);
        assertThat(datumEvent.getEventMetadata().getPrevBlockHash()).isEqualTo("c971bfb21d2732457f9febf79d9b02b20b9a3bef12c561a78b818bcb8b35a574");
        assertThat(datumEvent.getEventMetadata().getSlot()).isEqualTo(86420);
        assertThat(datumEvent.getEventMetadata().getEpochSlot()).isEqualTo(20);
        assertThat(datumEvent.getEventMetadata().getNoOfTxs()).isEqualTo(1);
        assertThat(datumEvent.getEventMetadata().isSyncMode()).isEqualTo(false);
        assertThat(datumEvent.getEventMetadata().isRemotePublish()).isEqualTo(false);
        assertThat(datumEvent.getEventMetadata().isParallelMode()).isEqualTo(true);
        assertThat(datumEvent.getWitnessDatums().size()).isEqualTo(0);
    }

    private List<Utxo> mockUtxos() {
        List<Utxo> utxos = new ArrayList<>();

        List<Amount> amountList = new ArrayList<>();
        amountList.add(Amount.builder()
                .assetName("lovelace")
                .quantity(new BigInteger("29999999999800000"))
                .unit("lovelace")
                .build());

        utxos.add(Utxo.builder()
                        .txHash("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625")
                        .index(0)
                        .address("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                        .amounts(null)
                .build());

        return utxos;
    }

    private TransactionBody mockTransactionBody() {
        List<TransactionOutput> transactionOutputList = new ArrayList<>();
        Set<TransactionInput> transactionInputList = new HashSet<>();
        List<Amount> amountList = new ArrayList<>();
        amountList.add(Amount.builder()
                .assetName("lovelace")
                .quantity(new BigInteger("29999999999800000"))
                .unit("lovelace")
                .build());

        transactionOutputList.add(TransactionOutput.builder()
                        .datumHash("15bf4a5efaf35d00d116f911226b2fb26217c456afce0d346062e325870bfe3c")
                        .address("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket")
                        .inlineDatum("679a55b523ff8d61942b2583b76e5d49498468164802ef1ebe513c685d6fb5c2")
                        .amounts(amountList)
                .build());

        transactionInputList.add(TransactionInput.builder()
                        .transactionId("5526b1373acfc774794a62122f95583ff17febb2ca8a0fe948d097e29cf99099")
                        .index(0)
                .build());

        return TransactionBody.builder()
                .txHash("b75ec46c406113372efeb1e57d9880856c240c9b531e3c680c1c4d8bf2253625")
                .inputs(transactionInputList)
                .outputs(transactionOutputList)
                .fee(BigInteger.valueOf(200000))
                .ttl(90000)
                .certificates(new ArrayList<>())
                .validityIntervalStart(0)
                .mint(new ArrayList<>())
                .netowrkId(0)
                .build();
    }

    private Witnesses mockWitnesses() {
        List<BootstrapWitness> bootstrapWitnessList = new ArrayList<>();

        bootstrapWitnessList.add(BootstrapWitness.builder()
                        .attributes("a1024101")
                        .chainCode("cd37d942b4eb28d5435df4d4e18bc7ed11388fe2ea43615f544f7c09c7b29f25")
                        .publicKey("7229838c0d5c5baaf0f09138853a52c7e7088bda6aa497af5606e5de5120ef99")
                        .signature("a6ace79954aeec7630c4c9376110a1c710d11af30eb056394aee03968251845d6fa1406c23f753b359068082d47bcf985fd14bf21feb8067bcf2234726578d0f")
                .build());

        return Witnesses.builder()
                .datums(new ArrayList<>())
                .bootstrapWitnesses(bootstrapWitnessList)
                .nativeScripts(new ArrayList<>())
                .plutusV1Scripts(new ArrayList<>())
                .plutusV2Scripts(new ArrayList<>())
                .plutusV3Scripts(new ArrayList<>())
                .redeemers(new ArrayList<>())
                .vkeyWitnesses(new ArrayList<>())
                .build();
    }
}
