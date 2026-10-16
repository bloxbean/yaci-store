# ADR 0005: Validation policy: what drops a token, what drops a property, and where the checks live

## Status

Proposed

The CIP-68 rule in decision 1 is implemented in PR #1220 (open); the CIP-26 behaviour in decision 2 is on `main`.

## Date

2026-10-14

## Context

ADR 0001 and ADR 0002 each decide, field by field, what the module does with input that does not fit a standard.
Read together they leave three questions unanswered, and a new contributor has to reconstruct the answers from the code:

- Is there one rule, or one per standard? The two ADRs do not say that CIP-26 and CIP-68 behave differently.
- Where do the checks live, in the database or in code? Nothing says why.
- What does a rejection leave behind? ADR 0002 decision 9 covers CIP-68 only.

This record states the policy in one place. It does not repeat the per-field tables: those stay in ADR 0001 decision 4 and
ADR 0002 decisions 3, 4 and 6.

## Decisions

### 1. CIP-68: a mandatory property drops the token, an optional one drops only itself

If a property the CIP requires is missing or does not fit the CIP, the datum is **not indexed**. If an optional property does
not fit, **only that property is left out** and the token is indexed. The rule is the same for fungible tokens, NFTs and RFTs;
which properties are mandatory depends on the label (ADR 0002 decision 3). A value that could break an insert or exhaust the
stack is bounded whatever the field (ADR 0002 decision 6).

*Why:* a mandatory property is the CIP's definition of a usable token, and a datum without it is not one. An optional
property is by definition not needed for that, so losing it should cost no more than itself. *Trade-off:* a token can be
stored with fewer properties than its datum has. The datum hex stays in the row, so the dropped value can be recovered.

### 2. CIP-26 follows the registry's own validator and drops the whole entry

A CIP-26 entry is validated as a whole by the `cf-tokens-cip26` library, the registry's validator, plus the module's range
check of `decimals` (ADR 0001 decision 4). If **any** field fails, including an optional one such as `ticker`, `url` or
`decimals`, the entry is rejected and not stored. The one exception is the logo: it is validated and stored in a separate
step, and a bad logo is skipped while the metadata is saved (ADR 0001 decision 5).

So CIP-26 does **not** follow decision 1: for an optional field it drops the token, where CIP-68 drops the property. The
difference is intended.

*Why:* a CIP-26 entry is validated on GitHub, in the registry, before it is merged, so by the time the module reads it the
entry has already passed the registry's own validation. The module's check is a second line of defence and runs the same
validator; an entry that fails it is one the registry should not have accepted. There is no reason to repair such an entry
field by field. A CIP-68 datum is the opposite case: anyone can put any datum on chain with no review, so the module has
to decide what to keep, and keeping what is valid is the better outcome. (That the registry validates on GitHub is the
maintainers' statement, not something the module's code can show.) *Trade-off:* a registry entry with one bad optional
value that slipped through is absent from the API, although the same value in a CIP-68 datum would cost only that value.

### 3. Validation is programmatic, in the write path

The checks are code: the CIP-68 parser and processor, the CIP-26 validator and the sync service. They are **not**
database constraints and **not** Bean Validation on the stored entities.

- The migrations have no `CHECK` constraints.
- The stored entities carry no validation annotations; no class outside the `api` package uses Bean Validation. In the `api`
  package it checks the request shapes (the subject pattern, the batch size of 1 to 100), where it produces a 400, and
  annotates the CIP-26 DTOs.

*Why:* a violated database constraint is raised at flush or commit, inside the block transaction, and a failure there stops
the sync (ADR 0004). A check in code can skip the one entry, name the reason in a WARN and carry on, which a constraint
cannot. It also lets a rule differ per label and per standard, which a column constraint cannot. *Trade-off:* the database
does not protect itself from a value the code lets through; a column that is too narrow still fails at the insert, which is
why the bounds of ADR 0002 decision 6 are checked against the column widths. New rules have to be written in code
and tested there.

### 4. A rejection is reported, with the reason

- A CIP-68 skip or property drop is one WARN with the policy id, the asset name and the reason, and a Micrometer counter
  (ADR 0002 decision 9).
- A CIP-26 rejection is one WARN with the subject and the reason. Each sync run also logs a tally at INFO (every 500
  files and at the end): `inserted`, `perm-skipped`, `transient`, `no-mapping` and `filename-mismatch`. A validation
  rejection counts as `perm-skipped`, together with non-transient database errors. These are log lines only; CIP-26 has no
  Micrometer counters, on purpose for now: bad CIP-26 entries are prevented on GitHub, where a checker runs on every pull
  request and the registry's validator rejects an invalid entry (the maintainers' statement, as in decision 2), so few reach
  the module and a counter would watch for something that is already guarded upstream. *Trade-off:* a bad entry that does get
  through shows up in a WARN and in the `perm-skipped` tally of a sync, not on a dashboard. If that changes, adding counters
  is a small change.
- A datum that does not have the CIP-68 shape at all is not logged, because reference NFT outputs can carry arbitrary
  datums.

Nothing is rejected silently except that last case. The exception is on purpose: the alternative is one line per unrelated
datum on chain.

## Consequences

- A contributor adding a field decides first whether it is mandatory in the CIP, then writes the check in code. There is no
  constraint to add or forget.
- The behaviour for a bad optional value is different in the two standards, on purpose (decision 2). Anyone comparing the
  two needs to know that, and a rejected CIP-26 entry points at the registry, not at the module.
- Because rules live in code, a change to a rule needs a resync to apply to rows already stored (ADR 0002 consequences).
- An unexpected exception in a check is not one of the cases this policy covers. ADR 0004 decides what happens to it.

## Open questions

- Whether CIP-26 ever needs Micrometer counters like the CIP-68 ones (ADR 0002 decision 9). It does not today (decision 4),
  because the registry checks entries on GitHub first. It would if rejected entries start to appear in a sync, and
  `perm-skipped` would then have to be split so that validation rejections are not mixed with database errors.

## References

- ADR 0001 decisions 4 and 5: what a CIP-26 entry must satisfy, and the logo
- ADR 0002 decisions 3, 4, 6 and 9: required fields, the strict `image`, bounds and reporting
- ADR 0004: what happens when a check throws
- `Cip26MetadataValidator`, `Cip68DatumParser`, `Cip68TokenService`
