alter table reward_calc_jobs
    add column time_taken bigint,
    add column error_message text;
