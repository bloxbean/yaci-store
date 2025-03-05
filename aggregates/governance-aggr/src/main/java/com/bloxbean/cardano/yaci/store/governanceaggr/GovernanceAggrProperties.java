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

    /**
     * Indicates whether the bootstrap phase is available in the Conway era.
     * <p>
     * - On public networks (e.g., mainnet, preview, preprod), the Conway era includes a bootstrap phase.
     * </p>
     * <p>
     * - On devnet environments (e.g., yaci-devkit), the bootstrap phase may exist or not exist
     * </p>
     * Default: {@code false}.
     */
    @Builder.Default
    private boolean isConwayBootstrapAvailable = false;
}
