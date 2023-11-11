package com.bloxbean.cardano.yaci.store.common.genesis;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.InputStream;

public class ConwayGenesis extends GenesisFile{

    public ConwayGenesis(File file) {
        super(file);
    }

    public ConwayGenesis(InputStream is) {
        super(is);
    }

    public ConwayGenesis(long protocolMagic) {
        super(protocolMagic);
    }

    @Override
    protected void readGenesisData(JsonNode genesisJson) {

    }

    @Override
    protected String getFileName() {
        return "conway-genesis.json";
    }
}
