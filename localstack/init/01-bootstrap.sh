#!/usr/bin/env bash
# Provision the full event-driven topology in LocalStack. This mirrors infra/terraform
# so the local stack behaves like the deployed one.
set -euo pipefail

REGION="${AWS_DEFAULT_REGION:-us-east-1}"
ACCOUNT_ID="000000000000"
MAX_RECEIVE_COUNT=3

echo "[bootstrap] creating SNS topics..."
awslocal sns create-topic --name order-events   >/dev/null
awslocal sns create-topic --name payment-events  >/dev/null
ORDER_TOPIC_ARN="arn:aws:sns:${REGION}:${ACCOUNT_ID}:order-events"
PAYMENT_TOPIC_ARN="arn:aws:sns:${REGION}:${ACCOUNT_ID}:payment-events"

# Helper: create a main queue + its DLQ with a redrive policy.
create_queue_with_dlq() {
  local name="$1"
  local dlq="${name}-dlq"
  awslocal sqs create-queue --queue-name "$dlq" >/dev/null
  local dlq_arn="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${dlq}"
  awslocal sqs create-queue --queue-name "$name" \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${dlq_arn}\\\",\\\"maxReceiveCount\\\":\\\"${MAX_RECEIVE_COUNT}\\\"}\"}" >/dev/null
  echo "[bootstrap] queue ${name} (+${dlq}) ready"
}

echo "[bootstrap] creating SQS queues + DLQs..."
create_queue_with_dlq payment-queue
create_queue_with_dlq order-status-queue
create_queue_with_dlq notification-queue

PAYMENT_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:payment-queue"
ORDER_STATUS_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:order-status-queue"
NOTIFICATION_QUEUE_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:notification-queue"

echo "[bootstrap] subscribing queues to topics (raw delivery)..."
# order-events -> payment-queue
awslocal sns subscribe --topic-arn "$ORDER_TOPIC_ARN" --protocol sqs \
  --notification-endpoint "$PAYMENT_QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true"}' >/dev/null

# payment-events -> order-status-queue + notification-queue
awslocal sns subscribe --topic-arn "$PAYMENT_TOPIC_ARN" --protocol sqs \
  --notification-endpoint "$ORDER_STATUS_QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true"}' >/dev/null
awslocal sns subscribe --topic-arn "$PAYMENT_TOPIC_ARN" --protocol sqs \
  --notification-endpoint "$NOTIFICATION_QUEUE_ARN" \
  --attributes '{"RawMessageDelivery":"true"}' >/dev/null

echo "[bootstrap] creating DynamoDB table 'payments'..."
awslocal dynamodb create-table \
  --table-name payments \
  --attribute-definitions AttributeName=orderId,AttributeType=S AttributeName=customerId,AttributeType=S \
  --key-schema AttributeName=orderId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --global-secondary-indexes '[{"IndexName":"customer-index","KeySchema":[{"AttributeName":"customerId","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"}}]' >/dev/null

# Deploy the notification Lambda if the shaded jar has been built and mounted.
LAMBDA_JAR="/opt/code/lambda/notification-lambda.jar"
if [[ -f "$LAMBDA_JAR" ]]; then
  echo "[bootstrap] deploying notification-lambda..."
  awslocal lambda create-function \
    --function-name notification-lambda \
    --runtime java21 \
    --handler com.example.eps.notification.NotificationHandler::handleRequest \
    --memory-size 512 --timeout 30 \
    --role "arn:aws:iam::${ACCOUNT_ID}:role/lambda-role" \
    --zip-file "fileb://${LAMBDA_JAR}" >/dev/null
  awslocal lambda create-event-source-mapping \
    --function-name notification-lambda \
    --event-source-arn "$NOTIFICATION_QUEUE_ARN" \
    --batch-size 5 \
    --function-response-types ReportBatchItemFailures >/dev/null
  echo "[bootstrap] notification-lambda wired to notification-queue"
else
  echo "[bootstrap] NOTE: ${LAMBDA_JAR} not found; skipping Lambda (run 'mvn package' first)."
fi

echo "[bootstrap] done. Topology is ready."
