terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4"
    }

    random = {
      source  = "hashicorp/random"
      version = "~> 3"
    }
  }
}

data "aws_availability_zones" "available_zones" {
  state = "available"
}
