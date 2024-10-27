package com.bloxbean.cardano.yaci.store.adapot;

import com.bloxbean.cardano.yaci.store.adapot.reward.storage.RewardCalcJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl.RewardCalcJobMapper;
import com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl.RewardCalcJobRepository;
import com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl.RewardCalcJobStorageImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.AdaPotRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.EpochStakeRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
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
    public RewardStorage rewardStorage(InstantRewardRepository instantRewardRepository, RewardRepository rewardRepository, Mapper rewardMapper) {
        return new RewardStorageImpl(instantRewardRepository, rewardRepository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardStorageReader rewardStorageReader(InstantRewardRepository instantRewardRepository, RewardRepository rewardRepository, Mapper rewardMapper) {
        return new RewardStorageReaderImpl(instantRewardRepository, rewardRepository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochStakeStorage epochStakeStorage(EpochStakeRepository epochStakeRepository, Mapper epochStakeMapper) {
        return new EpochStakeStorageImpl(epochStakeRepository, epochStakeMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardCalcJobStorage rewardCalcJobStorage(RewardCalcJobRepository rewardCalcJobRepository, RewardCalcJobMapper rewardCalcJobMapper) {
        return new RewardCalcJobStorageImpl(rewardCalcJobRepository, rewardCalcJobMapper);
    }

}
