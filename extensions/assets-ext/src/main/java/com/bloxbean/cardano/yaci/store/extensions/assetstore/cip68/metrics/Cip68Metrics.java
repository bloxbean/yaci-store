package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.DatumRejection;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Micrometer counters for CIP-68 indexing, so the totals of what is indexed and what is not can be read from
 * {@code /actuator/prometheus} instead of counting log lines. The tags have a small fixed set of values.
 * <ul>
 *   <li>{@value #INDEXED}: datums stored, tagged {@code label} (222, 333, 444).</li>
 *   <li>{@value #SKIPPED}: datums not stored, tagged {@code label} and {@code reason} (a
 *       {@link DatumRejection.Reason}, {@code parse_failure} for a datum that could not be decoded, or
 *       {@code invalid_version} for a version CIP-68 does not define; the label is {@code unknown} for these two,
 *       since it is derived later).</li>
 *   <li>{@value #MULTI_LABEL}: reference NFTs that have user tokens of several labels, tagged {@code labels}
 *       (for example {@code 222+333}) and {@code outcome} ({@code indexed} or {@code skipped}).</li>
 *   <li>{@value #DROPPED_PROPERTIES}: properties left out of a stored datum, tagged {@code kind}
 *       ({@code constructor}, {@code key_unsupported}, {@code key_collision}).</li>
 * </ul>
 * The counters count datums processed: they start at zero on every restart, and a block replayed after a chain
 * rollback is counted again.
 */
@Component
public class Cip68Metrics {

    public static final String INDEXED = "yaci.store.assets.cip68.datums.indexed";
    public static final String SKIPPED = "yaci.store.assets.cip68.datums.skipped";
    public static final String DROPPED_PROPERTIES = "yaci.store.assets.cip68.properties.dropped";

    public static final String MULTI_LABEL = "yaci.store.assets.cip68.datums.multi_label";
    public static final String INDEXED_OUTCOME = "indexed";
    public static final String SKIPPED_OUTCOME = "skipped";

    public static final String PARSE_FAILURE = "parse_failure";
    public static final String INVALID_VERSION = "invalid_version";

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

    public void indexed(int label) {
        registry.counter(INDEXED, "label", String.valueOf(label)).increment();
    }

    public void skipped(int label, DatumRejection.Reason reason) {
        registry.counter(SKIPPED, "label", String.valueOf(label), "reason", reason.tag()).increment();
    }

    public void parseFailure() {
        registry.counter(SKIPPED, "label", "unknown", "reason", PARSE_FAILURE).increment();
    }

    public void invalidVersion() {
        registry.counter(SKIPPED, "label", "unknown", "reason", INVALID_VERSION).increment();
    }

    /** A reference NFT with user tokens of several labels, tagged {@code labels} (for example {@code 222+333}) and {@code outcome}. */
    public void multiLabel(List<Integer> labels, String outcome) {
        registry.counter(MULTI_LABEL, "labels", labels.stream().map(String::valueOf).collect(Collectors.joining("+")),
                "outcome", outcome).increment();
    }

    public void propertyDropped(String kind) {
        registry.counter(DROPPED_PROPERTIES, "kind", kind).increment();
    }
}
