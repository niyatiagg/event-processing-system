package com.example.eps.notification;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.eps.common.events.PaymentResultEvent;
import com.example.eps.common.events.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationHandlerTest {

    private final NotificationHandler handler = new NotificationHandler();

    @Test
    void parse_handlesSnsEnvelope() throws Exception {
        String envelope = """
                {"Type":"Notification","Message":"{\\"eventId\\":\\"e1\\",\\"orderId\\":\\"o1\\",\\"customerId\\":\\"c1\\",\\"paymentId\\":\\"p1\\",\\"amount\\":12.5,\\"currency\\":\\"USD\\",\\"status\\":\\"COMPLETED\\",\\"reason\\":null,\\"processedAt\\":\\"2026-01-01T00:00:00Z\\"}"}
                """;
        PaymentResultEvent event = handler.parse(envelope);
        assertThat(event.orderId()).isEqualTo("o1");
        assertThat(event.status()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void parse_handlesRawPayload() throws Exception {
        String raw = "{\"eventId\":\"e2\",\"orderId\":\"o2\",\"customerId\":\"c2\",\"paymentId\":\"p2\",\"amount\":7,\"currency\":\"EUR\",\"status\":\"FAILED\",\"reason\":\"gateway_declined\",\"processedAt\":\"2026-01-01T00:00:00Z\"}";
        PaymentResultEvent event = handler.parse(raw);
        assertThat(event.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(event.reason()).isEqualTo("gateway_declined");
    }

    @Test
    void handleRequest_reportsFailureForUnparseableMessage() {
        SQSEvent.SQSMessage good = new SQSEvent.SQSMessage();
        good.setMessageId("good-1");
        good.setBody("{\"eventId\":\"e\",\"orderId\":\"o\",\"customerId\":\"c\",\"paymentId\":\"p\",\"amount\":1,\"currency\":\"USD\",\"status\":\"COMPLETED\",\"reason\":null,\"processedAt\":\"2026-01-01T00:00:00Z\"}");

        SQSEvent.SQSMessage bad = new SQSEvent.SQSMessage();
        bad.setMessageId("bad-1");
        bad.setBody("not-json");

        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(good, bad));

        SQSBatchResponse response = handler.handleRequest(event, null);

        assertThat(response.getBatchItemFailures()).hasSize(1);
        assertThat(response.getBatchItemFailures().get(0).getItemIdentifier()).isEqualTo("bad-1");
    }
}
