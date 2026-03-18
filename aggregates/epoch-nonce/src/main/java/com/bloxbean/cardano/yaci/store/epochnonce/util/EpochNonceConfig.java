package com.bloxbean.cardano.yaci.store.epochnonce.util;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Configuration for epoch nonce computation.
 * <p>
 * The stability window for the candidate nonce freeze cutoff is era-dependent:
 * <ul>
 *   <li>Pre-Conway (Shelley through Babbage): {@code floor(3k/f)} — e.g. 129,600 slots</li>
 *   <li>Conway+: {@code ceiling(4k/f)} — e.g. 172,800 slots</li>
 * </ul>
 * <p>
 * The change was introduced in ouroboros-consensus v0.15.0.0 which set Conway's
 * {@code praosRandomnessStabilisationWindow} to {@code computeRandomnessStabilisationWindow}
 * (ceiling(4k/f)) instead of the previous {@code computeStabilityWindow} (3k/f).
 * See also cardano-ledger erratum 17.3.
 */
@Component
@Slf4j
public class EpochNonceConfig {
    private final long preConwayStabilityWindow;
    private final long conwayStabilityWindow;
    private final long epochLength;

    public EpochNonceConfig(GenesisConfig genesisConfig) {
        this.preConwayStabilityWindow = (long) Math.floor((3.0 * genesisConfig.getSecurityParam()) / genesisConfig.getActiveSlotsCoeff());
        this.conwayStabilityWindow = genesisConfig.getRandomnessStabilisationWindow();
        this.epochLength = genesisConfig.getEpochLength();

        log.info("EpochNonceConfig: preConwayStabilityWindow={} (3k/f), conwayStabilityWindow={} (4k/f), epochLength={}",
                preConwayStabilityWindow, conwayStabilityWindow, epochLength);
    }

    /**
     * Returns the era-specific stability window.
     * <p>
     * Conway+ uses {@code ceiling(4k/f)}, pre-Conway uses {@code floor(3k/f)}.
     */
    public long getStabilityWindow(Era era) {
        if (era != null && era.getValue() >= Era.Conway.getValue()) {
            return conwayStabilityWindow;
        }
        return preConwayStabilityWindow;
    }

    public long getEpochLength() {
        return epochLength;
    }
}
