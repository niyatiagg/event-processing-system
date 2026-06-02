package com.example.eps.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted by the payment-service after a payment attempt resolves. Consumed by the
 * order-service (to update order status) and the notification-lambda (to notify the customer).
 */
public record PaymentResultEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("orderId") String orderId,
        @JsonProperty("customerId") String customerId,
        @JsonProperty("paymentId") String paymentId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("status") PaymentStatus status,
        @JsonProperty("reason") String reason,
        @JsonProperty("processedAt") Instant processedAt) {

    @JsonCreator
    public PaymentResultEvent {
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.COMPLETED;
    }
}
