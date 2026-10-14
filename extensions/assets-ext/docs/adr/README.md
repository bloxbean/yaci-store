# Architecture decision records: assets-ext

Decisions specific to the `assets-ext` module (CIP-26 and CIP-68 token metadata). They record where the
module is strict or lenient, what it does with data that does not fit the standards, and why. They do not
restate CIP-26 or CIP-68.

The records are numbered within this module, independently of the other ADRs in the repository, since
yaci-store is modular and these decisions only concern this module.

| ADR | Title |
|---|---|
| [0001](0001-cip-26-offchain-registry-ingestion.md) | CIP-26 off-chain registry ingestion |
| [0002](0002-cip-68-onchain-datum-handling.md) | CIP-68 on-chain datum handling |
| [0003](0003-token-metadata-read-model-and-api.md) | Token metadata read model and API |
| [0004](0004-cip-68-processing-failures-and-the-sync.md) | A CIP-68 processing failure and the sync |
| [0005](0005-validation-policy.md) | Validation policy: what drops a token, what drops a property, and where the checks live |

A record has a status (`Proposed`, `Accepted`, `Superseded`), a date, the context, the decisions with their
trade-offs, the consequences and the open questions. When a decision changes, add a new record that supersedes
the old one instead of rewriting history.
