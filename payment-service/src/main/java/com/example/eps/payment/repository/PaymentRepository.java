package com.example.eps.payment.repository;

import com.example.eps.payment.domain.PaymentRecord;
import io.awspring.cloud.dynamodb.DynamoDbTemplate;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

@Repository
public class PaymentRepository {

    private final DynamoDbTemplate dynamoDbTemplate;

    public PaymentRepository(DynamoDbTemplate dynamoDbTemplate) {
        this.dynamoDbTemplate = dynamoDbTemplate;
    }

    public Optional<PaymentRecord> findByOrderId(String orderId) {
        Key key = Key.builder().partitionValue(orderId).build();
        return Optional.ofNullable(dynamoDbTemplate.load(key, PaymentRecord.class));
    }

    public PaymentRecord save(PaymentRecord record) {
        return dynamoDbTemplate.save(record);
    }
}
