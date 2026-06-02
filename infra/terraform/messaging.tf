# ---------------------------------------------------------------------------
# SNS topics (pub/sub fan-out)
# ---------------------------------------------------------------------------
resource "aws_sns_topic" "order_events" {
  name = "${local.name_prefix}-order-events"
  tags = local.common_tags
}

resource "aws_sns_topic" "payment_events" {
  name = "${local.name_prefix}-payment-events"
  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# SQS queues + dead-letter queues (fault tolerance)
# ---------------------------------------------------------------------------
locals {
  queues = {
    payment       = "payment-queue"
    order_status  = "order-status-queue"
    notification  = "notification-queue"
  }
}

resource "aws_sqs_queue" "dlq" {
  for_each                  = local.queues
  name                      = "${local.name_prefix}-${each.value}-dlq"
  message_retention_seconds = 1209600 # 14 days
  tags                      = local.common_tags
}

resource "aws_sqs_queue" "main" {
  for_each                   = local.queues
  name                       = "${local.name_prefix}-${each.value}"
  visibility_timeout_seconds = 60
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlq[each.key].arn
    maxReceiveCount     = var.max_receive_count
  })
  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# SNS -> SQS subscriptions with message-attribute filtering
# ---------------------------------------------------------------------------

# order-events -> payment-queue
resource "aws_sns_topic_subscription" "order_to_payment" {
  topic_arn            = aws_sns_topic.order_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.main["payment"].arn
  raw_message_delivery = true
}

# payment-events -> order-status-queue
resource "aws_sns_topic_subscription" "payment_to_order_status" {
  topic_arn            = aws_sns_topic.payment_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.main["order_status"].arn
  raw_message_delivery = true
}

# payment-events -> notification-queue
resource "aws_sns_topic_subscription" "payment_to_notification" {
  topic_arn            = aws_sns_topic.payment_events.arn
  protocol             = "sqs"
  endpoint             = aws_sqs_queue.main["notification"].arn
  raw_message_delivery = true
}

# Allow each topic to deliver to its target queues.
data "aws_iam_policy_document" "queue_policies" {
  for_each = local.queues

  statement {
    sid     = "AllowSNSDelivery"
    effect  = "Allow"
    actions = ["sqs:SendMessage"]
    principals {
      type        = "Service"
      identifiers = ["sns.amazonaws.com"]
    }
    resources = [aws_sqs_queue.main[each.key].arn]
    condition {
      test     = "ArnEquals"
      variable = "aws:SourceArn"
      values = each.key == "payment" ? [aws_sns_topic.order_events.arn] : [aws_sns_topic.payment_events.arn]
    }
  }
}

resource "aws_sqs_queue_policy" "main" {
  for_each  = local.queues
  queue_url = aws_sqs_queue.main[each.key].id
  policy    = data.aws_iam_policy_document.queue_policies[each.key].json
}
