package com.example.eps.order.service;

import com.example.eps.common.events.OrderCreatedEvent;
import com.example.eps.common.events.OrderStatus;
import com.example.eps.common.events.PaymentResultEvent;
import com.example.eps.order.domain.OrderEntity;
import com.example.eps.order.messaging.OrderEventPublisher;
import com.example.eps.order.repository.OrderRepository;
import com.example.eps.order.web.dto.CreateOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderEntity createOrder(CreateOrderRequest request) {
        OrderEntity order = new OrderEntity(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.amount(),
                request.currency());
        orderRepository.save(order);
        log.info("Created order orderId={} customerId={} amount={} {}",
                order.getId(), order.getCustomerId(), order.getAmount(), order.getCurrency());

        // Published after the entity is persisted so consumers never see a phantom order.
        eventPublisher.publishOrderCreated(new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getId(),
                order.getCustomerId(),
                order.getAmount(),
                order.getCurrency(),
                Instant.now()));
        return order;
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrder(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> listOrders(String customerId) {
        if (customerId != null && !customerId.isBlank()) {
            return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        }
        return orderRepository.findAll();
    }

    /**
     * Idempotently applies a payment outcome. Idempotency matters because SQS guarantees
     * at-least-once delivery, so the same event may arrive more than once.
     */
    @Transactional
    public void applyPaymentResult(PaymentResultEvent event) {
        OrderEntity order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

        if (order.getStatus() != OrderStatus.CREATED) {
            log.info("Ignoring duplicate/late payment result for orderId={} currentStatus={}",
                    order.getId(), order.getStatus());
            return;
        }

        if (event.isSuccessful()) {
            order.markPaid();
            log.info("Order marked PAID orderId={}", order.getId());
        } else {
            order.markPaymentFailed(event.reason());
            log.info("Order marked PAYMENT_FAILED orderId={} reason={}", order.getId(), event.reason());
        }
        orderRepository.save(order);
    }
}
