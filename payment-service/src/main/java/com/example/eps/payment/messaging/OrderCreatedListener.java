package com.example.eps.payment.messaging;

import com.example.eps.common.events.OrderCreatedEvent;
import com.example.eps.payment.service.PaymentService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumes OrderCreated events from the payment-queue (subscribed to order-events SNS topic).
 *
 * Fault tolerance: an exception leaves the message on the queue for redelivery. After the
 * configured maxReceiveCount the message lands in payment-queue-dlq.
 */
@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final PaymentService paymentService;

    public OrderCreatedListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @SqsListener("${app.aws.sqs.payment-queue:payment-queue}")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received ORDER_CREATED orderId={} amount={} {}",
                event.orderId(), event.amount(), event.currency());
        paymentService.process(event);
    }
}
