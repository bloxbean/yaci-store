package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import com.bloxbean.cardano.yaci.store.blockfrost.common.exception.BFErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Turns the response of a Blockfrost request into a 504 when one of its queries timed out, even if the code
 * that ran the query caught the exception and went on to build an empty or partial result (see #1171).
 */
@Slf4j
@ControllerAdvice(basePackages = BFQueryTimeoutInterceptor.BLOCKFROST_PACKAGE)
public class BFTimeoutResponseAdvice implements ResponseBodyAdvice<Object> {

    public static final String MESSAGE = "The query took too long to complete.";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!BFQueryTimeoutContext.isTimedOut())
            return body;

        HttpStatus status = HttpStatus.GATEWAY_TIMEOUT;
        response.setStatusCode(status);
        if (body instanceof BFErrorResponse error && error.getStatusCode() == status.value())
            return body;

        log.warn("Blockfrost request {} hit the query timeout; replacing its response with {}",
                request.getURI().getPath(), status.value());

        if (!AbstractJackson2HttpMessageConverter.class.isAssignableFrom(selectedConverterType))
            return body instanceof String ? MESSAGE : null;

        return timeoutResponse();
    }

    public static BFErrorResponse timeoutResponse() {
        HttpStatus status = HttpStatus.GATEWAY_TIMEOUT;
        return new BFErrorResponse(status.getReasonPhrase(), MESSAGE, status.value());
    }
}
