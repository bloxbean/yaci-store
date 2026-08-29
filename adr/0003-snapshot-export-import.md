# ADR 0003: Parquet Snapshot Export and PostgreSQL Import

## Status

Accepted, implemented. Amended 2026-08-29 with corrections that implementation
evidence required; each amendment is marked **Implementation note**.

## Date

2026-08-28 (amended 2026-08-29)

## Context

A full Yaci Store mainnet sync currently takes roughly three days. The analytics
store already exports most operational data to Parquet managed by DuckLake under
a configurable data directory. Distributing those files as a snapshot and
bulk-loading them into a newly migrated PostgreSQL schema can reduce bootstrap
time substantially.

The existing analytics directory is not, however, a database backup:

- analytics tables are exported independently, using both daily and epoch
  partitions;
- the DuckLake catalog can advance between table exports;
- current-epoch exports can be partial;
- the Parquet and operational PostgreSQL schemas are not identical;
- operational state such as `cursor_`, `era`, job state, sequences, constraints,
  and indexes is not part of the analytics export; and
- a checksum of an archive proves transport integrity, not that the restored
  database can resume chain sync correctly.

The local preprod analytics directory examined for this ADR is approximately
7.4 GB and contains 23,857 Parquet files. Its latest `block` row is in epoch 309
at slot 131,932,781, while the `epoch` table ends at epoch 308 and the epoch-309
`adapot` data ends at slot 131,846,412. This demonstrates that the maximum block
in DuckLake cannot be used directly as the PostgreSQL restart cursor.

### Feasibility-study findings

The private
[`yaci-store-parquet-loader`](https://github.com/Kartiiyer12/yaci-store-parquet-loader)
proof of concept was reviewed at commit
[`de1dcaa`](https://github.com/Kartiiyer12/yaci-store-parquet-loader/commit/de1dcaa1b723929e6c3a5aa150ed94166682e924).
It established that DuckDB can read the Parquet files and insert into PostgreSQL
through its PostgreSQL extension and binary COPY path.

On its mainnet test system, the PoC reported:

| Phase | Result |
|---|---:|
| Load 2,071,386,845 rows | 50.7 minutes |
| Build 232 primary keys and indexes | 5.81 hours |
| Total restore | 6.66 hours |
| Resulting PostgreSQL database | 1,092 GB |

The often-quoted 50-minute result is therefore the heap-load phase, not the time
until the database is ready for normal use.

The PoC deferred primary keys as well as indexes. The product importer will keep
the base constraints created by Flyway and leave only the indexes managed by the
normal admin CLI workflow unapplied. Its load and post-import index timings must
therefore be measured independently rather than assumed to match the PoC.

The PoC also found that only 4 of 44 non-empty exported tables were direct
copies. The current mapping includes:

- 26 tables with timestamp-to-Unix-seconds conversion;
- 8 tables with other type conversions;
- 7 column renames, including nullable columns that can otherwise lose data
  silently;
- generated UUIDs for `mir` and `voting_procedure`; and
- re-aggregation of flattened `address_utxo` asset rows into PostgreSQL JSONB.

The known column renames are:

| Target table | PostgreSQL column | Parquet column |
|---|---|---|
| `epoch` | `number` | `epoch` |
| `constitution` | `active_epoch` | `epoch` |
| `instant_reward` | `earned_epoch` | `epoch` |
| `reward` | `earned_epoch` | `epoch` |
| `reward_rest` | `earned_epoch` | `epoch` |
| `unclaimed_reward_rest` | `earned_epoch` | `epoch` |
| `pool_registration` | `vrf_key` | `vrf_key_hash` |

For `address_utxo`, the current analytics view has one row per asset. Import
must group by `(tx_hash, output_index)`, rebuild `amounts` as JSONB, derive
`lovelace_amount`, and join the block Parquet data to recover the block number.
The current view does not export `owner_addr_full`, so that value cannot be
recovered from existing snapshots.

The PoC deliberately did not validate application restart behavior. It also
identified operational tables without a Parquet counterpart, empty exports for
some tables, random replacement UUIDs, and downstream data such as Rosetta's
`transaction_size` that is outside Yaci Store and cannot be reconstructed from
the current export.

## Decision

Add snapshot export and import commands to `admin-cli`, backed by a reusable
snapshot component. Treat a snapshot as a versioned, validated product format,
not as an arbitrary ZIP of the analytics directory.

The commands will use the datasource and schema from the same Spring
configuration used by the admin CLI. Passwords must not be accepted as command
arguments or written to the manifest or logs.

The high-level flow is:

```text
PostgreSQL -> analytics exporters -> immutable DuckLake files
                                      |
                                      v
                         epoch-aligned snapshot manifest
                                      |
                         independent ZIP parts + checksums
                                      |
                                      v
empty migrated PostgreSQL <- transform/load <- extracted Parquet
          |
          v
control state + validation -> resume Yaci Store
          |
          v
manual admin CLI `apply-indexes`
```

### Component boundaries

Create a reusable `components:snapshot` module containing:

- snapshot manifest models and compatibility checks;
- DuckLake snapshot and file discovery;
- archive writing, extraction, and integrity verification;
- a YAML-backed, versioned table specification registry;
- generic declarative snapshot packagers and importers;
- named converter, SQL-transform, and Java-transform extension registries;
- the PostgreSQL bulk loader and import journal;
- control-state reconstruction; and
- structural and semantic validators.

`applications:admin-cli` will contain only Spring Shell command adapters and
progress reporting. It will depend on the snapshot component and DuckDB JDBC,
but it should not depend on the full analytics Spring application or instantiate
all store processors.

**Implementation note (DuckDB version).** A DuckLake catalog written by DuckDB
1.5.x uses storage version 68, which the 1.4.4.0 driver analytics-store pins
cannot open. The snapshot component therefore depends on a separate version
catalog entry pinned to 1.5.5.0, and `libs.duckdb-jdbc` is left untouched so
analytics behaviour does not change. The two never share a classpath, because
the admin CLI does not depend on analytics-store.

This separation also allows future use from a non-interactive container entry
point and integration tests.

## Snapshot Consistency Point

Every snapshot has one immutable Cardano point:

```text
(network, protocol magic, epoch, slot, block number, block hash,
 previous block hash, era, block time)
```

The point is not the latest exported block. It is the last block of the newest
epoch for which all required epoch-completion jobs and all required daily and
epoch exports are complete.

The exporter will:

1. Open a read transaction and pin one DuckLake catalog snapshot ID.
2. Find the highest candidate completed epoch supported by every required table.
3. Select the last block of that epoch as the Cardano snapshot point.
4. Require the point to be beyond the configured finality/security buffer.
5. Evaluate each table's versioned cutoff predicate at that point.
6. Record the resulting per-table row counts, bounds, and schema fingerprints.
7. Fail if any enabled table is missing, partial, unclassified, or extends past
   the point in a way its mapping cannot filter safely.

Typical cutoff rules include `slot <= :cutSlot`,
`spent_at_slot <= :cutSlot`, `epoch <= :completedEpoch`, or an explicitly
documented epoch offset. There must be no generic guess based only on a column
called `slot` or `epoch`.

**Implementation note (epoch partition column).** An epoch-partitioned exporter
reports `block_time` from `getPartitionColumn()`, because that is the expression
DuckLake partitions on, but it writes `epoch=N` directories and the exported data
carries an `epoch` column, not `block_time`. A specification for an
epoch-partitioned relation therefore names `epoch` as its partition column. The
first `snapshot inspect` run against the preprod export failed on exactly this
for all fourteen epoch-partitioned tables.

**Implementation note (which tables may gate).** Only a table guaranteed to
produce data for every epoch may constrain the point. A sparse table -- a
committee change, a pool retirement -- would otherwise drag the snapshot back to
its last active epoch. Sparse tables declare no gating rule and are constrained
only by their cutoff, with a separate coverage assertion checked after the point
is chosen. Against the preprod export the gating set is `adapot`,
`address_utxo`, `block`, `drep_dist`, `epoch`, `epoch_param`, `epoch_stake`,
`gov_epoch_activity` and `transaction`, and it selects epoch 308: the last block
of epoch 308 at slot 131,846,389, 4,011 blocks behind the newest exported block,
rather than the epoch-309 tip.

Selecting an epoch boundary means a restored node may need to replay up to one
epoch, but avoids importing partial reward, stake, governance, or AdaPot state.
This is an acceptable tradeoff compared with replaying the entire chain.

For the initial format, source Parquet files must be immutable, append-only, and
have a stable schema. A snapshot export fails if DuckLake deletion files or
unsupported schema evolution are present. Supporting deletion vectors will
require reading through the pinned DuckLake catalog or materializing a new
logical Parquet snapshot and is deferred.

## Snapshot Artifact Format

Use independent ZIP archives, not a PKZIP spanned archive. Independent parts
can be retried, mirrored, and verified separately, and a missing optional part
does not make every other archive unreadable.

Example distribution:

```text
yaci-preprod-e308-<snapshot-id>.manifest.json
yaci-preprod-e308-<snapshot-id>.part-00001.zip
yaci-preprod-e308-<snapshot-id>.part-00002.zip
...
SHA256SUMS
SHA256SUMS.sig
```

Files retain paths relative to the configured analytics data directory, for
example `analytics/main/block/date=2026-08-24/...parquet`. Each part also
contains a small part manifest. The top-level manifest maps every file and table
to its required part.

The default target part size will be configurable and initially set to 8 GiB.
Parquet files are never split; a file larger than the target size is placed in a
part by itself.

DuckLake already writes Zstandard-compressed Parquet. The PoC measured only
about 0.4% additional reduction from high-level outer compression. ZIP entries
will therefore use the `STORE` method by default, avoiding hours of redundant
CPU work. The pre-scan needed for file SHA-256 values also supplies ZIP sizes
and CRCs.

### Manifest

The canonical JSON manifest includes at least:

- snapshot format version and snapshot UUID;
- creation time and producer identity;
- Yaci Store version, Git commit, and snapshot specification format version;
- DuckDB, DuckLake, and Parquet format versions;
- pinned DuckLake catalog snapshot ID;
- network, protocol magic, and genesis hash;
- the complete Cardano consistency point;
- source module/profile set and pruning settings;
- Flyway history fingerprint and PostgreSQL schema fingerprint;
- per-table specification ID, specification version and digest, source and
  target names, owning module, load mode, cutoff rule, transform version,
  dependencies, row count, key, column fingerprint, observed source column
  types, min/max slot or epoch, and expected null counts;
- all archive parts, sizes, SHA-256 values, and contained files; and
- declared lossy or unsupported fields, which are empty for a production
  snapshot.

The manifest itself and `SHA256SUMS` will be signed for official snapshots.
Unsigned snapshots may be imported only with an explicit option and a warning.
Integrity verification is mandatory even when signature verification is
disabled.

The manifest contains no database URL, user name, password, host name, or local
absolute path.

**Implementation note (source column types).** The manifest records each table's
source column name and DuckLake type, not only a fingerprint of them. DuckDB
widens `INT32` to `BIGINT` when it reads Parquet, so an importer that planned
from the types it observes in the files would treat every `int32 -> integer`
column as an unsafe narrowing. The importer therefore plans against the types
the producing catalog recorded, while still comparing the column *names* with
the files so real schema drift fails.

**Implementation note (specification digest).** The recorded digest covers the
YAML specification *and* the SQL transform resource it references. Without that,
editing a transform would change what an import produces while still matching
the digest the manifest recorded.

## Table Inventory and Mapping Contract

Introduce one authoritative, machine-readable inventory of tables. Each entry
declares:

- owning module;
- whether it is chain data, derived data, or operational control state;
- source Parquet relation, if any;
- target PostgreSQL table;
- direct, transformed, reconstructed, intentionally empty, or unsupported load
  mode;
- cutoff and validation rules;
- primary/load key and cross-table dependencies; and
- whether pruning changes its expected contents.

This inventory should be generated or checked against Flyway migrations and
the rollback inventories during the build. A CI test must fail when a migration
adds a table or column without a snapshot classification. Deriving ownership
from Flyway filename prefixes is not a stable contract.

Table mapping is strict and versioned:

- automatic matching is allowed only after declared renames, generated fields,
  export-only fields, and conversions are applied;
- an unknown source or target column is an error;
- an absent nullable column is an error unless explicitly classified;
- type conversions are explicit;
- UUID replacements are deterministic, using a fixed UUIDv5 namespace and
  stable row identity; and
- every transform has unit and integration tests using real Parquet schemas.

This prevents the silent NULL behavior possible with a permissive name-based
mapper.

### Additive declarative snapshot framework

Use a declarative framework similar to the existing custom exporter mechanism,
but add it as a snapshot contract rather than replacing or changing any existing
exporter configuration.

The compatibility boundary is explicit: built-in `TableExporter` classes,
`CustomTableExporter`, `TableExporterRegistry`, exporter properties, admin
endpoints, and `application-custom-exporters.yml` retain their current behavior
and format. Existing deployments do not need a snapshot specification to keep
exporting analytics data.

The existing `TableExporter` implementations answer how data is selected and
written to analytics storage. They do not describe all information needed for a
restore, such as target columns, PostgreSQL defaults, cutoff semantics,
cross-table dependencies, control-state behavior, or validation. Reflecting on
or parsing those implementations would keep export and import tightly coupled
and would still leave important behavior implicit.

Instead, each snapshot-capable DuckLake relation has a versioned
`SnapshotTableSpec` loaded from a separate YAML resource. Its `source` section
identifies the relation already produced by an existing exporter. Its `import`,
consistency, dependency, and validation sections define snapshot behavior. It
is authoritative for snapshot packaging and restore, but it does not configure,
register, schedule, or execute the analytics exporter.

Specifications should be owned by the module that owns the target table, for
example:

```text
stores/staking/src/main/resources/snapshot/pool-registration.yml
stores/utxo/src/main/resources/snapshot/address-utxo.yml
aggregates/adapot/src/main/resources/snapshot/reward.yml
```

A build task validates and collects these module-owned resources into the
snapshot/admin distribution and generates a resource index. At runtime,
`components:snapshot` discovers them without classpath scanning, Spring beans,
or loading store processor classes. This avoids a single large YAML file, keeps
source definitions beside their migrations and domain code, and does not force
the admin CLI to depend on every store's runtime implementation.

An illustrative simple mapping is:

```yaml
snapshot-table:
  id: pool-registration
  spec-version: 1
  module: staking
  kind: chain-data

  source:
    exporter-id: pool_registration
    ducklake-relation: pool_registration
    partition:
      strategy: DAILY
      column: block_time

  consistency:
    cutoff:
      type: SLOT_LTE
      column: slot

  import:
    target-table: pool_registration
    mode: MAPPED
    columns:
      vrf_key:
        source: vrf_key_hash
      block_time:
        source: block_time
        converter: timestamp-to-epoch-seconds
    ignore-source-columns: [date]
    use-target-defaults: [create_datetime, update_datetime]
    batch-boundary: FILE_SET

  validation:
    key: [tx_hash, cert_index]
    bounds: [slot, epoch]
    required-columns: [tx_hash, pool_id, slot]
```

The actual keys come from the table's migration and must be checked by CI; the
example shows the shape of the contract rather than redefining PostgreSQL DDL.
Columns not listed under `import.columns` are matched by identical name only
when their source and target types are compatible. After explicit mappings,
ignored export fields, and declared target defaults are applied, every source
and target column must be accounted for. This keeps simple specs short while an
unexpected new or renamed column still fails validation.

The declarative framework supports four import modes:

| Mode | Purpose | Java table code required |
|---|---|---:|
| `DIRECT` | Exact source/target names and compatible types | No |
| `MAPPED` | Renames, named conversions, constants, omissions, defaults | No |
| `SQL` | DuckDB `SELECT` with joins, grouping, or reshaping | No table class |
| `HANDLER` | Control state or behavior not expressible as a safe SQL transform | Small SPI implementation |

Most current tables should use `DIRECT` or `MAPPED`. Common conversions are
named and implemented once in a `SnapshotValueConverterRegistry`, including:

- `timestamp-to-epoch-seconds`;
- checked numeric narrowing;
- `varchar-to-jsonb`;
- deterministic UUIDv5 generation; and
- explicit null/default handling.

Named converters are preferable to arbitrary inline expressions because they
are reusable, type-checked, documented, and easy to test across every table
that uses them.

For a complex but relational transformation, `SQL` mode references a trusted
SQL resource rather than embedding a large query in Java or YAML:

```yaml
snapshot-table:
  id: address-utxo
  spec-version: 1
  module: utxo
  source:
    exporter-id: address_utxo
    ducklake-relation: address_utxo
  consistency:
    cutoff:
      type: SLOT_LTE
      column: slot
  import:
    target-table: address_utxo
    mode: SQL
    select-resource: classpath:/snapshot/sql/address_utxo_flattened_v1.sql
    dependencies: [block]
    batch-boundary: WHOLE_PARTITION
    transform-version: 1
```

The generic importer supplies typed parameters such as `:files`,
`:dependencyFiles`, `:cutSlot`, and `:completedEpoch`, generates the target
column list, and performs the insert and journal update. The referenced SQL is a
DuckDB `SELECT`; it cannot contain target DDL, transaction control, `ATTACH`, or
arbitrary PostgreSQL statements.

`HANDLER` mode is the escape hatch, not the default. A
`SnapshotTransformProvider` SPI is selected by a stable name from YAML. It is
appropriate for reconstructing `cursor_`, `era`, or other operational state
whose lifecycle cannot be represented as a table-shaped query. Adding an
ordinary table or column must not require a new Java exporter/importer class.

### Relationship to current exporters

Existing exporters remain the only mechanism that produces analytics Parquet
and DuckLake data. The snapshot framework consumes their output:

- `source.exporter-id` links a specification to the stable ID/name already used
  by `TableExporterRegistry` and export state. This is descriptive and used for
  coverage validation; it does not register another exporter.
- `source.ducklake-relation` identifies the existing DuckLake table to package
  and read.
- Existing built-in Java exporters continue unchanged.
- Existing custom exporters continue to use
  `application-custom-exporters.yml` unchanged.
- A custom export becomes snapshot-importable by adding a separate
  `SnapshotTableSpec` that references its existing `name` and DuckLake relation.
  The original custom-exporter YAML does not need to be edited or migrated.
- New analytics exports can continue using either a Java exporter or the current
  custom-exporter YAML. Snapshot support remains an optional second descriptor.

The `snapshot export` command means packaging an already-produced, pinned
DuckLake file set into snapshot archives. It does not rerun or replace the
analytics exporters.

At export time the framework compares the actual DuckLake relation schema with
the specification. At import time it compares the local PostgreSQL schema and
the manifest's observed Parquet schema with the same specification. The
manifest records the table spec ID, version, and SHA-256 digest.

This makes schema drift fail during snapshot packaging, CI, or import preflight
instead of silently changing restored data. Analytics export behavior remains
backward compatible even when no snapshot spec exists.

### Custom specifications and trust

Executable mapping rules must never be accepted automatically from a downloaded
snapshot archive. The archive contains only spec identifiers, versions,
digests, and observed schema metadata.

Built-in specifications and SQL resources come from the matching Yaci Store
release. Plugins may contribute specifications and named transform providers
through an indexed resource/SPI mechanism. Operator-defined specifications are
loaded only from an explicit local `--spec-file` or configured directory and
require an acknowledgement such as `--allow-custom-specs` for import.

Registry merge rules are deterministic:

- `(table id, spec version)` is unique;
- custom definitions cannot silently override a built-in definition;
- the locally installed spec digest must equal the manifest digest;
- target schema and table names are identifier-validated and quoted;
- SQL mode accepts one read-only DuckDB `SELECT` resource; and
- every dependency must resolve to another selected table specification.

This preserves extensibility without turning a remotely downloaded YAML file
into privileged SQL execution.

### Specification validation and evolution

Publish a JSON Schema for the YAML format and bind it to typed Java records. A
specification is accepted only after syntactic and semantic validation.

Build-time contract tests will:

1. compare all Flyway-created tables and columns with the selected module specs;
2. compare exporter output schemas with declared source schemas;
3. compare import targets with PostgreSQL types, defaults, and nullability;
4. prepare every SQL transform against representative Parquet fixtures;
5. verify converter input/output types;
6. run export-to-import round trips for each mode; and
7. fail when a new migration table or column has no explicit classification.

Adding a normal table should therefore require a migration and one YAML spec.
Adding a normal column should require updating that spec and its fixture. Java
is needed only for a genuinely new reusable converter or handler.

Do not mutate a released specification. Schema or transform changes create a
new `spec-version`, while snapshot format changes are reserved for changes to
the overall archive/protocol. Import initially requires the exact local spec
version and digest recorded in the manifest.

### Deferred lossless `address_utxo` export

The initial implementation may use the existing flattened `address_utxo`
analytics export. `owner_addr_full` is not present and cannot be recovered from
the current files. This is an accepted temporary limitation and does not block
initial snapshot import development or qualification.

The manifest and import report must record `owner_addr_full` as unavailable so
the limitation is visible and cannot be mistaken for exact database parity.

**Implementation note (reconstruction fidelity).** Comparing the reconstructed
rows with an independently synced preprod database over a 46,000-slot window,
all 19,325 rows matched on every scalar column, and 19,318 of them matched the
`amounts` JSONB byte for byte. Reaching that required two corrections the
original text did not anticipate:

- the analytics view applies `NULLIF(asset_name, '')`, so an asset with an empty
  name arrives as NULL. The transform restores the empty string, because NULL is
  unambiguous for a non-lovelace asset;
- the operational writer stores the multiasset entries in canonical CBOR order --
  lovelace first, then by policy id, then by asset name with shorter names
  before longer ones -- not in plain unit order.

The seven residual rows carry an identical set of assets and quantities in a
different array order, from historical transactions whose multiasset map was not
canonically ordered. No value differs.

**Implementation note (row counts for regrouping transforms).** A transform that
regroups rows produces fewer rows than it reads, so the source row count is not
the count the importer must reproduce. A specification declares
`validation.source-key` for that case, and the manifest records the number of
distinct source keys: 22,194,183 for `address_utxo`, against 46 million
flattened source rows.

A later snapshot-format/exporter revision will add a lossless,
snapshot-oriented projection. It should retain the operational row shape,
including `amounts`, `owner_addr_full`, and block number, or at least carry all
fields needed to reconstruct it exactly. Compatibility will be controlled by
the snapshot format and table specification versions.

### Tables not currently exported

Operational and profile-specific tables without Parquet data must be
classified individually. They must not be silently left empty. Depending on the
table, the decision can be to:

- add a lossless exporter;
- reconstruct it deterministically from other snapshot data;
- initialize it as control state;
- prove that it is empty for the declared module/profile; or
- reject that profile as unsupported.

This applies in particular to `cursor_`, `era`, `adapot_jobs`, current/cache
tables, `epoch_nonce`, block/transaction CBOR tables, and downstream extension
tables. The first production release supports only module profiles for which
every target table has an explicit, tested classification.

**Implementation note (classifications used).** The implementation gives every
target table exactly one of five restore modes: `IMPORT`, `HANDLER`,
`EMPTY_EXPECTED`, `RUNTIME_REBUILT` or `NOT_RESTORED`, and a CI check fails when
a migration adds a table without one. `cursor_`, `era` and `adapot_jobs` are
`HANDLER`; the `local_*` governance views are `RUNTIME_REBUILT`; the assets
extension and operational log tables are `EMPTY_EXPECTED`.

**Implementation note (the account module).** The account module is
`NOT_RESTORED`, for a reason the original text did not have: its export is not
merely absent, it is a *downsample*. `AddressBalanceExporter` and
`StakeAddressBalanceExporter` keep only the latest balance per address per day
(`ROW_NUMBER() ... WHERE rn = 1`), so the export is correct for analytics and
wrong as a restore source -- importing it would produce a database whose
historical balances look complete and are not. `address_balance`,
`stake_address_balance`, `address_tx_amount`, both `*_current` caches and
`account_config` are therefore left empty and declared. A lossless account
export is the prerequisite for supporting that module, not a snapshot-format
change.

**Implementation note (`era` reconstruction).** Rebuilding one era row from the
first imported block of each era reproduces the independently synced preprod
`era` table exactly for eras 2 to 7. It additionally produces an era 1 (Byron)
row at slot 0, block 0, which a normally synced database does not always have.
That is harmless: `EraRepository.findFirstNonByronEra()` selects `era > 1`, so
the Shelley start slot the epoch calculation depends on is unchanged.

### Ledger-state qualification findings

**Implementation note (export chain facts, derive instance facts).** Running a
restored database with the ledger-state profile across real epoch boundaries and
comparing the AdaPot against Koios preprod distilled a rule the original text
lacked: a table should be *exported* when its content is a fact about the chain,
and *derived by the importer* when its correct value in the restored database
differs from its value in the source. `era` (which block started each era) and
the pool margin rationals are chain facts and belong in the export;
`account_config` (this instance's balance watermark), `cursor_` and
`adapot_jobs` are instance facts whose correct restored value is the snapshot
point, which only the importer knows -- exporting them would silently assert
progress the restored database has not made.

**Implementation note (epoch-boundary snapshot tagging).** Verified in code and
against the preprod job table: the reward-calculation job for the N to N+1
boundary carries epoch N+1 and runs at the first block of epoch N+1. It writes
`adapot` N+1, rewards spendable in N+1 (earned N-1), `epoch_stake` tagged N
(`takeStakeSnapshot(epoch - 1)`), and `drep_dist` tagged N+1. A snapshot taken
at the last block of epoch N therefore correctly contains `epoch_stake` tagged
at most N-1 and `drep_dist` tagged at most N, and the first job after restore
recreates the rest. `takeStakeSnapshot` deletes and rewrites its tag
unconditionally, so importing one extra `epoch_stake` epoch is redundant but
harmless.

**Implementation note (pool margin rationals are required for reward
calculation).** The analytics export carries `pool_registration.margin` as a
float; the reward calculation deliberately reads the exact
`margin_numerator`/`margin_denominator` because double arithmetic drifts from
Haskell ledger math (`PoolDetails.getMargin`). With those columns restored as
NULL, every pool's operator/member split is computed with margin zero. Measured
on preprod: 10,545 of 11,129 member rewards for the first recomputed epoch
differed, and the AdaPot treasury drifted +28,639 lovelace at epoch 299,
accumulating every epoch. With the rationals backfilled and the boundary
replayed, the restored database computed reserves and treasury byte-identical to
Koios for every recomputed epoch. Until the exporter carries the rationals, the
ledger-state profile must not be run on a restored database, and the
specification says so in its declared limitation.

## Export Command

Proposed command shape:

```text
snapshot inspect --data-dir <analytics-dir>
snapshot export --data-dir <analytics-dir> --output <dir> \
  --profile <profile> --part-size 8GiB [--spec-file <yaml>]
snapshot verify --manifest <manifest>
```

`snapshot inspect` is read-only and reports candidate consistency points,
coverage, schema drift, lossy mappings, estimated archive size, and blockers.

`snapshot export` performs the following stages:

1. Validate the data directory, catalog, DuckDB/DuckLake compatibility, and
   available output space.
2. Pin a catalog snapshot and select the epoch-aligned Cardano point.
3. Resolve the exact immutable file set and table-specific predicates.
4. Compare DuckLake counts and bounds with the source PostgreSQL database where
   available.
5. Build the manifest and deterministic content-based batch IDs.
6. Stream files into independent ZIP parts without recompressing Parquet.
7. Re-read and verify every part checksum.
8. Write the canonical manifest and signature last, using atomic rename.

New files created by continuous analytics export after the catalog snapshot was
pinned are not included. For the first release, snapshot export and DuckLake
garbage collection must be mutually exclusive. An advisory/maintenance lock
will enforce this when both operations are managed by Yaci Store.

## Import Command

Proposed command shape:

```text
snapshot import --manifest <manifest> --work-dir <dir> \
  --workers <n> --memory-limit <size> \
  [--spec-file <yaml> --allow-custom-specs]
snapshot import-status
snapshot validate --manifest <manifest> [--online]
```

The target schema must first be created by the matching Yaci Store release with:

```properties
store.sync-auto-start=false
```

This allows Flyway to create the exact enabled-module schema without starting
chain sync. The Yaci Store application must then be stopped while import runs.
The initial importer accepts only PostgreSQL.

Import refuses to run unless:

- the archive and signature policy pass;
- network and protocol magic match configuration;
- snapshot, Yaci Store, mapping, Flyway, and schema fingerprints are compatible;
- all tables required by the target profile are covered;
- the target schema contains no chain data, or contains a matching incomplete
  import journal; and
- Yaci Store is not concurrently writing to the schema.

There is no implicit `--force` cleanup. Clearing a non-empty schema is a
separate, explicit destructive operation.

### Resumable bulk load

Use DuckDB to scan and transform Parquet and the DuckDB PostgreSQL extension to
write through PostgreSQL's binary COPY path, as validated by the PoC.

Work is a content-addressed queue of `(table, batch)` items. Batch identity is
derived from snapshot ID, table, transform version, and sorted input file
digests, rather than an ordinal that changes when tuning batch size.

Each worker has:

- its own DuckDB connection;
- a bounded memory limit;
- its own spill directory; and
- its own PostgreSQL transaction.

The insert and completion record in `_yaci_snapshot_import` commit in the same
target transaction. A crash can therefore be resumed without duplicating an
already committed batch. The journal and a final import report remain until all
validation is complete, then the temporary journal table is removed.

Whole partition directories form the minimum `address_utxo` batch because a
UTxO can otherwise be emitted by more than one grouped batch. Batch sizing,
worker count, memory, spill space, and minimum free disk are configurable.
Defaults should be conservative and the importer aborts before crossing the
configured disk floor.

### Index handling

Snapshot import does not create, drop, defer, or rebuild admin-managed indexes.
Index creation remains the existing Yaci Store operational workflow used after
a normal initial sync.

The target must contain the base tables, primary keys, unique constraints, and
other schema objects created by Flyway for the matching release. The optional
and read indexes managed through `index.yml` and `extra-index.yml` should not be
applied before bulk import. If they already exist, the importer reports that
loading may be slower but does not remove them.

After import and validation, the operator manually runs the existing admin CLI
command:

```text
apply-indexes
```

The existing `--skip-extra-indexes` option can be used when only the default
read indexes are wanted. `verify-indexes` remains the way to check their state.
Index creation is not part of the snapshot import journal or the import-ready
status; its progress, retry, disk requirements, and completion follow the normal
admin CLI process.

After loading data, the importer still resets every affected sequence with
`setval` based on the imported maximum and its empty-table semantics.

## Restart State Reconstruction

Importing table rows is insufficient to resume Yaci Store. Before marking the
import complete, reconstruct and validate at least:

### `era`

Build the era transition rows from the first imported block of each era and the
known network genesis configuration. Compare them with known network transition
points where available.

### `cursor_`

Seed cursor rows from the final imported blocks using the configured
`store.event-publisher-id`. Include block hash, previous hash, slot, block
number, and era.

Do not seed only the final cursor. Preserve at least
`store.cardano.cursor-no-of-blocks-to-keep` rows (default 2,160) ending at the
snapshot point so an immediate normal chain rollback can select a previous
cursor safely.

The final cursor must exactly match the manifest point and an imported `block`
row. No imported chain or derived row may be beyond the table-specific cutoff.

### Job and derived state

For enabled aggregate modules, initialize job and cache tables according to
their explicit inventory rules. Completed historical derived data must not be
scheduled again, while an incomplete epoch must be replayed from the chosen
epoch boundary. In particular, absence of `adapot_jobs` must not be treated as
proof that AdaPot state is valid.

Startup will still execute Yaci Store's normal point validation against the
Cardano node and publish its normal restart rollback events. The importer must
not bypass those mechanisms.

## Validation and Readiness Gates

Validation has four levels. All offline levels must pass before sync is enabled.

### 1. Artifact validation

- signature policy;
- manifest and part SHA-256 checks;
- ZIP path traversal and duplicate-entry rejection;
- uncompressed-size and file-count limits;
- exact required part set; and
- per-file digest verification after extraction.

### 2. Schema and load validation

- exact Flyway and schema fingerprints;
- expected table and column inventory;
- per-table row counts after cutoff;
- min/max slot, block, epoch, and time bounds;
- expected null counts and type conversions;
- primary-key uniqueness and base constraint validity;
- valid sequences; and
- no unfinished import batches.

### 3. Semantic validation

At minimum:

- manifest point equals the last imported block and final cursor;
- the retained block tail has a continuous `prev_hash` chain;
- transaction and block references resolve where the configured pruning profile
  says they should;
- `address_utxo` has one row per `(tx_hash, output_index)`, valid JSONB amounts,
  one lovelace entry, and a matching lovelace total;
- source and target counts/fingerprints match for every transform;
- era transitions and current epoch agree with the final block;
- derived/job tables agree with the completed epoch; and
- there are no rows beyond the declared table cutoff.

Full-table cryptographic content hashing is expensive and is not required for
the first release. The producer records deterministic per-partition aggregate
fingerprints and samples in addition to file digests. File digests protect the
exact producer output; producer-side source comparisons establish semantic
correctness.

### 4. Application acceptance

The preprod and mainnet release process must demonstrate:

1. Start the matching Yaci Store release with `store.sync-auto-start=false` and
   run API/read smoke tests.
2. Start against a Cardano node and confirm the snapshot point is accepted.
3. Observe the normal startup rollback and replay behavior.
4. Sync a configured number of blocks past the snapshot.
5. Gracefully restart and confirm the same tip.
6. Kill the process during sync, restart, and confirm recovery.
7. Exercise a rollback within retained cursor history.
8. Compare critical APIs and database invariants with a conventionally synced
   instance.

Only after this gate passes is the import status changed from `VALIDATING` to
`READY`.

## Compatibility Policy

The initial importer requires an exact snapshot format, exact local table
specification versions and digests, and an exact target schema fingerprint. The
recommended operational sequence is:

1. create and import using the Yaci Store release named in the manifest;
2. validate and start it successfully; then
3. upgrade Yaci Store normally so Flyway applies later migrations.

Importing directly into an arbitrary newer schema is rejected until an explicit
compatibility adapter exists and is tested. Network mismatch is never
overridable.

Pruned and unpruned snapshots are different profiles. The manifest records UTxO,
transaction, account, reward, and epoch-stake pruning settings, and validation
uses those settings when deciding whether historical references must exist.

## CLI Safety and Operational Behavior

- Export and import support non-interactive execution with stable exit codes and
  machine-readable progress output.
- Secrets are redacted from logs and exceptions.
- Archive extraction rejects absolute paths, `..`, links, and entries outside
  the selected data/work directory.
- Import uses a PostgreSQL advisory lock and refuses concurrent import or Yaci
  Store writes.
- Progress is durable and restartable; deleting a partial import is never an
  automatic recovery action.
- Every completion report distinguishes archive verification, heap load,
  semantic validation, and application readiness durations. The later manual
  index build is reported by the existing admin CLI workflow.

## Implementation Plan

### Phase 0: Close correctness gaps

1. Define the typed `SnapshotTableSpec` model, YAML JSON Schema, loader,
   deterministic registry merge rules, and resource index.
2. Add built-in specifications for the authoritative module/table inventory and
   CI drift checks.
3. Implement strict schema auditing against current Parquet and PostgreSQL
   schemas.
4. Record the current `address_utxo.owner_addr_full` limitation in the manifest
   and import report.
5. Classify or add exporters for control, cache, job, CBOR, epoch nonce, and
   profile-specific tables.
6. Change epoch export semantics so partial current-epoch data cannot be marked
   permanently complete, or exclude it through the completed-epoch snapshot
   rules.
7. Define deterministic identities for exported rows whose UUID is currently
   omitted.

### Phase 1: Snapshot format and export

1. Add `components:snapshot`, the specification registry, and manifest
   serialization.
2. Implement adapters that associate specs with existing exporter IDs and
   DuckLake relations without changing exporter registration or configuration.
3. Implement pinned DuckLake file discovery and epoch-aligned point selection.
4. Implement `snapshot inspect`, coverage checks, stored ZIP parts, checksums,
   and signing hooks.
5. Add unit tests for deterministic manifests, spec digests, part boundaries,
   and archive safety.

### Phase 2: PostgreSQL import

1. Implement the generic `DIRECT`, `MAPPED`, and `SQL` import executors.
2. Implement the named converter registry and `SnapshotTransformProvider` SPI.
3. Add strict specifications for every supported table.
4. Implement content-addressed batches and transactional import journaling.
5. Express the PoC's timestamp, rename, and JSONB mappings declaratively and
   move the `address_utxo` reshape into a versioned DuckDB SQL resource.
6. Implement sequence reset and import disk guards.
7. Add `HANDLER` providers only for required control-state reconstruction.

### Phase 3: Validation

1. Add structural and semantic validators and import reports.
2. Add source-versus-snapshot producer checks.
3. Add application acceptance automation for resume, restart, crash recovery,
   and rollback.

### Phase 4: Preprod qualification

Use the existing local preprod analytics directory and configured empty
`preprodrestore` schema to:

1. run `snapshot inspect` and capture all current coverage/lossiness blockers;
2. perform an import using the current flattened `address_utxo` representation
   and verify that the known `owner_addr_full` limitation is reported;
3. measure heap load and index build separately;
4. compare it with the source preprod database;
5. validate the reconstructed era and 2,160-row cursor tail; and
6. resume from the completed-epoch point and exercise restart and rollback.

The existing data is suitable for developing and benchmarking the loader. It is
not, by itself, a production-quality snapshot because the consistency point,
table coverage, control state, and restart behavior have not yet been
validated. The missing `owner_addr_full` field is a separately documented,
accepted limitation for the initial format.

### Phase 5: Mainnet qualification and publication

1. Add the lossless `address_utxo` snapshot projection, including
   `owner_addr_full`.
2. Generate a lossless snapshot from the release candidate.
3. Import into a clean mainnet PostgreSQL instance on documented hardware.
4. publish archive, heap-load, index-build, final database size, and total-ready
   timings;
5. complete the application acceptance matrix; and
6. publish independently downloadable parts, manifest, checksums, signature,
   and operator documentation.

## Alternatives Considered

### Zip the entire data directory without a manifest

Rejected. It has no atomic catalog point, common chain point, schema contract,
module coverage, checksums per part, or restart metadata. It can produce a
database that looks populated but is not safe to resume.

### Productize the Python PoC as a separate tool

Rejected as the product architecture. The PoC is valuable evidence and its
batching techniques should be retained, but mapping, compatibility, migrations,
restart state, and validation belong with the Yaci Store release that defines
them. A separate script would drift.

### Derive import mappings from existing Java exporters

Rejected. Exporters describe source queries and partitioning, but not the full
PostgreSQL target contract, cutoff semantics, defaults, dependencies, control
state, or validation. Import behavior inferred from Java code would be brittle
and would force every future importer change through exporter internals. The
shared YAML specification links to an existing exporter while remaining the
authoritative restore contract.

### Put every transformation in unrestricted YAML SQL

Rejected. It would minimize Java initially but make type safety, reuse, schema
evolution, validation, and security worse. Structured mappings and named
converters cover routine cases. Trusted, read-only DuckDB SQL resources handle
relational reshaping, and the Java SPI remains available for lifecycle behavior.

### PostgreSQL `pg_dump`

Not selected as the distribution format. It preserves PostgreSQL shape better,
but is larger, PostgreSQL-specific, slower to restore in the measured PoC, and
does not avoid rebuilding indexes. It remains useful as a producer-side control
for validation.

### PostgreSQL physical base backup

Rejected for general distribution. It is fast and exact but tied to PostgreSQL
major version and physical layout, is operationally large, and is less suitable
for module-specific or cross-platform snapshots.

### Manage indexes inside snapshot import

Rejected. Yaci Store already has the manual `apply-indexes` and
`verify-indexes` workflow used after normal sync. Duplicating index lifecycle,
retry, and definition management in the importer would add unnecessary risk and
complexity. The importer only warns if admin-managed indexes were applied before
the load.

### Resume from the maximum exported block

Rejected. Daily and epoch tables progress independently, as demonstrated by the
local preprod data. The resulting cursor can be ahead of required derived state.

### Snapshot at the current chain tip

Deferred. It requires an atomic pause/barrier across chain sync, aggregate jobs,
analytics export, and DuckLake commit. A completed-epoch point is simpler,
stable, and still removes almost all initial-sync time.

## Consequences

### Positive

- New users can bootstrap in hours instead of days.
- The format remains compact and independently downloadable.
- Strict compatibility and table coverage prevent silent partial restores.
- Import is resumable, while index creation remains the familiar manual admin
  CLI step.
- Epoch-aligned restart and cursor history preserve normal Yaci Store rollback
  behavior.
- Most new tables and columns require only a migration and a YAML specification,
  not a new Java exporter/importer pair.
- One shared specification and digest connects exporter output, the manifest,
  importer behavior, and validation.
- The table inventory improves migration, rollback, module packaging, and
  analytics schema discipline beyond this feature.

### Negative

- Analytics Parquet cannot be treated as a free backup; several export and
  schema gaps must be fixed first.
- Manual index creation may remain the dominant post-import cost and must be
  benchmarked separately from import.
- The snapshot component adds DuckDB/DuckLake and PostgreSQL-specific operational
  complexity to admin tooling.
- The specification format, converter registry, and SQL resource rules become a
  compatibility surface that requires documentation and contract tests.
- Snapshot-enabled custom tables require a separate snapshot spec in addition to
  their existing custom-exporter configuration.
- Production snapshots require storage for both Parquet distribution and the
  much larger restored PostgreSQL database plus temporary index-build space.
- Exact schema matching means snapshots are initially release-specific.
- Users resume from the last fully completed epoch rather than the publication
  tip and must replay the remaining blocks.

## Final Position

Snapshot bootstrap is feasible and materially faster than genesis sync, but the
deliverable must be a validated, versioned restore protocol. The first release
should provide independent stored ZIP parts, a signed manifest, strict table
mapping, resumable bulk load, explicit control-state reconstruction, and an
epoch-aligned restart point. Indexes will continue to be applied later through
the existing admin CLI command. The current preprod export
and PoC are appropriate inputs for implementation and qualification, not yet a
snapshot that should be distributed to users as production-ready.
