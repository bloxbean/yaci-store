package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRegistrationEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.event.StakeRegDeregEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.event.StakingDepositEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.model.PoolEntity;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Blocks are delivered at least once: after a restart the store processes blocks again from its cursor. Processing a
 * pool registration again must store the same row as the first time.
 */
@SpringBootTest
@RecordApplicationEvents
class PoolStatusProcessorReplayIT {
    private static final String POOL = "7cc911f04229b39ac3760d9458e4bfe32c62a7898f8421175ee191dc";
    private static final String POOL_B = "b0b911f04229b39ac3760d9458e4bfe32c62a7898f8421175ee191dc";
    private static final String POOL_C = "c0c911f04229b39ac3760d9458e4bfe32c62a7898f8421175ee191dc";
    private static final BigInteger DEPOSIT = BigInteger.valueOf(500_000_000);

    @Autowired
    private PoolStatusProcessor processor;

    @Autowired
    private PoolStatusRepository repository;

    @Autowired
    private PoolStorage poolStorage;

    @MockBean
    private DepositParamService depositParamService;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(depositParamService.getPoolDeposit(anyInt())).thenReturn(DEPOSIT);
    }

    @Test
    void replayingTheFirstRegistrationKeepsItARegistration() {
        processor.handlePoolRegistration(registration(62368918L, 148, "tx1", 0, 0));
        var first = rows();

        processor.handlePoolRegistration(registration(62368918L, 148, "tx1", 0, 0));

        var row = rows().get(0);
        assertThat(rows()).hasSize(1);
        assertThat(row.getStatus()).isEqualTo(PoolStatusType.REGISTRATION);
        assertThat(row.getAmount()).isEqualTo(DEPOSIT);
        assertThat(row.getActiveEpoch()).isEqualTo(150);
        assertThat(row.getRegistrationSlot()).isEqualTo(62368918L);
        assertThat(rows()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateDateTime")
                .isEqualTo(first);
    }

    @Test
    void replayingTheFirstRegistrationAfterLaterCertificatesWereStoredKeepsAllRows() {
        processor.handlePoolRegistration(registration(62368918L, 148, "tx1", 0, 0));
        processor.handlePoolRegistration(registration(64580884L, 153, "tx2", 1, 0));
        var first = rows();

        // A restart replays both blocks
        processor.handlePoolRegistration(registration(62368918L, 148, "tx1", 0, 0));
        processor.handlePoolRegistration(registration(64580884L, 153, "tx2", 1, 0));

        var after = rows();
        assertThat(after).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateDateTime")
                .isEqualTo(first);
        assertThat(after.get(0).getStatus()).isEqualTo(PoolStatusType.REGISTRATION);
        assertThat(after.get(1).getStatus()).isEqualTo(PoolStatusType.UPDATE);
        assertThat(after.get(1).getAmount()).isEqualTo(BigInteger.ZERO);
        assertThat(after.get(1).getActiveEpoch()).isEqualTo(156);
        assertThat(after.get(1).getRegistrationSlot()).isEqualTo(62368918L);
    }

    @Test
    void anUpdateInTheSameBlockAsTheRegistrationIsStillAnUpdate() {
        var event = registration(62368918L, 148, "tx1", 0, 0);
        event.getPoolRegistrations().add(cert("tx2", 1, 0, 62368918L));

        processor.handlePoolRegistration(event);

        var after = rows();
        assertThat(after).extracting(PoolEntity::getStatus)
                .containsExactly(PoolStatusType.REGISTRATION, PoolStatusType.UPDATE);
        assertThat(after.get(1).getRegistrationSlot()).isEqualTo(62368918L);
    }

    @Test
    void replayingARegistrationAndAnUpdateInTheSameTransactionKeepsTheirOrder() {
        var event = registration(62368918L, 148, "tx1", 0, 0);
        event.getPoolRegistrations().add(cert("tx1", 0, 1, 62368918L));
        var firstAnswer = processor.handlePoolRegistration(event);
        var first = rows();

        var event2 = registration(62368918L, 148, "tx1", 0, 0);
        event2.getPoolRegistrations().add(cert("tx1", 0, 1, 62368918L));
        var replayAnswer = processor.handlePoolRegistration(event2);

        assertThat(firstAnswer).extracting(Pool::getStatus)
                .containsExactly(PoolStatusType.REGISTRATION, PoolStatusType.UPDATE);
        // The REGISTRATION count published in StakingDepositEvent comes from this list
        assertThat(replayAnswer).extracting(Pool::getStatus)
                .containsExactly(PoolStatusType.REGISTRATION, PoolStatusType.UPDATE);
        assertThat(rows()).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateDateTime")
                .isEqualTo(first);
    }

    @Test
    void replayingAReRegistrationAfterTheRetirementKeepsItARegistration() {
        processor.handlePoolRegistration(registration(10_000L, 10, "tx1", 0, 0));
        // Written at the first block of the retire epoch
        poolStorage.save(List.of(Pool.builder()
                .poolId(POOL).txHash("tx-retire").txIndex(0).certIndex(0)
                .status(PoolStatusType.RETIRED).amount(DEPOSIT)
                .epoch(12).retireEpoch(12).registrationSlot(10_000L)
                .slot(12_000L).blockNumber(120L).blockHash("block-12000").blockTime(12_000L)
                .build()));
        processor.handlePoolRegistration(registration(13_000L, 13, "tx2", 0, 0));
        var first = rows();

        // A restart replays everything from the first registration
        processor.handlePoolRegistration(registration(10_000L, 10, "tx1", 0, 0));
        var replayAnswer = processor.handlePoolRegistration(registration(13_000L, 13, "tx2", 0, 0));

        assertThat(replayAnswer).extracting(Pool::getStatus).containsExactly(PoolStatusType.REGISTRATION);
        var after = rows();
        assertThat(after).usingRecursiveFieldByFieldElementComparatorIgnoringFields("updateDateTime")
                .isEqualTo(first);
        assertThat(after).extracting(PoolEntity::getStatus)
                .containsExactly(PoolStatusType.REGISTRATION, PoolStatusType.RETIRED, PoolStatusType.REGISTRATION);
        assertThat(after.get(2).getRegistrationSlot()).isEqualTo(13_000L);
        assertThat(after.get(2).getAmount()).isEqualTo(DEPOSIT);
    }

    @Test
    void aBlockProcessedAgainBeforePreCommitIsCountedOnce() {
        processor.processPoolRegistrationEvent(registration(62368918L, 148, "tx1", 0, 0));
        // A later listener failed, so there was no pre-commit. The block is processed again.
        processor.processPoolRegistrationEvent(registration(62368918L, 148, "tx1", 0, 0));
        processor.handleCommitEventToProcessPoolStatus(preCommit(62368918L, 148));

        assertThat(depositEvent().getStakePoolRegistrationCount()).isEqualTo(1);
        assertThat(rows()).extracting(PoolEntity::getStatus).containsExactly(PoolStatusType.REGISTRATION);
    }

    @Test
    void aRestartRollbackDropsCachedEventsOfTheFailedBatch() {
        // A batch fails before pre-commit
        processor.processStakeKeyRegDeregs(stakeRegistration(62368918L, "tx0"));
        processor.processPoolRegistrationEvent(registration(62368918L, 148, "tx1", 0, 0));
        processor.processPoolRegistrationEvent(registration(POOL_B, 62368950L, 148, "tx2", 0, 0));

        // The restart rolls back to the cursor. The chain it then follows no longer has the block of pool B.
        processor.handleRollback(rollbackTo(62368900L));
        processor.processPoolRegistrationEvent(registration(62368918L, 148, "tx1", 0, 0));
        processor.processPoolRegistrationEvent(registration(POOL_C, 62368960L, 148, "tx3", 0, 0));
        processor.handleCommitEventToProcessPoolStatus(preCommit(62368960L, 148));

        var deposit = depositEvent();
        assertThat(deposit.getStakePoolRegistrationCount()).isEqualTo(2);
        assertThat(deposit.getStakeKeyRegistrationCount()).isZero();
        assertThat(rows()).extracting(PoolEntity::getPoolId).containsExactly(POOL, POOL_C);
    }

    private StakingDepositEvent depositEvent() {
        var published = events.stream(StakingDepositEvent.class).toList();
        assertThat(published).hasSize(1);
        return published.get(0);
    }

    private static PreCommitEvent preCommit(long slot, int epoch) {
        return new PreCommitEvent(EventMetadata.builder().slot(slot).epochNumber(epoch).blockHash("block-" + slot).build());
    }

    private static RollbackEvent rollbackTo(long slot) {
        return RollbackEvent.builder().rollbackTo(new Point(slot, "block-" + slot)).build();
    }

    private static StakeRegDeregEvent stakeRegistration(long slot, String txHash) {
        var metadata = EventMetadata.builder().slot(slot).blockHash("block-" + slot).build();
        var detail = StakeRegistrationDetail.builder()
                .txHash(txHash).type(CertificateType.STAKE_REGISTRATION).slot(slot).build();
        return new StakeRegDeregEvent(metadata, List.of(detail));
    }

    private List<PoolEntity> rows() {
        var all = new ArrayList<>(repository.findAll());
        all.sort(Comparator.comparing(PoolEntity::getSlot).thenComparing(PoolEntity::getTxIndex)
                .thenComparing(PoolEntity::getCertIndex));
        return all;
    }

    private static PoolRegistrationEvent registration(long slot, int epoch, String txHash, int txIndex, int certIndex) {
        return registration(POOL, slot, epoch, txHash, txIndex, certIndex);
    }

    private static PoolRegistrationEvent registration(String poolId, long slot, int epoch, String txHash, int txIndex,
                                                      int certIndex) {
        var metadata = EventMetadata.builder()
                .slot(slot)
                .epochNumber(epoch)
                .block(slot / 100)
                .blockHash("block-" + slot)
                .blockTime(slot)
                .build();
        var certs = new ArrayList<PoolRegistration>();
        certs.add(cert(poolId, txHash, txIndex, certIndex, slot));
        return new PoolRegistrationEvent(metadata, certs);
    }

    private static PoolRegistration cert(String txHash, int txIndex, int certIndex, long slot) {
        return cert(POOL, txHash, txIndex, certIndex, slot);
    }

    private static PoolRegistration cert(String poolId, String txHash, int txIndex, int certIndex, long slot) {
        return PoolRegistration.builder()
                .poolId(poolId)
                .txHash(txHash)
                .txIndex(txIndex)
                .certIndex(certIndex)
                .slot(slot)
                .build();
    }
}
