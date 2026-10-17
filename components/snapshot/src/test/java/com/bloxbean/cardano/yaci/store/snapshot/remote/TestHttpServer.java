package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves a directory over plain HTTP, as a public bucket would. With {@code breakFirstPartRead}, the
 * first GET of every {@code .zip} stalls half way, as a dropped connection does, so a client's stall
 * detection and retry are exercised.
 */
public final class TestHttpServer implements AutoCloseable {

    private final HttpServer server;

    public TestHttpServer(Path root, boolean breakFirstPartRead) throws IOException {
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
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (var out = exchange.getResponseBody()) {
                        if (breakFirstPartRead && file.toString().endsWith(".zip") && broken.add(file.toString())) {
                            out.write(bytes, 0, bytes.length / 2);
                            out.flush();
                            // Keep the connection open with nothing more to send.
                            Thread.sleep(10_000);
                            throw new IOException("stalled");
                        }
                        out.write(bytes);
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

    @Override
    public void close() {
        server.stop(0);
    }
}
