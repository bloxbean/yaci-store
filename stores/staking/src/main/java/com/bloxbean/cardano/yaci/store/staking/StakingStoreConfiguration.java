package com.bloxbean.cardano.yaci.store.staking;

import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorageReader;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.PoolStorageImpl;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.PoolStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.StakeStorageImpl;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.StakeStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.PoolRetirementRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.StakeRegistrationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.staking",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.staking"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.staking"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.staking"})
@EnableTransactionManagement
public class StakingStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StakingStorage stakingStorage(StakeRegistrationRepository registrationRepository,
                                         DelegationRepository delegationRepository,
                                         StakingMapper stakingMapper) {
        return new StakeStorageImpl(registrationRepository, delegationRepository, stakingMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoolStorage poolStorage(PoolRegistrationRepository registrationRepository, PoolRetirementRepository retirementRepository,
                                   PoolMapper poolMapper) {
        return new PoolStorageImpl(registrationRepository, retirementRepository, poolMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public StakingStorageReader stakingStorageReader(StakeRegistrationRepository registrationRepository,
                                                     DelegationRepository delegationRepository,
                                                     StakingMapper stakingMapper) {
        return new StakeStorageReaderImpl(registrationRepository, delegationRepository, stakingMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoolStorageReader poolStorageReader(PoolRegistrationRepository registrationRepository, PoolRetirementRepository retirementRepository,
                                               PoolMapper poolMapper) {
        return new PoolStorageReaderImpl(registrationRepository, retirementRepository, poolMapper);
    }
}
