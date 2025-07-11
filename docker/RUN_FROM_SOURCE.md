# Running Yaci Store from Source Using Docker

This guide explains how to build and run Yaci Store locally using Docker and Docker Compose.

---

## ☕ Prerequisite

To build Yaci Store from source, **Java 21** is required.  
Make sure it's installed and set as your active Java version.

---

## 🔧 1. Build Yaci Store

Run the following command from the project root to build Yaci Store:

```bash
./gradlew clean build
```

---

## 🐳 2. Build the Docker Image

Build the Docker image with the `dev` tag (from the project root folder):

```bash
docker build --target yaci-store -t bloxbean/yaci-store:dev .
```

---

## 📂 3. Run Yaci Store with Docker Compose

1. Navigate to the `docker/compose` folder at the top level of the project:

   ```bash
   cd docker/compose
   ```

2. Create a `.env` file and add the following line:

   ```env
   tag=dev
   ```

3. Start the Yaci Store container with PostgreSQL from `docker` folder:

   ```bash
   docker compose -f compose/yaci-store-monolith.yml up
   ```

This will start the Yaci Store application along with a PostgreSQL instance.

> The configuration files are located under the `docker/config` folder and are mounted into the Docker container.

---

## 📘 Optional: Enable Ledger State

To run Yaci Store with the `ledger-state` profile enabled:

1. Open the `env` file in the `docker` folder.
2. Uncomment the following line:

   ```env
   SPRING_PROFILES_ACTIVE=ledger-state
   ```

---

## ✅ Done

You should now have Yaci Store running locally with Docker!
