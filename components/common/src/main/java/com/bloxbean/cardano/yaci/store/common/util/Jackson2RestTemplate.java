package com.bloxbean.cardano.yaci.store.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Framework 7 gives a {@link RestTemplate} the Jackson 3 JSON converter when Jackson 3 is on the classpath.
 * Jackson 3 ignores Jackson 2 annotations such as {@code @JsonNaming(SnakeCaseStrategy)} on Yaci Store's domain
 * classes ({@code AddressUtxo}, {@code UtxoKey}, ...), so JSON exchanged with another Yaci Store would no longer map.
 * This puts a Jackson 2 converter first, so JSON is read and written as before Spring Boot 4.
 */
public final class Jackson2RestTemplate {
    private Jackson2RestTemplate() {
    }

    @SuppressWarnings("removal")
    public static RestTemplate useJackson2(RestTemplate restTemplate, ObjectMapper objectMapper) {
        restTemplate.getMessageConverters().removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        restTemplate.getMessageConverters().addFirst(new MappingJackson2HttpMessageConverter(objectMapper));
        return restTemplate;
    }
}
