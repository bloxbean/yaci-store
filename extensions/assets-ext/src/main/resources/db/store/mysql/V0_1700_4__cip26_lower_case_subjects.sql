-- assets-ext: CIP-26 subjects are stored in lower case, like CIP-68 subjects and the asset units clients query with. A
-- subject is base16, so its case carries no meaning, but a registry file named in upper case was stored as written and
-- could only be found by a query in upper case (39 subjects on mainnet). New entries are lower-cased by the sync
-- (MappingsUtil.normalizeSubject); this lower-cases the ones already stored, so that an upgrade stays in place. The logo
-- is on the same row.

-- The comparison is binary: with the usual case-insensitive collation, 'ABC' = 'abc' and the row would be left as it is.
-- That collation also makes the primary key case-insensitive, so an upper-case entry cannot have a lower-case twin here.
UPDATE cip26_metadata SET subject = LOWER(subject) WHERE BINARY subject <> BINARY LOWER(subject);
