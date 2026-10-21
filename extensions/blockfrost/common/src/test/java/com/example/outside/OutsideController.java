package com.example.outside;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Controller outside the Blockfrost packages, used to verify the advice scoping. */
@RestController
public class OutsideController {

    @GetMapping("/outside/db")
    String db() {
        throw new QueryTimeoutException("canceling statement due to statement timeout");
    }

    @GetMapping("/outside/status")
    String status() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
    }
}
