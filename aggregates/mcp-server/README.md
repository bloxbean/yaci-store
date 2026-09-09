# MCP Server

The mcp-server module exposes yaci-store to AI agents over the
[Model Context Protocol](https://modelcontextprotocol.io). It registers 27 tools — ad-hoc
analytics SQL, address balances, dApp identification, token/IPFS metadata and Cardano conversion
utilities — through Spring AI's MCP server.

It is **off by default.** The analytics tools include unauthenticated ad-hoc SQL over the exported
chain data, so turning them on is a deliberate operator decision.

## Quickstart

Activate the `mcp` profile (and `analytics` for the SQL tools):

```bash
java -jar yaci-store.jar --spring.profiles.active=analytics,mcp
```

`config/application-mcp.yml` sets `yaci.store.mcp-server.enabled=true`, which is the only switch
that matters. Point an MCP client at:

```
http://localhost:8080/mcp
```

Without the profile, `/mcp` returns 404 and no tools exist.

## How the opt-in works

Three layers, all keyed to `yaci.store.mcp-server.enabled` (default `false`):

1. **`McpServerConfig` is conditional.** `@ConditionalOnProperty(prefix = "yaci.store.mcp-server",
   name = "enabled", havingValue = "true")` — no `matchIfMissing`, so an absent property means off.
2. **The `@ComponentScan` sits on that conditional class.** The tool services live in
   `com.bloxbean.cardano.yaci.store.mcp.server.*`, which no application scans on its own
   (`YaciStoreApplication` is `@SpringBootApplication` in `...store.app`). With the flag off they
   are never instantiated — including the scheduled dApp-registry sync, so no background job runs
   either.
3. **The HTTP endpoint follows the same flag.** `applications/all`'s `application.yml` sets
   `spring.ai.mcp.server.enabled: ${yaci.store.mcp-server.enabled:false}`, so Spring AI's `/mcp`
   endpoint does not exist by default — not merely "registers no tools".

A second, independent gate sits underneath: `McpAnalyticsService` and `McpBalanceService` are also
`@ConditionalOnProperty(yaci.store.analytics.query.enabled=true)`. Enabling the MCP server alone
gives you the utility and dApp tools but **not** ad-hoc SQL.

`McpServerConfig` injects every service `@Autowired(required = false)` and registers only the ones
that exist, so the tool set composes from whichever subsystems are enabled.

## Tools

### Analytics (require `yaci.store.analytics.query.enabled=true`)

| Tool | Purpose |
|---|---|
| `analytics-list-tables` | Discover tables: row counts, partitioning, date ranges, `dataScope`, row limits. Call this first. |
| `analytics-describe-table` | Column names and DuckDB types for one table |
| `analytics-execute-sql` | Read-only DuckDB SQL, with optional `maxRows` and `timeoutSeconds` |
| `analytics-address-balance` | Real-time balance of one address, from live PostgreSQL |
| `analytics-top-balances` | Top N addresses by ADA balance, from the exported data |

### dApp registry

| Tool | Purpose |
|---|---|
| `dapp-lookup` | Find a dApp by name |
| `dapp-reverse-lookup` | Identify a dApp from an address, policy ID or script hash |
| `dapp-list-by-category` | List by category (DEX, NFT Marketplace, Lending, ...) |
| `dapp-list-all` | List registered dApps |
| `dapp-registry-status` | Registry size, index sizes, last sync |

### External metadata (outbound HTTP)

| Tool | Purpose |
|---|---|
| `get-token-registry-metadata` | Cardano Token Registry metadata for one asset |
| `get-token-registry-metadata-batch` | Same, batched (max 100), fetched on virtual threads |
| `fetch-ipfs-content` | Fetch an IPFS document (e.g. a governance anchor) |

### Cardano utilities (no external calls, no database)

| Tool | Purpose |
|---|---|
| `cardano-network-info` | Network type, protocol magic, slot lengths, epoch length, genesis start |
| `cardano-blockchain-time-info` | Slot/time context for a slot or timestamp |
| `cardano-slot-to-timestamp` / `cardano-timestamp-to-slot` | Slot ↔ wall-clock conversion |
| `cardano-slots-to-timestamps-batch` | Batched slot → timestamp |
| `cardano-format-timestamp` | Human-readable timestamp formatting |
| `cardano-amount-units-info` | Lovelace/ADA unit reference |
| `convert-lovelace-to-ada` | Lovelace → ADA |
| `convert-metadata-cbor-to-json` / `convert-datum-cbor-to-json` | CBOR → JSON |
| `extract-stake-address` | Stake address from a base address |
| `address-to-payment-hash` | Payment key/script hash from an address |
| `script-hash-to-address` | Enterprise script address from a script hash |
| `gov-action-id-from-bech32` | Decode a bech32 governance action ID |

`cardano-network-info` is worth calling before any slot-range query — it carries the conversion
guide that keeps time→slot arithmetic correct.

## Configuration

```yaml
yaci:
  store:
    mcp-server:
      enabled: true                    # the switch; default false

      dapp-registry:
        enabled: true                  # default true (only reachable when mcp-server is on)
        external-registry:
          enabled: false               # default false — no outbound HTTP unless you ask
          url: https://raw.githubusercontent.com/Cardano-Fans/crfa-offchain-data-registry/main/dApps
          schedule: "0 0 2 * * ?"      # daily 02:00
          auto-merge: true
          timeout-seconds: 30
          fail-silently: true
        dapps: {}                      # locally configured entries, keyed by network

      tools:
        external-metadata:
          enabled: true                # token registry + IPFS tools
```

Local dApp entries take `name`, `displayName`, `category`, `description`, `scriptAddresses`,
`policyIds` and `contractHashes`. With `external-registry.enabled=false` and no local `dapps`, the
registry is empty and `dapp-reverse-lookup` returns nothing — `dapp-registry-status` will show
`totalDApps: 0` and `lastSuccessfulSync: "Never"`.

Row limits and timeouts for the analytics tools come from the query layer
(`yaci.store.analytics.query.*`), not from here. The effective values are reported in every
`analytics-execute-sql` result (`row_limit`, `timeout_seconds`) and in the `row_limit` hint of
`analytics-list-tables`.

## Architecture

The analytics tools are a thin façade over
[analytics-query](../analytics-query/README.md) — the same Spring beans the REST API uses,
called in-process rather than over HTTP:

```
MCP client  ──▶  /mcp (Spring AI)  ──▶  McpAnalyticsService
                                          │  AnalyticsSchemaService
                                          │  SqlValidator
                                          └▶ AnalyticsQueryExecutor
                                               └▶ ParquetReadConnectionProvider (DuckDB)
                                                    ├─ exported Parquet / DuckLake
                                                    └─ live PostgreSQL (when federation is on)
```

So MCP inherits the query layer's SQL validation, row caps and timeout ceilings automatically, and
picks up live-data federation with no MCP-side change: when
`yaci.store.analytics.query.live-data-enabled=true`, `SELECT ... FROM block` resolves to the
unified view and reaches the chain tip.

One deliberate exception: `analytics-address-balance` bypasses the unified views and queries live
PostgreSQL directly with a parameterized statement. A UTXO created before the federation boundary
(in Parquet) can be spent after it (in PostgreSQL), which would make an anti-join report it as
unspent.

```
mcp/server/
  analytics/  - McpAnalyticsService (SQL tools), McpBalanceService (balances)
  config/     - McpServerConfig: the conditional gate and ToolCallbackProvider
  dapp/       - McpDAppRegistryService, ExternalDAppRegistryFetcher, DAppRegistryProperties
  external/   - McpExternalMetadataService (token registry, IPFS)
  model/      - Tool response records
  util/       - McpCardanoUtilService (conversions, address/slot helpers)
```

## Security notes

- Ad-hoc SQL through `analytics-execute-sql` is **unauthenticated**, exactly like the analytics
  REST `/sql` endpoint. Both are off by default; put an authenticating proxy in front of `/mcp`
  before exposing it beyond localhost.
- Statements are validated by the query layer's `SqlValidator` (single `SELECT`/`WITH` only,
  blocked keywords and functions, no filesystem or remote-URL literals) and run on a read-only
  DuckDB connection.
- `dapp-registry.external-registry` and `tools.external-metadata` perform **outbound HTTP**
  (GitHub, the Cardano Token Registry, IPFS gateways). Leave them off in an egress-restricted
  deployment.
- Errors surface to the model as `CallToolResult(isError=true, message)` with sanitized text, so a
  failed query returns a correctable reason rather than a stack trace.
