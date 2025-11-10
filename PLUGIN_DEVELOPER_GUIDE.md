# Plugin Developer Guide: Testing & Development

This guide is for **Yaci Store plugin framework developers** who need to quickly build, test, and verify plugin functionality (filters, schedulers, actions) during development.

---

## Quick Summary

**Fastest Path to Testing:**
1. Build the project with polyglot support
2. Start PostgreSQL
3. Run with the dev script
4. Verify plugin execution in logs

---

## Prerequisites

- Java 21+
- Docker (for PostgreSQL)
- Gradle (for building)

---

## Step 1: Build the Project

Build Yaci Store with plugin support:

```bash
# Clean build with polyglot plugin libraries
./gradlew clean build -x test

# Verify polyglot libraries were built
ls -lh components/plugin-polyglot/build/libs/plugin-libs/
# Should show: graalvm-*, truffle-*, js-*, python-* JARs
```

**What gets built:**
- Main JAR: `applications/all/build/libs/yaci-store-*.jar` (includes MVEL support)
- Polyglot JAR: `components/plugin-polyglot/build/libs/plugin-polyglot-*.jar`
- Polyglot libs: `components/plugin-polyglot/build/libs/plugin-libs/*.jar` 

---

## Step 2: Start PostgreSQL

```bash
# Start PostgreSQL container
cd docker
docker compose -f compose/postgres-compose.yml up -d
cd ..

# Verify it's running
docker ps | grep postgres
# Should show: yaci-store-postgres running on port 54333
```

**Database connection details:**
- Host: `localhost:54333`
- Database: `yaci_store`
- User: `yaci`
- Password: `dbpass`

---

## Step 3: Run with Plugin Support

The `dev-run.sh` script simplifies running Yaci Store with different profiles and plugin configurations.

Update `application.properties` to configure Postgres db connection details.

### Option A: Test Filter Plugins

**1. Enable plugins in `config/application-plugins.yml`:**
```yaml
store:
  plugins:
    enabled: true  # Change from false to true
```

**2. Add following filter in config:**
```yaml
filters:
  block.save:
    - name: "simple-counter-test"
      lang: "js"
      inline-script: |
        var count = state.get('count') || 0;
        count++;
        state.put('count', count);
        console.log('>>> FILTER EXECUTED - Count: ' + count + ' - Items: ' + items.length);
        return items;
```

**3. Run the application:**
```bash
./dev-run.sh --enable-plugins
```

**What this does:**
- `--enable-plugins` loads GraalVM polyglot libraries for JavaScript/Python
- Uses config from `config/application-plugins.yml`
- Note: `plugins` profile is included by default in the JAR

**4. Watch for in logs:**
```
INFO  - Initializing PluginRegistry...
INFO  - Created 1 filter plugin(s) for filter key: block.save
>>> FILTER EXECUTED - Count: 1 - Items: 1
>>> FILTER EXECUTED - Count: 2 - Items: 1
```

### Option B: Test Scheduler Plugins

**1. Enable plugins in `config/application-plugins.yml`:**
```yaml
store:
  plugins:
    enabled: true  # Change from false to true
```

**2. Add scheduler plugin to config:**
```yaml
schedulers:
  - name: "my-test-scheduler"
    lang: "js"
    inline-script: |
      var count = state.get('count') || 0;
      count++;
      state.put('count', count);
      console.log('>>> Scheduler executed: ' + count);
    schedule:
      type: INTERVAL
      value: "10"  # seconds
```

**3. Run the application:**
```bash
./dev-run.sh --enable-plugins
```

**4. Watch for scheduler execution:**
```
>>> MVEL Scheduler executed! Count: 1
>>> JavaScript Scheduler executed! Count: 1
```

### Option C: Test with Ledger State Profile

```bash
./dev-run.sh --profile ledger-state
```

### Option D: Test Multiple Profiles

```bash
./dev-run.sh --profile plugins,ledger-state
```

### Option E: Test with Custom JVM Options

```bash
./dev-run.sh --profile plugins --jvm-opts "-Xmx8g -Xms4g"
```

---

## Step 4: Verify Plugin Execution

### Check Logs

Watch for plugin initialization:
```
INFO  - Initializing PluginRegistry...
INFO  - Plugin support enabled: true
INFO  - Registered 1 filter plugin(s)
```

**MVEL plugins work without polyglot libraries.**
