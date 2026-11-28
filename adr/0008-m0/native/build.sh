#!/bin/bash
# Native-image smoke test for the DuckDB JDBC driver.
# usage (inside a GraalVM/Mandrel builder container, with the driver jar in /w):
#   docker run --rm --platform linux/arm64 --network none -v "$PWD":/w --entrypoint bash \
#     quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25 /w/build.sh <duckdb_jdbc jar> <platform>
# <platform> is the jar's library suffix, e.g. linux_arm64 or linux_amd64.
set -e
JAR=${1:?driver jar}; PLAT=${2:?platform}
cd /w && mkdir -p out && javac -cp "$JAR" -d out Smoke.java
native-image --no-fallback -cp "out:$JAR" -o smoke-plain Smoke >/tmp/b1.log 2>&1 && echo "build plain: ok" || { echo "build plain: FAILED"; tail -5 /tmp/b1.log; }
native-image --no-fallback -cp "out:$JAR" -H:IncludeResources="libduckdb_java.so_$PLAT" -o smoke-res Smoke >/tmp/b2.log 2>&1 && echo "build with resource: ok" || { echo "build with resource: FAILED"; tail -5 /tmp/b2.log; }
for b in smoke-plain smoke-res; do [ -x $b ] && { echo "== run $b"; ./$b 2>&1 | grep -E 'native OK|Exception|Error|Caused' | head -3; }; done
