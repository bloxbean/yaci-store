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

## Verification

Once started, you can verify the MCP server is running by checking:

1. **Logs**: Check the container logs for successful startup
   ```bash
   tail -f logs/ledger-state.log
   ```

2. **MCP Endpoint**: The MCP server should be accessible (default port configuration in your setup)

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
