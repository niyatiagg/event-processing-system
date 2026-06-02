variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Project name, used as a resource name prefix"
  type        = string
  default     = "eps"
}

variable "environment" {
  description = "Deployment environment (dev/staging/prod)"
  type        = string
  default     = "dev"
}

variable "max_receive_count" {
  description = "Number of receive attempts before a message is moved to its DLQ"
  type        = number
  default     = 3
}

variable "order_service_image" {
  description = "ECR image URI for the order-service"
  type        = string
  default     = "PLACEHOLDER_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com/eps/order-service:latest"
}

variable "payment_service_image" {
  description = "ECR image URI for the payment-service"
  type        = string
  default     = "PLACEHOLDER_ACCOUNT.dkr.ecr.us-east-1.amazonaws.com/eps/payment-service:latest"
}

variable "lambda_jar_path" {
  description = "Path to the built notification-lambda shaded jar"
  type        = string
  default     = "../../notification-lambda/target/notification-lambda.jar"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "orders"
}

variable "db_password" {
  description = "PostgreSQL master password (supply via TF_VAR_db_password / secrets manager in real use)"
  type        = string
  default     = "ChangeMe123!"
  sensitive   = true
}

variable "desired_count" {
  description = "Number of Fargate tasks per service"
  type        = number
  default     = 1
}

variable "alarm_email" {
  description = "Email subscribed to the CloudWatch alarm SNS topic (optional)"
  type        = string
  default     = ""
}
