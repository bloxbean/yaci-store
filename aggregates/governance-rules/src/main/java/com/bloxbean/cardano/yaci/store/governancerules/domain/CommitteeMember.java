package com.bloxbean.cardano.yaci.store.governancerules.domain;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitteeMember {
    private String coldKey;
    private String hotKey;
    private CredentialType credType;
    private Integer startEpoch;
    private Integer expiredEpoch;
}
