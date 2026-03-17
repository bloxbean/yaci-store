package com.bloxbean.cardano.yaci.store.epochnonce.util;

import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Configuration for epoch nonce computation.
 * <p>
 * Uses {@code floor(3k/f)} as the stability window for the candidate nonce freeze cutoff.
 * <p>
 * Despite the Haskell source naming {@code computeRandomnessStabilisationWindow = ceiling(4k/f)},
 * the consensus layer's {@code PraosParams.praosRandomnessStabilisationWindow} is actually populated
 * with {@code computeStabilityWindow} (3k/f). This has been empirically verified against dbsync
 * for 30+ preprod epochs.
 */
@Component
@Slf4j
public class EpochNonceConfig {
    private final long stabilityWindow;
    private final long epochLength;

    public EpochNonceConfig(GenesisConfig genesisConfig) {
        this.stabilityWindow = (long) Math.floor((3.0 * genesisConfig.getSecurityParam()) / genesisConfig.getActiveSlotsCoeff());
        this.epochLength = genesisConfig.getEpochLength();

        log.info("EpochNonceConfig: stabilityWindow={} (3k/f), epochLength={}", stabilityWindow, epochLength);
    }

    public long getStabilityWindow() {
        return stabilityWindow;
    }

    public long getEpochLength() {
        return epochLength;
    }
}
