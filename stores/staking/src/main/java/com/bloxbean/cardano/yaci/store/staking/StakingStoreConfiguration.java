package com.bloxbean.cardano.yaci.store.staking;

import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.PoolStorageImpl;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.StakeRegistrationStorageImpl;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.DelegationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.PoolRegistrationRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.PoolRetirementRepository;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.repository.StakeRegistrationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(StakingStoreProperties.class)
public class StakingStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StakingStorage stakingStorage(StakeRegistrationRepository registrationRepository,
                                         DelegationRepository delegationRepository,
                                         StakingMapper stakingMapper) {
        return new StakeRegistrationStorageImpl(registrationRepository, delegationRepository, stakingMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoolStorage poolStorage(PoolRegistrationRepository registrationRepository, PoolRetirementRepository retirementRepository,
                                   PoolMapper poolMapper) {
        return new PoolStorageImpl(registrationRepository, retirementRepository, poolMapper);
    }
}
