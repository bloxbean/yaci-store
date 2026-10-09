package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser.Cip68DatumParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.Cip68Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.Cip68MetadataRepository;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * A transaction that mints several CIP-68 tokens.
 * <ul>
 *   <li>One reference NFT per output (the usual pattern, each with its own datum): every token is indexed.</li>
 *   <li>Several reference NFTs in one output: a nested (version 4) datum carries the metadata of each, so each is
 *       indexed with its own entry. A flat datum is the metadata of every reference NFT in the output, as the CIP's
 *       retrieval steps read it, so all of them are indexed with it and one warning lists them.</li>
 * </ul>
 * Runs the real parser, token service and processor with the log captured; only the repository is mocked.
 */
class Cip68MultipleReferenceNftsTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    private static final String REF = Cip68Constants.REFERENCE_TOKEN_PREFIX;
    private static final String A = HexUtil.encodeHexString("A".getBytes());
    private static final String B = HexUtil.encodeHexString("B".getBytes());
    private static final String C = HexUtil.encodeHexString("C".getBytes());

    private Cip68MetadataRepository repository;
    private Cip68Processor processor;
    private ListAppender<ILoggingEvent> logs;
    private Logger processorLogger;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68MetadataRepository.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(), repository);
        processorLogger = (Logger) LoggerFactory.getLogger(Cip68Processor.class);
        logs = new ListAppender<>();
        logs.start();
        processorLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        processorLogger.detachAppender(logs);
    }

    @Test
    void indexesEveryReferenceNftOfANestedDatumInOneOutput() {
        String datum = nestedDatum(Map.of(A, nft("Token A"), B, nft("Token B"), C, nft("Token C")));

        List<Cip68Metadata> rows = index(output(datum, refUnit(A), refUnit(B), refUnit(C)));

        assertThat(rows).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("Token A", "Token B", "Token C");
        assertThat(rows).extracting(Cip68Metadata::getAssetName)
                .containsExactlyInAnyOrder(REF + A, REF + B, REF + C);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getPolicyId()).isEqualTo(POLICY);
            assertThat(row.getVersion()).isEqualTo(4L);
            assertThat(row.getDatum()).isEqualTo(datum);
        });
        assertThat(warnings()).isEmpty();
    }

    @Test
    void indexesOnlyTheReferenceNftsThatHaveAnEntry() {
        // three reference NFTs in the output, but the datum has an entry for two of them
        String datum = nestedDatum(Map.of(A, nft("Token A"), C, nft("Token C")));

        List<Cip68Metadata> rows = index(output(datum, refUnit(A), refUnit(B), refUnit(C)));

        assertThat(rows).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("Token A", "Token C");
    }

    @Test
    void ignoresEntriesWhoseReferenceNftIsNotInTheOutput() {
        String datum = nestedDatum(Map.of(A, nft("Token A"), B, nft("Token B"), C, nft("Token C")));

        List<Cip68Metadata> rows = index(output(datum, refUnit(A), refUnit(B)));

        assertThat(rows).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("Token A", "Token B");
    }

    @Test
    void indexesASingleReferenceNftOfANestedDatumAsBefore() {
        String datum = nestedDatum(Map.of(A, nft("Token A"), B, nft("Token B")));

        List<Cip68Metadata> rows = index(output(datum, refUnit(B)));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.getName()).isEqualTo("Token B");
            assertThat(row.getAssetName()).isEqualTo(REF + B);
        });
    }

    @Test
    void indexesEveryReferenceNftOfAFlatDatumWithTheSameMetadataAndWarns() {
        // the CIP's retrieval steps read the datum of the output the reference NFT is in, whatever else it holds
        String datum = flatDatum(nft("Flat token"));

        List<Cip68Metadata> rows = index(output(datum, refUnit(A), refUnit(B)));

        assertThat(rows).extracting(Cip68Metadata::getAssetName).containsExactlyInAnyOrder(REF + A, REF + B);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getName()).isEqualTo("Flat token");
            assertThat(row.getDatum()).isEqualTo(datum);
        });
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                .contains("holds 2 reference NFTs").contains("flat CIP-68 datum")
                .contains(POLICY + REF + A).contains(POLICY + REF + B).contains("indexing it for all of them"));
    }

    @Test
    void aVersion4DatumWithoutTheNestedKeyIsFlat() {
        MapPlutusData flat = nft("Flat v4");

        List<Cip68Metadata> rows = index(output(datum(flat, 4), refUnit(A), refUnit(B)));

        assertThat(rows).extracting(Cip68Metadata::getAssetName).containsExactlyInAnyOrder(REF + A, REF + B);
        assertThat(warnings()).hasSize(1);
    }

    @Test
    void indexesEveryTokenOfACollectionMintedWithOneOutputEach() {
        // the usual pattern: one output, one datum, one reference NFT, for each token; nothing is warned about
        List<Cip68Metadata> rows = index(
                output(flatDatum(nft("One")), refUnit(A)),
                output(flatDatum(nft("Two")), refUnit(B)),
                output(flatDatum(nft("Three")), refUnit(C)));

        assertThat(rows).extracting(Cip68Metadata::getName).containsExactlyInAnyOrder("One", "Two", "Three");
        assertThat(warnings()).isEmpty();
    }

    @Test
    void handlesANestedOutputAndSeparateOutputsInTheSameTransaction() {
        String nested = nestedDatum(Map.of(A, nft("Nested A"), B, nft("Nested B")));

        List<Cip68Metadata> rows = index(
                output(nested, refUnit(A), refUnit(B)),
                output(flatDatum(nft("Separate")), refUnit(C)));

        assertThat(rows).extracting(Cip68Metadata::getName)
                .containsExactlyInAnyOrder("Nested A", "Nested B", "Separate");
    }

    // ---------- helpers ----------

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static String refUnit(String baseHex) {
        return POLICY + REF + baseHex;
    }

    /** An output with the given datum that holds the given assets, all with quantity 1. */
    private static AddressUtxo output(String datum, String... units) {
        List<Amt> amounts = new ArrayList<>();
        for (String unit : units) {
            amounts.add(Amt.builder().unit(unit).quantity(BigInteger.ONE).build());
        }
        return AddressUtxo.builder().txHash(TX_HASH).inlineDatum(datum).amounts(amounts).build();
    }

    /** Indexes one transaction with these outputs and returns the saved rows (none if nothing was saved). */
    private List<Cip68Metadata> index(AddressUtxo... outputs) {
        List<AddressUtxo> list = new ArrayList<>();
        for (int i = 0; i < outputs.length; i++) {
            AddressUtxo o = outputs[i];
            list.add(AddressUtxo.builder().txHash(o.getTxHash()).txIndex(0).outputIndex(i)
                    .inlineDatum(o.getInlineDatum()).amounts(o.getAmounts()).build());
        }
        processor.processTransaction(AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(list).build()))
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
        try {
            verify(repository).saveAll(captor.capture());
        } catch (AssertionError notSaved) {
            verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.any());
            return List.of();
        }
        List<Cip68Metadata> rows = new ArrayList<>();
        captor.getValue().forEach(rows::add);
        return rows;
    }

    /** NFT metadata: a name, a description and an image. */
    private static MapPlutusData nft(String name) {
        MapPlutusData map = new MapPlutusData();
        map.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
        map.put(BytesPlutusData.of("description"), BytesPlutusData.of("About " + name));
        map.put(BytesPlutusData.of("image"), BytesPlutusData.of("ipfs://Qm" + name.replace(' ', '_')));
        return map;
    }

    /** {"721": {policy: {asset name without label: metadata}}} for version 4, entries in the given order. */
    private static String nestedDatum(Map<String, MapPlutusData> entriesByBaseNameHex) {
        MapPlutusData byAsset = new MapPlutusData();
        new LinkedHashMap<>(entriesByBaseNameHex).forEach((baseHex, metadata) ->
                byAsset.put(BytesPlutusData.of(HexUtil.decodeHexString(baseHex)), metadata));
        MapPlutusData byPolicy = new MapPlutusData();
        byPolicy.put(BytesPlutusData.of(HexUtil.decodeHexString(POLICY)), byAsset);
        MapPlutusData root = new MapPlutusData();
        root.put(BytesPlutusData.of("721"), byPolicy);
        return datum(root, 4);
    }

    private static String flatDatum(MapPlutusData metadata) {
        return datum(metadata, 1);
    }

    private static String datum(MapPlutusData properties, long version) {
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(version)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
