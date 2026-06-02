package com.example.eps.order.messaging;

import com.example.eps.common.events.PaymentResultEvent;
import com.example.eps.order.service.OrderService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumes payment outcomes from the order-status-queue (subscribed to the payment-events
 * SNS topic) and updates the order aggregate accordingly.
 *
 * Fault tolerance: if processing throws, Spring Cloud AWS does not delete the SQS message,
 * so it is redelivered. After maxReceiveCount (configured on the queue redrive policy) the
 * message is moved to the order-status-queue-dlq for inspection.
 */
@Component
public class PaymentResultListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentResultListener.class);

    private final OrderService orderService;

    public PaymentResultListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @SqsListener("${app.aws.sqs.order-status-queue:order-status-queue}")
    public void onPaymentResult(PaymentResultEvent event) {
        log.info("Received payment result orderId={} status={} eventId={}",
                event.orderId(), event.status(), event.eventId());
        orderService.applyPaymentResult(event);
    }
}
