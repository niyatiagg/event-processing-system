package com.example.eps.payment.service;

import com.example.eps.common.events.OrderCreatedEvent;
import com.example.eps.common.events.PaymentResultEvent;
import com.example.eps.common.events.PaymentStatus;
import com.example.eps.payment.domain.PaymentRecord;
import com.example.eps.payment.messaging.PaymentEventPublisher;
import com.example.eps.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final PaymentGateway paymentGateway;
    private final BigDecimal autoApproveLimit;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentEventPublisher eventPublisher,
                          PaymentGateway paymentGateway,
                          @Value("${app.payment.auto-approve-limit:1000}") BigDecimal autoApproveLimit) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentGateway = paymentGateway;
        this.autoApproveLimit = autoApproveLimit;
    }

    /**
     * Processes a payment for an order. Idempotent: a DynamoDB record keyed on orderId means
     * a redelivered OrderCreated event re-emits the previous result instead of double-charging.
     */
    public void process(OrderCreatedEvent event) {
        Optional<PaymentRecord> existing = paymentRepository.findByOrderId(event.orderId());
        if (existing.isPresent()) {
            log.info("Payment already processed for orderId={}, re-emitting prior result", event.orderId());
            eventPublisher.publishPaymentResult(toEvent(existing.get()));
            return;
        }

        PaymentGateway.Result result = paymentGateway.charge(
                event.customerId(), event.amount(), event.currency(), autoApproveLimit);

        PaymentRecord record = new PaymentRecord();
        record.setOrderId(event.orderId());
        record.setPaymentId(UUID.randomUUID().toString());
        record.setCustomerId(event.customerId());
        record.setAmount(event.amount().toPlainString());
        record.setCurrency(event.currency());
        record.setStatus(result.approved() ? PaymentStatus.COMPLETED.name() : PaymentStatus.FAILED.name());
        record.setReason(result.reason());
        record.setProcessedAt(Instant.now().toString());
        paymentRepository.save(record);

        log.info("Processed payment orderId={} status={} paymentId={}",
                record.getOrderId(), record.getStatus(), record.getPaymentId());

        eventPublisher.publishPaymentResult(toEvent(record));
    }

    private PaymentResultEvent toEvent(PaymentRecord record) {
        return new PaymentResultEvent(
                UUID.randomUUID().toString(),
                record.getOrderId(),
                record.getCustomerId(),
                record.getPaymentId(),
                new BigDecimal(record.getAmount()),
                record.getCurrency(),
                PaymentStatus.valueOf(record.getStatus()),
                record.getReason(),
                Instant.now());
    }
}
