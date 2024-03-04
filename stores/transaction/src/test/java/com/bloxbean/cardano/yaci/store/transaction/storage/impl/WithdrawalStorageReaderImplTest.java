package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapperImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.stream.IntStream;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class WithdrawalStorageReaderImplTest {

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    private TxnMapper mapper;

    private WithdrawalStorageReader withdrawalStorageReader;

    private WithdrawalStorage withdrawalStorage;

    @BeforeEach
    void setUp() {
        mapper = new TxnMapperImpl();
        withdrawalStorageReader = new WithdrawalStorageReaderImpl(withdrawalRepository, mapper);
        withdrawalStorage = new WithdrawalStorageImpl(withdrawalRepository, mapper);
    }

    @Test
    void getWithdrawals() {
        List<Withdrawal> withdrawals = getTestWithdrwals();
        withdrawalStorage.save(withdrawals);

        List<Withdrawal> withdrawalsFromDb = withdrawalStorageReader.getWithdrawals(3, 10, Order.desc);
        assertThat(withdrawalsFromDb).isNotNull();
        assertThat(withdrawalsFromDb).hasSize(10);
        assertThat(withdrawalsFromDb.get(0).getSlot()).isEqualTo(1069L);
        assertThat(withdrawalsFromDb.get(9).getSlot()).isEqualTo(1060L);
    }

    @Test
    void getWithdrawalsByTxHash() {
        List<Withdrawal> withdrawals = getTestWithdrwals();
        withdrawalStorage.save(withdrawals);

        List<Withdrawal> withdrawalsFromDb = withdrawalStorageReader.getWithdrawalsByTxHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de031050");
        assertThat(withdrawalsFromDb).isNotNull();
        assertThat(withdrawalsFromDb.get(0).getSlot()).isEqualTo(1050L);
        assertThat(withdrawalsFromDb.get(0).getAddress()).isEqualTo("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq901050");
    }

    @Test
    void getWithdrawalsByTxHash_notExists() {
        List<Withdrawal> withdrawals = getTestWithdrwals();
        withdrawalStorage.save(withdrawals);

        List<Withdrawal> withdrawalsFromDb = withdrawalStorageReader.getWithdrawalsByTxHash("gha6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de031050");
        assertThat(withdrawalsFromDb).isNotNull();
        assertThat(withdrawalsFromDb).isEmpty();
    }

    @Test
    void getWithdrawalsByAddress() {
        List<Withdrawal> withdrawals = getTestWithdrwalsWithTwoAddresses();
        withdrawalStorage.save(withdrawals);

        List<Withdrawal> withdrawalsFromDb = withdrawalStorageReader.getWithdrawalsByAddress("stake1ggsxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq901050", 0, 10, Order.desc);
        assertThat(withdrawalsFromDb).isNotNull();
        assertThat(withdrawalsFromDb).hasSize(10);
        assertThat(withdrawalsFromDb).allMatch(w -> w.getAddress().equals("stake1ggsxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq901050"));
    }

    @Test
    void getWithdrawalsByAddress_notExists() {
        List<Withdrawal> withdrawals = getTestWithdrwalsWithTwoAddresses();
        withdrawalStorage.save(withdrawals);

        List<Withdrawal> withdrawalsFromDb = withdrawalStorageReader.getWithdrawalsByAddress("stake1hhsxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq901050", 0, 10, Order.desc);
        assertThat(withdrawalsFromDb).isNotNull();
        assertThat(withdrawalsFromDb).isEmpty();
    }

    private List<Withdrawal> getTestWithdrwals() {
        return (List<Withdrawal>) IntStream.range(1000, 1100)
                .mapToObj(i -> Withdrawal.builder()
                        .txHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de03" + i)
                        .address("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq90" + i)
                        .amount(adaToLovelace(1000))
                        .slot((long)i)
                        .epoch(30)
                        .blockNumber(200L + i)
                        .blockTime(123456789L)
                        .build()).toList();

    }

    private List<Withdrawal> getTestWithdrwalsWithTwoAddresses() {
        return (List<Withdrawal>) IntStream.range(1000, 1100)
                .mapToObj(i -> Withdrawal.builder()
                        .txHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de03" + i)
                        .address(i % 2 == 0? "stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq9056" : "stake1ggsxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq901050")
                        .amount(adaToLovelace(1000))
                        .slot((long)i)
                        .epoch(30)
                        .blockNumber(200L + i)
                        .blockTime(123456789L)
                        .build()).toList();

    }
}
