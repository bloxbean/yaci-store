package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Starts the query time budget for requests handled by a Blockfrost controller, and clears it afterwards.
 */
public class BFQueryTimeoutInterceptor implements HandlerInterceptor {

    static final String BLOCKFROST_PACKAGE = "com.bloxbean.cardano.yaci.store.blockfrost";

    private final Duration timeout;

    public BFQueryTimeoutInterceptor(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        BFQueryTimeoutContext.clear();
        if (isBlockfrostHandler(handler) && timeout != null && timeout.isPositive())
            BFQueryTimeoutContext.begin(timeout);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        BFQueryTimeoutContext.clear();
    }

    static boolean isBlockfrostHandler(Object handler) {
        return handler instanceof HandlerMethod handlerMethod
                && (handlerMethod.getBeanType().getPackageName() + ".").startsWith(BLOCKFROST_PACKAGE + ".");
    }
}
