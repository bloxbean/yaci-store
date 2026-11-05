-- assets-ext: CIP-26 subjects are stored in lower case, like CIP-68 subjects and the asset units clients query with. A
-- subject is base16, so its case carries no meaning, but a registry file named in upper case was stored as written and
-- could only be found by a query in upper case (39 subjects on mainnet). New entries are lower-cased by the sync
-- (MappingsUtil.normalizeSubject); this lower-cases the ones already stored, so that an upgrade stays in place. The logo
-- is on the same row.

-- An entry in upper case whose lower-case twin is also stored would collide: the twin is kept (none on mainnet).
DELETE FROM cip26_metadata
WHERE subject <> LOWER(subject)
  AND LOWER(subject) IN (SELECT subject FROM cip26_metadata);

UPDATE cip26_metadata SET subject = LOWER(subject) WHERE subject <> LOWER(subject);
