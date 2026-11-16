package com.bloxbean.cardano.yaci.store.analytics.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogSchemasTest {

    private static final String US = "yaci/preprod";
    private static final String OTHER = "yaci/mainnet";

    @TempDir
    Path tempDir;

    @Test
    void newInstallationCreatesTheCatalogInTheConnectionSchema() throws Exception {
        FakeCatalogs db = new FakeCatalogs();
        CatalogSchemas.Resolution r = CatalogSchemas.resolve(null, "preprod", US, exportPath(), db);
        assertEquals("preprod", r.schema());
        assertFalse(r.exists());
        assertFalse(r.legacyPublic());
        assertTrue(r.claimOwner(), "a new catalog records its owner");
    }

    @Test
    void existingCatalogInPublicKeepsBeingUsed() throws Exception {
        FakeCatalogs db = new FakeCatalogs().catalog("public", null, exportPath());
        CatalogSchemas.Resolution r = CatalogSchemas.resolve(null, "preprod", US, exportPath(), db);
        assertEquals("public", r.schema());
        assertTrue(r.exists());
        assertTrue(r.legacyPublic());
        assertTrue(r.claimOwner(), "an unclaimed catalog is claimed by the first instance using it");
    }

    @Test
    void secondInstanceIsRefusedTheCatalogOfTheFirst() {
        // public catalog already claimed by the mainnet instance; preprod instance starts unset
        FakeCatalogs db = new FakeCatalogs().catalog("public", OTHER, exportPath());
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CatalogSchemas.resolve(null, "preprod", US, exportPath(), db));
        assertTrue(e.getMessage().contains("belongs to another"), e.getMessage());
        assertTrue(e.getMessage().contains("catalog-schema"), e.getMessage());
    }

    @Test
    void unclaimedPublicCatalogOfAnotherExportPathIsNotTaken() {
        FakeCatalogs db = new FakeCatalogs().catalog("public", null, tempDir.resolve("elsewhere").toString());
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CatalogSchemas.resolve(null, "preprod", US, exportPath(), db));
        assertTrue(e.getMessage().contains("catalog-schema"), e.getMessage());
    }

    @Test
    void ownSchemaWinsOverPublic() throws Exception {
        FakeCatalogs db = new FakeCatalogs()
                .catalog("public", OTHER, "/other")
                .catalog("preprod", US, exportPath());
        CatalogSchemas.Resolution r = CatalogSchemas.resolve(null, "preprod", US, exportPath(), db);
        assertEquals("preprod", r.schema());
        assertFalse(r.claimOwner());
    }

    @Test
    void configuredSchemaThatWouldOrphanTheExistingCatalogIsRefused() {
        FakeCatalogs db = new FakeCatalogs().catalog("public", US, exportPath());
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CatalogSchemas.resolve("analytics", "preprod", US, exportPath(), db));
        assertTrue(e.getMessage().contains("catalog-schema=public"), e.getMessage());
    }

    @Test
    void configuredSchemaNextToAnotherInstancesPublicCatalogIsFine() throws Exception {
        FakeCatalogs db = new FakeCatalogs().catalog("public", OTHER, "/other");
        CatalogSchemas.Resolution r = CatalogSchemas.resolve("preprod_lake", "preprod", US, exportPath(), db);
        assertEquals("preprod_lake", r.schema());
        assertFalse(r.exists());
    }

    @Test
    void configuredSchemaIsUsedAsIs() throws Exception {
        FakeCatalogs db = new FakeCatalogs().catalog("lake", US, exportPath());
        CatalogSchemas.Resolution r = CatalogSchemas.resolve(" lake ", "preprod", US, exportPath(), db);
        assertEquals("lake", r.schema());
        assertTrue(r.exists());
    }

    @Test
    void publicSourceSchemaBehavesAsBefore() throws Exception {
        FakeCatalogs db = new FakeCatalogs();
        assertEquals("public", CatalogSchemas.resolve(null, "public", "yaci/public", exportPath(), db).schema());
        assertEquals("public", CatalogSchemas.resolve(null, null, "yaci/public", exportPath(), db).schema());
    }

    @Test
    void sameDirectoryToleratesSpellingAndLinks() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("data/analytics"));
        Path link = Files.createSymbolicLink(tempDir.resolve("link"), dir);
        assertTrue(CatalogSchemas.sameDirectory(dir + "/", dir.toString()));
        assertTrue(CatalogSchemas.sameDirectory(tempDir + "/data/./x/../analytics", dir.toString()));
        assertTrue(CatalogSchemas.sameDirectory(link.toString(), dir + "/"));
        assertFalse(CatalogSchemas.sameDirectory(tempDir.resolve("data").toString(), dir.toString()));
        assertFalse(CatalogSchemas.sameDirectory(null, dir.toString()));
    }

    private String exportPath() {
        return tempDir.resolve("analytics").toString();
    }

    private static final class FakeCatalogs implements CatalogSchemas.CatalogLookup {
        private final Map<String, Map<String, String>> catalogs = new HashMap<>();

        FakeCatalogs catalog(String schema, String owner, String dataPath) {
            Map<String, String> meta = new HashMap<>();
            meta.put("data_path", dataPath + "/");
            if (owner != null) {
                meta.put(CatalogSchemas.OWNER_KEY, owner);
            }
            catalogs.put(schema, meta);
            return this;
        }

        @Override
        public boolean hasCatalog(String schema) {
            return catalogs.containsKey(schema);
        }

        @Override
        public String metadata(String schema, String key) {
            Map<String, String> meta = catalogs.get(schema);
            return meta == null ? null : meta.get(key);
        }
    }
}
