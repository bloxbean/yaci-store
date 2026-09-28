package com.bloxbean.cardano.yaci.store.epoch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolParamsDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void conwayFieldsAreSerializedAsStrings() throws Exception {
        ProtocolParamsDto dto = ProtocolParamsDto.builder()
                .committeeMinSize(7)
                .committeeMaxTermLength(146)
                .govActionLifetime(6)
                .govActionDeposit(BigInteger.valueOf(100000000000L))
                .drepDeposit(BigInteger.valueOf(500000000))
                .drepActivity(20)
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.get("committee_min_size").isTextual()).isTrue();
        assertThat(json.get("committee_min_size").asText()).isEqualTo("7");
        assertThat(json.get("committee_max_term_length").asText()).isEqualTo("146");
        assertThat(json.get("committee_max_term_length").isTextual()).isTrue();
        assertThat(json.get("gov_action_lifetime").isTextual()).isTrue();
        assertThat(json.get("gov_action_lifetime").asText()).isEqualTo("6");
        assertThat(json.get("gov_action_deposit").isTextual()).isTrue();
        assertThat(json.get("gov_action_deposit").asText()).isEqualTo("100000000000");
        assertThat(json.get("drep_deposit").isTextual()).isTrue();
        assertThat(json.get("drep_deposit").asText()).isEqualTo("500000000");
        assertThat(json.get("drep_activity").isTextual()).isTrue();
        assertThat(json.get("drep_activity").asText()).isEqualTo("20");
    }

    @Test
    void votingThresholdsUseBlockfrostNames() throws Exception {
        ProtocolParamsDto dto = ProtocolParamsDto.builder()
                .pvtPPSecurityGroup(new BigDecimal("0.51"))
                .dvtPPNetworkGroup(new BigDecimal("0.67"))
                .dvtPPEconomicGroup(new BigDecimal("0.68"))
                .dvtPPTechnicalGroup(new BigDecimal("0.69"))
                .dvtPPGovGroup(new BigDecimal("0.75"))
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertThat(json.get("pvt_p_p_security_group").decimalValue()).isEqualByComparingTo("0.51");
        assertThat(json.get("pvtpp_security_group").decimalValue()).isEqualByComparingTo("0.51");
        assertThat(json.get("dvt_p_p_network_group").decimalValue()).isEqualByComparingTo("0.67");
        assertThat(json.get("dvt_p_p_economic_group").decimalValue()).isEqualByComparingTo("0.68");
        assertThat(json.get("dvt_p_p_technical_group").decimalValue()).isEqualByComparingTo("0.69");
        assertThat(json.get("dvt_p_p_gov_group").decimalValue()).isEqualByComparingTo("0.75");
        assertThat(json.has("dvt_ppnetwork_group")).isFalse();
        assertThat(json.has("dvt_ppeconomic_group")).isFalse();
        assertThat(json.has("dvt_pptechnical_group")).isFalse();
        assertThat(json.has("dvt_ppgov_group")).isFalse();
    }

    @Test
    void nullConwayFieldsAreOmitted() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(new ProtocolParamsDto()));

        assertThat(json.has("drep_deposit")).isFalse();
        assertThat(json.has("gov_action_deposit")).isFalse();
    }

    @Test
    void extraEntropyIsAlwaysIncluded() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(new ProtocolParamsDto()));

        assertThat(json.has("extra_entropy")).isTrue();
        assertThat(json.get("extra_entropy").isNull()).isTrue();
    }

    @Test
    void conwayFieldsCanBeReadFromStringsAndNumbers() throws Exception {
        String fromStrings = "{\"drep_deposit\":\"500000000\",\"gov_action_deposit\":\"100000000000\",\"drep_activity\":\"20\",\"committee_min_size\":\"7\"}";
        String fromNumbers = "{\"drep_deposit\":500000000,\"gov_action_deposit\":100000000000,\"drep_activity\":20,\"committee_min_size\":7}";

        for (String json : new String[]{fromStrings, fromNumbers}) {
            ProtocolParamsDto dto = objectMapper.readValue(json, ProtocolParamsDto.class);
            assertThat(dto.getDrepDeposit()).isEqualTo(BigInteger.valueOf(500000000));
            assertThat(dto.getGovActionDeposit()).isEqualTo(BigInteger.valueOf(100000000000L));
            assertThat(dto.getDrepActivity()).isEqualTo(20);
            assertThat(dto.getCommitteeMinSize()).isEqualTo(7);
        }
    }
}
