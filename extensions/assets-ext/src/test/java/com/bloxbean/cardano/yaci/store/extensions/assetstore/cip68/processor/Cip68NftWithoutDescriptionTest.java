package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.processor;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * CIP-68 declares {@code ? description} for the 222 NFT and the 444 RFT, but {@code description}
 * (no {@code ?}) for the 333 FT. The datums below are real mainnet NFT datums with a name and an
 * image but no description; they used to be skipped, because every label was required to have one.
 * <p>
 * Runs the real parser, token service and processor; only the repository is mocked.
 */
class Cip68NftWithoutDescriptionTest {

    private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String BASE_NAME = "4e4654";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    /** Two picked at random from the mainnet datums that have an image but no description. */
    private static final Map<String, String> NFT_DATUMS_WITHOUT_DESCRIPTION = Map.of(
            "d87982a6446e616d65581854686520666f72676f7474656e20736f63696574792023364566696c657381a34373726358"
                    + "35697066733a2f2f516d54676d553238664156685a77556f66754545744e5666317150675779514b6d46345431397359"
                    + "4c4c67646157446e616d65581854686520666f72676f7474656e20736f6369657479202336496d656469615479706549"
                    + "696d6167652f706e6745696d6167655835697066733a2f2f516d54676d553238664156685a77556f66754545744e5666"
                    + "317150675779514b6d463454313973594c4c67646157476d616e616765724763726561746f72496d6564696154797065"
                    + "49696d6167652f706e674c636f6e747261637444617461d879860080d87980581cf5340f8bf1b4c3a7da72f3be0dc2ca"
                    + "daadb8fc088a944232a805f53dd87a80d87a8001",
            "The forgotten society #6",

            "d8799fa44a61747472696275746573d8799fae43456172444e6f6e654348617448526963652048617444426f64794a46"
                    + "6c6f70707920456172444e65636b444e6f6e6545436f6c6f724b4c696768742042726f776e454d6f75746846546f6e67"
                    + "7565464579656c6964444c617a7947436172726965724359657347436c6f7468657345506c61696e4745796562616c6c"
                    + "4343617449426c6f6f6474797065424f2b4a4261636b67726f756e64484c6176656e6465724b5370656369616c204675"
                    + "72444e6f6e654d457965204163636573736f7279444e6f6e6501ff4a636f6c6c656374696f6e58184c617a79204c6c61"
                    + "6d6173202d204d7574616e747320563245696d6167655835697066733a2f2f516d57387332755269694c6b52716e4b42"
                    + "536e536f56543933326432516973367741473853634676376d69425a39446e616d6550526f626f204c6c616d61202333"
                    + "34333401ff",
            "Robo Llama #3434");

    /** Datum from bloxbean/yaci-store#1159: no description, and `image` is an empty byte string. */
    private static final String NFT_NO_DESCRIPTION_EMPTY_IMAGE =
            "d87982a3446e616d65464e465420233145696d616765404c636f6e747261637444617461d879860181581cdc9acfee35"
                    + "243d123e8f10bc58692a6bc5aa3135c7eafc2aac9daafcd87a80581c9abc17656a6d1c24688292777c18c1ce599845a5"
                    + "88f4d893c1884da2d87a80d87a8001";

    private Cip68MetadataRepository repository;
    private Cip68Processor processor;

    @BeforeEach
    void setUp() {
        repository = mock(Cip68MetadataRepository.class);
        processor = new Cip68Processor(new Cip68TokenService(repository), new Cip68DatumParser(), repository);
    }

    @Test
    void savesNftsWithoutDescription() {
        NFT_DATUMS_WITHOUT_DESCRIPTION.forEach((datum, name) -> {
            org.mockito.Mockito.clearInvocations(repository);

            processor.processTransaction(event(datum, "000de140"));

            Cip68Metadata saved = savedRow();
            assertThat(saved.getName()).as(name).isEqualTo(name);
            assertThat(saved.getLabel()).as(name).isEqualTo(Cip68Constants.LABEL_NFT);
            assertThat(saved.getDescription()).as(name).isNull();
            assertThat(saved.getDatum()).as(name).isEqualTo(datum);
        });
    }

    @Test
    void dropsAnNftWhoseImageIsEmpty() {
        // no description is fine for an NFT, but CIP-68 requires an image and this datum's is empty
        processor.processTransaction(event(NFT_NO_DESCRIPTION_EMPTY_IMAGE, "000de140"));

        verify(repository, never()).saveAll(any());
    }

    @Test
    void savesRichFungibleTokensWithoutDescription() {
        String datum = NFT_DATUMS_WITHOUT_DESCRIPTION.keySet().iterator().next();

        processor.processTransaction(event(datum, "001bc280"));

        Cip68Metadata saved = savedRow();
        assertThat(saved.getLabel()).isEqualTo(Cip68Constants.LABEL_RFT);
        assertThat(saved.getDescription()).isNull();
    }

    @Test
    void stillSkipsFungibleTokensWithoutDescription() {
        // 333 requires a description, so the same datum is dropped when it belongs to an FT
        NFT_DATUMS_WITHOUT_DESCRIPTION.forEach((datum, name) -> {
            processor.processTransaction(event(datum, "0014df10"));

            verify(repository, never()).saveAll(any());
        });
    }

    @Test
    void savesAnNftShapedReferenceNftWithoutCoMintedUserTokenAsNft() {
        // No user token in the transaction: the label is inferred from the datum. This one has an image and
        // no ticker or logo, so it is an NFT, and an NFT does not need a description (before: 333, dropped)
        String datum = NFT_DATUMS_WITHOUT_DESCRIPTION.keySet().iterator().next();

        processor.processTransaction(event(datum, null));

        assertThat(savedRow().getLabel()).isEqualTo(Cip68Constants.LABEL_NFT);
    }

    private Cip68Metadata savedRow() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Cip68Metadata>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<Cip68Metadata> rows = new java.util.ArrayList<>();
        captor.getValue().forEach(rows::add);
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }

    /** A transaction with the reference NFT (with the datum) and, optionally, a co-minted user token. */
    private AddressUtxoEvent event(String datum, String userTokenPrefix) {
        AddressUtxo refNft = AddressUtxo.builder()
                .txHash(TX_HASH).txIndex(0).inlineDatum(datum)
                .amounts(List.of(amount("000643b0" + BASE_NAME)))
                .build();
        List<AddressUtxo> outputs = userTokenPrefix == null
                ? List.of(refNft)
                : List.of(refNft, AddressUtxo.builder().txHash(TX_HASH).txIndex(1)
                        .amounts(List.of(amount(userTokenPrefix + BASE_NAME))).build());
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(outputs).build()))
                .build();
    }

    private static Amt amount(String assetName) {
        return Amt.builder().unit(POLICY_ID + assetName).quantity(BigInteger.ONE).build();
    }
}
