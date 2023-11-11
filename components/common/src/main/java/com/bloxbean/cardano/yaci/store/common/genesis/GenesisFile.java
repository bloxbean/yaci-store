package com.bloxbean.cardano.yaci.store.common.genesis;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.exception.StoreRuntimeException;
import com.bloxbean.cardano.yaci.store.common.genesis.util.GenesisFileUtil;
import com.bloxbean.cardano.yaci.store.common.util.FileUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public abstract class GenesisFile {
    public static final String GENESIS_RESOURCE_FOLDER = "store/networks/";
    protected ProtocolParams protocolParams;

    private ObjectMapper objectMapper = new ObjectMapper();

    public GenesisFile() {
    }

    public GenesisFile(File genesisFile) {
        log.info("Loading genesis file {}", genesisFile.getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(genesisFile)) {
            var jsonNode = parseGenesisFile(fis);
            readGenesisData(jsonNode);
        } catch (IOException e) {
            throw new StoreRuntimeException("Shelley genesis file not found at path : " + genesisFile);
        }
    }

    public GenesisFile(InputStream is) {
        var jsonNode = parseGenesisFile(is);
        readGenesisData(jsonNode);
    }

    public GenesisFile(long protocolMagic) {
        String networkFolder = GenesisFileUtil.getGenesisfileDefaultFolder(protocolMagic);
        if (networkFolder == null) {
            throw new StoreRuntimeException("Genesis files for this network is not found. " +
                    "Please configure through store.cardano.<era>-genesis-file properties.");
        }

        log.info("Loading default genesis files for network : {}", networkFolder);

        InputStream is = FileUtil.class.getClassLoader().getResourceAsStream(GENESIS_RESOURCE_FOLDER + networkFolder + "/" + getFileName());
        var jsonNode = parseGenesisFile(is);
        readGenesisData(jsonNode);
    }

    protected abstract void readGenesisData(JsonNode genesisJson);
    protected abstract String getFileName();

    public ProtocolParams getProtocolParams() {
        return protocolParams;
    }

    private JsonNode parseGenesisFile(InputStream is) {
        return parseJson(is);
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
