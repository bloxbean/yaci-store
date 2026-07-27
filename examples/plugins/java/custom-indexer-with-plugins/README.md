# yaci-custom-indexer

A custom Cardano indexer that embeds **yaci-store as a library** (via the Spring Boot starters)
and registers three Java filter plugins directly in the app — the "library" deployment model, as
opposed to dropping an ext-jar into `plugins/ext-jars` of the stock yaci-store app.

The three filters registered by default are:

| Plugin | Extension point | Behavior |
|---|---|---|
| `AddressFilterPlugin` | `utxo.unspent.save` | keeps only UTXOs whose `ownerAddr` is in a configured address set |
| `ExpressionFilterPlugin` | `metadata.save` | keeps only metadata labels `721` (NFT) and `1967` (pool) — Java equivalent of the SpEL filter `label == '721' or label == '1967'` |
| `CollectionFilterPlugin` | `metadata.save` | keeps only metadata whose body contains `image` and is longer than 100 chars — Java equivalent of the SpEL filter `body.contains('image') and body.length() > 100` |

## How the plugins are applied here

This example is a **subproject of the yaci-store build** (registered in the root
`settings.gradle` as `include 'examples:plugins:java:custom-indexer-with-plugins'`). It depends on
the yaci-store modules directly via `project(':starters:...')` coordinates, so it always builds
against the in-tree source — no composite build / `dependencySubstitution` is required.

Because the indexer is a Spring Boot app that depends on `yaci-store-spring-boot-starter` +
`yaci-store-utxo-spring-boot-starter` + `yaci-store-metadata-spring-boot-starter`, the plugin
system is auto-configured: `YaciStoreAutoConfiguration` (registered via the starter's
`AutoConfiguration.imports`) `@Import`s `StoreConfiguration`, which `@ComponentScan`s
`com.bloxbean.cardano.yaci.store.plugin` — so `JavaStorePluginFactory` and `PluginRegistry` are
Spring beans automatically. No `scanBasePackages` on the main class is needed.

The plugin classes live in this app's own source (`com.bloxbean.example.*`), so they are on
the app classpath (`BOOT-INF/classes`). `JavaStorePluginFactory` resolves them via
`Class.forName("...")` — **no `plugins/ext-jars` and no `-Dloader.path` are required**.

## Project layout

```
custom-indexer-with-plugins/        # a yaci-store subproject (registered in root settings.gradle)
├── build.gradle                    # Spring Boot plugin + project(':starters:...') deps
├── README.md
└── src/main/
    ├── java/com/bloxbean/example/
    │   ├── CustomIndexerApplication.java          # @SpringBootApplication
    │   ├── AddressFilterPlugin.java               # utxo.unspent.save filter
    │   ├── ExpressionFilterPlugin.java             # metadata.save filter (label 721/1967)
    │   ├── CollectionFilterPlugin.java             # metadata.save filter (image body > 100)
    │   └── *Examples.java                         # optional docs examples (disabled by default)
    └── resources/
        ├── application.yml                        # network + DB + store.plugins config
        └── application-mainnet.yml                # mainnet overrides
```

There is intentionally **no `settings.gradle`, `gradlew`, or `gradle/` wrapper** here — the project
is built with the yaci-store Gradle wrapper from the repo root.

## Prerequisites

- JDK 21
- The yaci-store checkout (this example lives under `examples/plugins/java/` in it) with the
  `lang=java` plugin SPI (`components/plugin/.../impl/java/`) present.
- No external database — `application.yml` uses in-memory H2 (`jdbc:h2:mem:mydb`), yaci-store's
  default DB. Data is ephemeral (lost on shutdown). The H2 console is enabled at
  `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:mydb`, user `sa` / `password`).
- A Cardano node reachable for sync (defaults to the public preprod node
  `preprod-node.play.dev.cardano.org:3001`; override `store.cardano.*` for preview/mainnet/local).

## Build

From the **yaci-store repo root**:

```bash
./gradlew :examples:plugins:java:custom-indexer-with-plugins:bootJar
```

Produces `examples/plugins/java/custom-indexer-with-plugins/build/libs/yaci-custom-indexer-<version>.jar`
(a ~136 MB fat jar; `<version>` is the yaci-store version, e.g. `3.0.0-beta3`). Verify the plugin
classes and the local plugin system are bundled:

```bash
unzip -l examples/plugins/java/custom-indexer-with-plugins/build/libs/yaci-custom-indexer-*.jar \
  | grep -E "AddressFilterPlugin|ExpressionFilterPlugin|CollectionFilterPlugin"
# BOOT-INF/classes/com/bloxbean/example/AddressFilterPlugin.class
# BOOT-INF/classes/com/bloxbean/example/ExpressionFilterPlugin.class
# BOOT-INF/classes/com/bloxbean/example/CollectionFilterPlugin.class
# (the local yaci-store plugin SPI lives inside BOOT-INF/lib/yaci-store-plugin-<version>.jar)
```

## Configuration

`src/main/resources/application.yml` sets:

- `store.cardano.*` — network (preprod by default)
- `spring.datasource.*` — in-memory H2 (`jdbc:h2:mem:mydb`; ephemeral)
- `store.plugins.enabled: true` + the filter registrations:

  ```yaml
  store:
    plugins:
      enabled: true
      filters:
        utxo.unspent.save:
          - name: "address-filter"
            lang: java
            class-name: com.bloxbean.example.AddressFilterPlugin
        metadata.save:
          - name: "metadata-label-filter"
            lang: java
            class-name: com.bloxbean.example.ExpressionFilterPlugin
          - name: "metadata-body-filter"
            lang: java
            class-name: com.bloxbean.example.CollectionFilterPlugin
  ```

- `plugin.address-filter.addresses` — comma/whitespace-separated addresses to keep (empty ⇒ the
  `address-filter` is a no-op and keeps everything, handy for a first wiring check). The two
  metadata filters are unconditional.

## Run

```bash
./gradlew :examples:plugins:java:custom-indexer-with-plugins:bootRun
# or, after building:
java -jar examples/plugins/java/custom-indexer-with-plugins/build/libs/yaci-custom-indexer-<version>.jar
```

(No `-Dloader.path` needed — the plugins are bundled. Standard Spring Boot `JarLauncher` is used.)

On startup you should see the plugin registry created instead of "Plugin registry is disabled":

```
INFO - Initializing PluginRegistry...
INFO - Java Plugin Factory created >>
INFO - Created java FILTER plugin 'address-filter' from class com.bloxbean.example.AddressFilterPlugin
INFO - Created java FILTER plugin 'metadata-label-filter' from class com.bloxbean.example.ExpressionFilterPlugin
INFO - Created java FILTER plugin 'metadata-body-filter' from class com.bloxbean.example.CollectionFilterPlugin
INFO - AddressFilterPlugin 'address-filter' tracking 1 address(es): [...]
```

## Contrast with the ext-jar model

| | ext-jar plugin | this example (bundled) |
|---|---|---|
| Deployment | JAR dropped into `plugins/ext-jars` | bundled in the app fat jar |
| Plugin on classpath via | `-Dloader.path=...,plugins/ext-jars` (PropertiesLauncher) | `BOOT-INF/classes` (JarLauncher) |
| Host app | the stock yaci-store `all` app | your own Spring Boot app |
| yaci-store role | runtime host (provides classes) | compile + runtime library (project() deps) |
| Plugin recompile | rebuild the ext-jar, restart host | rebuild this subproject |
