package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ReadOnly(false)
@RequiredArgsConstructor
@Slf4j
public class ChainTipService {
    private final TipFinderService tipFinderService;
    private final EpochConfig epochConfig;
    private final EraService eraService;

    /**
     * Get the current tip and epoch number.
     * This method can only be used when the node is in Shelley or post-Shelley era.
     *
     * @return an Optional containing a Tuple with the current Tip and epoch number.
     */
    public synchronized Optional<Tuple<Tip, Integer>> getTipAndCurrentEpoch() {
        long startNanos = System.nanoTime();
        if (log.isDebugEnabled())
            log.debug("Fetching current tip and epoch. thread={}", Thread.currentThread().getName());
        try {
            var tip = tipFinderService.getTip().block(Duration.ofSeconds(10));

            if (tip != null) {
                int epoch = epochConfig.epochFromSlot(eraService.getFirstNonByronSlot(), Era.Shelley, tip.getPoint().getSlot());
                if (log.isDebugEnabled())
                    log.debug("Fetched current tip and epoch. slot={}, hash={}, epoch={}, durationMs={}",
                            tip.getPoint().getSlot(), tip.getPoint().getHash(), epoch, elapsedMs(startNanos));
                return Optional.of(new Tuple<>(tip, epoch));
            } else {
                log.warn("Unable to resolve current tip. durationMs={}", elapsedMs(startNanos));
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Unable to get the tip using TipFinderService after {} ms", elapsedMs(startNanos), e);
            return Optional.empty();
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
