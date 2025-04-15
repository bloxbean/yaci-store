package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.EpochParamRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.ProtocolParamsProposalRepository;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@SpringBootTest
class EpochRollbackProcessorIT {
    @Autowired
    private EpochParamProcessor epochParamProcessor;

    @Autowired
    private ProtocolParamsUpdateProcessor protocolParamsUpdateProcessor;

    @Autowired
    private EpochParamRepository epochParamRepository;

    @Autowired
    private ProtocolParamsProposalRepository protocolParamsRepository;

    @Autowired
    private EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;

    @MockBean
    private EraService eraService;

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/epoch_param_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEventOfEpochParamProcessor_shouldDeleteEpoch() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(2246440, "dffd1848e8ef26aadb1d4d05612825596ab697b27d2ea422fec077dd0de93254"))
                .currentPoint(new Point(2246440, "dffd1848e8ef26aadb1d4d05612825596ab697b27d2ea422fec077dd0de93254"))
                .build();

        epochParamProcessor.handleRollBack(rollbackEvent);

        int count = epochParamRepository.findAll().size();
        assertThat(count).isEqualTo(2);
    }

    @Test
    @SqlGroup({
            @Sql(value = "classpath:scripts/protocol_params_proposal_data.sql", executionPhase = BEFORE_TEST_METHOD)
    })
    void givenRollbackEventOfProtocolParamsUpdateProcessor_shouldDeleteEpoch() throws Exception {
        RollbackEvent rollbackEvent = RollbackEvent.builder()
                .rollbackTo(new Point(2246440, "dffd1848e8ef26aadb1d4d05612825596ab697b27d2ea422fec077dd0de93254"))
                .currentPoint(new Point(2246440, "dffd1848e8ef26aadb1d4d05612825596ab697b27d2ea422fec077dd0de93254"))
                .build();

        protocolParamsUpdateProcessor.handleRollback(rollbackEvent);

        int count = protocolParamsRepository.findAll().size();
        assertThat(count).isEqualTo(2);
    }
}
