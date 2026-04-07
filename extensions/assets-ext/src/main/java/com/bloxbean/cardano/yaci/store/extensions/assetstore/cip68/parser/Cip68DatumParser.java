package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.parser;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68DatumParser {

    public static final String DECIMALS = "decimals";
    public static final String DESCRIPTION = "description";
    public static final String LOGO = "logo";
    public static final String NAME = "name";
    public static final String TICKER = "ticker";
    public static final String URL = "url";

    /**
     * Parses a CIP-68 reference NFT inline datum.
     *
     * @param inlineDatum the hex encoded datum
     * @return the CIP-68 token metadata
     */
    public Optional<FungibleTokenMetadata> parse(String inlineDatum) {
        if (inlineDatum == null || inlineDatum.isBlank()) {
            return Optional.empty();
        }

        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            if (!(plutusData instanceof ConstrPlutusData cip68Data)) {
                return Optional.empty();
            }

            List<PlutusData> dataList = cip68Data.getData().getPlutusDataList();
            if (dataList.size() < 2 || !(dataList.getFirst() instanceof MapPlutusData properties)) {
                return Optional.empty();
            }

            if (!(dataList.get(1) instanceof BigIntPlutusData version)) {
                return Optional.empty();
            }

            return Optional.of(new FungibleTokenMetadata(
                    getNumericProperty(DECIMALS, properties).orElse(null),
                    getStringProperty(DESCRIPTION, properties).orElse(null),
                    getStringProperty(LOGO, properties).orElse(null),
                    getStringProperty(NAME, properties).orElse(null),
                    getStringProperty(TICKER, properties).orElse(null),
                    getStringProperty(URL, properties).orElse(null),
                    version.getValue().longValue()));

        } catch (Exception e) {
            log.warn("Unexpected error while parsing CIP FT Datum: {}", inlineDatum, e);
            return Optional.empty();
        }
    }

    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BytesPlutusData bytes -> Optional.of(new String(bytes.getValue()).replace("\0", ""));
            case null, default -> Optional.empty();
        };
    }

    private Optional<Long> getNumericProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        return switch (property) {
            case BigIntPlutusData bigInt -> Optional.of(bigInt.getValue().longValue());
            case null, default -> Optional.empty();
        };
    }
}
