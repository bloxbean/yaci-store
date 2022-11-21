# Yaci Indexer

A Cardano Indexer implementation using [Yaci](https://github.com/bloxbean/yaci)

## How to Build

```
$> git clone https://github.com/bloxbean/yaci-indexer
$> cd yaci-indexer
$> ./gradlew clean build
```

## Run

```
$> cd yaci-indexer
$> edit application.properties //datasource, network details
$> java -jar build/libs/yaci-indexer-<version>-SNAPSHOT.jar 
```
