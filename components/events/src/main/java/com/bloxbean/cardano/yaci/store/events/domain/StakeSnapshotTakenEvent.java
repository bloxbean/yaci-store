package com.bloxbean.cardano.yaci.store.events.domain;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StakeSnapshotTakenEvent {
    private int epoch;
}
