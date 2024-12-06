package com.bloxbean.cardano.yaci.store.governanceaggr;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepDistStorageReader;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.DRepDistStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.DRepStorageImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.GovActionProposalStatusStorageImpl;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.DRepDistMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.DRepMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.GovActionProposalStatusMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.DRepDistRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.DRepRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
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
        matchIfMissing = false
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableJpaRepositories(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.governanceaggr"})
@EnableTransactionManagement
@EnableScheduling
public class GovernanceAggrConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DRepStorage dRepStorage(DRepRepository dRepRepository, DRepMapper dRepMapper, DSLContext dslContext) {
        return new DRepStorageImpl(dRepRepository, dRepMapper, dslContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public GovActionProposalStatusStorage govActionProposalStatusStorage(GovActionProposalStatusRepository govActionProposalStatusRepository,
                                                                         GovActionProposalStatusMapper govActionProposalStatusMapper) {
        return new GovActionProposalStatusStorageImpl(govActionProposalStatusRepository, govActionProposalStatusMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DRepDistStorageReader dRepDistStorage(DRepDistRepository dRepDistRepository, DRepDistMapper dRepDistMapper) {
        return new DRepDistStorageReaderImpl(dRepDistRepository, dRepDistMapper);
    }
}
