#!/bin/bash
# usage: tipwait.sh <inst>  -- waits until the instance follows the tip (and its unspent table, if any, is built), then runs the tip matrix
n=$1; KIT=$(cd "$(dirname "$0")" && pwd); R=${RT_HOME:-$HOME/fault-injection-test}; mkdir -p "$R/logs"
q() { docker exec yaci-store-postgres-rt-$n psql -U yaci -d yaci_store -tAc "$1" 2>/dev/null; }
has_tbl=$(q "select count(*) from information_schema.tables where table_name='address_utxo_unspent'")
while true; do
  slot=$(q "select max(slot) from cursor_")
  now=$(( $(date +%s) - 1655683200 ))
  st=ok
  [ "$has_tbl" = "1" ] && st=$(q "select status from store_state where state_id='utxo.unspent_table'")
  echo "$(date -u +%FT%TZ) $n $slot unspent=$st" >> "$R/logs/progress.log"
  [ -n "$slot" ] && [ $((now - slot)) -lt 300 ] && { [ "$st" = "ok" ] || [ "$st" = "CURRENT" ]; } && break
  sleep 60
done
echo "$(date -u) $n at tip, starting tip matrix"
sleep 120
exec python3 "$KIT/runner.py" "$n" tip
