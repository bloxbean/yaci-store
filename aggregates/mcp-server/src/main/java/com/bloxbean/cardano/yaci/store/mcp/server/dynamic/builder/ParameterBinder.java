package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.builder;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.DynamicQueryRequest;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.FilterCondition;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.FilterOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds parameter values for JDBC parameterized queries.
 * Ensures all values are properly bound to prevent SQL injection.
 */
@Component
@Slf4j
public class ParameterBinder {

    /**
     * Binds all parameters from the request into a map for JDBC.
     */
    public Map<String, Object> bindParameters(DynamicQueryRequest request) {
        Map<String, Object> params = new HashMap<>();

        // Bind WHERE clause parameters
        bindFilterParameters(request.filters(), "param", params);

        // Bind HAVING clause parameters
        bindFilterParameters(request.having(), "having_param", params);

        log.debug("Bound {} parameters", params.size());
        return params;
    }

    private void bindFilterParameters(List<FilterCondition> filters, String paramPrefix, Map<String, Object> params) {
        for (int i = 0; i < filters.size(); i++) {
            FilterCondition filter = filters.get(i);

            if (filter.operator() == FilterOperator.IS_NULL ||
                filter.operator() == FilterOperator.IS_NOT_NULL) {
                // These operators don't require values
                continue;
            }

            if (filter.operator() == FilterOperator.BETWEEN) {
                // BETWEEN requires two parameters
                if (filter.value() instanceof List<?> list && list.size() == 2) {
                    params.put(paramPrefix + i + "_min", list.get(0));
                    params.put(paramPrefix + i + "_max", list.get(1));
                }
            } else {
                // Standard parameter binding
                params.put(paramPrefix + i, filter.value());
            }
        }
    }
}
