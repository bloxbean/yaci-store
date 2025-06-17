package com.bloxbean.cardano.yaci.store.metadata.domain;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
/**
 * Derived domain event for tx metadata. This event is published by {@link com.bloxbean.cardano.yaci.store.metadata.processor.MetadataProcessor}
 * and can be consumed by custom store implementations.
 */
public class TxMetadataEvent {
    private EventMetadata metadata;
    private List<TxMetadataLabel> txMetadataList;
}
