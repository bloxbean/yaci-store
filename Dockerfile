ARG JDK_TAG=24.0.1_9-jre-ubi9-minimal

FROM eclipse-temurin:${JDK_TAG} AS build
WORKDIR /app

FROM eclipse-temurin:${JDK_TAG} AS yaci-store
WORKDIR /app
COPY applications/all/build/libs/yaci-store*.jar /app/yaci-store.jar
COPY components/plugin-polyglot/build/libs/yaci-store-plugin-polyglot*.jar /app/plugins/yaci-store-plugin-polyglot.jar
COPY components/plugin-polyglot/build/libs/plugin-libs/*.jar /app/plugins/lib/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store.jar"]

FROM eclipse-temurin:${JDK_TAG} AS yaci-store-ledger-state
WORKDIR /app
COPY applications/ledger-state/build/libs/yaci-store-ledger-state*.jar /app/yaci-store-ledger-state.jar
COPY components/plugin-polyglot/build/libs/yaci-store-plugin-polyglot*.jar /app/plugins/yaci-store-plugin-polyglot.jar
COPY components/plugin-polyglot/build/libs/plugin-libs/*.jar /app/plugins/lib/
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "yaci-store-ledger-state.jar"]

FROM eclipse-temurin:${JDK_TAG} AS yaci-store-utxo-indexer
WORKDIR /app
COPY applications/utxo-indexer/build/libs/yaci-store-utxo-indexer*.jar /app/yaci-store-utxo-indexer.jar
COPY components/plugin-polyglot/build/libs/yaci-store-plugin-polyglot*.jar /app/plugins/yaci-store-plugin-polyglot.jar
COPY components/plugin-polyglot/build/libs/plugin-libs/*.jar /app/plugins/lib/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-utxo-indexer.jar"]

FROM eclipse-temurin:${JDK_TAG} AS yaci-store-admin
WORKDIR /app
COPY applications/admin/build/libs/yaci-store-admin*.jar /app/yaci-store-admin.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-admin.jar"]

FROM eclipse-temurin:${JDK_TAG} AS yaci-store-admin-cli
WORKDIR /app
COPY applications/admin-cli/build/libs/yaci-store-admin-cli*.jar /app/yaci-store-admin-cli.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-admin-cli.jar"]
