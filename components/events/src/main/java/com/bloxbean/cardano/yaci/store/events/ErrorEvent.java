package com.bloxbean.cardano.yaci.store.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorEvent {
    private Integer id; // Null if not persisted
    private Long block;
    private String errorCode;
    private String reason;
    private String details;
}
