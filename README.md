<div align="center">
<img src="static/YaciStore.png" width="200">

<h4>A Cardano datastore Java library using Yaci</h4>

[![Clean, Build](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml/badge.svg)](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml)
</div>

🔧 **Under Active Development:** This project is in the pre-alpha phase and is actively being developed. Please note that the APIs are subject to change as we continue to improve and expand the project. Your feedback and contributions are highly appreciated. Join us on this exciting journey!

Yaci Store is a modular Java library for developers who are keen on constructing their custom indexer solutions.
Its architecture ensures that every component within Yaci Store is accessible both as a standalone Java library and a
corresponding Spring Boot starter.

## How to Run 

**Pre-requisites:** Java 17

1. Download the latest binary and property file from [release page](https://github.com/bloxbean/yaci-store/releases)
2. You need to download ``yaci-store-all-<version>.jar`` and ``application.properties`` files
3. Create a sub folder ``config`` and copy ``application.properties`` file to ``config`` directory
4. Edit ``application.properties`` file to configure your datasource and network details
5. Run ``java -jar yaci-store-all-<version>.jar`` from the directory where you have copied the jar file.

## Build from source and Run 

**Pre-requisites:** Java 17

### Build

```
$> git clone https://github.com/bloxbean/yaci-store
$> cd yaci-store
$> ./gradlew clean build
```

### Run

```
$> cd yaci-store
$> edit config/application.properties //datasource, network details
$> java -jar applications/all/build/libs/yaci-store-all-<version>.jar
```

## Documents

### 1. [Overview](./docs/overview.md)
### 2. [Design](./docs/design.md)
### 3. [Getting Started - Out of Box Application](./docs/getting-started-out-of-box.md)
### 4. [Getting Started - Custom Application](./docs/getting-started-as-library.md)

## Known Limitations:

1. Currently, epoch aggregation metrics such as total transactions and total fees in an epoch are calculated at specific intervals.
This process takes some time on the **mainnet** due to the current implementation. Since this process is executed synchronously within the main flow, 
you may experience some lag in the sync process during aggregation. However, once the aggregation is completed, the sync process should quickly catch up with the latest data.

The current implementation of epoch aggregation will be replaced by a more efficient approach in a future release. 
Nonetheless, you have control over the epoch aggregation interval by adjusting the following flag in application.properties, which is currently set to 14400 (4 hours):

``` store.blocks.epoch-calculation-interval=14400 ```

# Support from YourKit

YourKit has generously granted the BloxBean projects an Open Source licence to use their excellent Java Profiler.

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>
