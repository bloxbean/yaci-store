## Aggregation App Getting Started

Using **yaci-store-aggregation-app**, you can track account balance related data. As account balance calculation is a resource intensive task, 
you need to run this application as a separate application.

Similar to main application, place  ``application.properties`` in a folder named "config"
in the same directory as the jar file. The application will automatically pick up the configuration file.

Swagger UI is available at http://localhost:8081/swagger-ui.html

1. [Mode 1 - Companion Mode](#mode1)
2. [Mode 2 - Standalone Mode](#mode2)
3. [Mode 3 - Standalone Mode with RocksDB Utxo Storage](#mode3)
4. [How to run as Docker Container](#docker)

This application can be run in three modes.

## Mode 1 - Companion Mode 

**Read UTxOs from the main database and write account balance to same database.**

In this mode, the aggregation app will start sync from the genesis block, but it doesn't write any UTxO data to the database.
It will only write account balance related data to the database. But it will read UTxOs from the database to calculate account balance.
So, you need to run this application along with **yaci-store-all** or **yaci-store-utxo-indexer** application.

**Note:** Since this application depends on the main database, it needs to be run after the main application has been started for some time.

1. Run aggregation application.

```
java -jar yaci-store-aggregation-app-<version>.jar
```

3. To clean account balance records from the database and start from the genesis block, use the following property.

```
java -Dstore.aggr.clean-db-before-start=true -jar yaci-store-aggregation-app-<version>.jar
```

## Mode 2 - Standalone Mode 

**Write UTxOs and account balance related data to a separate database.**

In this mode, the aggregation app will start sync from the genesis block and write UTxOs and account balance related data 
to a separate database instead of the main database. So, you don't need to run this application along with **yaci-store-all** or **yaci-store-utxo-indexer** application.

This mode needs an additional configuration file named ``application-aggr.properties``. You can download this
file from download section.

Similar to main application, place both  ``application.properties`` and ``application-aggr.properties`` in a folder named "config"
in the same directory as the jar file. The application will automatically pick up the configuration file.

1. Edit datasource properties in application.properties file to point to a separate database.
2. Edit ``application-aggr.properties`` file to enable flyway migration for the new database.

Make sure the **only** following properties are uncommented in ``application-aggr.properties`` file.

```
store.account.enabled=true
store.extensions.utxo-storage-type=default

spring.flyway.locations=classpath:db/store/{vendor}
spring.flyway.out-of-order=true

logging.file.name=./logs/yaci-store-aggr.log
```

3. Run aggregation application with ``aggr`` profile.

```
java -Dspring.profiles.active=aggr -jar yaci-store-aggregation-app-<version>.jar
```

4. To clean account balance records from the database and start from the genesis block, use the following property.

```
java -Dstore.aggr.clean-db-before-start=true -Dspring.profiles.active=aggr -jar yaci-store-aggregation-app-<version>.jar
```

## Mode 3 - Standalone Mode with RocksDB Utxo Storage (Experimental)

**Write UTxOs to embedded RocksDB storage and account balance related data to a separate database.**

In this mode, the aggregation app will start sync from the genesis block and write UTxOs to an embedded RocksDB storage
and account balance related data to a separate database instead of the main database. So, you don't need to run this application along with **yaci-store-all** or **yaci-store-utxo-indexer** application.

This mode needs an additional configuration file named ``application-aggr.properties``. You can download this
file from download section.

Similar to main application, place both  ``application.properties`` and ``application-aggr.properties`` in a folder named "config"

1. Edit datasource properties in application.properties file to point to a separate database.
2. Edit ``application-aggr.properties`` file to enable flyway migration for the new database.

Make sure the only following properties are uncommented in ``application-aggr.properties`` file.

```
store.account.enabled=true

spring.flyway.locations=classpath:db/store/{vendor}
spring.flyway.out-of-order=true

logging.file.name=./logs/yaci-store-aggr.log

store.rocksdb.base-dir=./rocksdb

store.extensions.utxo-storage-type=rocksdb
store.extensions.rocksdb-utxo-storage.write-batch-size=3000
store.extensions.rocksdb-utxo-storage.parallel-write=true
```

3. Run aggregation application with ``aggr`` profile.

```
java -Dspring.profiles.active=aggr -jar yaci-store-aggregation-app-<version>.jar
```

4. To clean account balance records from the database and start from the genesis block, use the following property.

```
java -Dstore.aggr.clean-db-before-start=true -Dspring.profiles.active=aggr -jar yaci-store-aggregation-app-<version>.jar
```

## How to run as Docker Container

To run the application as docker container, create an env file named "**env**" and set the required environment variables.

Please set the correct version in the docker command.

```
docker run --env-file env -p 8081:8081 bloxbean/yaci-store-aggregation-app:0.1.0-rc1
```
