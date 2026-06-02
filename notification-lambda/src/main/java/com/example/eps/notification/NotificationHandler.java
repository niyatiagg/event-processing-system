package com.example.eps.notification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.eps.common.events.PaymentResultEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS Lambda triggered by the notification-queue (subscribed to the payment-events SNS topic).
 * For each payment outcome it "sends" a customer notification (logged here; swap in SES/SMS in prod).
 *
 * Fault tolerance: returns an SQSBatchResponse listing only the failed message IDs so SQS
 * redrives just those (partial batch response). Repeated failures end up in notification-queue-dlq.
 */
public class NotificationHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context != null ? context.getLogger() : null;
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                PaymentResultEvent payment = parse(message.getBody());
                notifyCustomer(payment, logger);
            } catch (Exception ex) {
                log(logger, "Failed to process messageId=" + message.getMessageId() + " error=" + ex.getMessage());
                failures.add(SQSBatchResponse.BatchItemFailure.builder()
                        .withItemIdentifier(message.getMessageId())
                        .build());
            }
        }
        return SQSBatchResponse.builder().withBatchItemFailures(failures).build();
    }

    /**
     * SNS-to-SQS deliveries wrap the payload in an envelope under "Message". Raw deliveries
     * contain the payload directly. This handles both shapes.
     */
    PaymentResultEvent parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode payloadNode = root.has("Message") ? objectMapper.readTree(root.get("Message").asText()) : root;
        return objectMapper.treeToValue(payloadNode, PaymentResultEvent.class);
    }

    private void notifyCustomer(PaymentResultEvent payment, LambdaLogger logger) {
        String channelMessage = payment.isSuccessful()
                ? String.format("Hi %s, your payment of %s %s for order %s succeeded. Thank you!",
                payment.customerId(), payment.amount(), payment.currency(), payment.orderId())
                : String.format("Hi %s, your payment for order %s could not be processed (%s). Please try again.",
                payment.customerId(), payment.orderId(), payment.reason());
        log(logger, "NOTIFY customer=" + payment.customerId() + " | " + channelMessage);
    }

    private void log(LambdaLogger logger, String message) {
        if (logger != null) {
            logger.log(message + System.lineSeparator());
        } else {
            System.out.println(message);
        }
    }
}
