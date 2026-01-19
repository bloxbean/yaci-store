package com.bloxbean.cardano.yaci.store.adminui.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling SPA (Single Page Application) routing.
 * Serves index.html for all admin UI routes so that client-side routing works correctly.
 */
@RestController
public class SpaController {

    private static final Resource INDEX_HTML = new ClassPathResource("static/index.html");

    @GetMapping(value = {
            "/admin",
            "/admin/",
            "/admin/stores",
            "/admin/stores/**",
            "/admin/config",
            "/admin/config/**",
            "/admin/sync",
            "/admin/sync/**",
            "/admin/indexes",
            "/admin/indexes/**",
            "/admin/ledger-state",
            "/admin/ledger-state/**",
            "/admin/metrics",
            "/admin/metrics/**"
    }, produces = MediaType.TEXT_HTML_VALUE)
    public Resource serveIndex() {
        return INDEX_HTML;
    }
}
