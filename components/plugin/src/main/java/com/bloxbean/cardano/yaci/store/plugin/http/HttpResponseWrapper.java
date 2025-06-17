package com.bloxbean.cardano.yaci.store.plugin.http;

public class HttpResponseWrapper {
    private final int status;
    private final String body;

    public HttpResponseWrapper(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "HttpResponse(status=" + status + ", body=" + body + ")";
    }
}

