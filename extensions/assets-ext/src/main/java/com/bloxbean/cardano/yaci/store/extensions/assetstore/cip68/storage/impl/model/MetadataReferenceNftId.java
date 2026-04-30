package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class MetadataReferenceNftId {

    private String policyId;
    private String assetName;
    private Long slot;

}
