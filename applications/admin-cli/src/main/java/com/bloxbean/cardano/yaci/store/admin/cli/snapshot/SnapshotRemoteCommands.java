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
            description = "Upload an exported snapshot and add it to its network's index.json.")
    public void publish(
            @Option(longNames = "manifest", required = true, description = "Manifest of the exported snapshot")
            String manifest,
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "threads", defaultValue = "4", description = "Parts uploaded at once") int threads) {
        try (SnapshotStore store = support.remoteStore(remote)) {
            publish(store, Path.of(manifest).toAbsolutePath().normalize(), threads);
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw SnapshotCommands.failed("snapshot publish", e);
        }
    }

    static void publish(SnapshotStore store, Path manifest, int threads) throws Exception {
        var result = new SnapshotRepository(store).publish(manifest, threads, new ProgressPrinter(0));
        writeLn(success("Published snapshot %s to %s%s", result.entry().snapshotId(), store.describe(),
                result.directory()));
        writeLn("  Sent        : %d file(s), %s (%d already there)", result.filesSent(),
                SnapshotCliSupport.humanBytes(result.bytesSent()), result.filesSkipped());
        writeLn("  Duration    : %s", SnapshotCliSupport.humanMillis(result.millis()));
        writeLn("  Signed      : %s", result.entry().keyId() == null ? "no" : "key " + result.entry().keyId());
    }

    @Command(command = "snapshot list", description = "List the snapshots published for the configured network.")
    public void list(
            @Option(longNames = "remote", description = REMOTE_HELP) String remote,
            @Option(longNames = "network", description = "Network to list (default: the configured protocol magic's)")
            String network,
            @Option(longNames = "check", defaultValue = "false",
                    description = "Fetch each manifest and report whether this release can import it")
            boolean check) {
        String net = network != null && !network.isBlank() ? network : support.network();
        try (SnapshotStore store = support.remoteStore(remote)) {
            SnapshotRepository repo = new SnapshotRepository(store);
            SnapshotIndex index = repo.index(net);
            List<SnapshotIndex.Entry> entries = index.snapshots() == null ? List.of() : index.snapshots();
            writeLn(info("%d snapshot(s) for %s at %s", entries.size(), net, store.describe()));
            if (entries.isEmpty()) {
                return;
            }
            writeLn("  %-36s %-7s %6s %10s %10s %-30s %s", "id", "flavor", "epoch", "block", "size", "release", "signed");
            SnapshotSpecRegistry registry = check ? support.registry(null, false) : null;
            for (SnapshotIndex.Entry e : entries) {
                var d = SnapshotRepository.describe(e);
                writeLn("  %-36s %-7s %6s %10s %10s %-30s %s", d.get("id"), d.get("flavor"), d.get("epoch"),
                        d.get("block"), d.get("size"), d.get("release"), d.get("signed"));
                if (check) {
                    try {
                        repo.resolve(net, e.snapshotId(), null, compatibility(registry), null);
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

    @Command(command = "snapshot download",
            description = "Download a published snapshot into a directory, for an offline import or a mirror.")
    public void download(
            @Option(longNames = "snapshot", defaultValue = "latest", description = "Snapshot id (or a unique prefix), or latest")
            String snapshot,
            @Option(longNames = "flavor", defaultValue = "full", description = "full or pruned, for latest")
            String flavor,
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
            var resolved = repo.resolve(net, snapshot, flavor, compatibility(support.registry(null, false)), printer);
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

    @Command(command = "snapshot index", description = "Rebuild a network's index.json from the manifests in the store.")
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
}
