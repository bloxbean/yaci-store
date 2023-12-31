package com.bloxbean.cardano.yaci.store.extensions.utxo.rocksdb;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedNoFinderUtxoStorageTest extends RocksDBBaseTest {
    String address1 = "addr_test1xrk9x99ey79w25d3eyqd7zy0qmecl3h4ngqvy5w2yp06nxyhzcvnyv2r9xr86rm4f7dragz4ckudkrs06nylfrwllzcs63c0dy";
    String address2 = "addr_test1wr64gtafm8rpkndue4ck2nx95u4flhwf643l2qmg9emjajg2ww0nj";
    String paymentCred1 = HexUtil.encodeHexString(new Address(address1).getPaymentCredential().get().getBytes());
    String paymentCred2 = HexUtil.encodeHexString(new Address(address2).getPaymentCredential().get().getBytes());


    @Test
    void saveUnspent() {
        UtxoCache utxoCache = new UtxoCache();
        var utxoStorage = new RocksDBUtxoStorage(rocksDBConfig, utxoCache);
        var utxoStorageReader = new RocksDBUtxoStorageReader(rocksDBConfig);

        var utxos = getUtxos();
        utxoStorage.saveUnspent(getUtxos());
        utxoStorage.handleCommitEvent(new CommitEvent(null, null));

        var addrUtxos1 = utxoStorageReader.findUtxoByAddress(address1, 0, 100, Order.asc);
        var addrUtxos2 = utxoStorageReader.findUtxoByAddress(address2, 0, 100, Order.asc);

        assertThat(addrUtxos1).hasSize(3);
        assertThat(addrUtxos1).contains(utxos.get(0), utxos.get(1), utxos.get(2));

        assertThat(addrUtxos2).hasSize(1);
        assertThat(addrUtxos2).contains(utxos.get(3));
    }

    @Test
    void saveSpent() {
        UtxoCache utxoCache = new UtxoCache();
        var utxoStorage = new RocksDBUtxoStorage(rocksDBConfig, utxoCache);
        var utxoStorageReader = new RocksDBUtxoStorageReader(rocksDBConfig);

        var utxos = getUtxos();
        utxoStorage.saveUnspent(getUtxos());
        utxoStorage.handleCommitEvent(new CommitEvent(null, null));

        var spentUtxo1 = TxInput.builder()
                .txHash(utxos.get(0).getTxHash())
                .outputIndex(utxos.get(0).getOutputIndex())
                .spentAtBlock(6000L)
                .spentAtSlot(6000L)
                .spentTxHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .spentAtBlockHash("2f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .build();

        var spentUtxo2 = TxInput.builder()
                .txHash(utxos.get(3).getTxHash())
                .outputIndex(utxos.get(3).getOutputIndex())
                .spentAtBlock(6000L)
                .spentAtSlot(6000L)
                .spentTxHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .spentAtBlockHash("2f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .build();

        utxoStorage.saveSpent(List.of(spentUtxo1, spentUtxo2));
        utxoStorage.handleCommitEvent(new CommitEvent(null, null));

        var addrUtxos1 = utxoStorageReader.findUtxoByAddress(address1, 0, 100, Order.asc);
        var addrUtxos2 = utxoStorageReader.findUtxoByAddress(address2, 0, 100, Order.asc);

        var addrUtxosByPaymentCred = utxoStorageReader.findUtxoByPaymentCredential(paymentCred1, 0, 100, Order.asc);

        assertThat(addrUtxos1).hasSize(2);
        assertThat(addrUtxos1).contains(utxos.get(1), utxos.get(2));

        assertThat(addrUtxos2).hasSize(0);

        assertThat(addrUtxosByPaymentCred).hasSize(2);
        assertThat(addrUtxosByPaymentCred).contains(utxos.get(1), utxos.get(2));
    }

    @Test
    void  delete() {
        UtxoCache utxoCache = new UtxoCache();
        var utxoStorage = new RocksDBUtxoStorage(rocksDBConfig, utxoCache);
        var utxoStorageReader = new RocksDBUtxoStorageReader(rocksDBConfig);

        var utxos = getUtxos();
        utxoStorage.saveUnspent(getUtxos());
        utxoStorage.handleCommitEvent(new CommitEvent(null, null));

        utxoStorage.deleteUnspentBySlotGreaterThan(2000L);

        var saveUtxo = utxoStorage.findById(utxos.get(3).getTxHash(), utxos.get(3).getOutputIndex());
        assertThat(saveUtxo).isNotPresent();

        var addrUtxos1 = utxoStorageReader.findUtxoByAddress(address1, 0, 100, Order.asc);
        assertThat(addrUtxos1).hasSize(2);
        assertThat(addrUtxos1).contains(utxos.get(0), utxos.get(1));

    }

    private List<AddressUtxo> getUtxos() {
        var addressUtxo1 = AddressUtxo
                .builder()
                .txHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .outputIndex(0)
                .ownerAddr(address1)
                .ownerStakeAddr(null)
                .ownerPaymentCredential(paymentCred1)
                .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(1000000L)).build()))
                .blockHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .blockNumber(1000L)
                .slot(1000L)
                .build();

        var addressUtxo2 = AddressUtxo
                .builder()
                .txHash("2f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .outputIndex(1)
                .ownerAddr(address1)
                .ownerStakeAddr(null)
                .ownerPaymentCredential(paymentCred1)
                .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(2000000L)).build()))
                .blockHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .blockNumber(1000L)
                .slot(2000L)
                .build();

        var addressUtxo3 = AddressUtxo
                .builder()
                .txHash("3f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .outputIndex(5)
                .ownerAddr(address1)
                .ownerStakeAddr(null)
                .ownerPaymentCredential(paymentCred1)
                .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(3000000L)).build()))
                .blockHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .blockNumber(1000L)
                .slot(3000L)
                .build();

        var addressUtxo4 = AddressUtxo
                .builder()
                .txHash("ff50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .outputIndex(1)
                .ownerAddr(address2)
                .ownerStakeAddr(null)
                .ownerPaymentCredential(paymentCred2)
                .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(9000000L)).build()))
                .blockHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                .blockNumber(1000L)
                .slot(5000L)
                .build();

        return List.of(addressUtxo1, addressUtxo2, addressUtxo3, addressUtxo4);
    }

    @Test
    void findByAddressTest() {
        UtxoCache utxoCache = new UtxoCache();
        var utxoStorage = new RocksDBUtxoStorage(rocksDBConfig, utxoCache);
        var utxoStorageReader = new RocksDBUtxoStorageReader(rocksDBConfig);

        var adderssUtxoList = new ArrayList<AddressUtxo>();
        for (int i = 0; i < 10000; i++) {
            var addressUtxo = AddressUtxo
                    .builder()
                    .txHash(HexUtil.encodeHexString(Blake2bUtil.blake2bHash256((i +"").getBytes())))
                    .outputIndex(i)
                    .ownerAddr(address1)
                    .ownerStakeAddr(null)
                    .ownerPaymentCredential(paymentCred1)
                    .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(i)).build()))
                    .blockHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                    .blockNumber(10L * i)
                    .slot(2L * i)
                    .build();
            adderssUtxoList.add(addressUtxo);
        }

        utxoStorage.saveUnspent(adderssUtxoList);
        utxoStorage.handleCommitEvent(new CommitEvent(null, null));

        var list1 = utxoStorageReader.findUtxoByAddress(address1, 0, 100, Order.asc);
        var list2 = utxoStorageReader.findUtxoByAddress(address1, 5, 200, Order.asc);

        assertThat(list1).hasSize(100);
        assertThat(list2).hasSize(200);
        assertThat(list1.get(0).getOutputIndex()).isEqualTo(0);
        assertThat(list1.get(list1.size() - 1).getOutputIndex()).isEqualTo(99);

        assertThat(list2.get(0).getOutputIndex()).isEqualTo(1000);
        assertThat(list2.get(list2.size() - 1).getOutputIndex()).isEqualTo(1199);

    }

    @Test
    void findByPaymentCredTest() {
        UtxoCache utxoCache = new UtxoCache();
        var utxoStorage = new RocksDBUtxoStorage(rocksDBConfig, utxoCache);
        var utxoStorageReader = new RocksDBUtxoStorageReader(rocksDBConfig);

        var adderssUtxoList = new ArrayList<AddressUtxo>();
        for (int i = 0; i < 10000; i++) {
            var addressUtxo = AddressUtxo
                    .builder()
                    .txHash(HexUtil.encodeHexString(Blake2bUtil.blake2bHash256((i +"").getBytes())))
                    .outputIndex(i)
                    .ownerAddr(address1)
                    .ownerStakeAddr(null)
                    .ownerPaymentCredential(paymentCred1)
                    .amounts(List.of(Amt.builder().unit("lovelace").quantity(BigInteger.valueOf(i)).build()))
                    .blockHash("1f50e930ade51be0a99672d07c4ae17d66c9da4568293723a084371be4b88a9b")
                    .blockNumber(10L * i)
                    .slot(2L * i)
                    .build();
            adderssUtxoList.add(addressUtxo);
        }

        utxoStorage.saveUnspent(adderssUtxoList);
        utxoStorage.handleCommitEvent(new CommitEvent(null, null));

        var list1 = utxoStorageReader.findUtxoByPaymentCredential(paymentCred1, 0, 100, Order.asc);
        var list2 = utxoStorageReader.findUtxoByPaymentCredential(paymentCred1, 5, 200, Order.asc);

        assertThat(list1).hasSize(100);
        assertThat(list2).hasSize(200);
        assertThat(list1.get(0).getOutputIndex()).isEqualTo(0);
        assertThat(list1.get(list1.size() - 1).getOutputIndex()).isEqualTo(99);

        assertThat(list2.get(0).getOutputIndex()).isEqualTo(1000);
        assertThat(list2.get(list2.size() - 1).getOutputIndex()).isEqualTo(1199);

    }

}
