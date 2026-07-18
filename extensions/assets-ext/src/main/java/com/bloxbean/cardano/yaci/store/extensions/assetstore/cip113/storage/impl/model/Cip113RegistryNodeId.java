package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Cip113RegistryNodeId {

    private String key;
    private Long slot;
    private String txHash;

}
