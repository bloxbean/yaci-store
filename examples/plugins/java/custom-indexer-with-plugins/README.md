# Custom Indexer with Java Plugins

Example Spring Boot indexer that embeds **yaci-store as a library** and loads Java plugins from the
application classpath (`BOOT-INF/classes`). No ext-jar and no `-Dloader.path` are needed.

## Included plugins

Enabled by default:

| Plugin | Type | Extension point | Behavior |
|---|---|---|---|
| `AddressFilterPlugin` | Filter | `utxo.unspent.save` | Keeps UTXOs whose `ownerAddr` is configured in `plugin.address-filter.addresses`; empty config keeps all. |
| `ExpressionFilterPlugin` | Filter | `metadata.save` | Keeps metadata labels `721` and `1967`. |

Included but disabled in `application.yml`:

| Plugin | Type | Demonstrates |
|---|---|---|
| `BlockEventExamples` | Event handler | Writing block data through `context().files()` |
| `TransactionEventExample` | Event handler | Exporting transactions as CSV through `context().files()` |

## Build

Run from the yaci-store repository root:

```bash
./gradlew :examples:plugins:java:custom-indexer-with-plugins:bootJar
```

Jar output:

```text
examples/plugins/java/custom-indexer-with-plugins/build/libs/yaci-custom-indexer-<version>.jar
```

## Run

```bash
./gradlew :examples:plugins:java:custom-indexer-with-plugins:bootRun
```

Or:

```bash
java -jar examples/plugins/java/custom-indexer-with-plugins/build/libs/yaci-custom-indexer-<version>.jar
```

Mainnet profile:

```bash
java -jar examples/plugins/java/custom-indexer-with-plugins/build/libs/yaci-custom-indexer-<version>.jar \
  --spring.profiles.active=mainnet
```

Defaults:

- Network: preprod (`preprod-node.play.dev.cardano.org:3001`)
- DB: in-memory H2 (`jdbc:h2:mem:mydb`, user `sa`, password `password`)
- H2 console: `http://localhost:8080/h2-console`

## Configuration

`src/main/resources/application.yml` registers Java plugins with `lang: java` and `class-name`:

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
```

Address filter configuration:

```yaml
plugin:
  address-filter:
    addresses: "addr_test1..."
```

To enable the optional event handlers, uncomment the `store.plugins.event-handlers` section in
`application.yml`.

## Java plugin shape

Extend the matching base class and expose the `(PluginDef, PluginType, PluginContext)` constructor:

```java
public class MyFilterPlugin extends JavaFilterPlugin<MyDomainType> {
    public MyFilterPlugin(PluginDef def, PluginType type, PluginContext context) {
        super(def, type, context);
    }

    @Override
    public Collection<MyDomainType> filter(Collection<MyDomainType> items) {
        return items.stream().filter(item -> true).toList();
    }
}
```

Useful context methods:

- `context().namedJdbc()`
- `context().http()`
- `context().files()`
- `context().environment()`
- `state()` / `globalState()`
