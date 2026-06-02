# ---------------------------------------------------------------------------
# notification-lambda: triggered by the notification-queue
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "notification_lambda" {
  name               = "${local.name_prefix}-notification-lambda"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume.json
  tags               = local.common_tags
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.notification_lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# Permission to consume from the notification-queue.
data "aws_iam_policy_document" "lambda_sqs" {
  statement {
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
    ]
    resources = [aws_sqs_queue.main["notification"].arn]
  }
}

resource "aws_iam_role_policy" "lambda_sqs" {
  name   = "${local.name_prefix}-lambda-sqs"
  role   = aws_iam_role.notification_lambda.id
  policy = data.aws_iam_policy_document.lambda_sqs.json
}

resource "aws_cloudwatch_log_group" "notification_lambda" {
  name              = "/aws/lambda/${local.name_prefix}-notification"
  retention_in_days = 14
  tags              = local.common_tags
}

resource "aws_lambda_function" "notification" {
  function_name    = "${local.name_prefix}-notification"
  role             = aws_iam_role.notification_lambda.arn
  runtime          = "java21"
  handler          = "com.example.eps.notification.NotificationHandler::handleRequest"
  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)
  memory_size      = 512
  timeout          = 30

  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.notification_lambda.name
  }

  tags = local.common_tags
}

resource "aws_lambda_event_source_mapping" "notification" {
  event_source_arn                   = aws_sqs_queue.main["notification"].arn
  function_name                      = aws_lambda_function.notification.arn
  batch_size                         = 5
  maximum_batching_window_in_seconds = 5
  function_response_types            = ["ReportBatchItemFailures"]
}
