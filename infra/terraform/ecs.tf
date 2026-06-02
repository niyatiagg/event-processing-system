# ---------------------------------------------------------------------------
# ECR repositories for the service images
# ---------------------------------------------------------------------------
resource "aws_ecr_repository" "order_service" {
  name                 = "${var.project}/order-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
  image_scanning_configuration {
    scan_on_push = true
  }
  tags = local.common_tags
}

resource "aws_ecr_repository" "payment_service" {
  name                 = "${var.project}/payment-service"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
  image_scanning_configuration {
    scan_on_push = true
  }
  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# ECS cluster
# ---------------------------------------------------------------------------
resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# IAM: task execution role (pull images, write logs) + task role (app permissions)
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "${local.name_prefix}-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
  tags               = local.common_tags
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task" {
  name               = "${local.name_prefix}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
  tags               = local.common_tags
}

# order-service: publish to order-events, consume order-status-queue
data "aws_iam_policy_document" "order_task" {
  statement {
    actions   = ["sns:Publish"]
    resources = [aws_sns_topic.order_events.arn]
  }
  statement {
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
    ]
    resources = [aws_sqs_queue.main["order_status"].arn]
  }
}

resource "aws_iam_role_policy" "order_task" {
  name   = "${local.name_prefix}-order-task"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.order_task.json
}

# payment-service: publish to payment-events, consume payment-queue, read/write DynamoDB
data "aws_iam_policy_document" "payment_task" {
  statement {
    actions   = ["sns:Publish"]
    resources = [aws_sns_topic.payment_events.arn]
  }
  statement {
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes",
      "sqs:GetQueueUrl",
    ]
    resources = [aws_sqs_queue.main["payment"].arn]
  }
  statement {
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:Query",
    ]
    resources = [
      aws_dynamodb_table.payments.arn,
      "${aws_dynamodb_table.payments.arn}/index/*",
    ]
  }
}

resource "aws_iam_role_policy" "payment_task" {
  name   = "${local.name_prefix}-payment-task"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.payment_task.json
}

# ---------------------------------------------------------------------------
# CloudWatch log groups for the services
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "order_service" {
  name              = "/ecs/${local.name_prefix}-order-service"
  retention_in_days = 14
  tags              = local.common_tags
}

resource "aws_cloudwatch_log_group" "payment_service" {
  name              = "/ecs/${local.name_prefix}-payment-service"
  retention_in_days = 14
  tags              = local.common_tags
}

# ---------------------------------------------------------------------------
# Task definitions
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "order_service" {
  family                   = "${local.name_prefix}-order-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "order-service"
      image     = var.order_service_image
      essential = true
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      environment = [
        { name = "SERVER_PORT", value = "8080" },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "DB_URL", value = "jdbc:postgresql://${aws_db_instance.orders.address}:5432/orders" },
        { name = "DB_USERNAME", value = var.db_username },
        { name = "DB_PASSWORD", value = var.db_password },
        { name = "ORDER_EVENTS_TOPIC_ARN", value = aws_sns_topic.order_events.arn },
        { name = "ORDER_STATUS_QUEUE", value = aws_sqs_queue.main["order_status"].name },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.order_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "order"
        }
      }
    }
  ])
  tags = local.common_tags
}

resource "aws_ecs_task_definition" "payment_service" {
  family                   = "${local.name_prefix}-payment-service"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "payment-service"
      image     = var.payment_service_image
      essential = true
      portMappings = [{ containerPort = 8081, protocol = "tcp" }]
      environment = [
        { name = "SERVER_PORT", value = "8081" },
        { name = "AWS_REGION", value = var.aws_region },
        { name = "PAYMENT_EVENTS_TOPIC_ARN", value = aws_sns_topic.payment_events.arn },
        { name = "PAYMENT_QUEUE", value = aws_sqs_queue.main["payment"].name },
        { name = "PAYMENTS_TABLE", value = aws_dynamodb_table.payments.name },
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.payment_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "payment"
        }
      }
    }
  ])
  tags = local.common_tags
}

# ---------------------------------------------------------------------------
# Services
# ---------------------------------------------------------------------------
resource "aws_ecs_service" "order_service" {
  name            = "${local.name_prefix}-order-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.order_service.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.services.id]
    assign_public_ip = true
  }

  tags = local.common_tags
}

resource "aws_ecs_service" "payment_service" {
  name            = "${local.name_prefix}-payment-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.payment_service.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.services.id]
    assign_public_ip = true
  }

  tags = local.common_tags
}
