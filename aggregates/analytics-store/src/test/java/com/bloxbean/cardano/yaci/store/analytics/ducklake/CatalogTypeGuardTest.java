package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
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
    void aNonPostgresDatasourceOrAnUnreachableOneIsSkipped() {
        var h2 = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:test");
        assertThatCode(new CatalogTypeGuard(h2, properties(), (u, n, p) -> {
            throw new AssertionError("probed");
        })::check).doesNotThrowAnyException();

        assertThatCode(new CatalogTypeGuard(pgDatasource(), properties(), (u, n, p) -> {
            throw new SQLException("connection refused");
        })::check).doesNotThrowAnyException();
    }
}
