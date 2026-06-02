package com.example.eps.common;

/**
 * Logical names of the SNS topics and SQS queues that wire the system together.
 * These names are referenced by the application configuration, the LocalStack
 * bootstrap scripts, and the Terraform definitions so they must stay in sync.
 */
public final class MessagingConstants {

    private MessagingConstants() {
    }

    // SNS topics
    public static final String ORDER_EVENTS_TOPIC = "order-events";
    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    // SQS queues
    public static final String PAYMENT_QUEUE = "payment-queue";
    public static final String ORDER_STATUS_QUEUE = "order-status-queue";
    public static final String NOTIFICATION_QUEUE = "notification-queue";

    // Dead-letter queues
    public static final String PAYMENT_DLQ = "payment-queue-dlq";
    public static final String ORDER_STATUS_DLQ = "order-status-queue-dlq";
    public static final String NOTIFICATION_DLQ = "notification-queue-dlq";

    // SNS message attribute carrying the EventType for subscription filtering
    public static final String ATTR_EVENT_TYPE = "eventType";
}
