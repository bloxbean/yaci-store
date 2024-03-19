package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.client.crypto.Base58;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.exception.StoreRuntimeException;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
@EqualsAndHashCode(callSuper = false)
public class ByronGenesis extends GenesisFile {
    public static final String ATTR_START_TIME = "startTime";
    public static final String ATTR_BLOCK_VERSION_DATA = "blockVersionData";
    public static final String ATTR_SLOT_DURATION = "slotDuration";
    public static final String ATTR_PROTOCOL_CONSTS = "protocolConsts";
    public static final String ATTR_PROTOCOL_MAGIC = "protocolMagic";
    public static final String ATTR_AVVM_DISTR = "avvmDistr";
    public static final String ATTR_NON_AVVM_BALANCES = "nonAvvmBalances";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, BigInteger> avvmDistr;
    private Map<String, BigInteger> nonAvvmBalances;
    private List<GenesisBalance> avvmGenesisBalances;
    private List<GenesisBalance> nonAvvmGenesisBalances;
    private long startTime;
    private long byronSlotLength;
    private long protocolMagic;

    public ByronGenesis(File byronGenesisFile) {
        super(byronGenesisFile);
    }

    public ByronGenesis(InputStream in) {
        super(in);
    }

    public ByronGenesis(long protocolMagic) {
        super(protocolMagic);
    }

    public long getByronSlotLength() {
        if (byronSlotLength == 0)
            return 0;
        else
            return byronSlotLength / 1000;
    }

    protected void readGenesisData(JsonNode byronJsonNode) {
        startTime = byronJsonNode.get(ATTR_START_TIME).asLong();
        byronSlotLength = byronJsonNode.get(ATTR_BLOCK_VERSION_DATA).get(ATTR_SLOT_DURATION).asLong() / 1000; //in second
        protocolMagic = byronJsonNode.get(ATTR_PROTOCOL_CONSTS).get(ATTR_PROTOCOL_MAGIC).asLong();

        JsonNode avvmDistrMap = byronJsonNode.get(ATTR_AVVM_DISTR);
        if (avvmDistrMap != null && avvmDistrMap.fields().hasNext()) {
            this.avvmDistr = convertAvvmDistribution(avvmDistrMap);
            this.avvmGenesisBalances = convertAvvmGenesisBalances(avvmDistr);
        } else {
            this.avvmDistr = new HashMap<>();
            this.avvmGenesisBalances = new ArrayList<>();
        }

        JsonNode nonAvvmBalancesMap = byronJsonNode.get(ATTR_NON_AVVM_BALANCES);
        if (nonAvvmBalancesMap != null && nonAvvmBalancesMap.fields().hasNext()) {
            this.nonAvvmBalances = convertNonAvvmBalances(nonAvvmBalancesMap);
            this.nonAvvmGenesisBalances = convertNonAvvmGenesisBalances(nonAvvmBalances);
        } else {
            this.nonAvvmBalances = new HashMap<>();
            this.nonAvvmGenesisBalances = new ArrayList<>();
        }
    }

    @Override
    protected String getFileName() {
        return "byron-genesis.json";
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

}
