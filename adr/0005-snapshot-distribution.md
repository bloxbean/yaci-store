# ADR 0005: Snapshot Distribution, Retention and Signing

## Status

Proposed. Extends [ADR 0003](0003-snapshot-export-import.md) (export and import) and
[ADR 0004](0004-selective-snapshot-import.md) (selective import).

## Date

2026-10-17

## Context

ADR 0003's export and import are qualified on mainnet: a 140 GiB archive of 46 parts
restores in 2h19m on a 32 vCPU / 62 GB VM, keys and validation included. Four gaps remain
before snapshots can be handed to operators.

1. **There is no distribution channel.** A snapshot is a local directory. Operators copy parts by
   hand, and there is no way to find the newest snapshot for a network, or one this release can
   import.
2. **Scoped imports pay for the whole archive.** ADR 0004 scopes the *load* to the stores the
   target enables, but every part is still verified and extracted. A blocks + transactions + UTxO
   indexer extracts governance, staking and reward history it then ignores.
3. **Most of the history is unused by many operators.** On mainnet, `address_utxo` is 357M rows
   and 292 GB in PostgreSQL. Only about 11M outputs are unspent. `epoch_stake` holds every epoch
   since Shelley. Yaci Store already has pruning services for both, so a pruned node is a
   supported configuration. Today it can only be reached by restoring everything and then
   deleting most of it.
4. **Snapshots are unsigned.** Export requires `--unsigned` and import `--allow-unsigned`. That is
   acceptable between machines one operator controls. It is not acceptable for a public download.

Requirements, in addition to ADR 0003's:

- **Generic S3.** Any S3-compatible service works: Cloudflare R2 (the production target), AWS S3
  and MinIO (local tests). No vendor-specific code paths beyond configuration.
- **Unauthenticated download.** A public bucket or CDN is readable over plain HTTPS without S3
  credentials.
- **Credentials stay in configuration.** Credentials are never command arguments. They are never
  logged, and never written to a manifest or index.
- **Bounded memory.** ADR 0003's rule still holds: memory is bounded by the unit of work, so a
  small machine is slower, never unable.
- **No surprises for a scoped indexer.** The stores to fetch are the stores the target enables,
  using the existing configuration (ADR 0004). There is no second mapping to maintain.

## Decision

### D1 — One storage abstraction, three implementations

The snapshot component gains a small `SnapshotStore` interface: `head`, `open` (a streaming
read), `put` (from a file or bytes, with an optional precondition) and `list`. Three
implementations sit behind it:

| Location | Implementation | Read | Write |
|---|---|---|---|
| `s3://bucket/prefix` | AWS SDK v2 `S3Client` with an endpoint override, path-style option and URL-connection HTTP client | yes | yes |
| `https://host/prefix` | JDK `HttpClient`, anonymous | yes | no |
| `/path` or `file:///path` | local file system (mirrors, NFS, tests) | yes | yes |

The AWS SDK is used rather than a hand-written SigV4 client. It is the reference implementation
of the protocol, including multipart uploads, retries and credential providers. The Netty and
Apache HTTP clients are excluded, and the JDK `URLConnection` client is used instead.
Request-checksum calculation is set to `WHEN_REQUIRED`: S3-compatible services do not all accept
the SDK's newer default trailing checksums. Integrity comes from the snapshot's own SHA-256
values in any case (D3).

### D2 — Bucket layout

```text
<prefix>/<network>/index.json
<prefix>/<network>/<snapshot-id>/<base>.manifest.json
<prefix>/<network>/<snapshot-id>/<base>.part-00001.zip
...
<prefix>/<network>/<snapshot-id>/SHA256SUMS
<prefix>/<network>/<snapshot-id>/SHA256SUMS.sig          (D9)
```

The directory names are the full snapshot UUID, and the file names are exactly what export wrote.
A downloaded snapshot directory is therefore byte-identical to the exported one, and every local
command (`verify`, `import --manifest`) works on it unchanged.

### D3 — Publish is resumable and the index is written last

`snapshot publish` uploads in this order. A failure at any step leaves the previous index
untouched.

1. **Parts, several at once.** Each part is uploaded as a multipart upload: 64 MiB pieces by
   default, a bounded number of concurrent pieces, read from the file and not buffered. R2's
   single-PUT limit is 5 GiB, and parts default to 8 GiB.
2. **`SHA256SUMS`, then its signature (D9).**
3. **The manifest.**
4. **`index.json`.**

Each object carries its SHA-256 as user metadata (`x-amz-meta-sha256`). A rerun skips any object
already present with the same size and digest, so an interrupted publish resumes.

`index.json` is rewritten with a precondition: `If-Match` on its ETag, or `If-None-Match: *` when
it does not exist yet. Two concurrent publishers therefore cannot silently drop each other's
entry. A store that rejects conditional writes gets an unconditional write and a warning.
`snapshot index --rebuild` regenerates the index from the manifests present under the network
prefix. That repairs drift, such as a snapshot deleted by hand.

### D4 — A compatibility-aware index

`index.json` lists one entry per snapshot:

- `snapshotId`, `flavor` (`full` or `pruned`, D8), `retention`;
- `network`, `protocolMagic`, `epoch`, `slot`, `blockNumber`, `blockHash`;
- `createdAt`, `yaciStoreVersion`, `formatVersion`, `specFormatVersion`;
- the manifest key, its SHA-256, the total part size, the part count, and whether it is signed.

`--snapshot latest` resolves to the newest snapshot of the requested flavor that this release can
import. Candidates are tried newest first. Each candidate's manifest is fetched, which is cheap
next to its parts, and checked against its digest in the index. It must then pass the same
`ManifestValidator` contract check that import's preflight runs. The first one that passes is
used. A newer snapshot that this CLI cannot load is skipped with a message naming the release
that can, so it is never selected only to fail in preflight. `snapshot list` shows the same
compatibility verdict for every entry.

### D5 — Import streams from the store

`snapshot import --from <location> --snapshot <id|latest>` resolves the snapshot and downloads
the manifest. It then runs the existing preflight against the target schema and streams each
required part straight into the extractor. The extractor already validates and hashes every
entry, and the part, as it reads them. Parts are written with sizes and CRCs in their local
headers, so a `ZipInputStream` over the HTTP body needs no seeking.

- **No local copy of the parts.** Peak disk is the extracted Parquet, 140 GiB on mainnet, rather
  than 280 GiB. Download and extraction overlap fully, with `--workers` parts at once.
- **Retries.** A failed or corrupt part is retried from the start, up to a bounded number of
  times. The entries it rewrites are the same files.
- **Coverage check.** The up-front `ZipFile` central-directory check (`ArchiveVerifier.structure`)
  needs a seekable file. It is replaced, for every import, by checking after extraction that
  every declared file of a required part was extracted exactly once. The per-part check during
  extraction already rejects undeclared, duplicate, oversized and wrongly hashed entries.
- **Offline use.** `snapshot download` writes the parts and manifest to a directory instead, for
  air-gapped targets, mirrors and repeated imports. `snapshot import --manifest` then works as
  before.

The network is never a free choice: it is the target's configured protocol magic, and preflight
still refuses a mismatch.

### D6 — Scope-aware parts and extraction

- **Export groups files by module and starts a new part when the module changes.** Parts are
  still never larger than the target size unless a single file is. On mainnet this adds a handful
  of smaller parts, but a scoped importer never needs a part that only carries another store's
  tables.
- **Import fetches and extracts only the parts that carry a selected table.** Within a part,
  entries of unselected tables are read past, still hashed for the part digest, and not written.
  Older exports, whose parts mix modules, still benefit from skipping the entries.
- **Only required parts are verified.** This supersedes ADR 0003's "all archive files are
  verified even for a subset restore". The manifest, which is signed through `SHA256SUMS`, pins
  every part's digest. A part that is never read cannot influence the result.

### D7 — Import-time `epoch_stake` retention

`snapshot import --epoch-stake-epochs N` loads only the newest N epoch partitions of
`epoch_stake`. It needs no export change: `epoch_stake` is partitioned by epoch, so this only
skips files.

- **N is at least 4.** `EpochStakePruningService` refuses less, since reward calculation reads
  E−3 to E.
- **The target must run with `store.adapot.epoch-stake-pruning-enabled=true`.** Otherwise it would
  present partial history as complete. Preflight refuses the import otherwise.
- **The retention is recorded in the import journal.** `snapshot validate` applies the same view
  of the manifest, so its row counts expect the kept partitions only.

The mechanism is generic: a `Retention` applies to any table with an `EPOCH` partition strategy,
by epoch partition value. It is enabled only where a pruning service defines the semantics.

### D8 — Pruned snapshots are built at export (superseded by D11)

*Superseded by D11: the pruning is still done at export, but its output is the pruned tier of the
one full snapshot rather than a second artifact. The reasoning below still applies to that tier.*

A *pruned* snapshot (`snapshot export --flavor pruned`) holds what a node running the matching
pruning settings would hold at the consistency point. It is published next to the full snapshot.

| Table | Kept |
|---|---|
| `address_utxo` | Outputs unspent at the point, plus outputs spent within the last `utxo-safe-blocks` blocks (default 2160, `UtxoPruningService`'s default) |
| `tx_input` | Inputs spent within the last `utxo-safe-blocks` blocks |
| `epoch_stake` | The newest `epoch-stake-epochs` epochs (default 8, the `epoch-stake-pruning-safe-epochs` default; minimum 4) |
| everything else | Unchanged |

The window keeps exactly the rows a rollback within the 2160-block cursor tail needs. Rolling back
an input un-spends an output, and that output must still exist.

**Why at export, not import.** Unspent outputs come from an anti-join of `address_utxo` against
`tx_input`. The build side is every spent input: about 350M keys on mainnet, which is 20 to 35 GB
of hash table. Doing that once, on the producer, keeps the importer's memory bound. It also
shrinks the download from 140 GiB to a fraction. The export runs it as one DuckDB query under the
configured memory limit, with a spill directory, so the larger-than-memory join spills to disk
rather than failing. Its output is written back as Parquet in the same daily partitions and
columns, so the importer's existing `address_utxo` transform, batching and validation are
unchanged. File digests, row counts (distinct source keys) and bounds are computed over the
rewritten files.

**Manifest.** The manifest gains `flavor` and `retention`, for example
`{"address_utxo.spent-safe-blocks":"2160","epoch_stake.epochs":"8"}`. Pruned rows are never
reported as lossy. The flavor is a declared profile, and it is printed by `list`, the dry run and
the import report.

**Target configuration.** Import refuses a pruned snapshot unless the target enables the
matching pruning: `store.utxo.pruning-enabled` for the UTxO retention and
`store.adapot.epoch-stake-pruning-enabled` for the epoch-stake retention. A pruned snapshot
restored into an unpruned node would serve partial history as complete.

**Declared consequences of a pruned node.** These are the same as running pruning yourself:

- address and transaction history APIs return only unspent outputs and recent spends;
- `epoch_stake` holds only recent epochs;
- a rollback deeper than the window is unsupported (the cursor tail is 2160 blocks in any case).

**Deferred: reward retention.** `RewardPruningService` keeps rewards not yet withdrawn, which
needs the withdrawal history, not a window. It is left out of the first pruned flavor.

### D9 — Ed25519 signatures over `SHA256SUMS`

- **What is signed.** Export signs `SHA256SUMS` with an Ed25519 private key. `SHA256SUMS` lists
  the manifest's and every part's SHA-256. The signature is the raw 64 bytes in
  `SHA256SUMS.sig`.
- **Keys.** Keys are standard PEM: PKCS#8 private and X.509 public. They come from
  `openssl genpkey -algorithm ed25519` or from `snapshot keygen`, and the JDK signs and verifies
  them with no extra library. Anyone can verify without Yaci Store:
  `openssl pkeyutl -verify -pubin -inkey snapshot-signing.pub -rawin -in SHA256SUMS -sigfile SHA256SUMS.sig`.
- **Key identity.** A key's id is the first 16 hex digits of the SHA-256 of its encoded public
  key. It is recorded in `SHA256SUMS.sig.json` next to the signature, so an operator can see which
  key signed it.
- **Import verification.** Import verifies the signature against the configured trusted public
  keys (`store.snapshot.trusted-keys`, a comma-separated list of PEM files). It then checks that
  the manifest's SHA-256 is the one `SHA256SUMS` lists, and that every part digest in the manifest
  equals the `SHA256SUMS` line for that part. From there the existing per-part and per-entry
  digests carry the trust down to every row.
- **Unsigned snapshots.** These still import with `--allow-unsigned`, with a warning, as today.
  `--unsigned` on export is still accepted when no signing key is configured.
- **No built-in trusted key yet.** Embedding an official public key in a release is a release-
  process decision and comes later. Until then, operators configure the key the publisher
  documents.

The index is not signed. It is only discovery: the snapshot it points to is verified
independently, and a tampered index can at worst point at a different, still-verified snapshot.
Pointing at an older snapshot is visible in the epoch the import reports.

### D10 — Configuration and commands

```properties
# Where snapshots are published to and imported from. The command option --remote overrides it.
store.snapshot.remote.url=s3://yaci-snapshots/v1
# S3-compatible endpoint; omit for AWS S3. R2: https://<account-id>.r2.cloudflarestorage.com
store.snapshot.remote.endpoint=
store.snapshot.remote.region=auto
store.snapshot.remote.path-style=true
# Or the environment: STORE_SNAPSHOT_REMOTE_ACCESS_KEY_ID / STORE_SNAPSHOT_REMOTE_SECRET_ACCESS_KEY.
# Unset means the AWS default credential chain (AWS_ACCESS_KEY_ID, profiles, ...).
store.snapshot.remote.access-key-id=
store.snapshot.remote.secret-access-key=
store.snapshot.signing-key=/secure/snapshot-signing.pem
store.snapshot.trusted-keys=/etc/yaci/snapshot-signing.pub
```

| Command | Purpose |
|---|---|
| `snapshot export … [--history-split] [--publish [--live]]` | export, optionally sign and publish in one step |
| `snapshot publish --manifest m.json [--remote …] [--live]` | upload a local snapshot, staged unless `--live` (D12) |
| `snapshot promote\|unlist --snapshot <id>` | add a staged snapshot to the index, or take one out |
| `snapshot list [--remote …] [--all] [--check]` | live (with `--all`, also staged) snapshots for the configured network |
| `snapshot download --snapshot latest\|<id> --output dir` | fetch for offline import or mirroring |
| `snapshot import --snapshot latest\|<id> [--remote …] --work-dir …` | stream, verify and import |
| `snapshot index --rebuild` | regenerate `index.json` from the live snapshots in the store |
| `snapshot keygen --output dir` | create an Ed25519 signing key pair |

`--remote` accepts any D1 location, so `snapshot import --remote https://snapshots.example.org/v1`
needs no credentials at all.

### D11 — One snapshot, with a full and a pruned tier

Publishing a full and a pruned snapshot doubles the bucket storage (about 141 + 73 GiB per mainnet
snapshot) and the upload, and makes users choose between two artifacts. Pruning at import instead
keeps one artifact but costs every pruning user the full UTxO download, plus the anti-join on their
machine. Instead, the one snapshot carries both forms of the pruned tables, each in parts of its own:

| Table | `full` (full import only) | `pruned` (pruning import only) | both |
|---|---|---|---|
| `address_utxo` | Every original file, unchanged | A copy of every row of the outputs unspent at the point or spent within the window (D8) | |
| `tx_input` | Every original file, unchanged | A copy of the inputs spent within the window | |
| `epoch_stake` | Epochs older than the newest 8, unchanged | | The newest 8 epochs |

- **Why a copy, not a split.** A first version rewrote `address_utxo` into disjoint current and
  history files. On mainnet, writing the 1.47B history rows took 35 minutes under a 4 GB DuckDB
  limit. Keeping the originals and adding a copy of the 29M current rows costs the existing pruning
  pass (about 4 minutes) and about 1 GiB in the archive. The full import is byte-for-byte what it was.
- **Manifest.** Each file has an optional `tier` (`full` or `pruned`), and each tier is its own part
  group (`<module>/<tier>`), so it starts new parts. Each copied table records a `historySplit`
  (`key`, `value`, the pruned tier's `rowCount` and `bounds`). The table's own count and bounds stay the
  full table's. `epoch_stake` needs no split record: the import picks any number of epochs from the
  per-epoch files, and the older parts are only read for more than 8 epochs.
- **Import.** `Retention.forTarget` adds a split's `key=value` to the import's retention when the
  target runs the matching pruning (`store.utxo.pruning-enabled`) with a window no wider than the
  split's (`store.utxo.pruning-safe-blocks`). A wider window loads the full table and lets the target's
  pruning trim it. `--epoch-stake-epochs` defaults to the target's `store.adapot.epoch-stake-safe-epochs`
  when it prunes `epoch_stake`. `Retention.apply` turns the retention into the view: with a split, the
  files not marked `full`, with the split's count and bounds; otherwise, the files not marked `pruned`.
  That view drives file and part selection, the validator (through the journal's retention) and the
  journal's resume check, so no import ever loads both forms.
- **Extensible.** A new pruned table needs a copy in the exporter, a retention key, the target setting
  it depends on (`Retention.requiredSetting`), and, for a window, the setting that widens it
  (`windowSetting`). `transaction_cbor` with `store.transaction.cbor-retention-slots` is the obvious
  next one, once it has an analytics export.

### D12 — Staged and live snapshots

A snapshot must be importable before anyone else can find it, so that it can be tested in the real
location first. `publish` writes the snapshot's index entry to `<network>/<id>/snapshot.json`, with
`"live": false`, and leaves `index.json` alone unless `--live` is passed. `latest`, `list` and a listing
page only see `index.json`. `import --snapshot <full id>` falls back to `snapshot.json`, so a staged
snapshot can still be imported by its id. `promote` re-checks the manifest digest, sets `live` and
adds the entry to the index. `unlist` reverses it. `index --rebuild` includes only live snapshots, and
republishing a live snapshot keeps it live. Staging hides a snapshot from discovery; it does not
protect it: its files are as readable as a live one's.

### D13 — What a snapshot entry shows

The manifest records the release's git commit (`yaciStoreCommit`, from `git.properties`), since a
release version carries no commit. Each index entry has the point (epoch, slot, block, block time), the
version and commit, size, parts, modules, history splits, signing key id and the manifest digest. A
remote import and a download print these before any part is fetched, and mark a staged snapshot.
`index.json` is the public JSON for an external listing page; a page on another origin needs the
bucket's CORS policy to allow `GET`.

## Alternatives considered

- **Shell out to `rclone`, `aws s3` or `curl`.** This adds a runtime dependency. It also loses
  digest-while-downloading and streaming extraction, and it cannot do the compatibility-aware
  `latest`.
- **A hand-written SigV4 client on the JDK `HttpClient`.** This would avoid about 8 MB of
  dependencies, but re-implements signing, multipart, retries and credential discovery, which are
  the parts most likely to be subtly wrong against a non-AWS service.
- **Download every part, then extract.** This is simpler, but doubles peak disk and serialises
  download and extraction. It remains available as `snapshot download`.
- **Prune `address_utxo` at import.** This puts the 20 to 35 GB anti-join on every importer, and
  each one still downloads all of the history it then discards.
- **minisign or GPG signatures.** minisign's key files need scrypt and BLAKE2b, and GPG needs an
  external binary and a keyring. Raw Ed25519 over PEM keys is verifiable with stock `openssl` and
  the JDK.
- **Sign the index.** The index only aids discovery. Signing it would make every publish a signing
  operation without adding a guarantee the per-snapshot signature lacks.

## Consequences

**Positive**

- One command restores a node from a public bucket, with no credentials and no manual copying.
  It picks a snapshot this release can actually import.
- A scoped indexer downloads and extracts only its stores' parts.
- One snapshot also makes a small UTxO node practical: a pruning target downloads, loads and
  stores only the pruned tier.
- End-to-end integrity and authenticity from the publisher's key down to each Parquet file,
  verifiable with standard tools.

**Negative**

- The admin CLI gains the AWS SDK (S3 module, URL-connection client).
- The pruned tier adds the pruning pass (about 4 minutes on mainnet) and about 1 GiB to every export.
- A pruning target carries the pruned node's limitations, which the operator accepts through
  their pruning configuration.

## Implementation plan

Each step is its own commit on `feature/snapshot-distribution`, stacked on ADR 0003's branch.

1. Module-aligned parts at export; scope-aware extraction and part selection at import;
   import-time `--epoch-stake-epochs`.
2. Qualify a scoped import on mainnet: a blocks + transactions + UTxO profile created with
   `store.schema-only=true`, imported, validated, then synced forward.
3. `SnapshotStore` (local, S3, HTTPS), `publish`, `list`, `index --rebuild`, `download`, and
   streaming `import --snapshot`. Tests cover the file store everywhere, MinIO in an env-gated
   integration test, and a final run against R2.
4. The pruned flavor at export, its manifest fields, and the import configuration checks.
   Qualified on mainnet: export, import, validate, then sync forward.
5. Signing: `keygen`, sign at export, and verify at import and in `verify`.

## Open questions

- **The public base URL and bucket prefix for official snapshots.** This is a release-process
  decision. The code takes any location.
- **Publication cadence and retention of old snapshots in the bucket.** This could be an
  `index --prune --keep N`. It is deferred until the cadence is known.
- **Distribution of the official signing key.** It could be in the release notes, the docs, or
  embedded in the CLI. Rotation follows from the key id in `SHA256SUMS.sig.json`.

## Implementation notes

**Status as of 2026-10-17.** D1 to D13 are implemented on `feature/snapshot-distribution` (PR #1267).
The numbers below for the pruned export come from the D8 flavor, before D11 replaced it with the
pruned tier. That tier is computed by the same steps.

**Scoped import on mainnet.** The target ran blocks, epoch, staking and governance, imported from export
#6 (140.8 GiB, parts written before D6, so they mix modules).

- The import read 5 of 18 parts (36.9 GiB) and extracted 4.6 GiB.
- Import and validation took 3m17s, and all 127 checks passed.
- The application then synced from the point to the chain tip with no errors.

A later export with module-aligned parts has 22 single-module parts, so a scoped import reads only its
modules' parts.

**Cross-store runtime dependency.** In the Conway era, `epoch_param` is written by the AdaPot job. An
`epoch` store without the ledger-state profile therefore stops updating protocol parameters after the
point, exactly as a normal sync with the same flags does. This is documented as an example of the
store dependencies that scoped imports do not check.

**Stalled downloads.** The JDK `HttpClient` timeout covers only the response headers. A connection that
stalls mid-body would otherwise block forever. A stall guard closes a stream that sends nothing for two
minutes, and the per-part retry fetches the part again. The integration test serves parts that stall
halfway on the first read.

**Remote import at scale.** Export #6 was published to a directory store in 3m01s. A second publish
skipped every file in 3m35s; for a local store this re-hashes the files, while S3 compares the recorded
digest instead. The store was served over plain HTTP, and `snapshot import --snapshot latest` streamed
the scoped target's 5 parts in 1m07s, the same as reading them from local disk. Validation passed.

**Pruned export memory.** The first design, a single anti-join written into date partitions, ran out
of memory under `--memory-limit 4GB`. The implementation splits the work so that memory is bounded by
a bucket:

1. The keys of inputs spent before the window, and the keys of every output, are written to the spill
   directory in 64 hash buckets.
2. Each bucket's outputs are anti-joined with that bucket's spent keys.
3. The kept outputs' rows are written 32 date partitions at a time.

The mainnet pruned export under 4 GB kept:

| Table | Kept | Out of |
|---|---|---|
| `address_utxo` outputs | 10,899,118 (29.3M rows) | 356,770,869 |
| `epoch_stake` rows | 10,694,920 (epochs 652 to 659) | 499,975,373 |
| `tx_input` rows | 35,525 (the 2160-block window) | 345,907,276 |

Pruning took 4m22s and the whole signed export 7m55s. The archive is 72.7 GiB, against 140.8 GiB for
the full snapshot.

**MinIO for local tests.** MinIO's images and binaries can no longer be downloaded. The S3 integration
test runs against any S3-compatible endpoint given in the environment.

**One artifact on mainnet (D11).** A signed export at epoch 659 with the pruned tier took 10m24s (the
plain full export took 8m53s): the pruned tier took 4m17s under 4 GB, and the archive is 142.9 GiB in 34
parts (140.8 GiB without it). A first version that rewrote `address_utxo` into disjoint current and
history files spent 34m57s on the history write alone and was dropped.

- The pruned copies hold exactly the source rows of their keys: `address_utxo` 29,251,336 rows of
  10,899,118 outputs, and `tx_input` 35,525 rows. This was checked by count and by a hash of every
  column, and the counts equal the D8 pruned export's.
- Every table's row count equals export #6's at the same point. Nine tables are in different files,
  because the DuckLake catalog merged files after export #6; the rows of the new and the removed
  files are equal.
- A blocks + transaction + UTxO target with UTxO pruning imported it with the signature verified and
  no `--allow-unsigned`. It read 7 of 34 parts (34.3 GiB) and loaded 161,762,413 rows in 27m11s; all
  96 checks passed. The unspent set is identical to the full restore's: 10,863,593 outputs and
  37,115,529,274,342,681 lovelace.

**Cloudflare R2 (2026-10-17).** The S3 integration tests pass against the R2 bucket, including
anonymous reads through the bucket's custom domain. The signed mainnet snapshot `9ce14bda` was
published staged: 142.9 GiB, 38 files, in 23m45s, about 107 MB/s from the VM.

- Its `mainnet/index.json` did not exist, so it was not listed, while
  `<id>/snapshot.json` was public.
- A pruning target with no credentials imported it by id from `https://yaci-snapshot.bloxbean.org`. It
  streamed 7 of 34 parts (34.3 GiB) in 9m47s, about 60 MB/s from R2, and finished in 36m37s; validation
  passed. The unspent set again matched the full restore.
- `snapshot promote` then added it to `mainnet/index.json`.

