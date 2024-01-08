package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.GovActionProposalRepository;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.VotingProcedureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class GovernanceRollbackProcessorIT {

    @Autowired
    private VotingProcedureRepository votingProcedureRepository;

    @Autowired
    private GovActionProposalRepository govActionProposalRepository;

    @Autowired
    private GovernanceRollbackProcessor governanceRollbackProcessor;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/voting_procedure_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteVotingProcedures() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(14559346, "dffd1848e8ef26aadb1d4d05612825596ab697b27d2ea422fec077dd0de93254"))
                .currentPoint(new Point(15000000, "5ca2e98fe743c4dc92b323a6cd244825e663aa1e35fd3123487c8c0a170196e2"))
                .build();

        governanceRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = votingProcedureRepository.findAll().size();
        assertThat(count).isEqualTo(7);
    }

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/gov_action_proposal_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEvent_shouldDeleteGovActionProposals() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(14458412, "dffd1848e8ef26aadb1d4d05612825596ab697b27d2ea422fec077dd0de93254"))
                .currentPoint(new Point(15000000, "5ca2e98fe743c4dc92b323a6cd244825e663aa1e35fd3123487c8c0a170196e2"))
                .build();

        governanceRollbackProcessor.handleRollbackEvent(rollbackEvent);

        int count = govActionProposalRepository.findAll().size();
        assertThat(count).isEqualTo(8);
    }
}
