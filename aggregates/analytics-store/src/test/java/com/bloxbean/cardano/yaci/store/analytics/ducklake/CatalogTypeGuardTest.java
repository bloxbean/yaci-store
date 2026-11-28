package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.CatalogSchemas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTypeGuardTest {

    @TempDir
    Path dir;

    private static final String PG_URL = "jdbc:postgresql://localhost:5432/yaci_store";

    private AnalyticsStoreProperties properties() {
        AnalyticsStoreProperties p = new AnalyticsStoreProperties();
        p.getDucklake().setCatalogPath(dir.resolve("ducklake.catalog.db").toString());
        return p;
    }

    private static MockEnvironment pgDatasource() {
        return new MockEnvironment().withProperty("spring.datasource.url", PG_URL)
                .withProperty("spring.datasource.username", "yaci")
                .withProperty("spring.datasource.password", "secret");
    }

    @Test
    void defaultIsDuckDb() {
        assertThat(new AnalyticsStoreProperties().getDucklake().getCatalogType()).isEqualTo("duckdb");
    }

    @Test
    void refusesWhenTheDefaultWouldReplaceAnExistingPostgresCatalog() {
        AtomicReference<String> probed = new AtomicReference<>();
        var guard = new CatalogTypeGuard(pgDatasource(), properties(), (url, user, pw) -> {
            probed.set(url + "|" + user);
            return true;
        });

        assertThatThrownBy(guard::check).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yaci.store.analytics.ducklake.catalog-type=postgresql")
                .hasMessageNotContaining("secret");
        assertThat(probed).hasValue(PG_URL + "|yaci");
    }

    @Test
    void probesTheCatalogUrlWhenOneIsSet() {
        AnalyticsStoreProperties p = properties();
        p.getDucklake().setCatalogUrl("jdbc:postgresql://catalog:5432/lake");
        p.getDucklake().setCatalogUsername("lake");
        AtomicReference<String> probed = new AtomicReference<>();
        new CatalogTypeGuard(pgDatasource(), p, (url, user, pw) -> {
            probed.set(url + "|" + user);
            return false;
        }).check();
        assertThat(probed).hasValue("jdbc:postgresql://catalog:5432/lake|lake");
    }

    @Test
    void passesWithoutAnExistingPostgresCatalog() {
        assertThatCode(new CatalogTypeGuard(pgDatasource(), properties(), (u, n, p) -> false)::check)
                .doesNotThrowAnyException();
    }

    @Test
    void anExplicitSettingIsNeverQuestioned() {
        var env = pgDatasource().withProperty(CatalogTypeGuard.PROPERTY, "duckdb");
        assertThatCode(new CatalogTypeGuard(env, properties(), (u, n, p) -> {
            throw new AssertionError("probed");
        })::check).doesNotThrowAnyException();
    }

    @Test
    void anExistingDuckDbCatalogIsNotProbed() throws Exception {
        AnalyticsStoreProperties p = properties();
        Files.createFile(Path.of(p.getDucklake().getCatalogPath()));
        assertThatCode(new CatalogTypeGuard(pgDatasource(), p, (u, n, pw) -> {
            throw new AssertionError("probed");
        })::check).doesNotThrowAnyException();
    }

    @Test
    void aNonPostgresDatasourceIsSkipped() {
        var h2 = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:test");
        assertThatCode(new CatalogTypeGuard(h2, properties(), (u, n, p) -> {
            throw new AssertionError("probed");
        })::check).doesNotThrowAnyException();
    }

    @Test
    void aDatabaseThatCannotBeCheckedRefusesStartInsteadOfCreatingTheFile() {
        var guard = new CatalogTypeGuard(pgDatasource(), properties(), (u, n, p) -> {
            throw new SQLException("connection to dbname=yaci user=yaci password=secret host=db refused");
        });

        assertThatThrownBy(guard::check).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.datasource.url")
                .hasMessageContaining("yaci.store.analytics.ducklake.catalog-type=postgresql")
                .hasMessageContaining("yaci.store.analytics.ducklake.catalog-type=duckdb")
                .hasMessageContaining("password=***")
                .hasMessageNotContaining("secret")
                .hasNoCause();
    }

    @Test
    void aSeparateCatalogDatabaseThatCannotBeCheckedIsNamed() {
        AnalyticsStoreProperties p = properties();
        p.getDucklake().setCatalogUrl("jdbc:postgresql://catalog:5432/lake");
        var guard = new CatalogTypeGuard(pgDatasource(), p, (u, n, pw) -> {
            throw new SQLException("connection refused");
        });

        assertThatThrownBy(guard::check).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("yaci.store.analytics.ducklake.catalog-url");
    }

    @Test
    void aRelaxedFormOfTheSettingCountsAsSet() {
        var camelCase = pgDatasource().withProperty("yaci.store.analytics.ducklake.catalogType", "duckdb");
        assertThatCode(new CatalogTypeGuard(camelCase, properties(), (u, n, p) -> {
            throw new AssertionError("probed");
        })::check).doesNotThrowAnyException();

        StandardEnvironment fromEnvVar = new StandardEnvironment();
        fromEnvVar.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                "SPRING_DATASOURCE_URL", PG_URL,
                "YACI_STORE_ANALYTICS_DUCKLAKE_CATALOGTYPE", "duckdb")));
        assertThatCode(new CatalogTypeGuard(fromEnvVar, properties(), (u, n, p) -> {
            throw new AssertionError("probed");
        })::check).doesNotThrowAnyException();
    }

    /** Schema to its ducklake_metadata values; a schema absent from the map has no catalog. */
    private static CatalogSchemas.CatalogLookup catalogs(Map<String, Map<String, String>> bySchema) {
        return new CatalogSchemas.CatalogLookup() {
            @Override
            public boolean hasCatalog(String schema) {
                return bySchema.containsKey(schema);
            }

            @Override
            public String metadata(String schema, String key) {
                return bySchema.getOrDefault(schema, Map.of()).get(key);
            }
        };
    }

    @Test
    void theSchemasLookedAtAreCatalogSchemaThenTheUrlsCurrentSchemaThenPublic() {
        AnalyticsStoreProperties p = properties();
        var guard = new CatalogTypeGuard(pgDatasource(), p, (u, n, pw) -> false);
        assertThat(guard.candidateSchemas(PG_URL + "?currentSchema=preprod,public"))
                .containsExactly("preprod", "public");
        assertThat(guard.candidateSchemas(PG_URL)).containsExactly("public");

        p.getDucklake().setCatalogSchema("lake");
        assertThat(guard.candidateSchemas(PG_URL + "?currentSchema=preprod"))
                .containsExactly("lake", "preprod", "public");
    }

    /** A catalog this branch created in the source schema, recording its owner, is found; public is empty. */
    @Test
    void aCatalogOfThisNodeInItsOwnSchemaCounts() throws Exception {
        var lookup = catalogs(Map.of("preprod", Map.of(CatalogSchemas.OWNER_KEY, "yaci_store/preprod",
                "data_path", "/elsewhere")));
        assertThat(CatalogTypeGuard.hasOwnCatalog(lookup, new LinkedHashSet<>(List.of("preprod", "public")),
                "yaci_store/preprod", "./data/analytics")).isTrue();
    }

    /** Another instance's catalog in a shared database is not this node's, so the default may apply. */
    @Test
    void anotherInstancesCatalogDoesNotCount() throws Exception {
        var lookup = catalogs(Map.of("public", Map.of(CatalogSchemas.OWNER_KEY, "yaci_store/mainnet")));
        assertThat(CatalogTypeGuard.hasOwnCatalog(lookup, new LinkedHashSet<>(List.of("preprod", "public")),
                "yaci_store/preprod", "./data/analytics")).isFalse();
    }

    /** A beta catalog in public records no owner: it counts when it was written for this node's export path. */
    @Test
    void aBetaCatalogInPublicCountsWhenItsDataPathIsThisNodes() throws Exception {
        String exportPath = dir.resolve("analytics").toString();
        var lookup = catalogs(Map.of("public", Map.of("data_path", exportPath + "/")));
        assertThat(CatalogTypeGuard.hasOwnCatalog(lookup, new LinkedHashSet<>(List.of("public")),
                "yaci_store/public", exportPath)).isTrue();
        assertThat(CatalogTypeGuard.hasOwnCatalog(lookup, new LinkedHashSet<>(List.of("public")),
                "yaci_store/public", dir.resolve("other").toString())).isFalse();
    }
}
