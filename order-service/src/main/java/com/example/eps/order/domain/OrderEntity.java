package com.example.eps.order.domain;

import com.example.eps.common.events.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic lock so concurrent status updates from payment events don't clobber each other. */
    @Version
    private Long version;

    protected OrderEntity() {
    }

    public OrderEntity(String id, String customerId, BigDecimal amount, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = OrderStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
        this.failureReason = null;
        this.updatedAt = Instant.now();
    }

    public void markPaymentFailed(String reason) {
        this.status = OrderStatus.PAYMENT_FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
