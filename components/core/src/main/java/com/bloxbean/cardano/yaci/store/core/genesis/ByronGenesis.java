package com.bloxbean.cardano.yaci.store.core.genesis;

import com.bloxbean.cardano.client.crypto.Base58;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class ByronGenesis {
    public static final String ATTR_START_TIME = "startTime";
    public static final String ATTR_BLOCK_VERSION_DATA = "blockVersionData";
    public static final String ATTR_SLOT_DURATION = "slotDuration";
    public static final String ATTR_PROTOCOL_CONSTS = "protocolConsts";
    public static final String ATTR_PROTOCOL_MAGIC = "protocolMagic";
    public static final String ATTR_AVVM_DISTR = "avvmDistr";
    public static final String ATTR_NON_AVVM_BALANCES = "nonAvvmBalances";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, BigInteger> avvmDistr;
    private Map<String, BigInteger> nonAvvmBalances = new HashMap<>();
    private List<GenesisBalance> avvmGenesisBalances = new ArrayList<>();
    private List<GenesisBalance> nonAvvmGenesisBalances = new ArrayList<>();
    private long startTime;
    private long byronSlotLength;
    private long protocolMagic;

    public ByronGenesis(File byronGenesisFile) {
        try (FileInputStream fis = new FileInputStream(byronGenesisFile)) {
            parseByronGenesisFile(fis);
        } catch (IOException e) {
            throw new StoreRuntimeException("Byron genesis file not found at path : " + byronGenesisFile);
        }
    }

    public ByronGenesis(InputStream is) {
        parseByronGenesisFile(is);
    }

    public long getByronSlotLength() {
        if (byronSlotLength == 0)
            return 0;
        else
            return byronSlotLength / 1000;
    }

    private void parseByronGenesisFile(InputStream is) {
        ObjectNode byronJsonNode = parseJson(is);
        startTime = byronJsonNode.get(ATTR_START_TIME).asLong();
        byronSlotLength = byronJsonNode.get(ATTR_BLOCK_VERSION_DATA).get(ATTR_SLOT_DURATION).asLong() / 1000; //in second
        protocolMagic = byronJsonNode.get(ATTR_PROTOCOL_CONSTS).get(ATTR_PROTOCOL_MAGIC).asLong();

        JsonNode avvmDistrMap = byronJsonNode.get(ATTR_AVVM_DISTR);
        if (avvmDistrMap != null && avvmDistrMap.fields().hasNext()) {
            avvmDistr = convertAvvmDistribution(avvmDistrMap);
            avvmGenesisBalances = convertAvvmGenesisBalances(avvmDistr);
        }

        JsonNode nonAvvmBalancesMap = byronJsonNode.get(ATTR_NON_AVVM_BALANCES);
        if (nonAvvmBalancesMap != null && nonAvvmBalancesMap.fields().hasNext()) {
            nonAvvmBalances = convertNonAvvmBalances(nonAvvmBalancesMap);
            nonAvvmGenesisBalances = convertNonAvvmGenesisBalances(nonAvvmBalances);
        }
    }

    private Map<String, BigInteger> convertAvvmDistribution(JsonNode avvmDistrMap) {
        Map<String, BigInteger> _avvmDistr = new java.util.HashMap<>();
        avvmDistrMap.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            BigInteger value = new BigInteger(entry.getValue().asText());
            _avvmDistr.put(key, value);
        });

        return _avvmDistr;
    }

    private Map<String, BigInteger> convertNonAvvmBalances(JsonNode nonAvvmBalancesMap) {
        Map<String, BigInteger> _nonAvvmBalances = new HashMap<>();
        nonAvvmBalancesMap.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            BigInteger value = new BigInteger(entry.getValue().asText());
            _nonAvvmBalances.put(key, value);
        });

        return _nonAvvmBalances;
    }

    private List<GenesisBalance> convertAvvmGenesisBalances(Map<String, BigInteger> avvmBalances) {
        List<GenesisBalance> avvmGenesisBalances = new ArrayList<>();
        avvmBalances.entrySet().forEach(entry -> {
            String avvmAddress = entry.getKey();
            BigInteger value = entry.getValue();
            String byronAddress = AvvmAddressConverter.convertAvvmToByronAddress(avvmAddress)
                    .orElseThrow(() -> new StoreRuntimeException("Invalid avvm address"));
            String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(Base58.decode(byronAddress)));

            GenesisBalance genesisBalance = new GenesisBalance(byronAddress, txHash, value);
            avvmGenesisBalances.add(genesisBalance);
        });

        return avvmGenesisBalances;
    }

    private List<GenesisBalance> convertNonAvvmGenesisBalances(Map<String, BigInteger> nonAvvmBalances) {
        List<GenesisBalance> nonAvvmGenesisBalances = new ArrayList<>();
        nonAvvmBalances.entrySet().forEach(entry -> {
            String byronAddress = entry.getKey();
            BigInteger value = entry.getValue();
            String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(Base58.decode(byronAddress)));

            GenesisBalance genesisBalance = new GenesisBalance(byronAddress, txHash, value);
            nonAvvmGenesisBalances.add(genesisBalance);
        });

        return nonAvvmGenesisBalances;
    }

    private ObjectNode parseJson(InputStream is) {
        ObjectNode jsonNode;
        try {
            jsonNode = (ObjectNode) objectMapper.readTree(is);
        } catch (IOException e) {
            throw new StoreRuntimeException("Error parsing byron genesis file", e);
        }
        return jsonNode;
    }

}
