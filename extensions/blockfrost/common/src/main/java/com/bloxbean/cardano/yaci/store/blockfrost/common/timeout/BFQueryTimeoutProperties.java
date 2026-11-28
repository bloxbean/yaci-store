package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@ConfigurationProperties(prefix = "store.extensions.blockfrost", ignoreUnknownFields = true)
public class BFQueryTimeoutProperties {

    /**
     * Upper bound on the database time spent by one Blockfrost request. Each statement gets the time left in
     * the request's budget, so a request issuing several queries is bounded as a whole. A number without a unit
     * is in seconds. 0 disables the bound.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration queryTimeout = Duration.ofSeconds(30);
}
