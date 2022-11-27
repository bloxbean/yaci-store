[![Clean, Build](https://github.com/bloxbean/yaci-indexer/actions/workflows/build.yml/badge.svg)](https://github.com/bloxbean/yaci-indexer/actions/workflows/build.yml)

# Yaci Indexer (WIP)

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
$> edit config/application.properties //datasource, network details
$> java -jar application/build/libs/yaci-indexer-app-<version>-SNAPSHOT.jar 
```

## Modules
- common
- events
- core
- blocks
- utxo
- transaction
- script
