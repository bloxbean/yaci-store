package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRetirementRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.StakeRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class StakingRollbackProcessorIT {

    @Autowired
    private PoolRegistrationRepository poolRegistrationRepository;

    @Autowired
    private PoolRetirementRepository poolRetirementRepository;

    @Autowired
    private StakeRegistrationRepository stakeRegistrationRepository;

    @Autowired
    private DelegationRepository delegationRepository;

    @Autowired
    private PoolRegistrationProcessor poolRegistrationProcessor;

    @Autowired
    private StakeRegProcessor stakeRegProcessor;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/pool_registration_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeletePoolRegistrations() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10711808, "ee6917d74aaefad30070da203b32b1775cd6ad25783821c87c1e949d43c456a6"))
                .currentPoint(new Point(10848675, "4be9c649b57f5d437bfd6b54dd147de78828391373c1780cf38a10e7f4215879"))
                .build();

        poolRegistrationProcessor.handleRollbackEvent(rollbackEvent);

        int count = poolRegistrationRepository.findAll().size();
        assertThat(count).isEqualTo(26);
    }

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/pool_retirement_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeletePoolRetirements() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(18285013, "5e08df745784247f6e8c1154e3891f08c89387a21e4741d45a69c21ed694800c"))
                .currentPoint(new Point(31685620, "0b92a42028c7bdbb708ded1dd96d2811003a052353dae89d2f954b7ba9ea71b7"))
                .build();

        poolRegistrationProcessor.handleRollbackEvent(rollbackEvent);

        int count = poolRetirementRepository.findAll().size();
        assertThat(count).isEqualTo(13);
    }

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/stake_registration_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteStakeRegistration() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10646961, "ff77b3d3f22f96b68d06d8b08cc302267a72830144325f0073de4867a596b122"))
                .currentPoint(new Point(10654196, "488683c140954e06458ff9209b3ae16b761fbb2ab97ab05749288983c8284e81"))
                .build();

        stakeRegProcessor.handleRollbackEvent(rollbackEvent);

        int count = stakeRegistrationRepository.findAll().size();
        assertThat(count).isEqualTo(15);
    }

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/delegation_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteDelegations() {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(10678235, "f1afbaa3f96956f35062b0765da9cfb94fbf04155a88245a70edd57009490058"))
                .currentPoint(new Point(10711808, "ee6917d74aaefad30070da203b32b1775cd6ad25783821c87c1e949d43c456a6"))
                .build();

        stakeRegProcessor.handleRollbackEvent(rollbackEvent);

        int count = delegationRepository.findAll().size();
        assertThat(count).isEqualTo(31);
    }
}
