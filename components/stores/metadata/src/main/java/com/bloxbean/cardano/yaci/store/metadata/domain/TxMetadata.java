package com.bloxbean.cardano.yaci.store.metadata.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Slf4j
public class TxMetadata {
    private String label;
    private JsonNode body;
}
