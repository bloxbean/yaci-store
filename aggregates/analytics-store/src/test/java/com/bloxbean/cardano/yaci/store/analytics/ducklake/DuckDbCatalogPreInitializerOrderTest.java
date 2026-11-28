package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every DuckLake connection goes through {@link DuckDbConnectionHelper}, and its ATTACH creates a
 * missing DuckDB catalog file. The pre-initializer, which runs {@link CatalogTypeGuard} first, has to
 * run before that; {@code @Order} does not order singleton creation, so the helper asks for it.
 */
class DuckDbCatalogPreInitializerOrderTest {

    @TempDir
    Path dir;

    @Test
    void creatingTheConnectionHelperRunsThePreInitializerFirst() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getDucklake().setCatalogPath(dir.resolve("ducklake.catalog.db").toString());
        properties.setExportPath(dir.resolve("analytics").toString());

        new ApplicationContextRunner()
                // Nothing is created until asked for, so only the dependency can create the pre-initializer.
                .withBean(LazyInitializationBeanFactoryPostProcessor.class)
                .withBean(AnalyticsStoreProperties.class, () -> properties)
                .withPropertyValues(
                        "yaci.store.analytics.storage.type=ducklake",
                        "yaci.store.analytics.ducklake.catalog-type=duckdb",
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/yaci?currentSchema=cardano")
                .withUserConfiguration(DuckDbConnectionHelper.class, DuckDbCatalogPreInitializer.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(dir.resolve("ducklake.catalog.db")).doesNotExist();

                    ctx.getBean(DuckDbConnectionHelper.class);

                    assertThat(dir.resolve("ducklake.catalog.db")).exists();
                });
    }
}
