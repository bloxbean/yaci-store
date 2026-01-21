package com.bloxbean.cardano.yaci.store.adminui.controller;

import io.swagger.v3.oas.annotations.Hidden;
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
@Hidden
public class SpaController {

    private static final Resource INDEX_HTML = new ClassPathResource("static/index.html");

    @GetMapping(value = {
            "/admin-ui",
            "/admin-ui/",
            "/admin-ui/stores",
            "/admin-ui/stores/**",
            "/admin-ui/config",
            "/admin-ui/config/**",
            "/admin-ui/sync",
            "/admin-ui/sync/**",
            "/admin-ui/indexes",
            "/admin-ui/indexes/**",
            "/admin-ui/ledger-state",
            "/admin-ui/ledger-state/**",
            "/admin-ui/metrics",
            "/admin-ui/metrics/**"
    }, produces = MediaType.TEXT_HTML_VALUE)
    public Resource serveIndex() {
        return INDEX_HTML;
    }
}
