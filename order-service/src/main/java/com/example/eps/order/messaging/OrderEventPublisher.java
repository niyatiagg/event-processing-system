package com.example.eps.order.messaging;

import com.example.eps.common.MessagingConstants;
import com.example.eps.common.events.EventType;
import com.example.eps.common.events.OrderCreatedEvent;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes domain events to the order-events SNS topic. SNS fans the message out to
 * every subscribed SQS queue, decoupling the order-service from its consumers.
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final SnsTemplate snsTemplate;
    private final String orderEventsTopicArn;

    public OrderEventPublisher(SnsTemplate snsTemplate,
                               @Value("${app.aws.sns.order-events-arn:" + MessagingConstants.ORDER_EVENTS_TOPIC + "}")
                               String orderEventsTopicArn) {
        this.snsTemplate = snsTemplate;
        this.orderEventsTopicArn = orderEventsTopicArn;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        SnsNotification<OrderCreatedEvent> notification = SnsNotification.builder(event)
                .header(MessagingConstants.ATTR_EVENT_TYPE, EventType.ORDER_CREATED.name())
                .build();
        snsTemplate.sendNotification(orderEventsTopicArn, notification);
        log.info("Published ORDER_CREATED event orderId={} eventId={}", event.orderId(), event.eventId());
    }

    /** Exposed for clarity in logs/config. */
    public Map<String, String> destination() {
        return Map.of("topic", orderEventsTopicArn);
    }
}
