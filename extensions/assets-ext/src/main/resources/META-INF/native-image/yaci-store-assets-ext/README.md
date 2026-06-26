# GraalVM native-image reflection hints for assets-ext

This module embeds **JGit** to sync the CIP-26 cardano-token-registry from
GitHub (`TokenMetadataSyncService` → `GitService` → `CloneCommand` /
`DiffCommand` / `LogCommand`). No other yaci-store module uses JGit, so the
reflection hints live here and nowhere else.

## Why hints are needed

`org.eclipse.jgit.lib.Config#getEnum` reads enum-typed git config values
(`core.autocrlf`, `core.trustlooserefstat`, `diff.algorithm`,
`diff.renames`, …) by reflectively calling `enumClass.getMethod("values")`.
GraalVM static reachability analysis cannot see those calls — the enum
class arrives as a `Class<?>` parameter through generic plumbing — so the
auto-generated `public static values()` method is stripped from the native
image. At runtime JGit throws:

```
java.lang.IllegalArgumentException: Enumerated values of type
  org.eclipse.jgit.<pkg>$<EnumName> not available
Caused by: java.lang.NoSuchMethodException:
  org.eclipse.jgit.<pkg>$<EnumName>.values()
```

The effect on the CIP-26 sync is total: the cron fires, JGit aborts on the
first config read, the sync job logs the error and waits for the next
tick (which fails the same way). Zero entries reach `ft_offchain_metadata`.

## Enums registered

Three families, each on a distinct part of the sync's hot path:

- **`org.eclipse.jgit.lib.CoreConfig.*`** — read during repo init and
  working-tree setup (`CloneCommand` → `InitCommand` → `FileRepository` →
  `RefDirectory`). Exercised by the initial full-sync clone.
- **`org.eclipse.jgit.diff.{DiffAlgorithm.SupportedAlgorithm, DiffConfig.RenameDetectionType}`**
  — read by `DiffFormatter.setReader` during commit-history traversal
  (`GitService.getAllMappingDetails` uses `LogCommand` + diffs to resolve
  per-file `updatedAt` / `updatedBy`).
- **`org.eclipse.jgit.lib.CommitConfig.CleanupMode`** — read by
  `CommitConfig.<init>` when `Config.get(COMMIT_KEY)` materialises the
  section. Exercised by incremental sync: `PullCommand` → `RebaseCommand`
  → reads `commit.cleanup`. Only surfaces on cron ticks after the first
  (which did a clone, not a pull).

`allDeclaredMethods: true` keeps `values()` / `valueOf(String)` reachable;
`allDeclaredFields: true` keeps the enum constants introspectable.

## Extending

This is a class-of-bug — any enum-typed git config option on a code path
the sync exercises can surface the same symptom on a different enum class.
On a new `IllegalArgumentException: Enumerated values of type … not
available` from JGit, add the enum here.
