package com.bloxbean.cardano.yaci.store.api.governance.dto;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommitteeMemberDto {
    private String hash;

    private CredentialType credType;

    private Integer startEpoch;

    private Integer expiredEpoch;
}
