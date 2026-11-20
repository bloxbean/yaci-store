package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRegistrationEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
class PoolStatusProcessorIT {
    private static final String POOL_ID = "4f57b82ff7024b1712ff9b02d1bf8df1ef23db589d94fd2be0b61810";
    private static final int RETIRE_EPOCH = 103;
    private static final long FIRST_SLOT_OF_RETIRE_EPOCH = 1_000_000L;

    @Autowired
    private PoolStatusProcessor processor;

    @Autowired
    private PoolStorage poolStorage;

    @Autowired
    private PoolStatusRepository repository;

    @MockBean
    private DepositParamService depositParamService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(depositParamService.getPoolDeposit(anyInt())).thenReturn(BigInteger.valueOf(500_000_000L));
    }

    @Test
    void aPoolRegisteredAgainInTheFirstBlockOfItsRetireEpochIsActive() {
        poolStorage.save(List.of(
                row(PoolStatusType.REGISTRATION, "tx1", 0, 100, 500_000L),
                row(PoolStatusType.RETIRING, "tx2", 9, 101, 600_000L)));

        // First block of the retire epoch: the retirement takes effect, then tx 0 registers the pool again
        var firstBlock = metadata(RETIRE_EPOCH, FIRST_SLOT_OF_RETIRE_EPOCH);
        processor.handlePoolRetirementsDuringEpochChange(PreEpochTransitionEvent.builder()
                .metadata(firstBlock)
                .previousEpoch(RETIRE_EPOCH - 1)
                .epoch(RETIRE_EPOCH)
                .build());
        processor.processPoolRegistrationEvent(new PoolRegistrationEvent(firstBlock, new ArrayList<>(List.of(
                PoolRegistration.builder().poolId(POOL_ID).txHash("tx3").txIndex(0).certIndex(0).build()))));
        processor.handleCommitEventToProcessPoolStatus(new PreCommitEvent(firstBlock));

        assertThat(repository.findAll())
                .filteredOn(p -> p.getSlot() == FIRST_SLOT_OF_RETIRE_EPOCH)
                .extracting(PoolEntity::getStatus, PoolEntity::getTxIndex)
                .containsExactlyInAnyOrder(
                        tuple(PoolStatusType.RETIRED, -1),
                        tuple(PoolStatusType.REGISTRATION, 0));
        assertThat(poolStorage.findActivePools(RETIRE_EPOCH))
                .extracting(Pool::getPoolId)
                .containsExactly(POOL_ID);
    }

    private Pool row(PoolStatusType status, String txHash, int txIndex, int epoch, long slot) {
        return Pool.builder()
                .poolId(POOL_ID)
                .txHash(txHash)
                .txIndex(txIndex)
                .certIndex(0)
                .status(status)
                .amount(BigInteger.ZERO)
                .epoch(epoch)
                .retireEpoch(status == PoolStatusType.RETIRING ? RETIRE_EPOCH : null)
                .registrationSlot(500_000L)
                .slot(slot)
                .blockHash("block-" + slot)
                .build();
    }

    private EventMetadata metadata(int epoch, long slot) {
        return EventMetadata.builder()
                .epochNumber(epoch)
                .slot(slot)
                .block(slot)
                .blockHash("block-" + slot)
                .build();
    }
}
