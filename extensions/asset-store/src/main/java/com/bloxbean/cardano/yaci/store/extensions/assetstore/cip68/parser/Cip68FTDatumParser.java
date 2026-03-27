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
public class Cip68FTDatumParser {

    public static final String DECIMALS = "decimals";
    public static final String DESCRIPTION = "description";
    public static final String LOGO = "logo";
    public static final String NAME = "name";
    public static final String TICKER = "ticker";
    public static final String URL = "url";

    /**
     * Manually parses Cip68 Fungible Token Datum
     *
     * @param inlineDatum the hex encoded datum
     * @return the Cip68 Fungible Token Metadata
     */
    public Optional<FungibleTokenMetadata> parse(String inlineDatum) {

        if (inlineDatum == null || inlineDatum.trim().isEmpty()) {
            return Optional.empty();
        }

        try {

            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            if (plutusData instanceof ConstrPlutusData cip68Data) {

                List<PlutusData> dataList = cip68Data.getData().getPlutusDataList();

                if (dataList.size() < 2 || !(dataList.getFirst() instanceof MapPlutusData properties)) {
                    return Optional.empty();
                }

                Optional<Long> decimals = getNumericProperty(DECIMALS, properties);
                Optional<String> description = getStringProperty(DESCRIPTION, properties);
                Optional<String> logo = getStringProperty(LOGO, properties);
                Optional<String> name = getStringProperty(NAME, properties);
                Optional<String> ticker = getStringProperty(TICKER, properties);
                Optional<String> url = getStringProperty(URL, properties);

                PlutusData versionData = dataList.get(1);

                if (!(versionData instanceof BigIntPlutusData version)) {
                    return Optional.empty();
                }

                return Optional.of(new FungibleTokenMetadata(decimals.orElse(null),
                        description.orElse(null),
                        logo.orElse(null),
                        name.orElse(null),
                        ticker.orElse(null), url.orElse(null),
                        version.getValue().longValue()));

            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error while parsing CIP FT Datum: {}", inlineDatum, e);
            return Optional.empty();
        }

    }

    private Optional<String> getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BytesPlutusData bytes) {
            return Optional.of(new String(bytes.getValue()).replace("\0", ""));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> getNumericProperty(String propertyName, MapPlutusData mapPlutusData) {
        PlutusData property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BigIntPlutusData bigInteger) {
            return Optional.of(bigInteger.getValue().longValue());
        } else {
            return Optional.empty();
        }
    }

}
