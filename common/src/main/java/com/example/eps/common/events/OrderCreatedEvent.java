package com.example.eps.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted by the order-service after an order is durably persisted. Consumed by the
 * payment-service to attempt payment.
 */
public record OrderCreatedEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("createdAt") Instant createdAt) {

    @JsonCreator
    public OrderCreatedEvent {
        // canonical compact constructor enables Jackson deserialization of the record
    }
}
