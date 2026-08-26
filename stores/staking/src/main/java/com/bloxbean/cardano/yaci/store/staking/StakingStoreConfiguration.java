package com.bloxbean.cardano.yaci.store.staking;

import com.bloxbean.cardano.yaci.store.staking.storage.*;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.*;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.PoolMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.mapper.StakingMapper;
import com.bloxbean.cardano.yaci.store.staking.storage.impl.repository.*;
import org.jooq.DSLContext;
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
    public final static String STORE_STAKING_ENABLED = "store.staking.enabled";

    @Bean
    @ConditionalOnMissingBean
    public StakingCertificateStorage stakingStorage(StakeRegistrationRepository registrationRepository,
                                                    DelegationRepository delegationRepository,
                                                    StakingMapper stakingMapper) {
        return new StakeCertificateStorageImpl(registrationRepository, delegationRepository, stakingMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoolCertificateStorage poolCertificateStorage(PoolRegistrationRepository registrationRepository, PoolRetirementRepository retirementRepository,
                                              PoolMapper poolMapper) {
        return new PoolCertificateStorageImpl(registrationRepository, retirementRepository, poolMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public StakingCertificateStorageReader stakingStorageReader(StakeRegistrationRepository registrationRepository,
                                                                DelegationRepository delegationRepository,
                                                                StakingMapper stakingMapper) {
        return new StakeCertificateStorageReaderImpl(registrationRepository, delegationRepository, stakingMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoolCertificateStorageReader poolCertificateStorageReader(PoolRegistrationRepository registrationRepository, PoolRetirementRepository retirementRepository,
                                                          PoolMapper poolMapper) {
        return new PoolCertificateStorageReaderImpl(registrationRepository, retirementRepository, poolMapper);
    }


    @Bean
    @ConditionalOnMissingBean
    public PoolStorage poolStorage(PoolStatusRepository depositRepository, PoolMapper poolMapper) {
        return new PoolStorageImpl(depositRepository, poolMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public PoolStorageReader poolStorageReader(DSLContext dslContext) {
        return new PoolStorageReaderImpl(dslContext);
    }
}
