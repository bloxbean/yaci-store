FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

FROM eclipse-temurin:21-jdk AS yaci-store-all
WORKDIR /app
COPY applications/all/build/libs/yaci-store-all*.jar /app/yaci-store-all.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-all.jar"]

FROM eclipse-temurin:21-jdk AS yaci-store-aggregation-app
WORKDIR /app
COPY applications/aggregation-app/build/libs/yaci-store-aggregation-app*.jar /app/yaci-store-aggregation-app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "yaci-store-aggregation-app.jar"]

FROM eclipse-temurin:21-jdk AS yaci-store-utxo-indexer
WORKDIR /app
COPY applications/utxo-indexer/build/libs/yaci-store-utxo-indexer*.jar /app/yaci-store-utxo-indexer.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-utxo-indexer.jar"]

FROM eclipse-temurin:21-jdk AS yaci-store-admin
WORKDIR /app
COPY applications/admin/build/libs/yaci-store-admin*.jar /app/yaci-store-admin.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "yaci-store-admin.jar"]
