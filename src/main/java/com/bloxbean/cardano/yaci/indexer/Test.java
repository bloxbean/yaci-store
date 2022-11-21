package com.bloxbean.cardano.yaci.indexer;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readTree(new File("/Users/satya/work/bloxbean/yaci-indexer/preprod-byron-genesis.json"));
        System.out.println(json);

        System.out.println(HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(json.toString().getBytes(StandardCharsets.UTF_8))));
    }
}
