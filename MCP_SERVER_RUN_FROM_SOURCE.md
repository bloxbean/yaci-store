# Running Yaci Store with MCP Server Support from Source

This guide provides step-by-step instructions to build and run Yaci Store with MCP server support from source.

## Prerequisites

- **Java 21** - Make sure Java 21 is installed and configured
- **Docker** - Docker installed and running
- **Git** - Git command line tools

## Step 1: Clone the Repository

```bash
git clone https://github.com/bloxbean/yaci-store.git
cd yaci-store
```

## Step 2: Checkout the MCP Server Branch

The MCP server module is only available in the `feat/mcp_server` branch:

```bash
git checkout feat/mcp_server
```

## Step 3: Build the Project

Build the project JARs using Gradle (skipping tests for faster build):

```bash
./gradlew clean build -x test
```

## Step 4: Build Docker Images

Build the required Docker images with the `mcp_dev` tag:

```bash
# Build Yaci Store image
docker build --target yaci-store -t bloxbean/yaci-store:mcp_dev .

# Build Yaci Store Admin CLI image
docker build --target yaci-store-admin-cli -t bloxbean/yaci-store-admin-cli:mcp_dev .
```

## Step 5: Configure Docker Compose

Navigate to the docker folder:

```bash
cd docker
```

### 5.1 Create Docker Compose Environment File

Create an `.env` file in the `docker/compose/` directory:

```bash
# Create the file
cat > compose/.env << EOF
tag=mcp_dev
EOF
```

This ensures Docker Compose uses the locally built `mcp_dev` tag.

### 5.2 Configure Spring Profiles

Edit the `docker/config/env` file to enable both `ledger-state` and `mcp` profiles:

```properties
SPRING_PROFILES_ACTIVE=ledger-state,mcp
```

The `ledger-state` profile is required for ada pot calculations and governance state calculations.

### 5.3 Configure Cardano Node Connection

Edit `docker/config/application.properties` to point to your Cardano node endpoint.

Update the following properties as needed:

```properties
# Example configuration (adjust based on your setup)
cardano.node.host=<your-node-host>
cardano.node.port=<your-node-port>
```

### 5.4 Enable Account Balance Calculation

Edit `docker/config/application-ledger-state.properties` to enable account balance features.

Make sure the following properties are set:

```properties
store.account.address-balance-enabled=true
store.account.stake-address-balance-enabled=true
```

Also ensure these properties are uncommented and enabled:

```properties
store.account.content-aware-rollback=true
store.account.current-balance-enabled=true
```

## Step 6: Start Yaci Store

From the `docker` folder, start Yaci Store:

```bash
./yaci-store.sh start
```

## Step 7: Post-Sync Database Setup (Required for MCP Tools)

⚠️ **Important**: These steps must be performed AFTER the initial blockchain sync completes and reaches the tip.

### 7.1 Wait for Initial Sync to Complete

Monitor the sync progress by watching the logs:

```bash
# From the docker folder
tail -f logs/ledger-state.log
```

Look for log messages indicating sync has reached the tip. The sync process can take several hours to days depending on the network (preprod vs mainnet) and your hardware.

### 7.2 Apply Core Performance Indexes

Once sync reaches the tip, apply the core read-optimized indexes:

```bash
# From the docker folder
./admin-cli.sh
```

At the interactive prompt, run:

```
apply-index
```

This applies all performance indexes required by Yaci Store query tools. This step is essential for optimal query performance.

**Note**: The `apply-index` command must be run AFTER sync reaches the tip to ensure indexes are built efficiently.

### 7.3 Execute MCP-Specific SQL Files

The MCP server requires additional database views and materialized views for advanced query capabilities. Execute the following SQL files in order:

**Connect to PostgreSQL**:

```bash
# From the yaci-store root directory
PGPASSWORD=dbpass psql -h localhost -p 54333 -U yaci -d yaci_store
```

**Execute SQL files in this order**:

```sql
-- 1. Core MCP server performance indexes
\i aggregates/mcp-server/sql/indexes.sql

-- 2. Flattened UTXO views for dynamic queries
\i aggregates/mcp-server/sql/create_utxo_views.sql

-- 3. Payment credential lookup materialized view (refreshes daily)
\i aggregates/mcp-server/sql/address-credential-mapping.sql

-- 4. Token holder statistics materialized view (refreshes every 3 hours)
\i aggregates/mcp-server/sql/token-holder-summary-mv.sql

-- 5. Portfolio diversity stats materialized view (refreshes every 12 hours)
\i aggregates/mcp-server/sql/address-token-diversity-mv.sql

-- 6. Address Balance indexes
\i aggregates/mcp-server/sql/abc-index.sql    

-- Exit psql
\q
```

**Files to EXCLUDE**:
- `abc-queries.sql` - Sample queries for testing only

**What these SQL files do**:

1. **indexes.sql**: Creates performance indexes for MCP query tools (utxo_balance_aggregation, token_holders, etc.)
2. **create_utxo_views.sql**: Creates flattened views that simplify UTXO queries with assets
3. **address-credential-mapping.sql**: Materialized view for fast payment credential lookups (enables Franken address search)
4. **token-holder-summary-mv**: Pre-aggregated token holder statistics (546x-72,000x faster queries)
5. **address-token-diversity-mv**: Pre-aggregated portfolio diversity metrics (1,090x faster queries)
6. **abc-index.sql**: Creates additional indexes for address_balance and address_balance_current tables

You should see three materialized views:
- `address_credential_mapping`
- `address_token_diversity_mv`
- `token_holder_summary_mv`

**Materialized View Refresh Schedule**:
- `address_credential_mapping`: Daily at 2:00 AM
- `token_holder_summary_mv`: Every 3 hours
- `address_token_diversity_mv`: Every 12 hours

These are refreshed automatically by the MCP server's scheduled tasks.

## Verification

Once started, you can verify the MCP server is running by checking:

1. **Logs**: Check the container logs for successful startup
   ```bash
   tail -f logs/ledger-state.log
   ```

2. **MCP Endpoint**: The MCP server should be accessible (default port configuration in your setup) Example: http://localhost:8080/sse

## Stopping Yaci Store

To stop the services:

```bash
./yaci-store.sh stop
```

## Troubleshooting

### Java Version Issues

Ensure Java 21 is active:

```bash
java -version
```

## Notes

- The `feat/mcp_server` branch contains experimental MCP server features
- Make sure to use the `mcp_dev` tag consistently across all configurations
- The ledger-state profile is essential for ada pot and governance calculations
