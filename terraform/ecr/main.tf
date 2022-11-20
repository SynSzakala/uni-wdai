terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

resource "aws_ecr_repository" "api" {
  name                 = "api"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

resource "aws_ecr_repository" "converter" {
  name                 = "converter"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}
