package com.example.eps.payment.messaging;

import com.example.eps.common.MessagingConstants;
import com.example.eps.common.events.EventType;
import com.example.eps.common.events.PaymentResultEvent;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final SnsTemplate snsTemplate;
    private final String paymentEventsTopicArn;

    public PaymentEventPublisher(SnsTemplate snsTemplate,
                                 @Value("${app.aws.sns.payment-events-arn:" + MessagingConstants.PAYMENT_EVENTS_TOPIC + "}")
                                 String paymentEventsTopicArn) {
        this.snsTemplate = snsTemplate;
        this.paymentEventsTopicArn = paymentEventsTopicArn;
    }

    public void publishPaymentResult(PaymentResultEvent event) {
        EventType type = event.isSuccessful() ? EventType.PAYMENT_COMPLETED : EventType.PAYMENT_FAILED;
        SnsNotification<PaymentResultEvent> notification = SnsNotification.builder(event)
                .header(MessagingConstants.ATTR_EVENT_TYPE, type.name())
                .build();
        snsTemplate.sendNotification(paymentEventsTopicArn, notification);
        log.info("Published {} event orderId={} paymentId={}", type, event.orderId(), event.paymentId());
    }
}
