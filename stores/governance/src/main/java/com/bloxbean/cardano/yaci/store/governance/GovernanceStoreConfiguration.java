package com.bloxbean.cardano.yaci.store.governance;

import com.bloxbean.cardano.yaci.store.governance.storage.GovActionProposalStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.VotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.GovActionProposalStorageImpl;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.VotingProcedureStorageImpl;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.GovActionProposalMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.VotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.GovActionProposalRepository;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.VotingProcedureRepository;
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
        prefix = "store.governance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.governance"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.governance"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.governance"})
@EnableTransactionManagement
public class GovernanceStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GovActionProposalStorage governanceActionStorage(GovActionProposalRepository govActionProposalRepository,
                                                            GovActionProposalMapper govActionProposalMapper) {
        return new GovActionProposalStorageImpl(govActionProposalRepository, govActionProposalMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public VotingProcedureStorage votingProposalStorage(VotingProcedureRepository votingProcedureRepository,
                                                        VotingProcedureMapper votingProcedureMapper) {
        return new VotingProcedureStorageImpl(votingProcedureRepository, votingProcedureMapper);
    }
}
