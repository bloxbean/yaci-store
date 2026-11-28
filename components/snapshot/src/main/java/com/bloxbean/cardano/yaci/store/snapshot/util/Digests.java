package com.bloxbean.cardano.yaci.store.snapshot.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 helpers. Hex output is lowercase everywhere so digests compare byte-for-byte. */
public final class Digests {

    private Digests() {
    }

    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String sha256Hex(byte[] bytes) {
        return HexFormat.of().formatHex(sha256().digest(bytes));
    }

    public static String sha256Hex(String s) {
        return sha256Hex(s.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(Path file) throws IOException {
        return sha256Hex(file, null);
    }

    /** @param read told the size of every buffer read, for progress; may be null */
    public static String sha256Hex(Path file, java.util.function.LongConsumer read) throws IOException {
        MessageDigest md = sha256();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) {
                md.update(buf, 0, r);
                if (read != null) {
                    read.accept(r);
                }
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    /** SHA-256 and CRC-32 of a file, from one read. */
    public record FileDigest(long size, String sha256, long crc32) {}

    public static FileDigest sha256AndCrc32(Path file) throws IOException {
        MessageDigest md = sha256();
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        long size = 0;
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) {
                md.update(buf, 0, r);
                crc.update(buf, 0, r);
                size += r;
            }
        }
        return new FileDigest(size, HexFormat.of().formatHex(md.digest()), crc.getValue());
    }

    public static String hex(byte[] raw) {
        return HexFormat.of().formatHex(raw);
    }
}
