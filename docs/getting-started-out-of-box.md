## Use out-of-the-box applications

Yaci Store comes with few applications that you can use out-of-the-box. These applications are designed to be used as-is.

- **yaci-store-all:** This application bundles all available modules/stores into a single application. If you want to index all available data, this is the application you want.
- **yaci-store-utxo-indexer:** This application contains utxo store,  protocolparams store and submit module If you want a utxo indexer with transaction submission capability, this is the application you want.

The Jar files for these applications are available in the release section.

## Configuration

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

#### Genesis files Configuration

Configure genesis files location.

```
store.cardano.byron-genesis-file=/Users/satya/cardano-node/preprod/files/byron-genesis.json
store.cardano.shelley-genesis-file=/Users/satya/cardano-node/preprod/files/shelley-genesis.json
```

### Optional Configuration
The followings are optional configuration. You can leave them as-is.

#### N2C Configuration

The following properties are required for node-to-client (n2c) communication. This is required for transaction submission,
fetching protocol parameters etc. If you don't need these functionalities, you can leave them as-is.
```
#store.cardano.n2c-node-socket-path=/Users/satya/work/cardano-node/preprod-8.1.2/db/node.socket

# If you are accessing n2c through a relay like "socat", uncomment and edit the following properties.
#store.cardano.n2c-host=<relay_host>
#store.cardano.n2c-port=<relay_port>
```

#### Account aggregation Configuration

Using account module, you can track account balance related data. By default, this module is disabled. If you want to enable it, uncomment the following properties.
The status of this module is "**experimental**".

```
store.account.enabled=false
store.account.balance-aggregation-enabled=true
store.account.history-cleanup-enabled=false
```

#### Enable / Disable specif store

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
