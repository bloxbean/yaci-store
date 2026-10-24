package com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class BFProtocolParamsDtoJsonTest {

    @Test
    void eMaxAndNOptAreWrittenOnce() throws Exception {
        var mapper = Jackson2ObjectMapperBuilder.json().build();
        BFProtocolParamsDto dto = new BFProtocolParamsDto();
        dto.setEMax(18);
        dto.setNOpt(500);

        JsonNode node = mapper.readTree(mapper.writeValueAsString(dto));

        //As with Jackson 2.17 (Spring Boot 3): Jackson 2.18+ also wrote "emax" and "nopt"
        assertThat(node.get("e_max").asInt()).isEqualTo(18);
        assertThat(node.get("n_opt").asInt()).isEqualTo(500);
        assertThat(node.has("emax")).isFalse();
        assertThat(node.has("nopt")).isFalse();
    }
}
