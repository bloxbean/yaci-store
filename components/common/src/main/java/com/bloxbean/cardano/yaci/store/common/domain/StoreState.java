package com.bloxbean.cardano.yaci.store.common.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * How far a derived table or a backfill has been kept, stored in store_state. The module that owns
 * {@code stateId} decides what {@code status}, the block and {@code data} mean.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class StoreState {
    private String stateId;
    private String status;
    private Long slot;
    private Long block;
    private String blockHash;
    private String data;
    private LocalDateTime updateDateTime;
}
