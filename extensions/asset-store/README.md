# Asset Store Extension

Multi-standard Cardano token metadata indexer supporting CIP-26 (offchain), CIP-68 (on-chain reference NFTs), and CIP-113 (programmable tokens). Ported from [cf-token-metadata-registry](https://github.com/cardano-foundation/cf-token-metadata-registry).

## Add to your project

### Gradle

```gradle
dependencies {
    implementation 'com.bloxbean.cardano:asset-store-ext-spring-boot-starter:{version}'
}
```

Or as a project dependency within the yaci-store monorepo:

```gradle
dependencies {
    implementation project(':starters:asset-store-ext-spring-boot-starter')
}
```

### Maven

```xml
<dependency>
    <groupId>com.bloxbean.cardano</groupId>
    <artifactId>asset-store-ext-spring-boot-starter</artifactId>
    <version>{version}</version>
</dependency>
```

## Configuration

```properties
# Enable the extension
store.extensions.asset-store.enabled=true

# Enable CIP-26 GitHub sync (optional, disabled by default)
store.extensions.asset-store.cip26.sync-enabled=true
```

That's it. Everything else is auto-detected from `store.cardano.protocol-magic`:

- **CIP-26 git registry** — mainnet uses `cardano-foundation/cardano-token-registry`, preprod uses `input-output-hk/metadata-registry-testnet`. Other networks (preview, sanchonet) have no offchain registry and sync is skipped automatically.
- **CIP-68** — on-chain indexing, works on all networks, no configuration needed.
- **CIP-113 policy IDs** — maintained per-network in bundled property files (`cip113/cip113-{network}.properties`). Currently only preview has known policy IDs.

### Optional overrides

```properties
# Custom CIP-26 git registry (if you run your own)
store.extensions.asset-store.cip26.git.organization=my-org
store.extensions.asset-store.cip26.git.project-name=my-token-registry
store.extensions.asset-store.cip26.git.mappings-folder=mappings

# Custom CIP-113 policy IDs (overrides auto-detected values)
store.extensions.asset-store.cip113.registry-nft-policy-ids=abc123...,def456...

# V2 API default query priority (default: CIP_68,CIP_26)
store.extensions.asset-store.default-query-priority=CIP_68,CIP_26
```

## Programmatic usage (no REST)

Inject `TokenMetadataQueryService` for merged queries, or individual `StorageReader` interfaces for per-CIP access:

```java
@Autowired TokenMetadataQueryService tokenMetadata;

// Merged query — CIP-68 preferred over CIP-26, with CIP-113 extensions
Optional<Subject> subject = tokenMetadata.getSubject("policyId+assetNameHex");

// Per-CIP direct access
Optional<TokenMetadata> cip26 = tokenMetadata.getCip26Metadata("subject");
Optional<FungibleTokenMetadata> cip68 = tokenMetadata.getCip68Metadata("subject");
Optional<ProgrammableTokenCip113> cip113 = tokenMetadata.getCip113RegistryNode("policyId");
boolean programmable = tokenMetadata.isProgrammableToken("policyId");

// Or inject individual StorageReader interfaces
@Autowired Cip26StorageReader cip26Reader;
@Autowired Cip68StorageReader cip68Reader;
@Autowired Cip113StorageReader cip113Reader;
```

## REST API

### cf-token-metadata-registry compatible (drop-in replacement)

Hardcoded at `/api/v2` — clients only need to change the base URL.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v2/subjects/{subject}` | Merged metadata with priority, optional `property`, `query_priority`, `show_cips_details` params |
| POST | `/api/v2/subjects/query` | Batch query with same options |

### Per-CIP endpoints

Configurable prefix (default `/api/v1`):

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/metadata/{subject}` | CIP-26 offchain metadata |
| GET | `/api/v1/cip68/{policyId}/{assetName}` | CIP-68 reference NFT metadata |
| GET | `/api/v1/cip113/registry/{policyId}` | CIP-113 latest registry state |
| POST | `/api/v1/cip113/registry/query` | CIP-113 batch lookup |

## Health indicators

The extension contributes three [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health) `HealthIndicator` beans. They appear automatically under `/actuator/health` when actuator is enabled.

| Bean name | What it checks | Status values |
|-----------|---------------|---------------|
| `assetStoreOffchainSync` | CIP-26 GitHub sync job status | `UP` (sync done), `OUT_OF_SERVICE` (syncing/not started), `DOWN` (error) |
| `assetStoreOnchainConnection` | Cardano node connection alive and receiving blocks | `UP` (connected), `OUT_OF_SERVICE` (not receiving), `DOWN` (connection lost) |
| `assetStoreOnchainReadiness` | On-chain sync progress (CIP-68/CIP-113 indexing) | `UP` (synced to tip), `OUT_OF_SERVICE` (catching up), `DOWN` (connection lost) |

### Assigning to Kubernetes probe groups

The extension contributes the indicators but does **not** decide which probe group they belong to — that is your application's concern. Configure probe groups in your `application.properties`:

```properties
# Startup probe — check DB and node connection before accepting traffic
management.endpoint.health.group.startup.include=db,assetStoreOnchainConnection

# Liveness probe — is the process healthy?
management.endpoint.health.group.liveness.include=assetStoreOnchainConnection

# Readiness probe — is the service ready to serve accurate data?
management.endpoint.health.group.readiness.include=db,assetStoreOffchainSync,assetStoreOnchainReadiness
```

This gives you:
- `GET /actuator/health/startup` — fails if DB or node connection is down (pod restarts)
- `GET /actuator/health/liveness` — fails if node connection drops (pod restarts)
- `GET /actuator/health/readiness` — fails if still syncing or CIP-26 sync error (pod stops receiving traffic until caught up)

## Database

Flyway migrations are included for PostgreSQL, H2, and MySQL. Tables are created automatically when the extension is enabled:

- `token_metadata` — CIP-26 offchain metadata
- `token_logo` — CIP-26 logos
- `off_chain_sync_state` — CIP-26 sync tracking
- `metadata_reference_nft` — CIP-68 reference NFTs
- `cip113_registry_node` — CIP-113 registry nodes
