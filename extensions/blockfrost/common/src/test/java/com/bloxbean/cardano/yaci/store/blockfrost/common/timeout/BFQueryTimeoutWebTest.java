package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFDataAccessExceptionHandler;
import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFGlobalExceptionHandler;
import com.example.outside.OutsideController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Interceptor (budget scope) and response advice (#1171 safety net) wired as in the application.
 */
class BFQueryTimeoutWebTest {

    // Lives under com.bloxbean.cardano.yaci.store.blockfrost, so the interceptor and advices apply.
    @RestController
    static class BfController {
        @GetMapping("/bf/budget")
        String budget() {
            return String.valueOf(BFQueryTimeoutContext.isActive());
        }

        @GetMapping("/bf/ok")
        Map<String, Object> ok() {
            return Map.of("amount", List.of("1"));
        }

        // A reader that caught the timeout and fell back to an empty result
        @GetMapping("/bf/swallowed")
        Map<String, Object> swallowed() {
            BFQueryTimeoutContext.markTimedOut();
            return Map.of("amount", List.of());
        }

        @GetMapping("/bf/swallowed-text")
        String swallowedText() {
            BFQueryTimeoutContext.markTimedOut();
            return "";
        }

        // A reader that caught the timeout, returned Optional.empty(), and the service answered 404
        @GetMapping("/bf/swallowed-404")
        String swallowedNotFound() {
            BFQueryTimeoutContext.markTimedOut();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested component has not been found.");
        }
    }

    private static MockMvc mvc(Duration timeout) {
        return MockMvcBuilders.standaloneSetup(new BfController(), new OutsideController())
                .addInterceptors(new BFQueryTimeoutInterceptor(timeout))
                .setControllerAdvice(new BFTimeoutResponseAdvice(), new BFDataAccessExceptionHandler(),
                        new BFGlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
    }

    @Test
    void budgetIsSetForBlockfrostHandlersAndClearedAfterwards() throws Exception {
        mvc(Duration.ofSeconds(30)).perform(get("/bf/budget"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        assertThat(BFQueryTimeoutContext.isActive()).isFalse();
    }

    @Test
    void noBudgetOutsideBlockfrost() throws Exception {
        mvc(Duration.ofSeconds(30)).perform(get("/outside/budget"))
                .andExpect(content().string("false"));
    }

    @Test
    void zeroTimeoutDisablesTheBudget() throws Exception {
        mvc(Duration.ZERO).perform(get("/bf/budget"))
                .andExpect(content().string("false"));
    }

    @Test
    void normalResponseIsUntouched() throws Exception {
        mvc(Duration.ofSeconds(30)).perform(get("/bf/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount[0]").value("1"));
    }

    @Test
    void swallowedTimeout_isAnswered504InsteadOfEmpty200() throws Exception {
        mvc(Duration.ofSeconds(30)).perform(get("/bf/swallowed"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("Gateway Timeout"))
                .andExpect(jsonPath("$.message").value(BFTimeoutResponseAdvice.MESSAGE))
                .andExpect(jsonPath("$.status_code").value(504))
                .andExpect(jsonPath("$.amount").doesNotExist());
    }

    @Test
    void swallowedTimeout_onTextResponse_isAnswered504() throws Exception {
        mvc(Duration.ofSeconds(30)).perform(get("/bf/swallowed-text"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(content().string(BFTimeoutResponseAdvice.MESSAGE));
    }

    @Test
    void swallowedTimeout_turnedInto404_isAnswered504() throws Exception {
        mvc(Duration.ofSeconds(30)).perform(get("/bf/swallowed-404"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status_code").value(504));
    }
}
