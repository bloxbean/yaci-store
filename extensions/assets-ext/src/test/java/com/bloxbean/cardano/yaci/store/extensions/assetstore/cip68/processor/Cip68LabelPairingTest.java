package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The label of a reference NFT comes from the user token paired with it: the same policy and the
 * same base name (CIP-68: reference NFT {@code 000643b0 + base}, user token {@code <label> + base}).
 * A CIP-68 token of another policy that merely sits in the same transaction must not change it.
 * <p>
 * The two datums are real mainnet reference-NFT datums. The transactions are modelled on the real mints
 * (read from Koios): FLDT was minted next to its own 333 token and eight unrelated 222 NFTs of another
 * policy; Wrapped SILVER's reference NFT was minted in a platform transaction with unrelated 222 NFTs and no
 * user token of its own. Both used to be labelled 222. Runs the real parser, token service and processor;
 * only the repository is mocked.
 */
class Cip68LabelPairingTest {

    private static final String NFT = Cip68Constants.NFT_TOKEN_PREFIX;
    private static final String FT = Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
    private static final String RFT = Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX;
    private static final String REF = Cip68Constants.REFERENCE_TOKEN_PREFIX;

    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    /** Another project's policy: its CIP-68 NFTs are in the same transactions below. */
    private static final String OTHER_POLICY = "f0ff48bbb7" + "a".repeat(46);

    // Real reference-NFT datum of FLDT, policy 577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e, asset name 000643b0464c4454
    private static final String FLDT_POLICY = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e";
    private static final String FLDT_BASE = "464c4454";
    private static final String FLDT_DATUM =
            "d8799fa648646563696d616c73064b6465736372697074696f6e5f5840546865206f6666696369616c20746f6b656e20"
                    + "6f6620466c756964546f6b656e732c2061206c656164696e6720446546692065636f73797374656d206675656c582765"
                    + "6420627920696e6e6f766174696f6e20616e6420636f6d6d756e697479206261636b696e672eff446c6f676f58206874"
                    + "7470733a2f2f666c756964746f6b656e732e636f6d2f666c64742e706e67446e616d6544464c4454467469636b657244"
                    + "464c44544777656273697465581868747470733a2f2f666c756964746f6b656e732e636f6d2f0101ff";

    // Real reference-NFT datum of Wrapped SILVER, policy 2ac6f76eb65b4aeb17e4acd333dbade3ea71172a32d69da43a639d42, asset name 000643b053494c564552
    static final String SILVER_POLICY = "2ac6f76eb65b4aeb17e4acd333dbade3ea71172a32d69da43a639d42";
    static final String SILVER_BASE = "53494c564552";
    static final String SILVER_DATUM =
            "d8799fab46737570706c790044747970655153656c665265706f727465644173736574456f776e6572581c0e20e50c17"
                    + "1c50d2bd604ba3ef24f6019233a6e79b84c7d4e11bab0f4561737365744953494c564552204f5a467469636b65724653"
                    + "494c564552446e616d654e577261707065642053494c5645524b6465736372697074696f6e5821577261707065642053"
                    + "494c564552206f70657261746564206279205042472e696f48646563696d616c73064375726c4e68747470733a2f2f70"
                    + "62672e696f446c6f676f582168747470733a2f2f746f6b656e2e7062672e696f2f7277612d6c6f676f2e706e67447365"
                    + "6564d8799fd8799f582003b800a23d58fea5c1239871d7e956ac18c773622787486e8c44d1dac75cb88fff04ff01d879"
                    + "80ff";

    /** A datum for the 222 and 444 tests: those labels need an image, which the real FLDT datum (a 333 token with a logo) does not have. */
    private static final String IMAGE_DATUM = datum("name", "FLDT", "description", "A token", "image", "ipfs://Qm");

    private Cip68MetadataRepository repository;
    private Cip68Processor processor;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68MetadataRepository.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(), repository);
    }

    @Test
    void labelsFtAsFtWhenUnrelatedNftsOfAnotherPolicyShareTheTransaction() {
        // FLDT: its own 333 token (same policy, same base) next to eight unrelated 222 NFTs
        List<String> others = new ArrayList<>(unrelatedNfts(OTHER_POLICY, 8));
        others.add(FLDT_POLICY + FT + FLDT_BASE);

        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, FLDT_DATUM, others)).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void fallsBackToFtWhenOnlyUnrelatedNftsAreInTheTransaction() {
        // Wrapped SILVER: no user token of its own in this tx, six unrelated 222 NFTs of another policy
        assertThat(labelOf(SILVER_POLICY, SILVER_BASE, SILVER_DATUM, unrelatedNfts(OTHER_POLICY, 6)))
                .isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void ignoresAnNftOfTheSamePolicyWithAnotherBaseName() {
        // Same policy, but "<222> + other name" is somebody else's token, not FLDT's user token
        List<String> others = List.of(FLDT_POLICY + NFT + "4f74686572", FLDT_POLICY + FT + FLDT_BASE);

        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, FLDT_DATUM, others)).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void ignoresAnNftWithTheSameBaseNameUnderAnotherPolicy() {
        List<String> others = List.of(OTHER_POLICY + NFT + FLDT_BASE, FLDT_POLICY + FT + FLDT_BASE);

        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, FLDT_DATUM, others)).isEqualTo(Cip68Constants.LABEL_FT);
    }

    @Test
    void labelsAsNftWhenTheNftUserTokenIsPaired() {
        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, IMAGE_DATUM, List.of(FLDT_POLICY + NFT + FLDT_BASE)))
                .isEqualTo(Cip68Constants.LABEL_NFT);
    }

    @Test
    void labelsAsRftWhenTheRftUserTokenIsPaired() {
        List<String> others = new ArrayList<>(unrelatedNfts(OTHER_POLICY, 3));
        others.add(FLDT_POLICY + RFT + FLDT_BASE);

        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, IMAGE_DATUM, others)).isEqualTo(Cip68Constants.LABEL_RFT);
    }

    @Test
    void prefersNftOverFtWhenBothArePaired() {
        // Both a 222 and a 333 token with the same policy and base name: the row is stored with the first label, 222 (the
        // datum has a name, a description and an image, so it satisfies both; Cip68MultiLabelTest covers the ones that do not)
        List<String> others = List.of(FLDT_POLICY + FT + FLDT_BASE, FLDT_POLICY + NFT + FLDT_BASE);

        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, IMAGE_DATUM, others)).isEqualTo(Cip68Constants.LABEL_NFT);
    }

    @Test
    void matchesUnitsRegardlessOfHexCase() {
        List<String> others = List.of((FLDT_POLICY + NFT + FLDT_BASE).toUpperCase());

        assertThat(labelOf(FLDT_POLICY, FLDT_BASE, IMAGE_DATUM, others)).isEqualTo(Cip68Constants.LABEL_NFT);
    }

    /** Runs a transaction with the reference NFT (carrying the datum) plus one output per other unit. */
    private int labelOf(String policy, String baseName, String datum, List<String> otherUnits) {
        List<AddressUtxo> outputs = new ArrayList<>();
        outputs.add(AddressUtxo.builder().txHash(TX_HASH).txIndex(0).inlineDatum(datum)
                .amounts(List.of(amount(policy + REF + baseName))).build());
        for (int i = 0; i < otherUnits.size(); i++) {
            outputs.add(AddressUtxo.builder().txHash(TX_HASH).txIndex(i + 1)
                    .amounts(List.of(amount(otherUnits.get(i)))).build());
        }
        processor.processTransaction(AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(outputs).build()))
                .build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<Cip68Metadata> rows = new ArrayList<>();
        captor.getValue().forEach(rows::add);
        assertThat(rows).hasSize(1);
        return rows.getFirst().getLabel();
    }

    /** Distinct 222 NFTs of one policy, like a collection minted in the same transaction. */
    private static List<String> unrelatedNfts(String policy, int count) {
        return IntStream.range(0, count).mapToObj(i -> policy + NFT + "6e66742e%02x".formatted(i)).toList();
    }

    private static String datum(String... keyValues) {
        MapPlutusData properties = new MapPlutusData();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(BytesPlutusData.of(keyValues[i]), BytesPlutusData.of(keyValues[i + 1]));
        }
        try {
            return HexUtil.encodeHexString(CborSerializationUtil.serialize(
                    ConstrPlutusData.of(0, properties, BigIntPlutusData.of(1)).serialize()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Amt amount(String unit) {
        return Amt.builder().unit(unit).quantity(BigInteger.ONE).build();
    }
}
