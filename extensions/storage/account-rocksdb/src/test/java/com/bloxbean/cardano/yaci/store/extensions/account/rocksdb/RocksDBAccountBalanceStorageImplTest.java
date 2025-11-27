package com.bloxbean.cardano.yaci.store.extensions.account.rocksdb;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RocksDBAccountBalanceStorageImplTest extends RocksDBBaseTest {

    @Test
    void getAddressBalance() {
        var storage = new RocksDBAccountBalanceStorageImpl(rocksDBConfig);

        var addressBal1 = new AddressBalance();
        addressBal1.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal1.setUnit("lovelace");
        addressBal1.setSlot(100000L);
        addressBal1.setQuantity(BigInteger.valueOf(1000000000L));

        var addressBal2 = new AddressBalance();
        addressBal2.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal2.setUnit("lovelace");
        addressBal2.setSlot(200000L);
        addressBal2.setQuantity(BigInteger.valueOf(2000000000L));

        var addressBal3 = new AddressBalance();
        addressBal3.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal3.setUnit("lovelace");
        addressBal3.setSlot(300000L);
        addressBal3.setQuantity(BigInteger.valueOf(3000000000L));

        var addressBal4 = new AddressBalance();
        addressBal4.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal4.setUnit("424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal4.setSlot(200000L);
        addressBal4.setQuantity(BigInteger.valueOf(1200000000L));

        var addressBal5 = new AddressBalance();
        addressBal5.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal5.setUnit("424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal5.setSlot(300000L);
        addressBal5.setQuantity(BigInteger.valueOf(1300000000L));

        var addressBal6 = new AddressBalance();
        addressBal6.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal6.setUnit("524f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal6.setSlot(150000L);
        addressBal6.setQuantity(BigInteger.valueOf(1400000000L));

        var addressBal7 = new AddressBalance();
        addressBal7.setAddress("addr_test1xrk9x99ey79w25d3eyqd7zy0qmecl3h4ngqvy5w2yp06nxyhzcvnyv2r9xr86rm4f7dragz4ckudkrs06nylfrwllzcs63c0dy");
        addressBal7.setUnit("lovelace");
        addressBal7.setSlot(250000L);
        addressBal7.setQuantity(BigInteger.valueOf(1500000000L));

        storage.saveAddressBalances(List.of(addressBal1, addressBal2, addressBal3, addressBal4, addressBal5, addressBal6, addressBal7));
        var addressBalances = storage.getAddressBalance("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");

        var addressBalances2 = storage.getAddressBalance("addr_test1xrk9x99ey79w25d3eyqd7zy0qmecl3h4ngqvy5w2yp06nxyhzcvnyv2r9xr86rm4f7dragz4ckudkrs06nylfrwllzcs63c0dy");

        assertThat(addressBalances).hasSize(3);
        assertThat(addressBalances).contains(addressBal3, addressBal5, addressBal6);

        assertThat(addressBalances2).hasSize(1);
        assertThat(addressBalances2).contains(addressBal7);

    }

    @Test
    void getAddressBalanceByUnit() {
        var storage = new RocksDBAccountBalanceStorageImpl(rocksDBConfig);

        var addressBal1 = new AddressBalance();
        addressBal1.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal1.setUnit("lovelace");
        addressBal1.setSlot(100000L);
        addressBal1.setQuantity(BigInteger.valueOf(1000000000L));

        var addressBal2 = new AddressBalance();
        addressBal2.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal2.setUnit("lovelace");
        addressBal2.setSlot(200000L);
        addressBal2.setQuantity(BigInteger.valueOf(2000000000L));

        var addressBal3 = new AddressBalance();
        addressBal3.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal3.setUnit("lovelace");
        addressBal3.setSlot(300000L);
        addressBal3.setQuantity(BigInteger.valueOf(3000000000L));

        var addressBal4 = new AddressBalance();
        addressBal4.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal4.setUnit("424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal4.setSlot(200000L);
        addressBal4.setQuantity(BigInteger.valueOf(1200000000L));

        var addressBal5 = new AddressBalance();
        addressBal5.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal5.setUnit("424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal5.setSlot(300000L);
        addressBal5.setQuantity(BigInteger.valueOf(1300000000L));

        var addressBal6 = new AddressBalance();
        addressBal6.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal6.setUnit("524f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal6.setSlot(150000L);
        addressBal6.setQuantity(BigInteger.valueOf(1400000000L));

        var addressBal7 = new AddressBalance();
        addressBal7.setAddress("addr_test1xrk9x99ey79w25d3eyqd7zy0qmecl3h4ngqvy5w2yp06nxyhzcvnyv2r9xr86rm4f7dragz4ckudkrs06nylfrwllzcs63c0dy");
        addressBal7.setUnit("lovelace");
        addressBal7.setSlot(250000L);
        addressBal7.setQuantity(BigInteger.valueOf(1500000000L));

        storage.saveAddressBalances(List.of(addressBal1, addressBal2, addressBal3, addressBal4, addressBal5, addressBal6, addressBal7));
        var addressBalances1 = storage.getAddressBalance("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket", "lovelace", Long.MAX_VALUE);
        var addressBalance2 = storage.getAddressBalance("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket", "424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564", Long.MAX_VALUE);

        assertThat(addressBalances1.get()).isEqualTo(addressBal3);
        assertThat(addressBalance2.get()).isEqualTo(addressBal5);
    }

    @Test
    void deleteAddressBalance() {
        var storage = new RocksDBAccountBalanceStorageImpl(rocksDBConfig);

        var addressBal1 = new AddressBalance();
        addressBal1.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal1.setUnit("lovelace");
        addressBal1.setSlot(100000L);
        addressBal1.setQuantity(BigInteger.valueOf(1000000000L));

        var addressBal2 = new AddressBalance();
        addressBal2.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal2.setUnit("lovelace");
        addressBal2.setSlot(200000L);
        addressBal2.setQuantity(BigInteger.valueOf(2000000000L));

        var addressBal3 = new AddressBalance();
        addressBal3.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal3.setUnit("lovelace");
        addressBal3.setSlot(300000L);
        addressBal3.setQuantity(BigInteger.valueOf(3000000000L));

        var addressBal4 = new AddressBalance();
        addressBal4.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal4.setUnit("424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal4.setSlot(200000L);
        addressBal4.setQuantity(BigInteger.valueOf(1200000000L));

        var addressBal5 = new AddressBalance();
        addressBal5.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal5.setUnit("424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal5.setSlot(300000L);
        addressBal5.setQuantity(BigInteger.valueOf(1300000000L));

        var addressBal6 = new AddressBalance();
        addressBal6.setAddress("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket");
        addressBal6.setUnit("524f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564");
        addressBal6.setSlot(150000L);
        addressBal6.setQuantity(BigInteger.valueOf(1400000000L));

        var addressBal7 = new AddressBalance();
        addressBal7.setAddress("addr_test1xrk9x99ey79w25d3eyqd7zy0qmecl3h4ngqvy5w2yp06nxyhzcvnyv2r9xr86rm4f7dragz4ckudkrs06nylfrwllzcs63c0dy");
        addressBal7.setUnit("lovelace");
        addressBal7.setSlot(150000L);
        addressBal7.setQuantity(BigInteger.valueOf(1500000000L));

        var addressBal8 = new AddressBalance();
        addressBal8.setAddress("addr_test1xrk9x99ey79w25d3eyqd7zy0qmecl3h4ngqvy5w2yp06nxyhzcvnyv2r9xr86rm4f7dragz4ckudkrs06nylfrwllzcs63c0dy");
        addressBal8.setUnit("lovelace");
        addressBal8.setSlot(250000L);
        addressBal8.setQuantity(BigInteger.valueOf(2500000000L));

        storage.saveAddressBalances(List.of(addressBal1, addressBal2, addressBal3, addressBal4, addressBal5, addressBal6, addressBal7, addressBal8));

        storage.deleteAddressBalanceBySlotGreaterThan(200000L);

        var address1LovelaceBal = storage.getAddressBalance("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket", "lovelace", Long.MAX_VALUE);
        var address1Token1Bal = storage.getAddressBalance("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket", "424f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564", Long.MAX_VALUE);
        var address1Token2Bal = storage.getAddressBalance("addr_test1vz09v9yfxguvlp0zsnrpa3tdtm7el8xufp3m5lsm7qxzclgmzkket", "524f268a65632944ddfe17967208178082058cbe9044f53aee28697d4f7261636c6546656564", Long.MAX_VALUE);

        assertThat(address1LovelaceBal.get()).isEqualTo(addressBal2);
        assertThat(address1Token1Bal.get()).isEqualTo(addressBal4);
        assertThat(address1Token2Bal.get()).isEqualTo(addressBal6);
    }

    @Test
    void getStakeAddressBalance() {
        var storage = new RocksDBAccountBalanceStorageImpl(rocksDBConfig);

        var addressBal1 = new StakeAddressBalance();
        addressBal1.setAddress("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        addressBal1.setSlot(100000L);
        addressBal1.setQuantity(BigInteger.valueOf(1000000000L));

        var addressBal2 = new StakeAddressBalance();
        addressBal2.setAddress("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        addressBal2.setSlot(200000L);
        addressBal2.setQuantity(BigInteger.valueOf(2000000000L));

        var addressBal3 = new StakeAddressBalance();
        addressBal3.setAddress("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        addressBal3.setSlot(300000L);
        addressBal3.setQuantity(BigInteger.valueOf(3000000000L));

        var addressBal4 = new StakeAddressBalance();
        addressBal4.setAddress("stake_test1uqe3xxpujd2w00xhrq2r73ce4fkgwed6kldt73kstuc6qegg77n4k");
        addressBal4.setSlot(200000L);
        addressBal4.setQuantity(BigInteger.valueOf(1200000000L));

        var addressBal5 = new StakeAddressBalance();
        addressBal5.setAddress("stake_test1uqe3xxpujd2w00xhrq2r73ce4fkgwed6kldt73kstuc6qegg77n4k");
        addressBal5.setSlot(300000L);
        addressBal5.setQuantity(BigInteger.valueOf(1300000000L));

        var addressBal6 = new StakeAddressBalance();
        addressBal6.setAddress("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");
        addressBal6.setSlot(150000L);
        addressBal6.setQuantity(BigInteger.valueOf(1400000000L));

        var addressBal7 = new StakeAddressBalance();
        addressBal7.setAddress("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");
        addressBal7.setSlot(250000L);
        addressBal7.setQuantity(BigInteger.valueOf(1500000000L));

        storage.saveStakeAddressBalances(List.of(addressBal1, addressBal2, addressBal3, addressBal4, addressBal5, addressBal6, addressBal7));

        var addressBalance1 = storage.getStakeAddressBalance("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        var addressBalance2 = storage.getStakeAddressBalance("stake_test1uqe3xxpujd2w00xhrq2r73ce4fkgwed6kldt73kstuc6qegg77n4k");
        var addressBalance3 = storage.getStakeAddressBalance("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");

        assertThat(addressBalance1.get()).isEqualTo(addressBal3);
        assertThat(addressBalance2.get()).isEqualTo(addressBal5);
        assertThat(addressBalance3.get()).isEqualTo(addressBal7);
    }

    @Test
    void deleteStakeAddressBalance() {
        var storage = new RocksDBAccountBalanceStorageImpl(rocksDBConfig);

        var addressBal1 = new StakeAddressBalance();
        addressBal1.setAddress("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        addressBal1.setSlot(100000L);
        addressBal1.setQuantity(BigInteger.valueOf(1000000000L));

        var addressBal2 = new StakeAddressBalance();
        addressBal2.setAddress("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        addressBal2.setSlot(200000L);
        addressBal2.setQuantity(BigInteger.valueOf(2000000000L));

        var addressBal3 = new StakeAddressBalance();
        addressBal3.setAddress("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        addressBal3.setSlot(300000L);
        addressBal3.setQuantity(BigInteger.valueOf(3000000000L));

        var addressBal4 = new StakeAddressBalance();
        addressBal4.setAddress("stake_test1uqe3xxpujd2w00xhrq2r73ce4fkgwed6kldt73kstuc6qegg77n4k");
        addressBal4.setSlot(200000L);
        addressBal4.setQuantity(BigInteger.valueOf(1200000000L));

        var addressBal5 = new StakeAddressBalance();
        addressBal5.setAddress("stake_test1uqe3xxpujd2w00xhrq2r73ce4fkgwed6kldt73kstuc6qegg77n4k");
        addressBal5.setSlot(300000L);
        addressBal5.setQuantity(BigInteger.valueOf(1300000000L));

        var addressBal6 = new StakeAddressBalance();
        addressBal6.setAddress("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");
        addressBal6.setSlot(150000L);
        addressBal6.setQuantity(BigInteger.valueOf(1400000000L));

        var addressBal7 = new StakeAddressBalance();
        addressBal7.setAddress("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");
        addressBal7.setSlot(250000L);
        addressBal7.setQuantity(BigInteger.valueOf(1500000000L));

        storage.saveStakeAddressBalances(List.of(addressBal1, addressBal2, addressBal3, addressBal4, addressBal5, addressBal6, addressBal7));
        storage.deleteStakeAddressBalanceBySlotGreaterThan(200000L);

        var addressBalance1 = storage.getStakeAddressBalance("stake_test17q8snfkq7uhtcaksmwd6mpn85f84su623aagk2peaswvs3shre8vr");
        var addressBalance2 = storage.getStakeAddressBalance("stake_test1uqe3xxpujd2w00xhrq2r73ce4fkgwed6kldt73kstuc6qegg77n4k");
        var addressBalance3 = storage.getStakeAddressBalance("stake_test17rs4wd04fx4ls9rpdelrp9qwnh3w6cexlmgj42h5t0tvv8gjrqqjc");

        assertThat(addressBalance1.get()).isEqualTo(addressBal2);
        assertThat(addressBalance2.get()).isEqualTo(addressBal4);
        assertThat(addressBalance3.get()).isEqualTo(addressBal6);
    }
}
