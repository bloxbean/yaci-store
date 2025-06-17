package com.bloxbean.cardano.yaci.store.script.domain;

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
 * Derived domain event for tx script.This event is published by {@link com.bloxbean.cardano.yaci.store.script.processor.ScriptRedeemerDatumProcessor}
 * and can be consumed by external store implementations.
 */
public class TxScriptEvent {
    private EventMetadata metadata;
    private List<TxScript> txScriptList;
}
