package com.example.eps.payment.service;

import com.example.eps.common.events.OrderCreatedEvent;
import com.example.eps.common.events.PaymentResultEvent;
import com.example.eps.common.events.PaymentStatus;
import com.example.eps.payment.domain.PaymentRecord;
import com.example.eps.payment.messaging.PaymentEventPublisher;
import com.example.eps.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentEventPublisher eventPublisher;

    private PaymentService newService() {
        return new PaymentService(paymentRepository, eventPublisher, new PaymentGateway(), new BigDecimal("1000"));
    }

    private OrderCreatedEvent order(String id, String amount) {
        return new OrderCreatedEvent("e-" + id, id, "cust-1", new BigDecimal(amount), "USD", Instant.now());
    }

    @Test
    void process_approvesSmallAmount_savesAndPublishesCompleted() {
        when(paymentRepository.findByOrderId("o-1")).thenReturn(Optional.empty());
        PaymentService service = newService();

        service.process(order("o-1", "50.00"));

        verify(paymentRepository).save(any(PaymentRecord.class));
        ArgumentCaptor<PaymentResultEvent> captor = ArgumentCaptor.forClass(PaymentResultEvent.class);
        verify(eventPublisher).publishPaymentResult(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void process_declinesAmountOverLimit() {
        when(paymentRepository.findByOrderId("o-2")).thenReturn(Optional.empty());
        PaymentService service = newService();

        service.process(order("o-2", "5000.00"));

        ArgumentCaptor<PaymentResultEvent> captor = ArgumentCaptor.forClass(PaymentResultEvent.class);
        verify(eventPublisher).publishPaymentResult(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(captor.getValue().reason()).isEqualTo("amount_exceeds_limit");
    }

    @Test
    void process_isIdempotent_whenPaymentAlreadyExists() {
        PaymentRecord existing = new PaymentRecord();
        existing.setOrderId("o-3");
        existing.setPaymentId("p-existing");
        existing.setCustomerId("cust-1");
        existing.setAmount("10.00");
        existing.setCurrency("USD");
        existing.setStatus(PaymentStatus.COMPLETED.name());
        existing.setProcessedAt(Instant.now().toString());
        when(paymentRepository.findByOrderId("o-3")).thenReturn(Optional.of(existing));
        PaymentService service = newService();

        service.process(order("o-3", "10.00"));

        verify(paymentRepository, never()).save(any());
        verify(eventPublisher).publishPaymentResult(any());
    }
}
