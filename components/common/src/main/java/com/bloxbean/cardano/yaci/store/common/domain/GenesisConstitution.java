package com.bloxbean.cardano.yaci.store.common.domain;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class GenesisConstitution {
    private String anchorUrl;
    private String anchorHash;
    private String script;
}
