package com.bloxbean.cardano.yaci.store.governance.domain.local;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LocalCommitteeMember {
    private String hash;

    private Integer epoch;

    private CredentialType credType;

    private Integer expiredEpoch;

    private Long slot;
}
