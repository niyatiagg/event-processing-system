package com.example.eps.common.events;

/**
 * Lifecycle of an order as it moves through the event-driven pipeline.
 */
public enum OrderStatus {
    /** Persisted and OrderCreated event published; awaiting payment. */
    CREATED,
    /** Payment succeeded. */
    PAID,
    /** Payment failed; terminal unless retried. */
    PAYMENT_FAILED
}
