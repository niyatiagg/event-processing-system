# ---------------------------------------------------------------------------
# Alarm notifications
# ---------------------------------------------------------------------------
resource "aws_sns_topic" "alarms" {
  name = "${local.name_prefix}-alarms"
  tags = local.common_tags
}

resource "aws_sns_topic_subscription" "alarm_email" {
  count     = var.alarm_email == "" ? 0 : 1
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

# ---------------------------------------------------------------------------
# Alarm: any message landing in a DLQ means messages are failing repeatedly.
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "dlq_not_empty" {
  for_each = local.queues

  alarm_name          = "${local.name_prefix}-${each.value}-dlq-not-empty"
  alarm_description   = "Messages present in ${each.value}-dlq; investigate poison messages."
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.dlq[each.key].name
  }

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]
  tags          = local.common_tags
}

# ---------------------------------------------------------------------------
# Alarm: payment-queue backlog growing (consumers not keeping up).
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "payment_queue_backlog" {
  alarm_name          = "${local.name_prefix}-payment-queue-backlog"
  alarm_description   = "payment-queue depth high; payment-service may be unhealthy."
  namespace           = "AWS/SQS"
  metric_name         = "ApproximateNumberOfMessagesVisible"
  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 3
  threshold           = 100
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  dimensions = {
    QueueName = aws_sqs_queue.main["payment"].name
  }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = local.common_tags
}

# ---------------------------------------------------------------------------
# Alarm: notification-lambda errors.
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "${local.name_prefix}-notification-lambda-errors"
  alarm_description   = "notification-lambda is throwing errors."
  namespace           = "AWS/Lambda"
  metric_name         = "Errors"
  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.notification.function_name
  }

  alarm_actions = [aws_sns_topic.alarms.arn]
  tags          = local.common_tags
}

# ---------------------------------------------------------------------------
# Dashboard
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "${local.name_prefix}-overview"

  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric", x = 0, y = 0, width = 12, height = 6,
        properties = {
          title  = "SQS depth (main queues)"
          region = var.aws_region
          view   = "timeSeries"
          metrics = [
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.main["payment"].name],
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.main["order_status"].name],
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.main["notification"].name],
          ]
        }
      },
      {
        type = "metric", x = 12, y = 0, width = 12, height = 6,
        properties = {
          title  = "DLQ depth"
          region = var.aws_region
          view   = "timeSeries"
          metrics = [
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.dlq["payment"].name],
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.dlq["order_status"].name],
            ["AWS/SQS", "ApproximateNumberOfMessagesVisible", "QueueName", aws_sqs_queue.dlq["notification"].name],
          ]
        }
      },
      {
        type = "metric", x = 0, y = 6, width = 12, height = 6,
        properties = {
          title  = "Lambda invocations & errors"
          region = var.aws_region
          view   = "timeSeries"
          metrics = [
            ["AWS/Lambda", "Invocations", "FunctionName", aws_lambda_function.notification.function_name],
            ["AWS/Lambda", "Errors", "FunctionName", aws_lambda_function.notification.function_name],
          ]
        }
      },
      {
        type = "metric", x = 12, y = 6, width = 12, height = 6,
        properties = {
          title  = "ECS CPU / Memory utilization"
          region = var.aws_region
          view   = "timeSeries"
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.order_service.name],
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.payment_service.name],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.order_service.name],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.main.name, "ServiceName", aws_ecs_service.payment_service.name],
          ]
        }
      }
    ]
  })
}
