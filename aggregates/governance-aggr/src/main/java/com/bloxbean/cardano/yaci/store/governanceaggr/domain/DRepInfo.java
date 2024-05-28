package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DRepInfo {
    private String drepHash;

    private String drepId;

    private String anchorUrl;

    private String anchorHash;

    private Integer delegators;

    private Long totalStake;

    private Long createdAt;

    private DRepStatus status;
}
