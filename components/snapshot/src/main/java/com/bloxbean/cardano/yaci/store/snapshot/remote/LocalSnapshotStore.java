package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

/**
 * A directory as a snapshot store: a mirror, a network file system, or a test. The entity tag is the
 * content's SHA-256, so conditional writes work as on S3. Writes go through a temporary file and an
 * atomic rename, so a reader never sees half an object.
 */
public class LocalSnapshotStore implements SnapshotStore {

    private final Path root;

    public LocalSnapshotStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String describe() {
        return root.toString();
    }

    @Override
    public boolean writable() {
        return true;
    }

    private Path resolve(String key) throws IOException {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IOException("Key resolves outside the store: " + key);
        }
        return p;
    }

    @Override
    public Optional<ObjectInfo> head(String key) throws IOException {
        Path p = resolve(key);
        if (!Files.isRegularFile(p)) {
            return Optional.empty();
        }
        // The SHA-256 is not stored beside the file; a caller that needs it reads the object.
        return Optional.of(new ObjectInfo(Files.size(p), null, null));
    }

    @Override
    public InputStream open(String key) throws IOException {
        Path p = resolve(key);
        try {
            return Files.newInputStream(p);
        } catch (NoSuchFileException e) {
            throw new NoSuchFileException(key + " is not in " + root);
        }
    }

    @Override
    public List<String> list(String prefix) throws IOException {
        Path dir = resolve(prefix);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .map(p -> root.relativize(p).toString().replace('\\', '/'))
                    .filter(k -> !k.endsWith(".tmp"))
                    .sorted().toList();
        }
    }

    @Override
    public void put(String key, Path file, String sha256, LongConsumer sent) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (InputStream in = Files.newInputStream(file); OutputStream out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[1 << 20];
            int r;
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
                if (sent != null) {
                    sent.accept(r);
                }
            }
        }
        move(tmp, target);
    }

    @Override
    public synchronized boolean putIfUnchanged(String key, byte[] bytes, String ifMatch) throws IOException {
        Path target = resolve(key);
        String current = Files.isRegularFile(target) ? Digests.sha256Hex(target) : null;
        if (ifMatch == null ? current != null : !ifMatch.equals(current)) {
            return false;
        }
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        move(tmp, target);
        return true;
    }

    @Override
    public Optional<Tagged> read(String key) throws IOException {
        Path p = resolve(key);
        if (!Files.isRegularFile(p)) {
            return Optional.empty();
        }
        byte[] bytes = Files.readAllBytes(p);
        return Optional.of(new Tagged(bytes, Digests.sha256Hex(bytes)));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private static void move(Path tmp, Path target) throws IOException {
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
