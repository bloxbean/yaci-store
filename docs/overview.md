## What's Yaci Store?

Yaci Store is a modular Java library built for Java developers who are keen on constructing their custom indexer solutions.
Its architecture ensures that every component within Yaci Store is accessible both as a standalone Java library and a 
corresponding Spring Boot starter.

Developers have the flexibility to select the specific modules that align with their project requirements. 
Yaci Store also offers an out-of-box application that indexes commonly-used data sets.

## Yaci Store Module Categories

The modules in Yaci Store are divided into three main categories:

1. Core Modules
2. Stores 
3. Aggregates

### 1. Core Modules

Core modules serve a critical purpose. They read data directly from the blockchain and then broadcast various domain-specific events.
Since these events are essentially Spring events, developers have the freedom to write their own Spring event listeners. 
This allows them to tap into these events and process them accordingly. In addition to this, the core module monitors and records
the current point in the database.

Major core modules include:
    a. core
    b. common
    c. events

Specific events emitted by the core modules comprise:

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

### 2. Stores

A "store" in Yaci Store is more than just a name – it's a specialized module designed for a specific data type or use case. Each store boasts a set of capabilities:

- Event Listening: Tuned into events broadcasted by the core module.
- Data Processing: Efficiently processes data.
- Data Persistence: Saves data to a dedicated persistence store.
- REST Endpoints: Optionally provides REST endpoints for data retrieval.

Available Store Implementations:

- utxo: Focuses on UTXOs, extracting them from transaction data.
- block: Dedicated to handling and storing block data.
- transaction: Takes care of transaction data.
- assets: Manages data related to asset minting and burning.
- metadata: Retrieves and processes metadata events.
- script: Deals with the ScriptEvent, overseeing dataum and redeemers.
- staking: Handles all things staking – from stake address registration to pool registration and more.
- mir: All about Mir data.
- Protocol Params: Fetches protocol parameters from nodes via N2c. Stay tuned – there are plans to expand its capabilities to chronicle the history of protocol parameters.

Additional Modules:
- submit: Enables direct transaction submissions to nodes, either through n2c or the submit API.

Each of the mentioned stores is available as a Spring Boot starter. This means that integrating a specific store into your 
application is as straightforward as adding its Spring Boot starter as a dependency. 

### 3. Aggregates

While stores are designed to handle specific data types, aggregates are more comprehensive. They pool data from multiple stores to provide a unified perspective. So a single aggregate 
can have dependencies on multiple stores.

Available Aggregates:
    
- Account: It provides account balance related data.

### 4. Yaci Store Spring Boot Starters

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
