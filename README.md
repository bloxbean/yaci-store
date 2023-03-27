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

- Currently it doesn't store the genesis balances and Byron era transactions


# Support from YourKit

YourKit has generously granted the BloxBean projects an Open Source licence to use their excellent Java Profiler.

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>
