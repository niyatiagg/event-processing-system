package com.example.eps.payment.config;

import io.awspring.cloud.dynamodb.DynamoDbTableNameResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Forces a stable, explicit DynamoDB table name so the application, the LocalStack
 * bootstrap, and Terraform all agree (instead of relying on class-name derivation).
 */
@Configuration
public class DynamoDbConfig {

    @Bean
    DynamoDbTableNameResolver dynamoDbTableNameResolver(
            @Value("${app.aws.dynamodb.payments-table:payments}") String paymentsTable) {
        return new DynamoDbTableNameResolver() {
            @Override
            public <T> String resolve(Class<T> clazz) {
                return paymentsTable;
            }
        };
    }
}
