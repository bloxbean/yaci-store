ALTER TABLE datum ADD COLUMN slot BIGINT;
CREATE INDEX idx_datum_slot ON datum(slot);

ALTER TABLE script ADD COLUMN slot BIGINT;
CREATE INDEX idx_script_slot ON script(slot);
