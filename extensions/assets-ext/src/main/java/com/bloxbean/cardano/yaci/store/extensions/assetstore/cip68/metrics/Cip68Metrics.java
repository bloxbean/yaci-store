package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Micrometer counters for CIP-68 indexing, so the totals of what is indexed and what is wrong with it can be read from
 * {@code /actuator/prometheus} instead of counting log lines. The tags have a small fixed set of values.
 * <ul>
 *   <li>{@value #INDEXED}: datums stored, tagged {@code label} (222, 333, 444). A datum is stored whatever is wrong
 *       with it.</li>
 *   <li>{@value #VALIDATION_ERRORS}: the validation errors reported for stored datums, tagged {@code sub_standard}
 *       (the label, or {@code none} when the rule does not depend on one), {@code code} (for example
 *       {@code missing_value}) and {@code category} ({@code MANDATORY} or {@code OPTIONAL}). The field is not a tag:
 *       property names come from the chain.</li>
 *   <li>{@value #SKIPPED}: datums not stored. Only for what is not metadata at all: {@code parse_failure} for a datum
 *       that could not be decoded, or {@code processing_failure} for an output whose indexing threw an unexpected
 *       exception. The label is {@code unknown} for both, since it is derived later.</li>
 *   <li>{@value #MULTI_LABEL}: reference NFTs that have user tokens of several labels, tagged {@code labels}
 *       (for example {@code 222+333}).</li>
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
    public static final String VALIDATION_ERRORS = "yaci.store.assets.cip68.datums.validation_errors";
    public static final String DROPPED_PROPERTIES = "yaci.store.assets.cip68.properties.dropped";

    public static final String MULTI_LABEL = "yaci.store.assets.cip68.datums.multi_label";

    private static final String LABEL_TAG = "label";
    private static final String REASON_TAG = "reason";
    private static final String UNKNOWN_LABEL = "unknown";
    private static final String NO_SUB_STANDARD = "none";

    public static final String PARSE_FAILURE = "parse_failure";

    /** An unexpected exception while indexing an output: the output is skipped and the sync goes on. The one reason to alert on. */
    public static final String PROCESSING_FAILURE = "processing_failure";

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

    /** One count per error, tagged with the label it was found for, its code and its category. */
    public void validationErrors(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            registry.counter(VALIDATION_ERRORS,
                    "sub_standard", error.subStandard() == null ? NO_SUB_STANDARD : error.subStandard(),
                    "code", error.code(),
                    "category", error.fieldCategory().name()).increment();
        }
    }

    public void parseFailure() {
        registry.counter(SKIPPED, LABEL_TAG, UNKNOWN_LABEL, REASON_TAG, PARSE_FAILURE).increment();
    }

    /** An output whose indexing threw an unexpected exception; it is skipped. Unlike the other reasons, this one is a bug or a new kind of attack. */
    public void processingFailure() {
        registry.counter(SKIPPED, LABEL_TAG, UNKNOWN_LABEL, REASON_TAG, PROCESSING_FAILURE).increment();
    }

    /** A reference NFT with user tokens of several labels, tagged {@code labels} (for example {@code 222+333}). */
    public void multiLabel(List<Integer> labels) {
        registry.counter(MULTI_LABEL, "labels", labels.stream().map(String::valueOf).collect(Collectors.joining("+"))).increment();
    }

    public void propertyDropped(String kind) {
        registry.counter(DROPPED_PROPERTIES, "kind", kind).increment();
    }
}
