package com.bloxbean.cardano.yaci.store.adapot;

import com.bloxbean.cardano.yaci.store.adapot.job.storage.AdaPotJobStorage;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobMapper;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobRepository;
import com.bloxbean.cardano.yaci.store.adapot.job.storage.impl.AdaPotJobStorageImpl;
import com.bloxbean.cardano.yaci.store.adapot.storage.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.*;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.AdaPotMapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.*;
import org.jooq.DSLContext;
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
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.adapot"})
@EnableTransactionManagement
@EnableScheduling
public class AdaPotConfiguration {
    public final static String STORE_ADAPOT_ENABLED = "store.adapot.enabled";

    @Bean
    @ConditionalOnMissingBean
    public AdaPotStorage adaPotStorage(AdaPotRepository adaPotRepository, AdaPotMapper adaPotMapper) {
        return new AdaPotStorageImpl(adaPotRepository, adaPotMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardStorage rewardStorage(InstantRewardRepository instantRewardRepository,
                                       RewardRestRepository rewardRestRepository,
                                       RewardRepository rewardRepository,
                                       UnclaimedRewardRestRepository unclaimedRewardRestRepository,
                                       Mapper rewardMapper, DSLContext dslContext) {
        return new RewardStorageImpl(instantRewardRepository, rewardRestRepository, rewardRepository, unclaimedRewardRestRepository, rewardMapper, dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public RewardStorageReader rewardStorageReader(InstantRewardRepository instantRewardRepository, RewardRepository rewardRepository,
                                                   RewardRestRepository rewardRestRepository,
                                                   UnclaimedRewardRestRepository unclaimedRewardRestRepository,
                                                   Mapper rewardMapper) {
        return new RewardStorageReaderImpl(instantRewardRepository, rewardRepository, rewardRestRepository, unclaimedRewardRestRepository, rewardMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochStakeStorageReader epochStakeStorage(EpochStakeRepository epochStakeRepository, Mapper epochStakeMapper) {
        return new EpochStakeStorageReaderImpl(epochStakeRepository, epochStakeMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AdaPotJobStorage adaPotJobStorage(AdaPotJobRepository adaPotJobRepository, AdaPotJobMapper rewardCalcJobMapper) {
        return new AdaPotJobStorageImpl(adaPotJobRepository, rewardCalcJobMapper);
    }

}
