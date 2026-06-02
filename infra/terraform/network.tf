# Uses the account's default VPC/subnets to keep the demo focused on the event-driven
# pieces. For production, replace with a dedicated VPC module (private subnets + NAT).
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_security_group" "services" {
  name_prefix = "${local.name_prefix}-svc-"
  description = "Allow inbound HTTP to services and all egress"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "order-service HTTP"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "payment-service actuator"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

resource "aws_security_group" "db" {
  name_prefix = "${local.name_prefix}-db-"
  description = "PostgreSQL access from services"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "PostgreSQL from services"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.services.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

resource "aws_db_subnet_group" "orders" {
  name       = "${local.name_prefix}-orders-db"
  subnet_ids = data.aws_subnets.default.ids
  tags       = local.common_tags
}

resource "aws_db_instance" "orders" {
  identifier             = "${local.name_prefix}-orders"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  db_name                = "orders"
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.orders.name
  vpc_security_group_ids = [aws_security_group.db.id]
  skip_final_snapshot    = true
  publicly_accessible    = false
  tags                   = local.common_tags
}
