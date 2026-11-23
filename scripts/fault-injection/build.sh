#!/usr/bin/env bash
# usage: build.sh <jar-name> <git-ref> [PR number ...]
# Builds a yaci-store jar from <git-ref> of bloxbean/yaci-store, with the given pull requests merged on top, into
# $RT_HOME/jars/<jar-name>-<commit>.jar. inst.sh picks the newest <jar-name>-*.jar of an instance (see instances.conf).
#
#   build.sh baseline origin/main
#   build.sh candidate origin/main 1313 1271 1266
#   build.sh candidate origin/test/recovery-combined
set -eu
[ $# -ge 2 ] || { sed -n 2,8p "$0"; exit 2; }
NAME=$1; REF=$2; shift 2
R=${RT_HOME:-$HOME/fault-injection-test}; mkdir -p "$R/jars" "$R/logs"; cd "$R"
if [ -n "${RT_JAVA_HOME:-}" ]; then export JAVA_HOME=$RT_JAVA_HOME PATH=$RT_JAVA_HOME/bin:$PATH; fi
[ -d src/.git ] || git clone -q https://github.com/bloxbean/yaci-store.git src
cd src
git config user.name fault-injection-test; git config user.email fault-injection-test@localhost
git fetch -q origin '+refs/heads/*:refs/remotes/origin/*'
for p in "$@"; do git fetch -q origin "pull/$p/head:refs/remotes/pr/$p"; done
git checkout -q --detach "$REF"
for p in "$@"; do git merge -q --no-edit "refs/remotes/pr/$p"; done
C=$(git rev-parse --short HEAD)
./gradlew clean :applications:all:build -x test -x javadoc -PskipSigning=true --no-daemon -q
cp "$(ls -S applications/all/build/libs/yaci-store-*.jar | grep -vE 'plain|sources|javadoc' | head -1)" "$R/jars/$NAME-$C.jar"
{ echo "$(date -u +%FT%TZ) $NAME-$C.jar = $REF $(git rev-parse --short "$REF")"; for p in "$@"; do echo "  + PR #$p $(git rev-parse --short "refs/remotes/pr/$p")"; done; } >> "$R/jars/BUILD_INFO"
echo "built $R/jars/$NAME-$C.jar"
