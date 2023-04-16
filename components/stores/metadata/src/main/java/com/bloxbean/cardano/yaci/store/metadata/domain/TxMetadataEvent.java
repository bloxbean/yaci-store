package com.bloxbean.cardano.yaci.store.metadata.domain;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)

/**
 * Derived domain event for tx metadata. This event is published by {@link com.bloxbean.cardano.yaci.store.metadata.processor.MetadataProcessor}
 * and can be consumed by custom store implementations.
 */
public class TxMetadataEvent extends ApplicationEvent {

    private final EventMetadata eventMetadata;

    private final List<TxMetadataLabel> txMetadataList;

    public TxMetadataEvent(Object source,
                           EventMetadata eventMetadata,
                           List<TxMetadataLabel> txMetadataList) {
        super(source);
        this.eventMetadata = eventMetadata;
        this.txMetadataList = txMetadataList;
    }

}
