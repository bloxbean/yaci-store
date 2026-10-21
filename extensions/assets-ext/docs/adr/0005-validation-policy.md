# ADR 0005: Validation policy: what is checked, where the checks live, and what is reported

## Status

Proposed. It applies ADR 0006 (validation errors are reported, not enforced) and is implemented in PR #1220 (open).

## Date

2026-10-14

## Context

ADR 0001 and ADR 0002 each decide, field by field, what the module does with input that does not fit a standard. Read together
they leave three questions unanswered, and a new contributor has to reconstruct the answers from the code:

- Is there one rule, or one per standard?
- Where do the checks live, in the database or in code? Nothing says why.
- What does a check that fails leave behind?

This record states the policy in one place. It does not repeat the per-field rules: those are in ADR 0001 decision 4 and ADR 0002
decisions 3 and 4.

## Decisions

### 1. One rule for both standards: index it, and report what is wrong

For CIP-26 and CIP-68 alike, a token that breaks its CIP is **stored as written and served**, with a `validation_errors` entry for
each way it departs from the CIP (ADR 0006). Each entry says whether the CIP requires the field (`MANDATORY`) or not (`OPTIONAL`),
which standard and, for CIP-68, which label it was checked for. Nothing is dropped and nothing is repaired because of the CIP.

*Why:* the module indexes what is on-chain and in the registry. Whether a token is acceptable is for its owner to fix and for a
wallet to decide, and both need to see the token and the reason. *Trade-off:* the API returns tokens a strict reader of the CIP
would not accept.

### 2. The exception: what looks like an attack or cannot be stored

An input is dropped, with one WARN that says it looks like an attack, only when it is plainly not metadata: a datum too deep to
decode or not decodable, a property nested more than 100 levels, a CIP-26 entry whose subject cannot be a key or whose file name
differs from its subject, a logo far beyond any real size, and an output whose indexing throws something unexpected (ADR 0004).
The list is in ADR 0006 decision 3. It is closed: a rule is added to it only for input that is an attack, never because a CIP
says a value is wrong.

### 3. Validation is programmatic, in the write path

The checks are code: the CIP-68 parser, `Cip68Validator` and processor, the CIP-26 validator and the sync service. They are **not**
database constraints and **not** Bean Validation on the stored entities.

- The migrations have no `CHECK` constraints, and the text columns have no length limit.
- The stored entities carry no validation annotations. In the `api` package Bean Validation checks the request shapes (the subject
  pattern, the batch size of 1 to 100), where it produces a 400.

*Why:* a violated database constraint is raised at flush or commit, inside the block transaction, and a failure there stops the
sync (ADR 0004). A check in code can report the reason and carry on, which a constraint cannot. It also lets a rule differ per
label and per standard, which a column constraint cannot. *Trade-off:* the database does not protect itself from a value the code
lets through; new rules have to be written in code and tested there.

### 4. A failed check is reported in the row, not in the log

- Every way a token departs from its CIP is a `validation_errors` entry, stored with the row and returned by the API. That is the
  report.
- A WARN is for what is dropped (decision 2), which is also a row in `invalid_token` (ADR 0007). A token indexed with errors is a DEBUG line, since there is one per token and
  mainnet has tens of thousands.
- CIP-68 has Micrometer counters for the errors, by label, code and category (ADR 0006 decision 7). CIP-26 has none: its entries
  are checked on GitHub before they are merged (the maintainers' statement), and the sync logs a tally at INFO every 500 files and at
  the end: `inserted`, `perm-skipped`, `transient`, `no-mapping` and `filename-mismatch`. `perm-skipped` now means only an entry that
  was dropped, not one that failed validation.
- A datum that does not have the CIP-68 shape at all is not logged, because reference NFT outputs can carry arbitrary datums.

## Consequences

- A contributor adding a field decides first whether it is mandatory in the CIP, then writes the check in code as an error. There is
  no constraint to add or forget.
- Because rules live in code, a change to a rule needs a resync to apply to rows already stored.
- An unexpected exception in a check is not one of the cases this policy covers. ADR 0004 decides what happens to it.

## Open questions

- Whether CIP-26 ever needs Micrometer counters like the CIP-68 ones. Its errors are now in the rows, so a count by code can also
  be read from the database.

## References

- ADR 0006: the decision this record applies
- ADR 0001 decisions 4 and 5: what a CIP-26 entry must satisfy, and the logo
- ADR 0002 decisions 3, 4, 6 and 8: required fields, `image`, what is dropped and reporting
- ADR 0004: what happens when a check throws
- `Cip26MetadataValidator`, `Cip68DatumParser`, `Cip68Validator`
