# ADR 0012: Certified Leios transactions in the existing transaction store

## Status

Proposed

## Date

2026-11-25

## Context

A Leios ranking block can announce an endorser block (EB) and certify a different EB announced by its parent. An announcement alone has no ledger effect. On a certifying block, Yaci `0.5.0-pre17` delivers the certified EB transactions first, in EB order, followed by the ranking block's own transactions. Its `LeiosEndorserBlockRef` identifies the EB and the length of that prefix. [Yaci delivery contract](https://github.com/bloxbean/yaci/blob/v0.5.0-pre17/core/src/main/java/com/bloxbean/cardano/yaci/core/model/LeiosEndorserBlockRef.java)

On `main`, `TransactionProcessor` already assigns each delivered transaction its block hash and index. Consumers use those records for block membership and transaction ordering. [Store processor](https://github.com/bloxbean/yaci-store/blob/63bd2ff00b1d37cd34be160531c16dc2f3d66ee0/stores/transaction/src/main/java/com/bloxbean/cardano/yaci/store/transaction/processor/TransactionProcessor.java)

The Musashi w42 db-sync source records the announced EB hash and certificate presence on the block. Its doomsday-mode migration also permits the same transaction hash in two certified EBs on one chain even though the ledger applies it once. Yaci Store keys transaction rows by hash. This makes repeated inclusion a rollback case distinct from competing forks. The deployed explorer revision still needs verification. [Block columns](https://github.com/IntersectMBO/cardano-db-sync/blob/72e067fa064af549a00d263c890c179989cf4f40/schema/migration-2-0051-20260707.sql) | [Repeated transaction hashes](https://github.com/IntersectMBO/cardano-db-sync/blob/72e067fa064af549a00d263c890c179989cf4f40/schema/migration-2-0055-20260828.sql) | [Store transaction key](https://github.com/bloxbean/yaci-store/blob/63bd2ff00b1d37cd34be160531c16dc2f3d66ee0/stores/transaction/src/main/java/com/bloxbean/cardano/yaci/store/transaction/storage/impl/model/TxnEntity.java)

## Proposed decision

1. Target `main` with the smallest change needed to consume complete blocks from published Yaci. Keep one row per applied ledger transaction in the existing `transaction` table. For certified transactions, `block_hash` identifies the certifying ranking block and `tx_index` follows the complete block's order. Incomplete certified data must stop indexing before the checkpoint advances. An announcement must never create transaction, CBOR or UTxO rows for the announced EB.
2. If consumers request provenance, start with a nullable EB hash on those transaction rows. The prefix already determines each EB transaction's position. A null hash on historical rows means no provenance was recorded. It cannot establish that a transaction originated in a ranking block. Keep historical backfill optional. With provenance collection off, consumers must still see all adopted transactions and their effects. Preserve existing API response shapes and transaction counts for a given complete block. Keep provenance in the existing stores. If a consumer needs to query announcements or certificates, evaluate optional block metadata keyed by ranking-block hash.
3. Bind the certified EB reference to the block that produced the transaction event. Parallel writers must not infer it from the last announcement they saw. Rollback must remove orphaned provenance with its transactions and prevent an in-flight write from restoring it. Replay must yield identical hashes, positions and UTxO effects. The adopted block hash distinguishes competing blocks at the same slot.

## Sub-transactions

Dijkstra sub-transactions pose a separate identity and accounting question. One option is a child relation keyed by parent transaction hash and child index. Putting children directly into the existing transaction table would require every legacy transaction query to account for them. Before choosing, compare real ledger and db-sync examples for child identity, ordering, invalid parents and attribution of inputs, outputs and fees. Count each ledger effect once. Defer the schema and public API until that comparison is available.

## Validation before implementation

- A Musashi w42 PostgreSQL replay already checks the 237 certified transactions in block 10,027 against a db-sync reference, including their order. Rolling back to announcing block 10,026 removes the transactions and their effects. Replaying restores them. Publish the Musashi db-sync version, schema and representative queries at the same chain point before choosing columns. [Captured blocks](https://github.com/bloxbean/yaci/tree/613c6ec4a41762597b7d81713369285d35e8e08b/core/src/test/resources/leios/w42) | [PostgreSQL replay](https://github.com/Gero-Labs/yaci-store/pull/33)
- Exercise a skipped announcement and a block that both certifies and announces. With parallel processing enabled, interrupt a writer during rollback, adopt a competing block at the same slot and replay a transaction that appears on both forks. Check the canonical block hash, CBOR, UTxOs and checkpoint after restart.
- Re-list a transaction in consecutive certified EBs on one chain. Roll back only the second inclusion and check that the first effective confirmation and its UTxO changes survive. Compare with the ledger before choosing how to represent repeated observations.
- Compare existing transaction and block queries on a pre-Dijkstra fixture and on a complete Musashi block with provenance on and off. Check IDs, ordering, counts and serialized responses. Review the results in [#1424](https://github.com/bloxbean/yaci-store/issues/1424) and [#1425](https://github.com/bloxbean/yaci-store/issues/1425) before scheduling migrations or API changes.
