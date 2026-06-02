locals {
  name_prefix = "${var.project}-${var.environment}"

  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
    System      = "event-processing-system"
  }
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
