package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.metrics.Cip68Metrics;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The counters behind {@code /actuator/prometheus}: datums indexed, datums skipped (with the reason), properties
 * dropped. Runs the real parser, token service and processor with a {@link SimpleMeterRegistry}.
 */
class Cip68MetricsTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    private MeterRegistry registry;
    private Cip68Processor processor;
    private int next;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        Cip68Metrics metrics = new Cip68Metrics(registry);
        Cip68MetadataRepository repository = mock(Cip68MetadataRepository.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(metrics), repository, metrics);
    }

    @Test
    void countsIndexedDatumsPerLabel() {
        process(Cip68Constants.NFT_TOKEN_PREFIX, text("name", "N"), text("image", "ipfs://Qm"));
        process(Cip68Constants.NFT_TOKEN_PREFIX, text("name", "N2"), text("image", "https://x/y.png"));
        process(Cip68Constants.FUNGIBLE_TOKEN_PREFIX, text("name", "C"), text("description", "d"));

        assertThat(indexed(222)).isEqualTo(2);
        assertThat(indexed(333)).isEqualTo(1);
        assertThat(indexed(444)).isZero();
    }

    @Test
    void countsSkippedDatumsPerLabelAndReason() {
        process(Cip68Constants.NFT_TOKEN_PREFIX, text("description", "no name"), text("image", "ipfs://Qm"));
        process(Cip68Constants.NFT_TOKEN_PREFIX, text("name", "N"));
        process(Cip68Constants.NFT_TOKEN_PREFIX, text("name", "N"), text("image", "iagon://x"));
        process(Cip68Constants.FUNGIBLE_TOKEN_PREFIX, text("name", "C"));

        assertThat(skipped(222, "no_name")).isEqualTo(1);
        assertThat(skipped(222, "no_image")).isEqualTo(1);
        assertThat(skipped(222, "bad_image_scheme")).isEqualTo(1);
        assertThat(skipped(333, "no_description")).isEqualTo(1);
        assertThat(registry.find(Cip68Metrics.INDEXED).counters()).isEmpty();
    }

    @Test
    void countsABadLogoAsADroppedPropertyAndStillIndexesTheToken() {
        process(Cip68Constants.FUNGIBLE_TOKEN_PREFIX, text("name", "C"), text("description", "d"), text("logo", "iagon://logo"));

        assertThat(dropped("bad_logo_scheme")).isEqualTo(1);
        assertThat(indexed(333)).isEqualTo(1);
        assertThat(registry.find(Cip68Metrics.SKIPPED).counters()).isEmpty();
    }

    @Test
    void countsAnUndecodableDatumAsAParseFailure() {
        processor.processTransaction(event("zz-not-hex", Cip68Constants.NFT_TOKEN_PREFIX));

        assertThat(registry.get(Cip68Metrics.SKIPPED).tag("label", "unknown").tag("reason", "parse_failure").counter().count())
                .isEqualTo(1);
    }

    @Test
    void countsDroppedProperties() {
        MapPlutusData nested = new MapPlutusData();
        nested.put(BytesPlutusData.of("k"), ConstrPlutusData.of(0));
        process(Cip68Constants.NFT_TOKEN_PREFIX, text("name", "N"), text("image", "ipfs://Qm"),
                entry("owner", ConstrPlutusData.of(0)), entry("holders", ListPlutusData.of(ConstrPlutusData.of(1))),
                entry("deep", nested),
                Map.entry("1", (PlutusData) BytesPlutusData.of("from bytes")));

        assertThat(dropped("constructor")).isEqualTo(3);
        assertThat(dropped("key_unsupported")).isZero();
    }

    @Test
    void countsKeysLeftOutAndCollisions() {
        MapPlutusData props = new MapPlutusData();
        props.put(BytesPlutusData.of("name"), BytesPlutusData.of("N"));
        props.put(BytesPlutusData.of("image"), BytesPlutusData.of("ipfs://Qm"));
        props.put(ListPlutusData.of(BigIntPlutusData.of(1)), BytesPlutusData.of("list key"));
        props.put(BytesPlutusData.of("1"), BytesPlutusData.of("b"));
        props.put(BigIntPlutusData.of(1), BytesPlutusData.of("i"));
        processor.processTransaction(event(datum(props), Cip68Constants.NFT_TOKEN_PREFIX));

        assertThat(dropped("key_unsupported")).isEqualTo(1);
        assertThat(dropped("key_collision")).isEqualTo(1);
        assertThat(indexed(222)).isEqualTo(1);
    }

    // ---------- helpers ----------

    private double indexed(int label) {
        var c = registry.find(Cip68Metrics.INDEXED).tag("label", String.valueOf(label)).counter();
        return c == null ? 0 : c.count();
    }

    private double skipped(int label, String reason) {
        var c = registry.find(Cip68Metrics.SKIPPED).tag("label", String.valueOf(label)).tag("reason", reason).counter();
        return c == null ? 0 : c.count();
    }

    private double dropped(String kind) {
        var c = registry.find(Cip68Metrics.DROPPED_PROPERTIES).tag("kind", kind).counter();
        return c == null ? 0 : c.count();
    }

    @SafeVarargs
    private void process(String userTokenPrefix, Map.Entry<String, PlutusData>... entries) {
        MapPlutusData props = new MapPlutusData();
        for (Map.Entry<String, PlutusData> e : entries) {
            // a key made of digits is an integer key in these tests
            PlutusData key = e.getKey().chars().allMatch(Character::isDigit) ? BigIntPlutusData.of(Long.parseLong(e.getKey()))
                    : BytesPlutusData.of(e.getKey());
            props.put(key, e.getValue());
        }
        processor.processTransaction(event(datum(props), userTokenPrefix));
    }

    private AddressUtxoEvent event(String datum, String userTokenPrefix) {
        String base = "4e46" + String.format("%02x", next++);
        AddressUtxo refNft = AddressUtxo.builder().txHash(TX_HASH).txIndex(0).inlineDatum(datum)
                .amounts(List.of(Amt.builder().unit(POLICY + Cip68Constants.REFERENCE_TOKEN_PREFIX + base).quantity(BigInteger.ONE).build()))
                .build();
        AddressUtxo userToken = AddressUtxo.builder().txHash(TX_HASH).txIndex(1)
                .amounts(List.of(Amt.builder().unit(POLICY + userTokenPrefix + base).quantity(BigInteger.ONE).build())).build();
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(refNft, userToken)).build()))
                .build();
    }

    private static Map.Entry<String, PlutusData> text(String key, String value) {
        return Map.entry(key, BytesPlutusData.of(value));
    }

    private static Map.Entry<String, PlutusData> entry(String key, PlutusData value) {
        return Map.entry(key, value);
    }

    private static String datum(MapPlutusData properties) {
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
