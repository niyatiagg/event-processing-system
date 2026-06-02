resource "aws_dynamodb_table" "payments" {
  name         = "${local.name_prefix}-payments"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "orderId"

  attribute {
    name = "orderId"
    type = "S"
  }

  attribute {
    name = "customerId"
    type = "S"
  }

  global_secondary_index {
    name            = "customer-index"
    hash_key        = "customerId"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = local.common_tags
}
