#!/usr/bin/env bash
# usage: inst.sh <name> init|pg|proxy|start|stop|kill|status [extra app args]
# One yaci-store instance for the fault-injection test: its config, Postgres container, fault proxies and JVM.
# Instances are listed in instances.conf. See README.md.
set -u
KIT=$(cd "$(dirname "$0")" && pwd)
R=${RT_HOME:-$HOME/fault-injection-test}
JAVA_BIN=${RT_JAVA_HOME:-${JAVA_HOME:-}}; JAVA=${JAVA_BIN:+$JAVA_BIN/bin/}java
BYTEMAN=${RT_BYTEMAN_JAR:-$R/byteman.jar}
RELAY=${RT_RELAY:-preprod-node.play.dev.cardano.org:3001}
BASE_PROPS=${RT_BASE_PROPERTIES:-$KIT/../../config/application.properties}
CONF=$KIT/instances.conf; [ -f "$R/instances.conf" ] && CONF=$R/instances.conf
N=$1; CMD=$2; shift 2
read -r I KIND PROXIED PROFILES EXTRA < <(awk -v n="$N" '$1 == n && $1 !~ /^#/ {print $2, $3, $4, $5, $6}' "$CONF")
[ -n "${I:-}" ] || { echo "unknown instance $N (see $CONF)"; exit 2; }
[ "$EXTRA" = "-" ] && EXTRA=""
APP=$((18100+I)); PG=$((54370+I)); DBP=$((54380+I)); NP=$((13100+I)); NC=$((14100+I)); DC=$((14110+I))
C=yaci-store-postgres-rt-$N; V=rt-$N-pg; D=$R/inst/$N
JAR=$(ls -t "$R"/jars/"$KIND"-*.jar 2>/dev/null | head -1)
pid() { cat "$D/app.pid" 2>/dev/null; }
alive() { local p; p=$(pid); [ -n "$p" ] && kill -0 "$p" 2>/dev/null; }
case $CMD in
  init)
    mkdir -p "$D/config" "$D/logs" "$D/trig"
    cp "$BASE_PROPS" "$D/config/application.properties"
    NODEH=${RELAY%:*}; NODEP=${RELAY#*:}; DBPORT=$PG
    if [ "$PROXIED" = 1 ]; then NODEH=127.0.0.1; NODEP=$NP; DBPORT=$DBP; fi
    cat > "$D/config/application-rt.properties" <<PROPS
# Auto-recovery resilience test, instance $N ($KIND). Rosetta-style settings.
server.port=$APP
store.cardano.host=$NODEH
store.cardano.port=$NODEP
store.cardano.protocol-magic=1
spring.datasource.url=jdbc:postgresql://127.0.0.1:$DBPORT/yaci_store
spring.datasource.username=yaci
spring.datasource.password=rt
spring.datasource.hikari.maximum-pool-size=30
spring.threads.virtual.enabled=true
store.executor.enable-parallel-processing=true
store.continue-on-parse-error=true
store.admin.auto-recovery-enabled=true
store.admin.health-check-interval=30
store.block-receive-delay-seconds=120
store.live.enabled=false
logging.file.name=$D/logs/yaci-store.log
logging.logback.rollingpolicy.max-file-size=500MB
logging.logback.rollingpolicy.max-history=20
PROPS
    case ",$PROFILES," in *,ledger-state,*) ;; *) printf 'store.adapot.enabled=false\nstore.governance-aggr.enabled=false\n' >> "$D/config/application-rt.properties" ;; esac
    if [ -n "$EXTRA" ]; then echo "$EXTRA" | tr ',' '\n' >> "$D/config/application-rt.properties"; fi
    ;;
  pg)
    docker run -d --name "$C" --restart=no -e POSTGRES_USER=yaci -e POSTGRES_PASSWORD=rt -e POSTGRES_DB=yaci_store \
      --shm-size=2g -p 127.0.0.1:$PG:5432 -v "$V:/var/lib/postgresql" postgres:18 \
      -c shared_buffers=2GB -c max_wal_size=8GB -c max_locks_per_transaction=1024 -c max_connections=200 >/dev/null
    until docker exec "$C" pg_isready -U yaci -d yaci_store -q 2>/dev/null; do sleep 1; done; echo "pg $C ready on $PG"
    ;;
  proxy)
    [ "$PROXIED" = 1 ] || { echo "no proxies for $N"; exit 0; }
    #A proxy left from an earlier run would answer on these ports, and faults would go to it instead
    for p in $NP $NC $DBP $DC; do
      if (exec 3<>/dev/tcp/127.0.0.1/$p) 2>/dev/null; then
        echo "port $p is already in use (an old fault-proxy.py?): stop that process first"; exit 2
      fi
    done
    setsid nohup python3 "$KIT/fault-proxy.py" --mux --listen $NP --target $RELAY --ctl $NC >> "$D/logs/node-proxy.log" 2>&1 < /dev/null &
    setsid nohup python3 "$KIT/fault-proxy.py" --listen $DBP --target 127.0.0.1:$PG --ctl $DC >> "$D/logs/db-proxy.log" 2>&1 < /dev/null &
    sleep 1; python3 "$KIT/faultctl.py" $NC status; python3 "$KIT/faultctl.py" $DC status
    ;;
  start)
    alive && { echo "already running $(pid)"; exit 0; }
    [ -n "$JAR" ] || { echo "no $R/jars/$KIND-*.jar: run build.sh first"; exit 2; }
    [ -f "$BYTEMAN" ] || { echo "no Byteman agent at $BYTEMAN (see README.md)"; exit 2; }
    cd "$D"
    setsid nohup "$JAVA" -Xmx6g -XX:+ExitOnOutOfMemoryError -Drt.trig="$D/trig" \
      -javaagent:"$BYTEMAN=script:$KIT/rules.btm" \
      -jar "$JAR" --spring.profiles.active=$PROFILES "$@" >> "$D/logs/console.out" 2>&1 < /dev/null &
    echo $! > "$D/app.pid"; echo "started $N pid $! jar $(basename "$JAR")"
    ;;
  stop) alive && kill "$(pid)"; for i in $(seq 1 60); do alive || break; sleep 1; done; alive && echo "still running" || echo stopped ;;
  kill) alive && kill -9 "$(pid)"; echo killed ;;
  status) alive && echo "$N up pid $(pid)" || echo "$N down"; docker exec "$C" psql -U yaci -d yaci_store -Atc "select block_number, slot, to_timestamp(slot+1655683200) from cursor_ order by slot desc limit 1" ;;
esac
