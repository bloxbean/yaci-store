-- Spring Batch 6 renamed BATCH_JOB_SEQ to BATCH_JOB_INSTANCE_SEQ (Spring Batch migration/6.0).
-- H2 cannot rename a sequence: create the new one after the last job instance id, then drop the old one.
CREATE SEQUENCE BATCH_JOB_INSTANCE_SEQ;
ALTER SEQUENCE BATCH_JOB_INSTANCE_SEQ RESTART WITH (SELECT COALESCE(MAX(JOB_INSTANCE_ID), 0) + 1 FROM BATCH_JOB_INSTANCE);
DROP SEQUENCE BATCH_JOB_SEQ;
