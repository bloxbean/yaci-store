package com.bloxbean.cardano.yaci.store.events;

import com.bloxbean.cardano.yaci.core.model.Era;
import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class EpochChangeEvent {
    private EventMetadata metadata;
    private Integer previousEpoch;
    private Integer epoch;
    private Era previousEra;
    private Era era;
}
