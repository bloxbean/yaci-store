package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.exception.StoreRuntimeException;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.genesis.ByronGenesis;
import com.bloxbean.cardano.yaci.store.core.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
public class GenesisConfig {
    public static final int PREVIEW_EPOCH_LENGTH = 86400;
    private final long DEFAULT_EPOCH_LENGTH = 432000; //5 days
    public final static int DEFAULT_SECURITY_PARAM = 2160;

    private final long mainnetStartTime = 1_506_203_091;
    private final long testnetStartTime = 1_564_020_236;
    private final long preprodStartTime = 1_654_041_600;
    private final long previewStartTime = 1_666_656_000;

    private final StoreProperties storeProperties;
    private final ObjectMapper objectMapper;

    private long startTime;
    private String shelleyStartTime;
    private long epochLength;

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
        return  (long)(getEpochLength() / slotDuration(era));
    }

    public long getEpochLength() {
        if (epochLength > 0)
            return epochLength;

        NetworkType networkType = NetworkType.fromProtocolMagic(storeProperties.getProtocolMagic());
        if (networkType == NetworkType.MAINNET || networkType == NetworkType.PREPROD) {
            return DEFAULT_EPOCH_LENGTH;
        } else if (networkType == NetworkType.PREVIEW) {
            return PREVIEW_EPOCH_LENGTH;
        } else {
            return DEFAULT_EPOCH_LENGTH;
        }
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
        ByronGenesis byronGenesis = new ByronGenesis(new File(byronGenesisFile));
        startTime = byronGenesis.getStartTime();
        byronSlotLength = byronGenesis.getByronSlotLength(); //in second
        long protocolMagic = byronGenesis.getProtocolMagic();

        if (protocolMagic != storeProperties.getProtocolMagic())
            throw new StoreRuntimeException("Protocol magic mismatch. Expected : " + storeProperties.getProtocolMagic() +
                    ", found in byron genesis file : " + protocolMagic);
    }

    private void parseShelleyGenesisFile(String shelleyGenesisFile) {
        ShelleyGenesis shelleyGenesis = new ShelleyGenesis(new File(shelleyGenesisFile));

        shelleyStartTime = shelleyGenesis.getSystemStart();
        shelleySlotLength = shelleyGenesis.getSlotLength();
        activeSlotsCoeff = shelleyGenesis.getActiveSlotsCoeff();
        maxLovelaceSupply = shelleyGenesis.getMaxLovelaceSupply();
        epochLength = shelleyGenesis.getEpochLength();

        long networkMagic = shelleyGenesis.getNetworkMagic();
        if (networkMagic != storeProperties.getProtocolMagic())
            throw new StoreRuntimeException("Protocol magic mismatch. Expected : " + storeProperties.getProtocolMagic() +
                    ", found in shelley genesis file : " + networkMagic);
    }

    public List<GenesisBalance> getGenesisBalances() {
        //Parsing on-demand, as we don't want to keep the balances in memory
        List<GenesisBalance> genesisBalances = new ArrayList<>();
        if (!StringUtil.isEmpty(storeProperties.getByronGenesisFile())) {
            ByronGenesis byronGenesis = new ByronGenesis(new File(storeProperties.getByronGenesisFile()));
            if (byronGenesis.getAvvmGenesisBalances() != null && byronGenesis.getAvvmGenesisBalances().size() > 0)
                genesisBalances.addAll(byronGenesis.getAvvmGenesisBalances());
            if (byronGenesis.getNonAvvmGenesisBalances() != null && byronGenesis.getNonAvvmBalances().size() > 0)
                genesisBalances.addAll(byronGenesis.getNonAvvmGenesisBalances());
        }

        if (!StringUtil.isEmpty(storeProperties.getShelleyGenesisFile())) {
          ShelleyGenesis shelleyGenesis = new ShelleyGenesis(new File(storeProperties.getShelleyGenesisFile()));

          if (shelleyGenesis.getInitialFunds() != null && shelleyGenesis.getInitialFunds().size() > 0)
              genesisBalances.addAll(shelleyGenesis.getInitialFunds());
        }

        return genesisBalances;
    }

}
