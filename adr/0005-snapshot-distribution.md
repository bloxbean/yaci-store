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

### D8 — Pruned snapshots are built at export

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
| `snapshot export … [--flavor full\|pruned] [--publish]` | export, optionally sign and publish in one step |
| `snapshot publish --manifest m.json [--remote …]` | upload a local snapshot and add it to the index |
| `snapshot list [--remote …] [--all]` | snapshots for the configured network, with compatibility |
| `snapshot download --snapshot latest\|<id> [--flavor …] --output dir` | fetch for offline import or mirroring |
| `snapshot import --snapshot latest\|<id> [--flavor …] [--remote …] --work-dir …` | stream, verify and import |
| `snapshot index --rebuild` | regenerate `index.json` from the manifests in the store |
| `snapshot keygen --output dir` | create an Ed25519 signing key pair |

`--remote` accepts any D1 location, so `snapshot import --remote https://snapshots.example.org/v1`
needs no credentials at all.

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
- A pruned snapshot makes a small UTxO node practical: a fraction of the download, the load and
  the disk.
- End-to-end integrity and authenticity from the publisher's key down to each Parquet file,
  verifiable with standard tools.

**Negative**

- The admin CLI gains the AWS SDK (S3 module, URL-connection client).
- Two flavors double the producer's export and publish work, and the bucket storage.
- Pruned snapshots carry the pruned node's limitations, which the operator must accept
  explicitly through their pruning configuration.

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
