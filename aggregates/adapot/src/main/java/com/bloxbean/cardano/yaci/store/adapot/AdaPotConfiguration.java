package com.bloxbean.cardano.yaci.store.adapot;

import com.bloxbean.cardano.yaci.store.adapot.storage.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.adapot",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EnableTransactionManagement
@EnableScheduling
public class AdaPotConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AdaPotStorage adaPotStorage(AdaPotRepository adaPotRepository, AdaPotMapper adaPotMapper) {
        return new AdaPotStorageImpl(adaPotRepository, adaPotMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardStorage rewardStorage(RewardRepository repository, Mapper rewardMapper) {
        return new RewardStorageImpl(repository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardStorageReader rewardStorageReader(RewardRepository repository, Mapper rewardMapper) {
        return new RewardStorageReaderImpl(repository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardAccountStorage rewardAccountStorage(RewardAccountRepository rewardAccountRepository, Mapper rewardMapper) {
        return new RewardAccountStorageImpl(rewardAccountRepository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public WithdrawalStorage withdrawalStorage(WithdrawalRepository withdrawalRepository, Mapper rewardMapper) {
        return new WithdrawalStorageImpl(withdrawalRepository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochStakeStorage epochStakeStorage(EpochStakeRepository epochStakeRepository, Mapper epochStakeMapper) {
        return new EpochStakeStorageImpl(epochStakeRepository, epochStakeMapper);
    }

}
