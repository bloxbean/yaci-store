package com.bloxbean.cardano.yaci.store.transaction.storage.impl;

import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapper;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.mapper.TxnMapperImpl;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.model.WithdrawalEntity;
import com.bloxbean.cardano.yaci.store.transaction.storage.impl.repository.WithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class WithdrawalStorageImplTest {

    @Autowired
    private WithdrawalRepository withdrawalRepository;

    private TxnMapper mapper;

    private WithdrawalStorage withdrawalStorage;

    @BeforeEach
    void setUp() {
        mapper = new TxnMapperImpl();
        withdrawalStorage = new WithdrawalStorageImpl(withdrawalRepository, mapper);
    }

    @Test
    void save() {
        Withdrawal withdrawal1 = Withdrawal.builder()
                .txHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8")
                .address("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq90uxgd")
                .amount(adaToLovelace(1000))
                .slot(1000L)
                .epoch(30)
                .blockNumber(200L)
                .blockTime(123456789L)
                .build();

        Withdrawal withdrawal2 = Withdrawal.builder()
                .txHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8")
                .address("stake1u9q0cgjgpulrnqmqpkef2gpkj4d0svwxv2u2fps30xda2wswap4kk")
                .amount(adaToLovelace(2000))
                .slot(1000L)
                .epoch(30)
                .blockNumber(200L)
                .blockTime(123456789L)
                .build();

        withdrawalStorage.save(List.of(withdrawal1, withdrawal2));

        List<WithdrawalEntity> withdrawalsEntities = withdrawalRepository.findAll();
        var savedWithdrawals = withdrawalsEntities.stream()
                        .map(mapper::toWithdrawal).toList();
        assertNotNull(withdrawalsEntities);
        assertEquals(2, withdrawalsEntities.size());
        assertThat(savedWithdrawals).contains(withdrawal1, withdrawal2);
    }

    @Test
    void deleteBySlotGreaterThan() {
        Withdrawal withdrawal1 = Withdrawal.builder()
                .txHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8")
                .address("stake1uysxxfteap80gkuqz6jepzr8le52dvskf59z63480l77scq90uxgd")
                .amount(adaToLovelace(1000))
                .slot(1000L)
                .epoch(30)
                .blockNumber(200L)
                .blockTime(123456789L)
                .build();

        Withdrawal withdrawal2 = Withdrawal.builder()
                .txHash("f0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8")
                .address("stake1u9q0cgjgpulrnqmqpkef2gpkj4d0svwxv2u2fps30xda2wswap4kk")
                .amount(adaToLovelace(2000))
                .slot(2000L)
                .epoch(30)
                .blockNumber(200L)
                .blockTime(123456789L)
                .build();

        Withdrawal withdrawal3 = Withdrawal.builder()
                .txHash("e0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8")
                .address("stake1u9q0cgjgpulrnqmqpkef2gpkj4d0svwxv2u2fps30xda2wswap4kk")
                .amount(adaToLovelace(2000))
                .slot(3000L)
                .epoch(30)
                .blockNumber(200L)
                .blockTime(123456789L)
                .build();

        Withdrawal withdrawal4 = Withdrawal.builder()
                .txHash("g0a6e529be26c2326c447c39159e05bb904ff1f7900b6df3852dd539de0343e8")
                .address("stake1u9q0cgjgpulrnqmqpkef2gpkj4d0svwxv2u2fps30xda2wswap4kk")
                .amount(adaToLovelace(2000))
                .slot(4000L)
                .epoch(30)
                .blockNumber(200L)
                .blockTime(123456789L)
                .build();

        withdrawalStorage.save(List.of(withdrawal1, withdrawal2, withdrawal3, withdrawal4));

        withdrawalStorage.deleteBySlotGreaterThan(2003L);

        List<WithdrawalEntity> withdrawalsEntities = withdrawalRepository.findAll();
        var savedWithdrawals = withdrawalsEntities.stream()
                .map(mapper::toWithdrawal).toList();
        assertNotNull(withdrawalsEntities);
        assertEquals(2, withdrawalsEntities.size());
        assertThat(savedWithdrawals).contains(withdrawal1, withdrawal2);
    }
}
