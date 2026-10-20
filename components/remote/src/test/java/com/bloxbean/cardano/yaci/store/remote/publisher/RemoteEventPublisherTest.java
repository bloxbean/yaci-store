package com.bloxbean.cardano.yaci.store.remote.publisher;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.remote.RemoteProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RemoteEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void publishesJackson2JsonThatTheConsumerReads() throws Exception {
        StreamBridge streamBridge = mock(StreamBridge.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        RemoteProperties properties = new RemoteProperties();
        properties.setPublisherEvents(List.of("rollbackEvent"));
        RemoteEventPublisher publisher = new RemoteEventPublisher(streamBridge, properties);
        RollbackEvent event = RollbackEvent.builder()
                .rollbackTo(new Point(100, "aa"))
                .currentPoint(new Point(200, "bb"))
                .currentBlock(7)
                .build();

        publisher.handleRollbackEvent(event);

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(eq("rollbackEvent-out-0"), sent.capture());
        Message<byte[]> message = (Message<byte[]>) sent.getValue();
        assertThat(message.getHeaders().get(MessageHeaders.CONTENT_TYPE)).hasToString("application/json");
        String json = new String(message.getPayload(), StandardCharsets.UTF_8);
        assertThat(json).isEqualTo(objectMapper.writeValueAsString(event.toBuilder().remotePublish(true).build()));

        //RemoteEventConsumer reads the String with a plain Jackson 2 ObjectMapper
        RollbackEvent received = new ObjectMapper().readValue(json, RollbackEvent.class);
        assertThat(received.getRollbackTo().getSlot()).isEqualTo(100);
        assertThat(received.getCurrentBlock()).isEqualTo(7);
        assertThat(received.isRemotePublish()).isTrue();
    }
}
