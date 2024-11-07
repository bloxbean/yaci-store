package com.bloxbean.cardano.yaci.store.governanceaggr;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.*;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.*;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.CommitteeVoteMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.DRepMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.CommitteeVoteRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.DRepRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.LatestVotingProcedureRepository;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.governance-aggr",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
public class GovernanceAggrConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LatestVotingProcedureStorage latestVotingProposalStorage(LatestVotingProcedureRepository latestVotingProcedureRepository,
                                                                    LatestVotingProcedureMapper latestVotingProcedureMapper,
                                                                    DSLContext dsl) {
        return new LatestVotingProcedureStorageImpl(latestVotingProcedureRepository, latestVotingProcedureMapper, dsl);
    }

    @Bean
    @ConditionalOnMissingBean
    public LatestVotingProcedureStorageReader latestVotingProposalStorageReader(LatestVotingProcedureRepository latestVotingProcedureRepository,
                                                                                LatestVotingProcedureMapper latestVotingProcedureMapper) {
        return new LatestVotingProcedureStorageReaderImpl(latestVotingProcedureRepository, latestVotingProcedureMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommitteeVoteStorageReader committeeVotesStorageReader(CommitteeVoteRepository committeeVoteRepository,
                                                                  CommitteeVoteMapper committeeVoteMapper,
                                                                  DSLContext dsl) {
        return new CommitteeVoteStorageReaderImpl(committeeVoteRepository, committeeVoteMapper, dsl);
    }

    @Bean
    @ConditionalOnMissingBean
    public CommitteeVoteStorage committeeVotesStorage(CommitteeVoteRepository committeeVoteRepository,
                                                      CommitteeVoteMapper committeeVoteMapper) {
        return new CommitteeVoteStorageImpl(committeeVoteRepository, committeeVoteMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DRepStorage dRepStorage(DRepRepository dRepRepository, DRepMapper dRepMapper, DSLContext dslContext) {
        return new DRepStorageImpl(dRepRepository, dRepMapper, dslContext);
    }
}
