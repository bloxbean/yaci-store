package com.bloxbean.cardano.yaci.store.governance;

import com.bloxbean.cardano.yaci.store.governance.storage.*;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.*;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.*;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "store.governance",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
public class LocalGovernanceStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LocalGovActionProposalStatusStorage localGovActionProposalStatusStorage(LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository,
                                                                                   LocalGovActionProposalStatusMapper localGovActionProposalStatusMapper) {
        return new LocalGovActionProposalStatusStorageImpl(localGovActionProposalStatusRepository, localGovActionProposalStatusMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalGovActionProposalStatusStorageReader localGovActionProposalStatusStorageReader(LocalGovActionProposalStatusRepository localGovActionProposalStatusRepository,
                                                                                               LocalGovActionProposalStatusMapper localGovActionProposalStatusMapper) {
        return new LocalGovActionProposalStatusStorageReaderImpl(localGovActionProposalStatusRepository, localGovActionProposalStatusMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalCommitteeMemberStorage localCommitteeMemberStorage(LocalCommitteeMemberRepository localCommitteeMemberRepository,
                                                                   LocalCommitteeMemberMapper localCommitteeMemberMapper) {
        return new LocalCommitteeMemberStorageImpl(localCommitteeMemberRepository, localCommitteeMemberMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalConstitutionStorage localConstitutionStorage(LocalConstitutionRepository localConstitutionRepository,
                                                             LocalConstitutionMapper localConstitutionMapper) {
        return new LocalConstitutionStorageImpl(localConstitutionRepository, localConstitutionMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalCommitteeStorage localCommitteeStorage(LocalCommitteeRepository localCommitteeRepository,
                                                       LocalCommitteeMapper localCommitteeMapper) {
        return new LocalCommitteeStorageImpl(localCommitteeRepository, localCommitteeMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalTreasuryWithdrawalStorage localTreasuryWithdrawalStorage(LocalTreasuryWithdrawalRepository localTreasuryWithdrawalRepository,
                                                                         LocalTreasuryWithdrawalMapper localTreasuryWithdrawalMapper) {
        return new LocalTreasuryWithdrawalStorageImpl(localTreasuryWithdrawalRepository, localTreasuryWithdrawalMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalHardForkInitiationStorage localHardForkInitiationStorage(LocalHardForkInitiationRepository localHardForkInitiationRepository,
                                                                         LocalHardForkInitiationMapper localHardForkInitiationMapper) {
        return new LocalHardForkInitiationStorageImpl(localHardForkInitiationRepository, localHardForkInitiationMapper);
    }
}
