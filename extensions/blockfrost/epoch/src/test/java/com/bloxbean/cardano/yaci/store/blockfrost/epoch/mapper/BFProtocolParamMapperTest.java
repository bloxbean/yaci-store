package com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class BFProtocolParamMapperTest {
    private final BFProtocolParamMapper mapper = BFProtocolParamMapper.INSTANCE;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeGovernanceDepositsAsStrings() throws Exception {
        ProtocolParamsDto protocolParamsDto = ProtocolParamsDto.builder()
                .govActionDeposit(BigInteger.valueOf(100000000000L))
                .drepDeposit(BigInteger.valueOf(500000000L))
                .build();

        BFProtocolParamsDto bfProtocolParamsDto = mapper.toBFProtocolParamsDto(protocolParamsDto);
        String json = objectMapper.writeValueAsString(bfProtocolParamsDto);

        assertThat(bfProtocolParamsDto.getGovActionDeposit()).isEqualTo("100000000000");
        assertThat(bfProtocolParamsDto.getDrepDeposit()).isEqualTo("500000000");
        assertThat(json).contains("\"gov_action_deposit\":\"100000000000\"");
        assertThat(json).contains("\"drep_deposit\":\"500000000\"");
    }
}
