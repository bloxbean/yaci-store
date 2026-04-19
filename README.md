<div align="center">
<img src="static/YaciStore.svg" width="200">

<h3>A Cardano blockchain indexer and datastore built with Yaci</h3>

[![Clean, Build](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml/badge.svg)](https://github.com/bloxbean/yaci-store/actions/workflows/build.yml)
</div>

## Overview

**Yaci Store** is a modular, high-performance Cardano blockchain indexer and datastore that provides a flexible foundation for building blockchain applications. Built on top of the Yaci library, it offers both out-of-the-box functionality and extensive customization options through its plugin framework.

### Key Features

- **🚀 High Performance**: Parallel processing with virtual thread support for optimal throughput
- **📦 Modular Architecture**: Enable only the stores you need, reducing resource usage
- **🔌 Plugin Framework**: Extend and customize functionality without forking
- **🗄️ Multi-Database Support**: PostgreSQL, MySQL, and H2 out of the box
- **🌐 Multi-Network**: Support for mainnet, preprod, preview, sanchonet, and custom networks
- **📊 REST APIs**: Essential Blockfrost-compatible APIs for transaction building and submission, plus custom endpoints
- **🔧 Flexible Deployment**: Run as standalone application, embed as library, or deploy with Docker
- **✂️ Data Pruning**: Configurable pruning strategies for sustainable storage
- **🎯 Production Ready**: Battle-tested in multiple production environments

### Latest Releases

- **Stable Release**: [v2.0.0](https://github.com/bloxbean/yaci-store/releases/tag/v2.0.0) - Production-ready release
- **Pre-release**: [v2.1.0-pre3](https://github.com/bloxbean/yaci-store/releases/tag/v2.1.0-pre3) - Latest pre-release with new features and improvements

### Core Stores

- **Assets**: Native tokens, NFTs, and fungible tokens
- **Blocks**: Block data
- **Transactions**: Transaction details
- **UTxO**: Unspent transaction outputs
- **Staking**: Pools, delegations, registrations
- **Governance**: Proposals, votes, and DRep data
- **Scripts**: Plutus scripts and native scripts
- **Metadata**: Transaction metadata
- **Epoch**: Epoch boundaries and parameters

### Aggregation Modules

- **Account**: Address balance tracking
- **Ledger State**: Full ledger state calculations
- **Governance State**: Governance state calculations

## Quick Start

**Pre-requisites:** Java 21

### Option 1: Download Release

1. Download the latest binary from [release page](https://github.com/bloxbean/yaci-store/releases)
2. Check [store.yaci.xyz](https://store.yaci.xyz) for how to run in Docker or non-Docker environments

### Option 2: Build from Source

```bash
git clone https://github.com/bloxbean/yaci-store
cd yaci-store
./gradlew clean build
java -jar applications/all/build/libs/yaci-store-all-<version>.jar
```

## Configuration

Basic configuration example (application.properties):

```properties
# Network
store.cardano.host=preprod-node.play.dev.cardano.org
store.cardano.port=3001
store.cardano.protocol-magic=1

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/yaci_store
spring.datasource.username=postgres
spring.datasource.password=password

# Enable/Disable stores
store.utxo.enabled=true
store.assets.enabled=true
store.governance.enabled=false
```

## Used By

Yaci Store powers various production applications in the Cardano ecosystem:

- [**Cardano Rosetta Java**](https://github.com/cardano-foundation/cardano-rosetta-java) - Cardano Foundation's Rosetta API implementation
- [**FluidTokens Aquarium Node**](https://github.com/FluidTokens/ft-aquarium-node)
- [**CF Cardano Ballot**](https://github.com/cardano-foundation/cf-cardano-ballot) - Cardano Foundation's voting platform
- [**CF Reeve Platform**](https://github.com/cardano-foundation/cf-reeve-platform) - Cardano Foundation's Ledger on the Blockchain Project
- [**CF AdaHandle Resolver**](https://github.com/cardano-foundation/cf-adahandle-resolver) - AdaHandle resolution service
- [**CF Token Metadata Registry**](https://github.com/cardano-foundation/cf-token-metadata-registry) - Cardano Foundation's token metadata registry (off-chain and on-chain via CIP-26 and CIP-68)
- [**AdaMatic**](https://adamatic.xyz/) - Cardano automation platform
- [**SundaeSwap Scooper Analytics**](https://github.com/easy1staking-com/sundaeswap-scooper-analytics) - DEX analytics platform

Is your project using Yaci Store? We'd love to feature it here! Please open a PR or create an issue.

## Documentation

- 📚 **[Full Documentation](https://store.yaci.xyz/)**
- 🏗️ **[Architecture & Design](https://store.yaci.xyz/design)**
- 🚀 **[Getting Started Guide](https://store.yaci.xyz/getting-started/getting-started-2.x.x)**
- 🔧 **[Configuration Guide](https://store.yaci.xyz/stores/configuration)**
- 🔌 **[Plugin Development](https://store.yaci.xyz/plugins/plugin-getting-started)**

## Community & Support

- 💬 **[Discord Server](https://discord.gg/JtQ54MSw6p)** - Join our community
- 💡 **[Discussions](https://github.com/bloxbean/yaci-store/discussions)** - Share ideas and ask questions
- 🐛 **[Issues](https://github.com/bloxbean/yaci-store/issues)** - Report bugs or request features

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## Branching & releases

- `main` → active development for the next minor release (2.1.x)
- `release/2.0.x` → patch fixes for the 2.0.x line
- `next` → development of the next major version (3.0)

Please open pull requests against the `main` branch unless advised otherwise.

### Documentation source

The latest documentation source is maintained in the `main` branch.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Supported by

<p align="left">
  <img src="docs/public/images/cf-logo-text.svg" alt="Cardano Foundation" width="180"/>
</p>

The Cardano Foundation supports this project through engineering resources.


## Acknowledgments

### YourKit

YourKit has generously granted the BloxBean projects an Open Source licence to use their excellent Java Profiler.

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/.net/profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.
