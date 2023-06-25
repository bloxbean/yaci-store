<div align="center">
<img src="static/YaciStore.png" width="200">

<h4>A Cardano datastore Java library using Yaci</h4>

[![Clean, Build](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml/badge.svg)](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml)
</div>

🔧 **Under Active Development:** This project is in the pre-alpha phase and is actively being developed. Please note that the APIs are subject to change as we continue to improve and expand the project. Your feedback and contributions are highly appreciated. Join us on this exciting journey!

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

## Overview

It provides loosely coupled modules which can be used to compose your own application. Out of box, it provides a default application
which consists of all the modules.

Modules are divided into following categories

1. **Core Modules**
2. **Stores**

--

1. **Core Modules**

Core modules are responsible to fetch data from a Cardano node and publish different types of events.

core, common, events are some of the major core modules. Only local events are currently supported.

But in future, the remote events will be supported through different pluggable messaging infrastructure.

2. **Stores**

A store is a loosely coupled module specific to one type of data/usecase. 
A store has ability 
- to listen to events published by the core module
- process data 
- store data to a persistence store
- provides REST endpoints to retrieve data (optional)

Currently, the following store implementations are available.

- **blocks**   : Process block events and store blocks in database
- **utxo**     : Process transaction events, resolve utxos and store in database
- **transaction** : Process transaction events and store transactions in database
- **script**   : Process transaction events, resolve script redeemer, datum and store 
- **metadata** : Process aux data events to store metadata in database
- **assets**   : Process events to store mint/burn assets in database
- **protocolparams** :  Retrieve protocol params through n2c and store

**Note:** With remote eventing support, there will be option to deploy each store independently.

Yaci Store also has a "**submit**" module to support transaction submission.

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
