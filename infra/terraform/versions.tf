terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # For real use, configure a remote backend (S3 + DynamoDB lock). Left local for the demo.
  # backend "s3" {
  #   bucket = "my-tf-state"
  #   key    = "event-processing-system/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.aws_region
}
