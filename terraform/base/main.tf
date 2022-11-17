terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }

    random = {
      source  = "hashicorp/random"
      version = "~> 3.4"
    }
  }
}

data "aws_availability_zones" "available_zones" {
  state = "available"
}

# application load balancer
# ip target group
