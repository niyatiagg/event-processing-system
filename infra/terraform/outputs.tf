output "order_events_topic_arn" {
  description = "ARN of the order-events SNS topic"
  value       = aws_sns_topic.order_events.arn
}

output "payment_events_topic_arn" {
  description = "ARN of the payment-events SNS topic"
  value       = aws_sns_topic.payment_events.arn
}

output "queue_urls" {
  description = "URLs of the main SQS queues"
  value       = { for k, q in aws_sqs_queue.main : k => q.url }
}

output "dlq_urls" {
  description = "URLs of the dead-letter queues"
  value       = { for k, q in aws_sqs_queue.dlq : k => q.url }
}

output "payments_table_name" {
  description = "DynamoDB payments table name"
  value       = aws_dynamodb_table.payments.name
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "order_service_ecr_repo" {
  description = "ECR repository URL for order-service"
  value       = aws_ecr_repository.order_service.repository_url
}

output "payment_service_ecr_repo" {
  description = "ECR repository URL for payment-service"
  value       = aws_ecr_repository.payment_service.repository_url
}

output "notification_lambda_name" {
  description = "Notification Lambda function name"
  value       = aws_lambda_function.notification.function_name
}

output "rds_endpoint" {
  description = "PostgreSQL endpoint"
  value       = aws_db_instance.orders.address
}

output "dashboard_name" {
  description = "CloudWatch dashboard name"
  value       = aws_cloudwatch_dashboard.main.dashboard_name
}
