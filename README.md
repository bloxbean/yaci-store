[![Clean, Build](https://github.com/bloxbean/yaci-indexer/actions/workflows/build.yml/badge.svg)](https://github.com/bloxbean/yaci-indexer/actions/workflows/build.yml)

# Yaci Store (WIP)

A Cardano datastore implementation using [Yaci](https://github.com/bloxbean/yaci)

## How to Build

```
$> git clone https://github.com/bloxbean/yaci-store
$> cd yaci-store
$> ./gradlew clean build
```

## Run

```
$> cd yaci-store
$> edit config/application.properties //datasource, network details
$> java -jar application/build/libs/yaci-store-app-<version>-SNAPSHOT.jar 
```

## Modules
- common
- events
- core
- blocks
- utxo
- transaction
- script
