package com.bloxbean.cardano.yaci.store.remote.consumer;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.MintBurnEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxMintBurn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MintBurnEventJsonTest {

    @Test
    void txIndexSurvivesJsonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MintBurnEvent event = new MintBurnEvent(EventMetadata.builder().slot(100).build(),
                List.of(TxMintBurn.builder().txHash("abc").txIndex(2).amounts(List.of()).build()));

        MintBurnEvent parsed = objectMapper.readValue(objectMapper.writeValueAsString(event), MintBurnEvent.class);

        assertThat(parsed.getTxMintBurns().get(0).getTxHash()).isEqualTo("abc");
        assertThat(parsed.getTxMintBurns().get(0).getTxIndex()).isEqualTo(2);
    }
}
