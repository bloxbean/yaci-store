<div align="center">
<img src="static/YaciStore.png" width="200">

<h4>A Cardano datastore Java library using Yaci</h4>

[![Clean, Build](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml/badge.svg)](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml)
</div>

Yaci Store is a modular Java library for developers who are keen on constructing their custom indexer solutions.
Its architecture ensures that every component within Yaci Store is accessible both as a standalone Java library and a
corresponding Spring Boot starter.

## How to Run 

**Pre-requisites:** Java 21

1. Download the latest binary and property file from [release page](https://github.com/bloxbean/yaci-store/releases)
2. You need to download ``yaci-store-all-<version>.jar`` and ``application.properties`` files
3. Create a sub folder ``config`` and copy ``application.properties`` file to ``config`` directory
4. Edit ``application.properties`` file to configure your datasource and network details
5. Run ``java -jar yaci-store-all-<version>.jar`` from the directory where you have copied the jar file.

## Build from source and Run 

**Pre-requisites:** Java 21

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

## Run Release Build (Jar or Docker)
To run a release build, follow the instructions in [Getting Started - Out of Box Application](docs_site/pages/usage/getting-started-out-of-box.md)

## Documents

### 1. [Overview](https://store.yaci.xyz/)
### 2. [Design](https://store.yaci.xyz/design)
### 3. [Getting Started - Out of Box Application](https://store.yaci.xyz/usage/getting-started-out-of-box)
### 4. [Getting Started - Custom Application](https://store.yaci.xyz/usage/getting-started-as-library)


# Any questions, ideas or issues?

- Create a Github [Discussion](https://github.com/bloxbean/yaci-store/discussions)
- Create a Github [Issue](https://github.com/bloxbean/yaci-store/issues)
- [Discord Server](https://discord.gg/JtQ54MSw6p)

# Support from YourKit

YourKit has generously granted the BloxBean projects an Open Source licence to use their excellent Java Profiler.

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>
