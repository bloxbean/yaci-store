# Use Yaci Store as a Library

The power of Yaci Store is that it can be used as a library in your own application. This allows you to build your own application on top of Yaci Store.
You have full control over what data you want to index. This is achieved by using modular architecture. Not only you can include required stores in 
your application, you can also override the default behavior of the stores. For example, you can override the default behavior of the Utxo store to store 
utxos in a different database, or store only utxos specific to an address or store metadata with specific labels. This is achieved by using the
spring framework's dependency injection feature.

In this guide, we will show you how to use Yaci Store as a library in your own application.

## Prerequisites

- Java 17
- Spring Boot 3.x (Tested with Spring Boot 3.0.x, 3.1.x)

## Create a Spring Boot application

You can use the [Spring Initializr](https://start.spring.io/) to create a Spring Boot application. 

## Add Dependencies

Apart from standard spring dependencies, add the following dependency to your `build.gradle` or `pom.xml` file.
This will add the Yaci Store Core modules to your project as a dependency. Core modules are responsible for fetching data from the blockchain and
publishing them as events.

```groovy
implementation 'com.bloxbean.cardano:yaci-store-spring-boot-starter:<version>'
```

For pom.xml

```xml
<dependency>
    <groupId>com.bloxbean.cardano</groupId>
    <artifactId>yaci-store-spring-boot-starter</artifactId>
    <version>{version}</version>
</dependency>
```

## Add required store starters

Now you can add the required store starters to your project. For example, if you want to add the metadata store, add the following dependency.

```groovy
implementation 'com.bloxbean.cardano:yaci-store-metadata-spring-boot-starter:<version>'
```

For pom.xml
```xml
<dependency>
    <groupId>com.bloxbean.cardano</groupId>
    <artifactId>yaci-store-metadata-spring-boot-starter</artifactId>
    <version>{version}</version>
</dependency>
```

**Note:** You can add multiple store starters to your project. Full list of supported starters can be found [here](./overview.md?#spring-boot-starter)

## Configuration

Add following flyway configuration to your `application.yml` file. 

```xml
spring:
  flyway:
    locations: classpath:db/store/{vendor}
    out-of-order: true

apiPrefix: /api/v1
```

**Note:** If you are using Yaci Store 0.0.11 or earlier, use `classpath:db/migration/{vendor}` as the location.

You can add your application specific flyway configuration as well. For example, you may want to add another location for your application.
Flyway ``location`` property allows you to specify multiple locations. 

### Yaci Store Configuration (Mandatory)

Download the ``application.properties`` file from the release section and place it in a folder named "config" in your project root directory.
The application will automatically pick up the configuration file.

Alternatively, you can add all yaci store specific configurations in ``application.yml`` file as well.

Update configuration file with your own values. Some of the key properties are mentioned below.

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


### Optional Configuration
The followings are optional configuration. You can leave them as-is.

#### Genesis files Configuration

Configure genesis files location. This is required for few stores like Utxo store, account aggregate etc.

```
store.cardano.byron-genesis-file=/Users/satya/cardano-node/preprod/files/byron-genesis.json
store.cardano.shelley-genesis-file=/Users/satya/cardano-node/preprod/files/shelley-genesis.json
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

Now you are ready to use Yaci Store in your application. During application startup, Yaci Store will create all the required tables in the database and
start syncing the blockchain data and publish them as events and process them.

## Customization - Override default behavior

**Custom Storage**

Yaci Store provides a default storage implementation which stores all data in yaci store defined table. 
But you can override this default implementation and provide your own storage implementation.

For example: If you only want to store a specific metadata label, you can extend the default storage implementation and filter out the labels you want to store. 

**Custom Processor**

Ideally you don't need to have your own custom processor, but if you want to do some custom processing, you can listen to both core or derive events and do your custom processing. 

**Rollback Handling**

If you are using default storage implementation or a custom storage by extending default storage, Yaci Store will automatically handle rollbacks.

But if you have your own custom processor or different storage implementation using separate database/table, you need to handle rollbacks in your custom processor by listening to RollbackEvent.


**Note:** Check this [Metadata Indexer](https://github.com/bloxbean/metadata-indexer) project for a sample application using Yaci Store
