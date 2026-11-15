package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.admin.cli.common.CommandFailedException;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestValidator;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotIndex;
import com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotRepository;
import com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotStore;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.info;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.success;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.warn;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.writeLn;

/**
 * Publishing snapshots to, and finding and downloading them from, an S3-compatible bucket, a public
 * HTTPS location or a directory (ADR 0005). The location is {@code --remote} or
 * {@code store.snapshot.remote.url}; credentials only ever come from configuration.
 */
@Command(group = Groups.SNAPSHOT_CMD_GROUP)
@RequiredArgsConstructor
public class SnapshotRemoteCommands {

    static final String REMOTE_HELP = "Snapshot location: s3://bucket/prefix, https://host/prefix or a directory"
            + " (default: store.snapshot.remote.url)";

    private final SnapshotCliSupport support;

    @Command(command = "snapshot publish",
            description = "Upload an exported snapshot. It is staged (importable by id, not listed) unless --live.")
    public void publish(
            @Option(longNames = "manifest", required = true, description = "Manifest of the exported snapshot")
            String manifest,
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "threads", defaultValue = "4", description = "Parts uploaded at once") int threads,
            @Option(longNames = "live", defaultValue = "false",
                    description = "Add it to index.json at once, so list and latest see it (default: staged)")
            boolean live) {
        try (SnapshotStore store = support.remoteStore(remote)) {
            publish(store, Path.of(manifest).toAbsolutePath().normalize(), threads, live);
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot publish", e);
        }
    }

    static void publish(SnapshotStore store, Path manifest, int threads, boolean live) throws Exception {
        var result = new SnapshotRepository(store).publish(manifest, threads, live, new ProgressPrinter(0));
        writeLn(success("Published snapshot %s to %s", result.entry().snapshotId(), store.describe()));
        writeLn("  Directory   : %s", result.directory());
        writeLn("  Sent        : %d file(s), %s (%d already there)", result.filesSent(),
                SnapshotCliSupport.humanBytes(result.bytesSent()), result.filesSkipped());
        writeLn("  Duration    : %s", SnapshotCliSupport.humanMillis(result.millis()));
        writeLn("  Signed      : %s", result.entry().keyId() == null ? "no" : "key " + result.entry().keyId());
        if (result.entry().live()) {
            writeLn("  State       : live (in index.json)");
        } else {
            writeLn("  State       : staged. Test it with 'snapshot import --snapshot %s ...', then make it public"
                    + " with 'snapshot promote --snapshot %s'", result.entry().snapshotId(), result.entry().snapshotId());
        }
    }

    @Command(command = "snapshot promote", description = "Make a staged snapshot live: add it to its network's index.json.")
    public void promote(
            @Option(longNames = "snapshot", required = true, description = "Full id of the staged snapshot") String snapshot,
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "network", description = "Network (default: the configured protocol magic's)")
            String network) {
        String net = network != null && !network.isBlank() ? network : support.network();
        try (SnapshotStore store = support.remoteStore(remote)) {
            var entry = new SnapshotRepository(store).promote(net, snapshot);
            writeLn(success("Snapshot %s (epoch %d, %s) is live in %s/index.json", entry.snapshotId(), entry.epoch(),
                    entry.release(), net));
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot promote", e);
        }
    }

    @Command(command = "snapshot unlist",
            description = "Take a live snapshot out of index.json. Its files stay, and it can still be imported by id.")
    public void unlist(
            @Option(longNames = "snapshot", required = true, description = "Full id of the snapshot") String snapshot,
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "network", description = "Network (default: the configured protocol magic's)")
            String network) {
        String net = network != null && !network.isBlank() ? network : support.network();
        try (SnapshotStore store = support.remoteStore(remote)) {
            var entry = new SnapshotRepository(store).unlist(net, snapshot);
            writeLn(success("Snapshot %s is no longer listed in %s/index.json", entry.snapshotId(), net));
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot unlist", e);
        }
    }

    @Command(command = "snapshot list", description = "List the live snapshots published for the configured network.")
    public void list(
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "network", description = "Network to list (default: the configured protocol magic's)")
            String network,
            @Option(longNames = "all", defaultValue = "false",
                    description = "Also list staged snapshots, from the store itself (needs an s3:// or local location)")
            boolean all,
            @Option(longNames = "check", defaultValue = "false",
                    description = "Fetch each manifest and report whether this release can import it")
            boolean check) {
        String net = network != null && !network.isBlank() ? network : support.network();
        try (SnapshotStore store = support.remoteStore(remote)) {
            SnapshotRepository repo = new SnapshotRepository(store);
            List<SnapshotIndex.Entry> entries;
            if (all) {
                entries = repo.scan(net, null);
            } else {
                SnapshotIndex index = repo.index(net);
                entries = index.snapshots() == null ? List.of() : index.snapshots();
            }
            writeLn(info("%d %ssnapshot(s) for %s at %s", entries.size(), all ? "" : "live ", net, store.describe()));
            if (entries.isEmpty()) {
                return;
            }
            String format = "  %-36s %-6s %6s %10s %10s %-32s %s";
            writeLn(format, "id", "state", "epoch", "block", "size", "release", "signed");
            SnapshotSpecRegistry registry = check ? support.registry(null, false) : null;
            for (SnapshotIndex.Entry e : entries) {
                var d = SnapshotRepository.describe(e);
                writeLn(format, d.get("id"), d.get("state"), d.get("epoch"), d.get("block"), d.get("size"),
                        d.get("release"), d.get("signed"));
                if (check) {
                    try {
                        repo.resolve(net, e.snapshotId(), compatibility(registry), null);
                        writeLn(success("    importable by this release"));
                    } catch (Exception ex) {
                        writeLn(warn("    %s", ex.getMessage().replace("\n", "\n    ")));
                    }
                }
            }
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot list", e);
        }
    }

    /** What a resolved snapshot is, printed before it is downloaded or imported. */
    static void printSnapshot(SnapshotRepository.Resolved r, SnapshotStore store) {
        var e = r.entry();
        var m = r.manifest();
        writeLn(info("Snapshot %s from %s%s", e.snapshotId(), store.describe(), e.live() ? "" : "  [STAGED, not live]"));
        writeLn("  Network     : %s (protocol magic %d)", m.point().network(), m.point().protocolMagic());
        writeLn("  Point       : epoch %d, block %d, slot %d (%s)", m.point().epoch(), m.point().blockNumber(),
                m.point().slot(), java.time.Instant.ofEpochSecond(m.point().blockTime()));
        writeLn("  Created     : %s by Yaci Store %s", m.createdAt(), m.release());
        writeLn("  Size        : %s in %d parts", SnapshotCliSupport.humanBytes(e.sizeBytes()), e.parts());
        writeLn("  Stores      : %s", m.modules());
        if (!m.historySplits().isEmpty()) {
            writeLn("  History     : in parts of its own for %s", m.historySplits());
        }
        writeLn("  Signed      : %s", e.keyId() == null ? "no" : "key " + e.keyId());
    }

    @Command(command = "snapshot download",
            description = "Download a published snapshot into a directory, for an offline import or a mirror.")
    public void download(
            @Option(longNames = "snapshot", defaultValue = "latest", description = "Snapshot id (or a unique prefix), or latest")
            String snapshot,
            @Option(longNames = "output", required = true, description = "Directory to write the snapshot into")
            String output,
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "network", description = "Network (default: the configured protocol magic's)")
            String network,
            @Option(longNames = "threads", defaultValue = "4", description = "Parts downloaded at once") int threads) {
        String net = network != null && !network.isBlank() ? network : support.network();
        try (SnapshotStore store = support.remoteStore(remote)) {
            SnapshotRepository repo = new SnapshotRepository(store);
            ProgressPrinter printer = new ProgressPrinter(0);
            var resolved = repo.resolve(net, snapshot, compatibility(support.registry(null, false)), printer);
            printSnapshot(resolved, store);
            Path out = Path.of(output).toAbsolutePath().normalize();
            SnapshotCliSupport.ensureDirectory(out);
            Path manifest = repo.downloadMetadata(resolved, out);
            repo.downloadParts(resolved, out, p -> true, threads, printer);
            writeLn(success("Snapshot %s (epoch %d) downloaded to %s", resolved.entry().snapshotId(),
                    resolved.entry().epoch(), out));
            writeLn(info("Next: 'snapshot verify --manifest %s', then 'snapshot import --manifest %s ...'", manifest, manifest));
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot download", e);
        }
    }

    @Command(command = "snapshot index",
            description = "Rebuild a network's index.json from the live snapshots in the store.")
    public void index(
            @Option(longNames = "rebuild", defaultValue = "false", description = "Required: regenerate the index")
            boolean rebuild,
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "network", description = "Network (default: the configured protocol magic's)")
            String network) {
        if (!rebuild) {
            throw new CommandFailedException("snapshot index: pass --rebuild", null);
        }
        String net = network != null && !network.isBlank() ? network : support.network();
        try (SnapshotStore store = support.remoteStore(remote)) {
            SnapshotIndex index = new SnapshotRepository(store).rebuildIndex(net, support.protocolMagic(), new ProgressPrinter(0));
            writeLn(success("Rebuilt %s/index.json with %d snapshot(s)", net, index.snapshots().size()));
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot index", e);
        }
    }

    /** What stops this release importing a manifest, under {@code registry}'s selection. */
    static Function<SnapshotManifest, List<String>> compatibility(SnapshotSpecRegistry registry) {
        return m -> new ManifestValidator(registry).validate(m);
    }

    /**
     * What stops this release importing a manifest into {@code schema}: the specifications, and the
     * target's schema (its fingerprint and Flyway history, or each selected table's definition for a
     * scoped target). With it, {@code latest} skips a snapshot whose tables match but whose release
     * migrated the schema differently, and takes an older one that fits.
     */
    static Function<SnapshotManifest, List<String>> compatibility(SnapshotSpecRegistry registry,
                                                                  java.sql.Connection target, String schema) {
        return m -> {
            List<String> problems = new java.util.ArrayList<>(new ManifestValidator(registry).validate(m));
            if (problems.isEmpty()) {
                try {
                    problems.addAll(com.bloxbean.cardano.yaci.store.snapshot.load.SchemaCompatibility.check(
                            new com.bloxbean.cardano.yaci.store.snapshot.load.PgSchema(target, schema), m, registry));
                } catch (java.sql.SQLException e) {
                    problems.add("Unable to read the target schema: " + e.getMessage());
                }
            }
            return problems;
        };
    }
}
