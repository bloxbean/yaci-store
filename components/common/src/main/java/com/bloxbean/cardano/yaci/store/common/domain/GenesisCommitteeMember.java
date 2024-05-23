package com.bloxbean.cardano.yaci.store.common.domain;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class GenesisCommitteeMember {
    private String hash;
    private Boolean hasScript;
    private Integer expiredEpoch;
}
