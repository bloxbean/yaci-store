package com.bloxbean.cardano.yaci.store.core.configuration;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.NetworkType;
import com.bloxbean.cardano.yaci.store.common.exception.StoreRuntimeException;
import com.bloxbean.cardano.yaci.store.common.genesis.ByronGenesis;
import com.bloxbean.cardano.yaci.store.common.genesis.ShelleyGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisStaking;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Component
@Getter
@Slf4j
public class GenesisConfig {
    public static final int PREVIEW_EPOCH_LENGTH = 86400;
    public static final int DEFAULT_SECURITY_PARAM = 2160;
    private final long DEFAULT_EPOCH_LENGTH = 432000; //5 days

    private final long mainnetStartTime = 1_506_203_091;
    private final long testnetStartTime = 1_564_020_236;
    private final long preprodStartTime = 1_654_041_600;
    private final long previewStartTime = 1_666_656_000;

    private final StoreProperties storeProperties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private long startTime;
    private String shelleyStartTime;
    private long epochLength;
    private long byronEpochLength;

    private long byronSlotLength;
    private double shelleySlotLength;

    private double activeSlotsCoeff;
    private int securityParam;
    private BigInteger maxLovelaceSupply = BigInteger.valueOf(45000000000000000L);

    public GenesisConfig(StoreProperties storeProperties, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.storeProperties = storeProperties;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;

        downloadGenesisFilesIfDevkitNode();
        parseGenesisFiles();
    }

    private void downloadGenesisFilesIfDevkitNode() {
        if (!storeProperties.isDevkitNode())
            return;

        if (storeProperties.getByronGenesisFile() != null
                || storeProperties.getShelleyGenesisFile() != null
                || storeProperties.getAlonzoGenesisFile() != null
                || storeProperties.getConwayGenesisFile() != null) {

            throw new IllegalStateException("You can't set both `store.cardano.devkit-node=true` and the genesis file locations");
        }

        log.info("Trying to download genesis files from Yaci DevKit's admin endpoint...");
        try {
            String defaultDevkitAdminBaseUrl = "http://localhost:10000";
            var devkitGenesisFiles = DevkitGenesisFetcher.fetchDevkitGenesisFiles(defaultDevkitAdminBaseUrl);
            storeProperties.setByronGenesisFile(devkitGenesisFiles.byronGenesis());
            storeProperties.setShelleyGenesisFile(devkitGenesisFiles.shelleyGenesis());
            storeProperties.setAlonzoGenesisFile(devkitGenesisFiles.alonzoGenesis());
            storeProperties.setConwayGenesisFile(devkitGenesisFiles.conwayGenesis());

            log.info("<<< Genesis file locations are configured as follows >>>");
            log.info("Byron Genesis file: {}", storeProperties.getByronGenesisFile());
            log.info("Shelley Genesis file: {}", storeProperties.getShelleyGenesisFile());
            log.info("Alonzo Genesis file: {}", storeProperties.getAlonzoGenesisFile());
            log.info("Conway Genesis file: {}", storeProperties.getConwayGenesisFile());
        } catch (Exception e) {
            throw new IllegalStateException("The Devkit node's genesis files could not be fetched. Please configure them manually.", e);
        }
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
        if (era == Era.Byron)
            return byronEpochLength;
        else
            return getEpochLength();
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
        parseByronGenesisFile(storeProperties.getByronGenesisFile());
        parseShelleyGenesisFile(storeProperties.getShelleyGenesisFile());
    }

    private void parseByronGenesisFile(String byronGenesisFile) {
        ByronGenesis byronGenesis;
        if (!StringUtil.isEmpty(byronGenesisFile))
            byronGenesis = getByronGenesis(byronGenesisFile);
        else {
            byronGenesis = new ByronGenesis(storeProperties.getProtocolMagic());
        }

        startTime = byronGenesis.getStartTime();
        byronSlotLength = byronGenesis.getByronSlotLength(); //in second
        byronEpochLength = byronGenesis.getEpochLength();
        long protocolMagic = byronGenesis.getProtocolMagic();

        if (protocolMagic != storeProperties.getProtocolMagic())
            throw new StoreRuntimeException("Protocol magic mismatch. Expected : " + storeProperties.getProtocolMagic() +
                    ", found in byron genesis file : " + protocolMagic + ", genesis file : " + byronGenesisFile);
    }

    private void parseShelleyGenesisFile(String shelleyGenesisFile) {
        ShelleyGenesis shelleyGenesis;

        if (!StringUtil.isEmpty(shelleyGenesisFile))
            shelleyGenesis = getShelleyGenesis(shelleyGenesisFile);
        else
            shelleyGenesis = new ShelleyGenesis(storeProperties.getProtocolMagic());

        shelleyStartTime = shelleyGenesis.getSystemStart();
        shelleySlotLength = shelleyGenesis.getSlotLength();
        activeSlotsCoeff = shelleyGenesis.getActiveSlotsCoeff();
        securityParam = shelleyGenesis.getSecurityParam();
        maxLovelaceSupply = shelleyGenesis.getMaxLovelaceSupply();
        epochLength = shelleyGenesis.getEpochLength();

        long networkMagic = shelleyGenesis.getNetworkMagic();
        if (networkMagic != storeProperties.getProtocolMagic())
            throw new StoreRuntimeException("Protocol magic mismatch. Expected : " + storeProperties.getProtocolMagic() +
                    ", found in shelley genesis file : " + networkMagic + ", genesis file : " + shelleyGenesisFile);
    }

    public List<GenesisBalance> getGenesisBalances() {
        //Parsing on-demand, as we don't want to keep the balances in memory
        List<GenesisBalance> genesisBalances = new ArrayList<>();

        //Byron
        ByronGenesis byronGenesis;
        if (!StringUtil.isEmpty(storeProperties.getByronGenesisFile())) {
            byronGenesis = getByronGenesis(storeProperties.getByronGenesisFile());
        } else {
            byronGenesis = new ByronGenesis(storeProperties.getProtocolMagic());
        }
        if (byronGenesis.getAvvmGenesisBalances() != null && byronGenesis.getAvvmGenesisBalances().size() > 0)
            genesisBalances.addAll(byronGenesis.getAvvmGenesisBalances());
        if (byronGenesis.getNonAvvmGenesisBalances() != null && byronGenesis.getNonAvvmBalances().size() > 0)
            genesisBalances.addAll(byronGenesis.getNonAvvmGenesisBalances());


        //Shelley
        ShelleyGenesis shelleyGenesis;
        if (!StringUtil.isEmpty(storeProperties.getShelleyGenesisFile())) {
            shelleyGenesis = getShelleyGenesis(storeProperties.getShelleyGenesisFile());
        } else {
            shelleyGenesis = new ShelleyGenesis(storeProperties.getProtocolMagic());
        }

        if (shelleyGenesis.getInitialFunds() != null && shelleyGenesis.getInitialFunds().size() > 0)
            genesisBalances.addAll(shelleyGenesis.getInitialFunds());

        return genesisBalances;
    }

    //Only set for devnets
    public GenesisStaking getGenesisStaking() {
        ShelleyGenesis shelleyGenesis;
        if (!StringUtil.isEmpty(storeProperties.getShelleyGenesisFile())) {
            shelleyGenesis = getShelleyGenesis(storeProperties.getShelleyGenesisFile());
        } else {
            shelleyGenesis = new ShelleyGenesis(storeProperties.getProtocolMagic());
        }

        return shelleyGenesis.getGenesisStaking();
    }

    public long getRandomnessStabilisationWindow() {
        return Math.round((4 * securityParam) / activeSlotsCoeff);
    }

    @SneakyThrows
    private ByronGenesis getByronGenesis(String byronGenesisFile) {
        ByronGenesis byronGenesis;
        if (byronGenesisFile.startsWith("classpath:")) {
            byronGenesis = new ByronGenesis(resourceLoader.getResource(byronGenesisFile).getInputStream());
        } else {
            byronGenesis = new ByronGenesis(new File(byronGenesisFile));
        }

        return byronGenesis;
    }

    @SneakyThrows
    private ShelleyGenesis getShelleyGenesis(String shelleyGenesisFile) {
        ShelleyGenesis shelleyGenesis;
        if (shelleyGenesisFile.startsWith("classpath:")) {
            shelleyGenesis = new ShelleyGenesis(resourceLoader.getResource(shelleyGenesisFile).getInputStream());
        } else {
            shelleyGenesis = new ShelleyGenesis(new File(shelleyGenesisFile));
        }

        return shelleyGenesis;
    }

}
