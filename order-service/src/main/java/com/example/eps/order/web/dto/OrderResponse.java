package com.example.eps.order.web.dto;

import com.example.eps.common.events.OrderStatus;
import com.example.eps.order.domain.OrderEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String customerId,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(OrderEntity entity) {
        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
