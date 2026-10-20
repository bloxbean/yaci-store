package com.bloxbean.cardano.yaci.store.common.config;

import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * The applications' MVC setup: {@code @EnableWebMvc} plus {@link Jackson2WebMvc}. Without it Spring Framework 7
 * writes {@code {"txHash":..., "outputIndex":...}} with Jackson 3.
 */
class Jackson2WebMvcTest {

    @RestController
    static class UtxoKeyController {
        @GetMapping("/key")
        UtxoKey key() {
            return new UtxoKey("abc", 1);
        }
    }

    @Configuration
    @EnableWebMvc
    static class WebConfig implements WebMvcConfigurer {
        private final ApplicationContext applicationContext;

        WebConfig(ApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
            Jackson2WebMvc.useJackson2(builder, applicationContext);
        }

        @Bean
        UtxoKeyController utxoKeyController() {
            return new UtxoKeyController();
        }
    }

    @Test
    void writesSnakeCaseJsonAsWithSpringFramework6() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(WebConfig.class);
            context.refresh();
            MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

            mockMvc.perform(get("/key"))
                    .andExpect(content().json("{\"tx_hash\":\"abc\",\"output_index\":1}", true));
        }
    }
}
