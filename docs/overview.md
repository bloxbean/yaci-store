### **Table of content:**

### 1. [What's Yaci Store](#whats-yaci-store)
### 2. [Yaci Store Modules](#yaci-store-modules)
### 3. [Yaci Store Spring Boot Starters](#spring-boot-starter)

<a id="what-yaci-store"></a>
## What's Yaci Store?

Yaci Store is a modular Java library for Java developers who are keen on constructing their custom indexer solutions.
Its architecture ensures that every component within Yaci Store is accessible both as a standalone Java library and a 
corresponding Spring Boot starter.

Developers have the flexibility to select the specific modules that align with their project requirements.

## Yaci Store application

Yaci Store also offers an out-of-box application that indexes commonly-used data sets using the available modules.

<a id="yaci-store-modules"></a>
## Yaci Store Modules

The modules in Yaci Store are divided into three main categories:

1. Core Modules
2. Stores 
3. Aggregates

### 1. Core Modules

Core modules serve a critical purpose. They read data directly from Cardano blockchain and then publish various domain-specific events.
Since these events are essentially Spring events, developers have the freedom to write their own Spring event listeners. 
This allows them to tap into these events and process them accordingly. In addition to this, the core module monitors and records
the current point in the database.

**Major core modules include:**
1. core
2. common
3. events

**Events published by core modules:**

- BlockEvent 
- BlockHeaderEvent 
- ByronEbBlockEvent 
- ByronMainBlockEvent 
- RollbackEvent 
- TransactionEvent 
- MintBurnEvent
- ScriptEvent 
- CertificateEvent 
- GenesisBlockEvent

**Derived Events: Events published by stores**
- AddressUtxoEvent
- TxAssetEvent
- TxMetadataEvent
- DatumEvent
- TxScriptEvent

### 2. Stores

A "**store**" in Yaci Store is a specialized module designed for a specific data type or use case. Each store has a set of capabilities:

- Event Listening: Listen to events published by the core module.
- Data Processing: Processes event data.
- Data Persistence: Saves processed data to a dedicated persistence store.
- REST Endpoints: Optionally provides REST endpoints for data retrieval.

**Available Store Implementations:**

- **utxo:** Focuses on UTxOs, extracting them from transaction data.
- **block:** Dedicated to handling and storing block data.
- **transaction:** Takes care of transaction data.
- **assets:** Manages data related to asset minting and burning.
- **metadata:** Retrieves and processes metadata events.
- **script:** Deals with the ScriptEvent, get datums and redeemers.
- **staking:** Handles from stake address registration to pool registration and more.
- **mir:** All about Mir data.
- **Protocol Params:** Fetches protocol parameters from nodes via n2c. There are plans to expand its capabilities to store the history of protocol parameter updates.

**Additional Modules:**
- submit: Enables transaction submissions to nodes, either through n2c or the submit API.

Each of the mentioned stores is available as a ``Spring Boot starter``. This means that integrating a specific store into your 
application is as straightforward as adding its Spring Boot starter as a dependency. 

### 3. Aggregates

Aggregates are modules that handle different kind of data aggregation. They are responsible for aggregating data from different stores and persisting them in a persistent store.
Currently, the only available aggregate is "**Account**" (experimental) , which provides account balance related data. It depends on the "utxo" store and the event published by utxo store.

<a id="spring-boot-starter"></a>
## Yaci Store Spring Boot Starters

Each module in Yaci Store is available as a Spring Boot starter. This means that integrating a specific module into your 
application is as straightforward as adding its Spring Boot starter as a dependency. This ensures that developers can 
easily integrate Yaci Store into their Spring Boot applications with required modules and minimal configuration.

**Available Starters:**

- **yaci-store-spring-boot-starter :** This is the core starter that includes all the core modules. This starter includes libraries
required to fetch data from the blockchain and publish events.
- **yaci-store-utxo-spring-boot-starter :** This starter includes the UTXO store and related configuration including db migration scripts.
- **yaci-store-block-spring-boot-starter :** This starter includes the block store and related configuration including db migration scripts.
- **yaci-store-transaction-spring-boot-starter :** This starter includes the transaction store and related configuration including db migration scripts.
- **yaci-store-assets-spring-boot-starter :** This starter includes the assets store and related configuration including db migration scripts.
- **yaci-store-metadata-spring-boot-starter :** This starter includes the metadata store and related configuration including db migration scripts.
- **yaci-store-script-spring-boot-starter :** This starter includes the script store and related configuration including db migration scripts.
- **yaci-store-staking-spring-boot-starter :** This starter includes the staking store and related configuration including db migration scripts.
- **yaci-store-mir-spring-boot-starter :** This starter includes the mir store and related configuration including db migration scripts.
- **yaci-store-protocolparams-spring-boot-starter :** This starter includes the protocol params module and related configuration including db migration scripts.
- **yaci-store-account-spring-boot-starter :** This starter includes the account aggregate and related configuration including db migration scripts.
- **yaci-store-submit-spring-boot-starter :** This starter includes the submit module and related configuration including db migration scripts. Submit module is used to submit transactions to nodes.

