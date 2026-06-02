package com.example.eps.order.service;

import com.example.eps.common.events.PaymentStatus;
import com.example.eps.common.events.PaymentResultEvent;
import com.example.eps.common.events.OrderStatus;
import com.example.eps.order.domain.OrderEntity;
import com.example.eps.order.messaging.OrderEventPublisher;
import com.example.eps.order.repository.OrderRepository;
import com.example.eps.order.web.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderEventPublisher eventPublisher;

    @InjectMocks
    OrderService orderService;

    @Test
    void createOrder_persistsAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest("cust-1", new BigDecimal("42.50"), "USD");

        OrderEntity created = orderService.createOrder(request);

        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(orderRepository).save(any(OrderEntity.class));
        verify(eventPublisher, times(1)).publishOrderCreated(any());
    }

    @Test
    void applyPaymentResult_completed_marksOrderPaid() {
        OrderEntity order = new OrderEntity("o-1", "cust-1", new BigDecimal("10.00"), "USD");
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent(
                "e-1", "o-1", "cust-1", "p-1", new BigDecimal("10.00"), "USD",
                PaymentStatus.COMPLETED, null, Instant.now()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void applyPaymentResult_failed_marksOrderFailedWithReason() {
        OrderEntity order = new OrderEntity("o-2", "cust-1", new BigDecimal("10.00"), "USD");
        when(orderRepository.findById("o-2")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent(
                "e-2", "o-2", "cust-1", "p-2", new BigDecimal("10.00"), "USD",
                PaymentStatus.FAILED, "insufficient_funds", Instant.now()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getFailureReason()).isEqualTo("insufficient_funds");
    }

    @Test
    void applyPaymentResult_isIdempotent_onAlreadyResolvedOrder() {
        OrderEntity order = new OrderEntity("o-3", "cust-1", new BigDecimal("10.00"), "USD");
        order.markPaid();
        when(orderRepository.findById("o-3")).thenReturn(Optional.of(order));

        orderService.applyPaymentResult(new PaymentResultEvent(
                "e-3", "o-3", "cust-1", "p-3", new BigDecimal("10.00"), "USD",
                PaymentStatus.FAILED, "late_duplicate", Instant.now()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_publishesEventCarryingOrderId() {
        CreateOrderRequest request = new CreateOrderRequest("cust-9", new BigDecimal("5.00"), "EUR");

        OrderEntity created = orderService.createOrder(request);

        ArgumentCaptor<com.example.eps.common.events.OrderCreatedEvent> captor =
                ArgumentCaptor.forClass(com.example.eps.common.events.OrderCreatedEvent.class);
        verify(eventPublisher).publishOrderCreated(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(created.getId());
        assertThat(captor.getValue().amount()).isEqualByComparingTo("5.00");
    }
}
