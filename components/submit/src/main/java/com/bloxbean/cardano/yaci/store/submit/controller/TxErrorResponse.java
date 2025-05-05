package com.bloxbean.cardano.yaci.store.submit.controller;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TxErrorResponse(int statusCode, String error, String message) {
}
