package com.bloxbean.cardano.yaci.store.epochnonce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EpochNonceProperties {
    private boolean enabled;
}
