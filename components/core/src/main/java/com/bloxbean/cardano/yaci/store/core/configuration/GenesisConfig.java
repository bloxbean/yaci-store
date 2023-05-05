package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.exception.StoreRuntimeException;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class GenesisConfig {

    public static final int TOTAL_SECS_IN_5_DAYS_EPOCH = 432000;
    private final long mainnetStartTime = 1_506_203_091;
    private final long testnetStartTime = 1_564_020_236;
    private final long preprodStartTime = 1_654_041_600;
    private final long previewStartTime = 1_666_656_000;

    private final StoreProperties storeProperties;
    private final ObjectMapper objectMapper;

    private long startTime;
    private String shelleyStartTime;

    private long byronSlotLength;
    private double shelleySlotLength;

    private double activeSlotsCoeff;
    private BigInteger maxLovelaceSupply = BigInteger.valueOf(45000000000000000L);

    public GenesisConfig(StoreProperties storeProperties, ObjectMapper objectMapper) {
        this.storeProperties = storeProperties;
        this.objectMapper = objectMapper;

        parseGenesisFiles();
    }

    public double slotDuration(Era era) {
        if (era == Era.Byron) {
            if (byronSlotLength == 0)
                return 20; //20 sec
            else
                return byronSlotLength;
        } else {
            if (shelleySlotLength == 0)
                return 1; //1 sec
            else
                return shelleySlotLength;
        }
    }

    public long slotsPerEpoch(Era era) {
        return  (long)(TOTAL_SECS_IN_5_DAYS_EPOCH / slotDuration(era));
    }

    public long getStartTime(long protocolMagic) {
        if (startTime > 0)
            return startTime;

        NetworkType networkType = NetworkType.fromProtocolMagic(protocolMagic);
        if (networkType == NetworkType.MAINNET) {
            return mainnetStartTime;
        } else if (networkType == NetworkType.LEGACY_TESTNET) {
            return testnetStartTime;
        } else if (networkType == NetworkType.PREPROD) {
            return preprodStartTime;
        } else if (networkType == NetworkType.PREVIEW) {
            return previewStartTime;
        }

        return 0;
    }

    public long absoluteSlot(Era era, long epoch, long slotInEpoch) {
        return (slotsPerEpoch(era) * epoch) + slotInEpoch;
    }

    public BigInteger getMaxLovelaceSupply() {
        return maxLovelaceSupply;
    }

    public void parseGenesisFiles() {
        if (!StringUtil.isEmpty(storeProperties.getByronGenesisFile())) {
            //parse byron genesis file
            parseByronGenesisFile(storeProperties.getByronGenesisFile());
        }

        if (!StringUtil.isEmpty(storeProperties.getShelleyGenesisFile())) {
            //parse shelley genesis file
            parseShelleyGenesisFile(storeProperties.getShelleyGenesisFile());
        }
    }

    private void parseByronGenesisFile(String byronGenesisFile) {
        ObjectNode byronJsonNode = parseJson(byronGenesisFile);
        startTime = byronJsonNode.get("startTime").asLong();
        byronSlotLength = byronJsonNode.get("blockVersionData").get("slotDuration").asLong() / 1000; //in second

        long protocolMagic = byronJsonNode.get("protocolConsts").get("protocolMagic").asLong();
        if (protocolMagic != storeProperties.getProtocolMagic())
            throw new StoreRuntimeException("Protocol magic mismatch. Expected : " + storeProperties.getProtocolMagic() +
                    ", found in byron genesis file : " + protocolMagic);
    }

    private void parseShelleyGenesisFile(String shelleyGenesisFile) {
        ObjectNode shelleyJsonNode = parseJson(shelleyGenesisFile);
        shelleyStartTime = shelleyJsonNode.get("systemStart").asText();
        shelleySlotLength = shelleyJsonNode.get("slotLength").asDouble();
        activeSlotsCoeff = shelleyJsonNode.get("activeSlotsCoeff").asDouble();
        maxLovelaceSupply = new BigInteger(shelleyJsonNode.get("maxLovelaceSupply").asText());

        long networkMagic = shelleyJsonNode.get("networkMagic").asLong();
        if (networkMagic != storeProperties.getProtocolMagic())
            throw new StoreRuntimeException("Protocol magic mismatch. Expected : " + storeProperties.getProtocolMagic() +
                    ", found in shelley genesis file : " + networkMagic);
    }

    private ObjectNode parseJson(String genesisFile) {
        Path path = Paths.get(genesisFile);
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("Genesis file not found at " + genesisFile);
        }

        ObjectNode jsonNode;
        try {
            jsonNode = (ObjectNode)objectMapper.readTree(path.toFile());
        } catch (IOException e) {
            throw new StoreRuntimeException("Error parsing genesis file : " + genesisFile, e);
        }
        return jsonNode;
    }

}
