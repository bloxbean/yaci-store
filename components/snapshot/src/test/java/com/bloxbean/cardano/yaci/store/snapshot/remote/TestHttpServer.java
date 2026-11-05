package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serves a directory over plain HTTP, as a public bucket would, answering {@code Range: bytes=N-}
 * requests unless told to ignore them. With {@code breakFirstPartRead}, the first GET of every
 * {@code .zip} stalls half way, as a dropped connection does, so a client's stall detection and
 * resume are exercised.
 */
public final class TestHttpServer implements AutoCloseable {

    private static final Pattern RANGE = Pattern.compile("bytes=(\\d+)-");

    private final HttpServer server;
    private final AtomicInteger rangeRequests = new AtomicInteger();

    public TestHttpServer(Path root, boolean breakFirstPartRead) throws IOException {
        this(root, breakFirstPartRead, true);
    }

    public TestHttpServer(Path root, boolean breakFirstPartRead, boolean honourRange) throws IOException {
        Set<String> broken = ConcurrentHashMap.newKeySet();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            Path file = root.resolve(exchange.getRequestURI().getPath().substring(1)).normalize();
            try {
                if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                    exchange.sendResponseHeaders(404, -1);
                } else if (exchange.getRequestMethod().equals("HEAD")) {
                    exchange.getResponseHeaders().add("Content-Length", Long.toString(Files.size(file)));
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    byte[] bytes = Files.readAllBytes(file);
                    int from = 0;
                    String range = exchange.getRequestHeaders().getFirst("Range");
                    Matcher m = range == null ? null : RANGE.matcher(range);
                    if (m != null && m.matches()) {
                        rangeRequests.incrementAndGet();
                        if (honourRange) {
                            from = Integer.parseInt(m.group(1));
                            exchange.getResponseHeaders().add("Content-Range",
                                    "bytes " + from + "-" + (bytes.length - 1) + "/" + bytes.length);
                        }
                    }
                    exchange.sendResponseHeaders(from > 0 ? 206 : 200, bytes.length - from);
                    try (var out = exchange.getResponseBody()) {
                        if (breakFirstPartRead && file.toString().endsWith(".zip") && broken.add(file.toString())) {
                            out.write(bytes, 0, bytes.length / 2);
                            out.flush();
                            // Keep the connection open with nothing more to send.
                            Thread.sleep(10_000);
                            throw new IOException("stalled");
                        }
                        out.write(bytes, from, bytes.length - from);
                    }
                }
            } catch (IOException | InterruptedException e) {
                // the connection is dropped, as a failing network would
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    /** GETs that asked for a range, whether or not it was honoured. */
    public int rangeRequests() {
        return rangeRequests.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
