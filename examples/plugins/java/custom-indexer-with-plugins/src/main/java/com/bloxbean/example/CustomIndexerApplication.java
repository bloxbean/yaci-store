package com.bloxbean.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Custom Cardano indexer built on yaci-store as a library.
 *
 * <p>A plain {@code @SpringBootApplication} is enough: yaci-store's
 * {@code YaciStoreAutoConfiguration} (registered via the spring-boot-starter's
 * {@code AutoConfiguration.imports}) {@code @Import}s {@code StoreConfiguration}, which
 * {@code @ComponentScan}s {@code com.bloxbean.cardano.yaci.store.plugin} — so the plugin system
 * ({@code JavaStorePluginFactory}, {@code PluginRegistry}, …) is auto-configured, and
 * {@code store.plugins.*} from application.yml is bound into {@code StoreProperties}.</p>
 */
@SpringBootApplication
public class CustomIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomIndexerApplication.class, args);
    }
}
