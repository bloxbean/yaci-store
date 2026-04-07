# Yaci Store Docker Distribution

This distribution provides scripts to help you easily run Yaci Store, perform administrative tasks, and monitor the system using Prometheus and Grafana.

For more details and advanced usage, visit the official docs:  
👉 **https://store.yaci.xyz**

## 📦 Contents

Some of the key files and directories included in this distribution:

```

.
├── yaci-store.sh            # Start/stop Yaci Store application
├── admin-cli.sh             # Admin operations (apply-indexes, rollback, etc.)
├── monitoring.sh            # Start/stop Prometheus + Grafana monitoring stack
├── compose/
│   ├── .env                         # Environment file (contains Docker image tag)
│   ├── yaci-store-monolith.yml      # Compose file for Yaci Store and PostgreSQL
│   ├── admin-cli-compose.yml        # Compose file for admin CLI container
│   ├── monitoring.yml               # Compose file for monitoring stack
│   └── ...
├── config/                          # Yaci Store configuration files mounted inside the container
│   ├── env                          # Environment file (e.g., to enable ledger-state profile)
│   ├── application.properties
│   ├── application-ledger-state.properties
│   ├── application-plugins.yml
├── plugin-scripts/          # Folder for custom plugin scripts (mounted inside the container)
├── grafana/                 # Grafana configuration and dashboards
├── prometheus/              # Prometheus configuration

```

## 🖥️ Running Multiple Instances on the Same Host

You can run multiple Yaci Store instances (e.g., mainnet + preprod) on the same server. For each deployment, edit `compose/.env` and set a unique instance name and port set:

```env
# Unique identifier for this deployment (e.g., mainnet, preprod, preview)
COMPOSE_PROJECT_NAME=yaci-store-preprod
INSTANCE_NAME=preprod

YACI_STORE_PORT=8080
YACI_STORE_DB_PORT=54333
MONITORING_PROMETHEUS_PORT=9090
MONITORING_GRAFANA_PORT=3000
```

Then start normally with `./yaci-store.sh start` from each deployment directory.

---

## 🔧 Edit Configuration

Before starting Yaci Store, you may want to update the Cardano node connection details.

Edit the following values in `config/application.properties`:

```properties
store.cardano.host=preprod-node.world.dev.cardano.org
store.cardano.port=30000
store.cardano.protocol-magic=1
```

## 🚀 Running Yaci Store

Use `yaci-store.sh` to start, stop, or view logs of the Yaci Store application.

### ✅ Start

```bash
./yaci-store.sh start
````

This starts Yaci Store and PostgreSQL in the background.

* 📍 Yaci Store API runs on **[http://localhost:8080](http://localhost:8080)**
* 📖 API docs (Swagger UI): **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

### 🛑 Stop both Yaci Store and PostgreSQL

```bash
./yaci-store.sh stop
```

### 🛑 Stop only Yaci Store (PostgreSQL stays running)

```bash
./yaci-store.sh stop:yaci-store
```

### 📋 View logs

```bash
./yaci-store.sh logs                # Logs from all containers
./yaci-store.sh logs:yaci-store     # Only Yaci Store logs
./yaci-store.sh logs:db             # Only PostgreSQL logs
```

---

## ⚙️ Enable Ledger-State Profile

To run Yaci Store with the `ledger-state` profile:

1. Open the `env` file under the `config/` folder.
2. Uncomment or add the following line:

```env
SPRING_PROFILES_ACTIVE=ledger-state
```

3. Start the application:

```bash
./yaci-store.sh start
```

---

## 🛠️ Admin Operations

Use `admin-cli.sh` to run administrative tasks like applying indexes or rolling back to a specific epoch.

### ▶️ Apply Indexes

```bash
./admin-cli.sh apply-indexes
```

> ⚠️ This applies database indexes required for optimal read performance from API endpoints.

> 🕒 **Important:** Only run this **after the full sync is complete**. Applying indexes early can significantly slow down the sync process.

---

### 🔄 Rollback

```bash
./admin-cli.sh rollback-data --epoch <epoch_no>
```

> ⚠️ Yaci Store must be **stopped**, but PostgreSQL must remain **running** before performing rollback.
> In interactive mode, you'll be prompted to confirm.

---

## 📊 Monitoring (Prometheus + Grafana)

Use `monitoring.sh` to start or stop the monitoring stack.

### ✅ Start monitoring services

```bash
./monitoring.sh start
```

* 📍 **Prometheus**: [http://localhost:9090](http://localhost:9090)
* 📍 **Grafana**: [http://localhost:3000](http://localhost:3000)
  Default login: `admin / changeme`

### 🛑 Stop monitoring services

```bash
./monitoring.sh stop
```

### 🔍 Accessing PostgreSQL

To connect to the PostgreSQL database running in Docker, you can use the included `psql.sh` script:

```bash
./psql.sh
```

This script opens an interactive `psql` session inside the running PostgreSQL container.

> 💡 Make sure the Yaci Store stack is already running (`./yaci-store.sh start`) before using this script.
