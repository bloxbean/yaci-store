package com.bloxbean.cardano.yaci.store.utxo.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.UtxoStoreProperties;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.AmtRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UtxoRollbackProcessorIT {

    @Autowired
    private UtxoRollbackProcessor utxoRollbackProcessor;

    @Autowired
    private UtxoRepository utxoRepository;

    @Autowired
    private AmtRepository amtRepository;

    @Autowired
    private UtxoStoreProperties utxoStoreProperties;

    @Autowired
    private UtxoStorage utxoStorage;

    @Test
    void givenRollbackEvent_shouldDeleteAddressUtxos() throws Exception {
        AddressUtxo utxo1 = AddressUtxo.builder()
                .txHash("143844cb3e41c41d6c1e2aec22b83bf909d3ea286eb2bc8c806c8b99b11774c7")
                .outputIndex(0)
                .ownerAddr("addr_test1wz6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeAddr("stake_test1wz6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeCredential("wz6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .lovelaceAmount(adaToLovelace(1000))
                .amounts(List.of(Amt.builder()
                        .unit("lovelace")
                        .quantity(BigInteger.valueOf(1000))
                        .build(),
                        Amt.builder()
                                .unit("0254a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649")
                                .quantity(BigInteger.valueOf(2))
                                .build()
                )).slot(2000L)
                .blockNumber(200L)
                .blockTime(1600098L)
                .build();

        AddressUtxo utxo2 = AddressUtxo.builder()
                .txHash("243844cb3e41c41d6c1e2aec22b83bf909d3ea286eb2bc8c806c8b99b11774c7")
                .outputIndex(2)
                .ownerAddr("addr_test1pq6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeAddr("stake_test1pq6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeCredential("pq6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .lovelaceAmount(adaToLovelace(3))
                .amounts(List.of(Amt.builder()
                                .unit("lovelace")
                                .quantity(BigInteger.valueOf(2))
                                .build(),
                        Amt.builder()
                                .unit("9954a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649")
                                .quantity(BigInteger.valueOf(333))
                                .build(),
                        Amt.builder()
                                .unit("8854a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649")
                                .quantity(BigInteger.valueOf(2))
                                .build()
                )).slot(3000L)
                .blockNumber(300L)
                .blockTime(1600098L)
                .build();

        AddressUtxo utxo3 = AddressUtxo.builder()
                .txHash("343844cb3e41c41d6c1e2aec22b83bf909d3ea286eb2bc8c806c8b99b11774c7")
                .outputIndex(0)
                .ownerAddr("addr_test1wz6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeAddr("stake_test1wz6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeCredential("wz6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .lovelaceAmount(adaToLovelace(8))
                .amounts(List.of(Amt.builder()
                                .unit("lovelace")
                                .quantity(BigInteger.valueOf(8))
                                .build()
                )).slot(4000L)
                .blockNumber(400L)
                .blockTime(1600098L)
                .build();

        AddressUtxo utxo4 = AddressUtxo.builder()
                .txHash("443844cb3e41c41d6c1e2aec22b83bf909d3ea286eb2bc8c806c8b99b11774c7")
                .outputIndex(45)
                .ownerAddr("addr_test1lm6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeAddr("stake_test1lm6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .ownerStakeCredential("lm6zjuut6mx93dw8jvksqx4zh5zul6j8qg992myvw575gdsgwxjuc")
                .lovelaceAmount(adaToLovelace(9000))
                .amounts(List.of(Amt.builder()
                                .unit("lovelace")
                                .quantity(BigInteger.valueOf(9000))
                                .build(),
                        Amt.builder()
                                .unit("1454a6ffa78edb03ea8933dbd4ca078758dbfc0fc6bb0d28b7a9c89f4c454e4649")
                                .quantity(BigInteger.valueOf(56666666))
                                .build()
                )).slot(5000L)
                .blockNumber(500L)
                .blockTime(1600098L)
                .build();

        var utxos = List.of(utxo1, utxo2, utxo3, utxo4);

        //Save utxos
        utxoStorage.saveUnspent(utxos);

        var utxoKeys = utxos.stream()
                        .map(utxo -> new UtxoKey(utxo.getTxHash(), utxo.getOutputIndex()))
                        .toList();

        var savedUtxos = utxoStorage.findAllByIds(utxoKeys);
        var saveAmts = amtRepository.findAll();

        //Assert saved data
        assertThat(savedUtxos.size()).isEqualTo(4);
        assertThat(savedUtxos.get(0).getAmounts().size() > 0);
        assertThat(saveAmts.size()).isEqualTo(8);

        //Rollback
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(3000, "925347abf637eb2d436349b78589bb257e396c0a4cc133236b76e56ffebc57bb"))
                .currentPoint(new Point(9000, "64069e4f2351d25a572189c0df03f2c9e0a1200b9fe897cc5fb74106ed6ed6ad"))
                .build();

        utxoRollbackProcessor.handleRollbackEvent(rollbackEvent);

        var allUtxos = utxoRepository.findAll();
        var allAmts = amtRepository.findAll();

        assertThat(allUtxos).hasSize(2);
        assertThat(allAmts).hasSize(5);

        assertThat(allUtxos.get(0).getTxHash()).isEqualTo(utxo1.getTxHash());
        assertThat(allUtxos.get(0).getOutputIndex()).isEqualTo(utxo1.getOutputIndex());
        assertThat(allUtxos.get(0).getSlot()).isEqualTo(utxo1.getSlot());
        assertThat(allUtxos.get(0).getBlockNumber()).isEqualTo(utxo1.getBlockNumber());
        assertThat(allUtxos.get(0).getAmounts()).hasSize(utxo1.getAmounts().size());
        assertThat(allUtxos.get(0).getAmounts().stream().map(amtEntity -> Amt.builder()
                .unit(amtEntity.getUnit())
                .quantity(amtEntity.getQuantity())
                .build()
        ).toList()).contains(utxo1.getAmounts().toArray(new Amt[0]));

        assertThat(allUtxos.get(1).getTxHash()).isEqualTo(utxo2.getTxHash());
        assertThat(allUtxos.get(1).getOutputIndex()).isEqualTo(utxo2.getOutputIndex());
        assertThat(allUtxos.get(1).getSlot()).isEqualTo(utxo2.getSlot());
        assertThat(allUtxos.get(1).getBlockNumber()).isEqualTo(utxo2.getBlockNumber());
        assertThat(allUtxos.get(1).getAmounts()).hasSize(utxo2.getAmounts().size());
        assertThat(allUtxos.get(1).getAmounts().stream().map(amtEntity -> Amt.builder()
                .unit(amtEntity.getUnit())
                .quantity(amtEntity.getQuantity())
                .build()
        ).toList()).contains(utxo2.getAmounts().toArray(new Amt[0]));

    }

}
