package com.bloxbean.cardano.yaci.store.core.configuration;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DevkitGenesisFetcher {

    public static GenesisFiles fetchDevkitGenesisFiles(String adminBaseUrl) throws Exception {
        String genesisDownloadUrl = adminBaseUrl + "/local-cluster/api/admin/devnet/genesis/download";

        // Use the above url to download the genesis zip file. Extract it to a temp folder and return
        try (InputStream inputStream = new URL(genesisDownloadUrl).openStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            Path tempDir = Files.createTempDirectory("genesisFiles");
            ZipEntry entry;

            String byronGenesis = null;
            String shelleyGenesis = null;
            String alonzoGenesis = null;
            String conwayGenesis = null;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(entry.getName());
                Files.copy(zipInputStream, filePath);

                switch (entry.getName()) {
                    case "byron-genesis.json" -> byronGenesis = filePath.toString();
                    case "shelley-genesis.json" -> shelleyGenesis = filePath.toString();
                    case "alonzo-genesis.json" -> alonzoGenesis = filePath.toString();
                    case "conway-genesis.json" -> conwayGenesis = filePath.toString();
                }
            }

            if (byronGenesis == null || shelleyGenesis == null || alonzoGenesis == null || conwayGenesis == null) {
                throw new IllegalStateException("One or more genesis files are missing in the zip file");
            }

            return new GenesisFiles(byronGenesis, shelleyGenesis, alonzoGenesis, conwayGenesis);
        }
    }

    public record GenesisFiles(String byronGenesis, String shelleyGenesis, String alonzoGenesis, String conwayGenesis){}
}
