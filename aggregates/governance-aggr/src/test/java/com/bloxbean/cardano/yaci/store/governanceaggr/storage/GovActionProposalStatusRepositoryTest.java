package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.GovActionProposalStatusEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovActionProposalStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=none"}, showSql = false)
@ContextConfiguration(classes = GovActionProposalStatusRepositoryTest.Config.class)
class GovActionProposalStatusRepositoryTest {
    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = GovActionProposalStatusEntity.class)
    @EnableJpaRepositories(basePackageClasses = GovActionProposalStatusRepository.class)
    static class Config {
    }

    @Autowired
    private GovActionProposalStatusRepository repository;

    @Test
    @Sql(statements = """
            create table gov_action_proposal_status (
                gov_action_tx_hash varchar(64), gov_action_index int, epoch int,
                type varchar(64), status varchar(20), voting_stats json, update_datetime timestamp,
                primary key (gov_action_tx_hash, gov_action_index, epoch));
            """)
    void returnsAllActionsFromLatestRatificationEpochForTheRequestedType() {
        repository.saveAllAndFlush(List.of(
                status("older", 9, GovActionType.PARAMETER_CHANGE_ACTION, GovActionStatus.RATIFIED),
                status("parent", 10, GovActionType.PARAMETER_CHANGE_ACTION, GovActionStatus.RATIFIED),
                status("child", 10, GovActionType.PARAMETER_CHANGE_ACTION, GovActionStatus.RATIFIED),
                status("active", 11, GovActionType.PARAMETER_CHANGE_ACTION, GovActionStatus.ACTIVE),
                status("other-type", 11, GovActionType.NEW_CONSTITUTION, GovActionStatus.RATIFIED),
                status("current", 12, GovActionType.PARAMETER_CHANGE_ACTION, GovActionStatus.RATIFIED),
                status("future", 13, GovActionType.PARAMETER_CHANGE_ACTION, GovActionStatus.RATIFIED)));

        assertThat(repository.findLastEnactedProposals(GovActionType.PARAMETER_CHANGE_ACTION, 12))
                .extracting(GovActionProposalStatusEntity::getGovActionTxHash)
                .containsExactlyInAnyOrder("parent", "child");
        assertThat(repository.findLastEnactedProposals(GovActionType.PARAMETER_CHANGE_ACTION, 9)).isEmpty();
        assertThat(repository.findLastEnactedProposals(GovActionType.HARD_FORK_INITIATION_ACTION, 12)).isEmpty();
    }

    private GovActionProposalStatusEntity status(String hash, int epoch, GovActionType type, GovActionStatus status) {
        return GovActionProposalStatusEntity.builder().govActionTxHash(hash).govActionIndex(0)
                .epoch(epoch).type(type).status(status).build();
    }
}
