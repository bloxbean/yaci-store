package com.bloxbean.cardano.yaci.store.remote.consumer;

import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import com.bloxbean.cardano.yaci.store.events.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@ConditionalOnMissingBean(BlockFetchService.class)
//Initialize only if BlockFetchService is not present to avoid event publish loop
@Slf4j
public class RemoteEventConsumer {
    private final ApplicationEventPublisher publisher;

    private ObjectMapper objectMapper;

    public RemoteEventConsumer(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
        this.objectMapper = new ObjectMapper();
        log.info("RemoteEventConsumer initialized >>");
    }

    @Bean
    public Consumer<String> blockEvent() {
        return blockEventStr -> {
            BlockEvent blockEvent = parseJson(blockEventStr, BlockEvent.class);
            if (log.isDebugEnabled())
                log.info("Received: " + blockEvent);
            publisher.publishEvent(blockEvent);
        };
    }

    @Bean
    public Consumer<String> blockHeaderEvent() {
        return blockHeaderEventStr -> {
            BlockHeaderEvent blockHeaderEvent = parseJson(blockHeaderEventStr, BlockHeaderEvent.class);
            if (log.isDebugEnabled())
                log.info("Received: " + blockHeaderEvent);
            publisher.publishEvent(blockHeaderEvent);
        };
    }

    @Bean
    public Consumer<String> byronMainBlockEvent() {
        return byronMainBlockEventStr -> {
            ByronMainBlockEvent byronMainBlockEvent = parseJson(byronMainBlockEventStr, ByronMainBlockEvent.class);
            if (log.isDebugEnabled())
                log.debug("Received: " + byronMainBlockEvent);
            publisher.publishEvent(byronMainBlockEvent);
        };
    }

    @Bean
    public Consumer<String> byronEbBlockEvent() {
        return byronEbBlockEventStr -> {
            ByronEbBlockEvent byronEbBlockEvent = parseJson(byronEbBlockEventStr, ByronEbBlockEvent.class);
            if (log.isDebugEnabled())
                log.debug("Received: " + byronEbBlockEvent);
            publisher.publishEvent(byronEbBlockEvent);
        };
    }

    @Bean
    public Consumer<String> transactionEvent() {
        return transactionEventStr -> {
            TransactionEvent transactionEvent = parseJson(transactionEventStr, TransactionEvent.class);
            if (log.isDebugEnabled())
                log.debug("Received: " + transactionEvent);
            publisher.publishEvent(transactionEvent);
        };
    }

    @Bean
    public Consumer<String> rollbackEvent() {
        return rollbackEventStr -> {
            RollbackEvent rollbackEvent = parseJson(rollbackEventStr, RollbackEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + rollbackEvent);
            publisher.publishEvent(rollbackEvent);
        };
    }

    @Bean
    public Consumer<String> auxDataEvent() {
        return auxDataEventStr -> {
            AuxDataEvent auxDataEvent = parseJson(auxDataEventStr, AuxDataEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + auxDataEvent);
            publisher.publishEvent(auxDataEvent);
        };
    }

    @Bean
    public Consumer<String> certificateEvent() {
        return certificateEventStr -> {
            CertificateEvent certificateEvent = parseJson(certificateEventStr, CertificateEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + certificateEvent);
            publisher.publishEvent(certificateEvent);
        };
    }

    @Bean
    public Consumer<String> mintBurnEvent() {
        return mintBurnEventStr -> {
            MintBurnEvent mintBurnEvent = parseJson(mintBurnEventStr, MintBurnEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + mintBurnEvent);
            publisher.publishEvent(mintBurnEvent);
        };
    }

    @Bean
    public Consumer<String> scriptEvent() {
        return scriptEventStr -> {
            ScriptEvent scriptEvent = parseJson(scriptEventStr, ScriptEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + scriptEvent);
            publisher.publishEvent(scriptEvent);
        };
    }

    @Bean
    public Consumer<String> governanceEvent() {
        return governanceEventStr -> {
            GovernanceEvent governanceEvent = parseJson(governanceEventStr, GovernanceEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + governanceEvent);
            publisher.publishEvent(governanceEvent);
        };
    }

    @Bean
    public Consumer<String> genesisBlockEvent() {
        return genesisBlockEventStr -> {
            GenesisBlockEvent genesisBlockEvent = parseJson(genesisBlockEventStr, GenesisBlockEvent.class);
            if(log.isDebugEnabled())
                log.debug("Received: " + genesisBlockEvent);
            publisher.publishEvent(genesisBlockEvent);
        };
    }

    private <T> T parseJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
