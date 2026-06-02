package com.example.eps.common.events;

/**
 * High level classification of events that flow across the system. Carried as an
 * SNS message attribute so subscribers can filter without deserializing the body.
 */
public enum EventType {
    ORDER_CREATED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED
}
