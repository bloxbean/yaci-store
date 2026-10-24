package com.bloxbean.cardano.yaci.store.epoch.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolParamsDtoJsonTest {

    @Test
    void eMaxAndNOptAreWrittenOnce() throws Exception {
        //The mapper of the REST API's JSON converter (Jackson2WebMvc)
        String json = Jackson2ObjectMapperBuilder.json().build()
                .writeValueAsString(ProtocolParamsDto.builder().eMax(18).nOpt(150).minFeeA(44).build());

        //As with Jackson 2.17 (Spring Boot 3): Jackson 2.18+ also wrote "emax" and "nopt"
        JsonNode node = Jackson2ObjectMapperBuilder.json().build().readTree(json);
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        assertThat(names).containsExactly("min_fee_a", "e_max", "n_opt");
        assertThat(node.get("e_max").asInt()).isEqualTo(18);
        assertThat(node.get("n_opt").asInt()).isEqualTo(150);
    }
}
