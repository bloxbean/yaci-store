package com.bloxbean.cardano.yaci.store.governanceaggr;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.CommitteeVoteStorageImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.CommitteeVoteStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.LatestVotingProcedureStorageImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.LatestVotingProcedureStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.CommitteeVoteMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.CommitteeVoteRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.LatestVotingProcedureRepository;
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
        prefix = "store.governance-aggr",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableTransactionManagement
@EnableScheduling
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
}
