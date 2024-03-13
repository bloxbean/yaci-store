# Use out-of-the-box applications

Yaci Store comes with few applications that you can use out-of-the-box. These applications are designed to be used as-is.

- **yaci-store-all:** This application bundles all available modules/stores into a single application. If you want to index all available data, this is the application you want.
- **yaci-store-utxo-indexer:** This application contains utxo store,  protocolparams store and submit module If you want a utxo indexer with transaction submission capability, this is the application you want.
- **yaci-store-aggregation-app:** This application handles aggregation tasks like account balance calculation. This can be run as a separate application to calculate address balances.
For steps to run this application, please refer to [Aggregation App Getting Started](aggregation-app-getting-started.md) section.

The Jar files for these applications are available in the release section.

<hr>

#  Run Yaci Store All or Yaci Store Utxo Indexer

1. [Configuration](#configuration)
2. [Run As Jar](#run_as_jar)
3. [Run As Docker Container](#docker)
4. [Swagger UI](#swagger-ui)

## 1. Configuration <a id="configuration"></a>

Download the application.properties file from the release section and place it in a folder named "config" in the same directory as the jar file.
The application will automatically pick up the configuration file.

Update configuration file with your own values. Some of the key properties are mentioned below.

### Mandatory Configuration

#### Network Configuration

```
store.cardano.host=preprod-node.world.dev.cardano.org
store.cardano.port=30000
store.cardano.protocol-magic=1
```

#### Database Configuration

Uncomment and edit the following properties to configure the database connection.

```
spring.datasource.url=
spring.datasource.username=user
spring.datasource.password=
```

Additional configurations for database connection pool and batch insert.

```shell
spring.datasource.hikari.maximum-pool-size=30
spring.datasource.hikari.minimum-idle=5
spring.jpa.properties.hibernate.jdbc.batch_size=100
spring.jpa.properties.hibernate.order_inserts=true
```

#### Parallel Processing Configuration

The following properties are used to configure parallel processing. You can leave them as-is or change them based on
your machine configuration.

**Note:** If you are using parallel processing, you also need to configure the database connection pool size accordingly.

```
store.executor.enable-parallel-processing=true
store.executor.block-processing-threads=15
store.executor.event-processing-threads=30

store.executor.blocks-batch-size=200
store.executor.blocks-partition-size=10

store.executor.use-virtual-thread-for-batch-processing=false
store.executor.use-virtual-thread-for-event-processing=true
```


### Optional Configuration
The followings are optional configuration. You can leave them as-is and jump to the next section [Running the application](#running-the-application)

#### Genesis files Configuration for custom networks

This is only required if you are using any **custom** networks. For following public networks, the genesis files are already available in the application.
- mainnet
- preprod
- preview
- sanchonet


Configure genesis files location for custom network

```
store.cardano.byron-genesis-file=/Users/satya/cardano-node/files/byron-genesis.json
store.cardano.shelley-genesis-file=/Users/satya/cardano-node/files/shelley-genesis.json
```

#### N2C Configuration

The following properties are required for node-to-client (n2c) communication. This is required for transaction submission,
fetching protocol parameters etc. If you don't need these functionalities, you can leave them as-is.
```
#store.cardano.n2c-node-socket-path=/Users/satya/work/cardano-node/preprod-8.1.2/db/node.socket

# If you are accessing n2c through a relay like "socat", uncomment and edit the following properties.
#store.cardano.n2c-host=<relay_host>
#store.cardano.n2c-port=<relay_port>
```

#### Enable / Disable specific store

Even if you are using ``yaci-store-all`` application, you can enable/disable specific stores. **For example**, if you want to disable utxo store, you can do so by setting the following property to false.

```
store.utxo.enabled=false
```

This property is available for all stores.

```
store.<store_name></store_name>.enabled=false
```

#### Starting from a specific point

By default, sync starts from the genesis block. If you want to start from a specific point, you can do so by setting the following property.

```
store.cardano.sync-start-slot=2738868
store.cardano.sync-start-blockhash=a5c72a0e74cf066873ae7741c488acaef32746e1c4ac3b0d49c4acf4add1a47c

# For Byron block as start  point
#store.cardano.sync-start-byron-block-number=2737340

# For stop point
#store.cardano.sync-stop-slot=76667163
#store.cardano.sync-stop-blockhash=3e9a93afb174503befd4e8dabd52f73e6c4e9c3c76886713475dd43b00e6acbf
```

## 2. Run As Jar <a id="run_as_jar"></a>

For Yaci Store All, use the following command.

```shell
java -jar yaci-store-all-<version>.jar 
```

For Yaci Store Utxo Indexer, use the following command.
```shell
java -jar yaci-store-utxo-indexer-<version>.jar 
```

## 3. Run As Docker <a id="docker"></a>

To run the application as a docker container, you can refer to application.properties file in ``docker run`` command or create an env file.

### Using application.properties file

#### Yaci Store All docker image

```
docker run --env-file config/application.properties -p 8080:8080 bloxbean/yaci-store-all:0.1.0-rc1
```

#### Yaci Store Utxo Indexer docker image

``` 
docker run --env-file config/application.properties -p 8080:8080 bloxbean/yaci-store-utxo-indexer:0.1.0-rc1
```

### Using env file

To run the application as a docker container, you need to create a env file name "**env**" and set the following environment variables
with appropriate values.

```
STORE_CARDANO_HOST=preprod-node.world.dev.cardano.org
STORE_CARDANO_PORT=30000
STORE_CARDANO_PROTOCOL-MAGIC=1

SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/yacistore?currentSchema=preprod
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=

SPRING_DATASOURCE_HIKARI_MAXIMUM-POOL-SIZE=30
SPRING_DATASOURCE_HIKARI_MINIMUM-IDLE=5
SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_BATCH_SIZE=100
SPRING_JPA_PROPERTIES_HIBERNATE_ORDER_INSERTS=true

STORE_EXECUTOR_ENABLE-PARALLEL-PROCESSING=true
STORE_EXECUTOR_BLOCK-PROCESSING-THREADS=15
STORE_EXECUTOR_EVENT-PROCESSING-THREADS=30

STORE_EXECUTOR_BLOCKS-BATCH-SIZE=200
STORE_EXECUTOR_BLOCKS-PARTITION_SIZE=10

STORE_EXECUTOR_USE-VIRTUAL-THREAD-FOR-BATCH-PROCESSING=false
STORE_EXECUTOR_USE-VIRTUAL-THREAD-FOR-EVENT-PROCESSING=true
```

**Note:** To configure other optional properties, please refer to ``application.properties`` file or properties in the previous section.
You need to configure the properties in the env file in the correct format.

**Note:** If you are using parallel processing, you also need to configure no of threads and the database connection pool size accordingly.

### Yaci Store All docker image

```
docker run --env-file env -p 8080:8080 bloxbean/yaci-store-all:0.1.0-rc1
```

### Yaic Store Utxo Indexer docker image

```
docker run --env-file env -p 8080:8080 bloxbean/yaci-store-utxo-indexer:0.1.0-rc1
```

## 4. Swagger UI <a id="swagger-ui"></a>

By default, swagger UI for the application is available at http://localhost:8080/swagger-ui/index.html
