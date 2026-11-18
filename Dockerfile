# Runtime base image. Amazon Corretto 25 LTS (GPLv2 + Classpath Exception) on Amazon Linux 2023.
# 25.0.5.1 fixes CVE-2026-114938 ("Java4Shell"); JDK 24 is end-of-life and gets no fix.
# Pinned by multi-arch index digest (linux/amd64 + linux/arm64). Override with --build-arg BASE_IMAGE=...
ARG BASE_IMAGE=amazoncorretto:25.0.5-al2023-headless@sha256:d43cc2b87faf3e80d04659d315dd9f1dba2c1176cc9b48d9daed285856ac056c

FROM ${BASE_IMAGE} AS build
WORKDIR /app

FROM ${BASE_IMAGE} AS yaci-store
WORKDIR /app
COPY applications/all/build/libs/yaci-store*.jar /app/yaci-store.jar
COPY components/plugin-polyglot/build/libs/yaci-store-plugin-polyglot*.jar /app/plugins/yaci-store-plugin-polyglot.jar
COPY components/plugin-polyglot/build/libs/plugin-libs/*.jar /app/plugins/lib/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store.jar"]

FROM ${BASE_IMAGE} AS yaci-store-ledger-state
WORKDIR /app
COPY applications/ledger-state/build/libs/yaci-store-ledger-state*.jar /app/yaci-store-ledger-state.jar
COPY components/plugin-polyglot/build/libs/yaci-store-plugin-polyglot*.jar /app/plugins/yaci-store-plugin-polyglot.jar
COPY components/plugin-polyglot/build/libs/plugin-libs/*.jar /app/plugins/lib/
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "yaci-store-ledger-state.jar"]

FROM ${BASE_IMAGE} AS yaci-store-utxo-indexer
WORKDIR /app
COPY applications/utxo-indexer/build/libs/yaci-store-utxo-indexer*.jar /app/yaci-store-utxo-indexer.jar
COPY components/plugin-polyglot/build/libs/yaci-store-plugin-polyglot*.jar /app/plugins/yaci-store-plugin-polyglot.jar
COPY components/plugin-polyglot/build/libs/plugin-libs/*.jar /app/plugins/lib/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-utxo-indexer.jar"]

FROM ${BASE_IMAGE} AS yaci-store-admin
WORKDIR /app
COPY applications/admin/build/libs/yaci-store-admin*.jar /app/yaci-store-admin.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-admin.jar"]

FROM ${BASE_IMAGE} AS yaci-store-admin-cli
WORKDIR /app
COPY applications/admin-cli/build/libs/yaci-store-admin-cli*.jar /app/yaci-store-admin-cli.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-admin-cli.jar"]
