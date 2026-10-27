package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Micrometer counters for CIP-68 indexing, so the totals of what is indexed and what is dropped can be read from
 * {@code /actuator/prometheus} instead of counting log lines. The tags have a small fixed set of values. What is wrong with a stored token
 * is data, not a metric: it is in {@code validation_errors} (the API and SQL).
 * <ul>
 *   <li>{@value #INDEXED}: datums stored, tagged {@code label} (222, 333, 444). A datum is stored whatever is wrong
 *       with it.</li>
 *   <li>{@value #SKIPPED}: datums not stored. Only for what is not metadata at all: {@code parse_failure} for a datum
 *       that could not be decoded, or {@code processing_failure} for an output whose indexing threw an unexpected
 *       exception (the one to alert on). Tagged {@code reason} only.</li>
 *   <li>{@value #DROPPED_PROPERTIES}: properties left out of a stored datum because they look like an attack, tagged
 *       {@code kind} ({@code too_deep}).</li>
 * </ul>
 * The counters count datums processed: they start at zero on every restart, and a block replayed after a chain
 * rollback is counted again.
 */
@Component
public class Cip68Metrics {

    public static final String INDEXED = "yaci.store.assets.cip68.datums.indexed";
    public static final String SKIPPED = "yaci.store.assets.cip68.datums.skipped";
    public static final String DROPPED_PROPERTIES = "yaci.store.assets.cip68.properties.dropped";

    private static final String LABEL_TAG = "label";
    private static final String REASON_TAG = "reason";

    public static final String PARSE_FAILURE = "parse_failure";

    /** An unexpected exception while indexing an output: the output is skipped and the sync goes on. The one reason to alert on. */
    public static final String PROCESSING_FAILURE = "processing_failure";

    /**
     * A datum with no user token in its transaction and none of the CIP-68 token fields, for a reference NFT that is not
     * stored yet: it is not token metadata (a dApp's own state under the {@code 000643b0} prefix, an empty map).
     */
    public static final String NOT_TOKEN_METADATA = "not_token_metadata";

    /** The {@code kind} of a property that is left out because its value is nested deeper than the parser allows. */
    public static final String TOO_DEEP = "too_deep";

    private final MeterRegistry registry;

    @Autowired
    public Cip68Metrics(ObjectProvider<MeterRegistry> registry) {
        this(registry.getIfAvailable(SimpleMeterRegistry::new));
    }

    public Cip68Metrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Counters that nobody reads, for code that is built without a registry (tests). */
    public static Cip68Metrics noop() {
        return new Cip68Metrics(new SimpleMeterRegistry());
    }

    public void datumIndexed(int label) {
        registry.counter(INDEXED, LABEL_TAG, String.valueOf(label)).increment();
    }

    public void parseFailure() {
        registry.counter(SKIPPED, REASON_TAG, PARSE_FAILURE).increment();
    }

    /** An output whose indexing threw an unexpected exception; it is skipped. Unlike the other reasons, this one is a bug or a new kind of attack. */
    public void processingFailure() {
        registry.counter(SKIPPED, REASON_TAG, PROCESSING_FAILURE).increment();
    }

    public void notTokenMetadata() {
        registry.counter(SKIPPED, REASON_TAG, NOT_TOKEN_METADATA).increment();
    }

    public void propertyDropped(String kind) {
        registry.counter(DROPPED_PROPERTIES, "kind", kind).increment();
    }
}
