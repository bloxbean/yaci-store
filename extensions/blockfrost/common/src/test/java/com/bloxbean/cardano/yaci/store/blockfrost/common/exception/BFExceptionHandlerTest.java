package com.bloxbean.cardano.yaci.store.blockfrost.common.exception;

import com.example.outside.OutsideController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BFExceptionHandlerTest {

    // Lives under com.bloxbean.cardano.yaci.store.blockfrost, so the advices apply.
    @RestController
    static class BfController {
        @GetMapping("/bf/db")
        String db() {
            throw new QueryTimeoutException("canceling statement due to statement timeout");
        }

        @GetMapping("/bf/status")
        String status() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "not found");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new BfController(), new OutsideController())
                .setControllerAdvice(new BFDataAccessExceptionHandler(), new BFGlobalExceptionHandler())
                .build();
    }

    @Test
    void dataAccessException_returnsBlockfrost500() throws Exception {
        mvc.perform(get("/bf/db"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected response was received from the backend."))
                .andExpect(jsonPath("$.status_code").value(500));
    }

    @Test
    void responseStatusException_returnsBlockfrostErrorBody() throws Exception {
        mvc.perform(get("/bf/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("not found"))
                .andExpect(jsonPath("$.status_code").value(404));
    }

    @Test
    void controllerOutsideBlockfrostPackage_isNotHandled() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> mvc.perform(get("/outside/db"))).getCause())
                .isInstanceOf(QueryTimeoutException.class);

        // Default ResponseStatusException handling keeps the standard (non-Blockfrost) body.
        try {
            mvc.perform(get("/outside/status"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status_code").doesNotExist());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
