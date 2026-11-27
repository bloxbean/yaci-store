-- Postgres sessions of this database that are not idle (what the store is waiting on).

select pid, state, wait_event_type||':'||wait_event w, date_trunc('second',now()-xact_start) xact,
       left(regexp_replace(query,'\s+',' ','g'),90) q
from pg_stat_activity where datname=current_database() and state<>'idle' order by xact_start;
