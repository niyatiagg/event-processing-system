package com.example.eps.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated payment gateway. Approves charges at or below the auto-approve limit and
 * randomly fails a small fraction of charges to exercise the failure / DLQ paths.
 */
@Component
public class PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(PaymentGateway.class);

    public record Result(boolean approved, String reason) {
        static Result approve() {
            return new Result(true, null);
        }

        static Result decline(String reason) {
            return new Result(false, reason);
        }
    }

    public Result charge(String customerId, BigDecimal amount, String currency, BigDecimal autoApproveLimit) {
        log.debug("Charging customer={} amount={} {}", customerId, amount, currency);

        if (amount.compareTo(autoApproveLimit) > 0) {
            return Result.decline("amount_exceeds_limit");
        }
        // 10% simulated transient decline to demonstrate retries and DLQ behaviour.
        if (ThreadLocalRandom.current().nextInt(100) < 10) {
            return Result.decline("gateway_declined");
        }
        return Result.approve();
    }
}
