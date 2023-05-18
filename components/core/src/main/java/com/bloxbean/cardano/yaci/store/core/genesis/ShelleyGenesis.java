package com.bloxbean.cardano.yaci.store.core.genesis;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.exception.StoreRuntimeException;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.ToString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class ShelleyGenesis {
    public static final String ATTR_SYSTEM_START = "systemStart";
    public static final String ATTR_SLOT_LENGTH = "slotLength";
    public static final String ATTR_ACTIVE_SLOTS_COEFF = "activeSlotsCoeff";
    public static final String ATTR_MAX_LOVELACE_SUPPLY = "maxLovelaceSupply";
    public static final String ATTR_EPOCH_LENGTH = "epochLength";
    public static final String ATTR_NETWORK_MAGIC = "networkMagic";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private String systemStart;
    private double slotLength;
    private double activeSlotsCoeff;
    private BigInteger maxLovelaceSupply;
    private long epochLength;
    private long networkMagic;

    private List<GenesisBalance> initialFunds = new ArrayList<>();

    public ShelleyGenesis(File shelleyGenesisFile) {
        try (FileInputStream fis = new FileInputStream(shelleyGenesisFile)) {
            parseShelleyGenesisFile(fis);
        } catch (IOException e) {
            throw new StoreRuntimeException("Shelley genesis file not found at path : " + shelleyGenesisFile);
        }
    }

    public ShelleyGenesis(InputStream is) {
        parseShelleyGenesisFile(is);
    }

    private void parseShelleyGenesisFile(InputStream is) {
        JsonNode shelleyJsonNode = parseJson(is);
        systemStart = shelleyJsonNode.get(ATTR_SYSTEM_START).asText();
        slotLength = shelleyJsonNode.get(ATTR_SLOT_LENGTH).asDouble();
        activeSlotsCoeff = shelleyJsonNode.get(ATTR_ACTIVE_SLOTS_COEFF).asDouble();
        maxLovelaceSupply = new BigInteger(shelleyJsonNode.get(ATTR_MAX_LOVELACE_SUPPLY).asText());
        epochLength = shelleyJsonNode.get(ATTR_EPOCH_LENGTH).asLong();
        networkMagic = shelleyJsonNode.get(ATTR_NETWORK_MAGIC).asLong();

        JsonNode initialFundJson = shelleyJsonNode.get("initialFunds");
        if (initialFundJson != null && initialFundJson.fields().hasNext()) {
            initialFundJson.fields().forEachRemaining(entry -> {
                String address = entry.getKey();
                BigInteger amount = new BigInteger(entry.getValue().asText());

                String shelleyAddr;
                if (Networks.mainnet().getProtocolMagic() == networkMagic) { //mainnet address
                    shelleyAddr = new Address("addr", HexUtil.decodeHexString(address)).toBech32();
                } else {
                    shelleyAddr = new Address("addr_test", HexUtil.decodeHexString(address)).toBech32();
                }

                String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(HexUtil.decodeHexString(address)));
                initialFunds.add(new GenesisBalance(shelleyAddr, txHash, amount));
            });
        }
    }

    private ObjectNode parseJson(InputStream is) {
        ObjectNode jsonNode;
        try {
            jsonNode = (ObjectNode) objectMapper.readTree(is);
        } catch (IOException e) {
            throw new StoreRuntimeException("Error parsing shelley genesis file", e);
        }
        return jsonNode;
    }

}
