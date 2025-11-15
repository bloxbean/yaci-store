package com.bloxbean.cardano.yaci.store.governanceaggr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GovernanceAggrProperties {
    private boolean enabled;

    @Builder.Default
    private boolean isDevnetConwayBootstrapAvailable = false;

    // PostgreSQL Memory Configuration for DRep Distribution Snapshot Operations
    // Default is null to use PostgreSQL defaults
    private String drepDistWorkMem;
}
